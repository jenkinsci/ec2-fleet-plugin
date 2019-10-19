package com.amazon.jenkins.ec2fleet.fleet;

import com.amazon.jenkins.ec2fleet.FleetStateStats;
import com.amazon.jenkins.ec2fleet.Registry;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.ActiveInstance;
import com.amazonaws.services.ec2.model.BatchState;
import com.amazonaws.services.ec2.model.DescribeSpotFleetInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsResult;
import com.amazonaws.services.ec2.model.FleetType;
import com.amazonaws.services.ec2.model.ModifySpotFleetRequestRequest;
import com.amazonaws.services.ec2.model.SpotFleetLaunchSpecification;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfig;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfigData;
import hudson.util.ListBoxModel;
import org.springframework.util.ObjectUtils;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ThreadSafe
public class EC2SpotFleet implements EC2Fleet {

    @Override
    public void describe(
            final String awsCredentialsId, final String regionName, final String endpoint,
            final ListBoxModel model, final String selectedId, final boolean showAll) {
        final AmazonEC2 client = Registry.getEc2Api().connect(awsCredentialsId, regionName, endpoint);
        String token = null;
        do {
            final DescribeSpotFleetRequestsRequest req = new DescribeSpotFleetRequestsRequest();
            req.withNextToken(token);
            final DescribeSpotFleetRequestsResult result = client.describeSpotFleetRequests(req);
            for (final SpotFleetRequestConfig config : result.getSpotFleetRequestConfigs()) {
                final String curFleetId = config.getSpotFleetRequestId();
                final boolean selected = ObjectUtils.nullSafeEquals(selectedId, curFleetId);
                if (selected || showAll || isActiveAndMaintain(config)) {
                    final String displayStr = "EC2 Spot Fleet - " + curFleetId +
                            " (" + config.getSpotFleetRequestState() + ")" +
                            " (" + config.getSpotFleetRequestConfig().getType() + ")";
                    model.add(new ListBoxModel.Option(displayStr, curFleetId, selected));
                }
            }
            token = result.getNextToken();
        } while (token != null);
    }

    /**
     * @param config - config
     * @return return <code>true</code> not only for {@link BatchState#Active} but for any other
     * in which fleet in theory could accept load.
     */
    private boolean isActiveAndMaintain(final SpotFleetRequestConfig config) {
        return FleetType.Maintain.toString().equals(config.getSpotFleetRequestConfig().getType()) && isActive(config);
    }

    private boolean isActive(final SpotFleetRequestConfig config) {
        return BatchState.Active.toString().equals(config.getSpotFleetRequestState())
                || BatchState.Modifying.toString().equals(config.getSpotFleetRequestState())
                || BatchState.Submitted.toString().equals(config.getSpotFleetRequestState());
    }

    @Override
    public void modify(
            final String awsCredentialsId, final String regionName, final String endpoint,
            String id, int targetCapacity, int min, int max) {
        final ModifySpotFleetRequestRequest request = new ModifySpotFleetRequestRequest();
        request.setSpotFleetRequestId(id);
        request.setTargetCapacity(targetCapacity);
        request.setExcessCapacityTerminationPolicy("NoTermination");

        final AmazonEC2 ec2 = Registry.getEc2Api().connect(awsCredentialsId, regionName, endpoint);
        ec2.modifySpotFleetRequest(request);
    }

    public FleetStateStats getState(
            final String awsCredentialsId, final String regionName, final String endpoint,
            final String id) {
        final AmazonEC2 ec2 = Registry.getEc2Api().connect(awsCredentialsId, regionName, endpoint);

        String token = null;
        final Set<String> instances = new HashSet<>();
        do {
            final DescribeSpotFleetInstancesRequest request = new DescribeSpotFleetInstancesRequest();
            request.setSpotFleetRequestId(id);
            request.setNextToken(token);
            final DescribeSpotFleetInstancesResult res = ec2.describeSpotFleetInstances(request);
            for (final ActiveInstance instance : res.getActiveInstances()) {
                instances.add(instance.getInstanceId());
            }

            token = res.getNextToken();
        } while (token != null);

        final DescribeSpotFleetRequestsRequest request = new DescribeSpotFleetRequestsRequest();
        request.setSpotFleetRequestIds(Collections.singleton(id));
        final DescribeSpotFleetRequestsResult fleet = ec2.describeSpotFleetRequests(request);
        if (fleet.getSpotFleetRequestConfigs().isEmpty())
            throw new IllegalStateException("Fleet " + id + " can't be described");

        final SpotFleetRequestConfig fleetConfig = fleet.getSpotFleetRequestConfigs().get(0);
        final SpotFleetRequestConfigData fleetRequestConfig = fleetConfig.getSpotFleetRequestConfig();

        // Index configured instance types by weight:
        final Map<String, Double> instanceTypeWeights = new HashMap<>();
        for (SpotFleetLaunchSpecification launchSpecification : fleetRequestConfig.getLaunchSpecifications()) {
            final String instanceType = launchSpecification.getInstanceType();
            if (instanceType == null) continue;

            final Double instanceWeight = launchSpecification.getWeightedCapacity();
            final Double existingWeight = instanceTypeWeights.get(instanceType);
            if (instanceWeight == null || (existingWeight != null && existingWeight > instanceWeight)) {
                continue;
            }
            instanceTypeWeights.put(instanceType, instanceWeight);
        }

        return new FleetStateStats(id,
                fleetRequestConfig.getTargetCapacity(),
                new FleetStateStats.State(isActive(fleetConfig), fleetConfig.getSpotFleetRequestState()),
                instances,
                instanceTypeWeights);
    }

}
