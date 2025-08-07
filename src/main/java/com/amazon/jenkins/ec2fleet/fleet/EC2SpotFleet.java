package com.amazon.jenkins.ec2fleet.fleet;

import com.amazon.jenkins.ec2fleet.FleetStateStats;
import com.amazon.jenkins.ec2fleet.Registry;
import hudson.util.ListBoxModel;
import org.springframework.util.ObjectUtils;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/spot-fleet-requests.html#spot-fleet-states
 */
@ThreadSafe
public class EC2SpotFleet implements EC2Fleet {

    @Override
    public void describe(
            final String awsCredentialsId, final String regionName, final String endpoint,
            final ListBoxModel model, final String selectedId, final boolean showAll) {
        final Ec2Client client = Registry.getEc2Api().connect(awsCredentialsId, regionName, endpoint);
        for (DescribeSpotFleetRequestsResponse result : client.describeSpotFleetRequestsPaginator(DescribeSpotFleetRequestsRequest.builder().build())) {
            for (final SpotFleetRequestConfig config : result.spotFleetRequestConfigs()) {
                final String curFleetId = config.spotFleetRequestId();
                final boolean selected = ObjectUtils.nullSafeEquals(selectedId, curFleetId);
                if (selected || showAll || isActiveAndMaintain(config)) {
                    final String displayStr = "EC2 Spot Fleet - " + curFleetId +
                            " (" + config.spotFleetRequestState() + ")" +
                            " (" + config.spotFleetRequestConfig().type() + ")";
                    model.add(new ListBoxModel.Option(displayStr, curFleetId, selected));
                }
            }
        }
    }

    /**
     * @param config - config
     * @return return <code>true</code> not only for {@link BatchState#ACTIVE} but for any other
     * in which fleet in theory could accept load.
     */
    private static boolean isActiveAndMaintain(final SpotFleetRequestConfig config) {
        return FleetType.MAINTAIN.toString().equals(String.valueOf(config.spotFleetRequestConfig().type())) && isActive(config);
    }

    private static boolean isActive(final SpotFleetRequestConfig config) {
        return BatchState.ACTIVE.toString().equals(String.valueOf(config.spotFleetRequestState()))
                || BatchState.MODIFYING.toString().equals(String.valueOf(config.spotFleetRequestState()))
                || BatchState.SUBMITTED.toString().equals(String.valueOf(config.spotFleetRequestState()));
    }

    private static boolean isModifying(final SpotFleetRequestConfig config) {
        return BatchState.SUBMITTED.toString().equals(String.valueOf(config.spotFleetRequestState()))
                || BatchState.MODIFYING.toString().equals(String.valueOf(config.spotFleetRequestState()));
    }

    @Override
    public void modify(
            final String awsCredentialsId, final String regionName, final String endpoint,
            String id, int targetCapacity, int min, int max) {
        final ModifySpotFleetRequestRequest request = ModifySpotFleetRequestRequest.builder()
                .spotFleetRequestId(id)
                .targetCapacity(targetCapacity)
                .excessCapacityTerminationPolicy("NoTermination")
                .build();

        final Ec2Client ec2 = Registry.getEc2Api().connect(awsCredentialsId, regionName, endpoint);
        ec2.modifySpotFleetRequest(request);
    }

    @Override
    public FleetStateStats getState(
            final String awsCredentialsId, final String regionName, final String endpoint,
            final String id) {
        final Ec2Client ec2 = Registry.getEc2Api().connect(awsCredentialsId, regionName, endpoint);

        String token = null;
        final Set<String> instances = new HashSet<>();
        do {
            final DescribeSpotFleetInstancesRequest request = DescribeSpotFleetInstancesRequest.builder()
                    .spotFleetRequestId(id)
                    .nextToken(token)
                    .build();
            final DescribeSpotFleetInstancesResponse res = ec2.describeSpotFleetInstances(request);
            for (final ActiveInstance instance : res.activeInstances()) {
                instances.add(instance.instanceId());
            }

            token = res.nextToken();
        } while (token != null);

        final DescribeSpotFleetRequestsRequest request = DescribeSpotFleetRequestsRequest.builder()
                .spotFleetRequestIds(Collections.singleton(id))
                .build();
        final DescribeSpotFleetRequestsResponse fleet = ec2.describeSpotFleetRequests(request);
        if (fleet.spotFleetRequestConfigs().isEmpty())
            throw new IllegalStateException("Fleet " + id + " can't be described");

        final SpotFleetRequestConfig fleetConfig = fleet.spotFleetRequestConfigs().get(0);
        final SpotFleetRequestConfigData fleetRequestConfig = fleetConfig.spotFleetRequestConfig();

        // Index configured instance types by weight:
        final Map<String, Double> instanceTypeWeights = new HashMap<>();
        for (SpotFleetLaunchSpecification launchSpecification : fleetRequestConfig.launchSpecifications()) {
            final InstanceType instanceType = launchSpecification.instanceType();
            if (instanceType == null) continue;
            final String instanceTypeName = instanceType.toString();
            final Double instanceWeight = launchSpecification.weightedCapacity();
            final Double existingWeight = instanceTypeWeights.get(instanceTypeName);
            if (instanceWeight == null || (existingWeight != null && existingWeight > instanceWeight)) {
                continue;
            }
            instanceTypeWeights.put(instanceTypeName, instanceWeight);
        }

        return new FleetStateStats(id,
                fleetRequestConfig.targetCapacity(),
                new FleetStateStats.State(
                        isActive(fleetConfig),
                        isModifying(fleetConfig),
                        String.valueOf(fleetConfig.spotFleetRequestState())),
                instances,
                instanceTypeWeights);
    }

    private static class State {
        String id;
        Set<String> instances;
        SpotFleetRequestConfig config;
    }

    @Override
    public Map<String, FleetStateStats> getStateBatch(
            final String awsCredentialsId, final String regionName, final String endpoint,
            final Collection<String> ids) {
        final Ec2Client ec2 = Registry.getEc2Api().connect(awsCredentialsId, regionName, endpoint);

        List<State> states = new ArrayList<>();
        for (String id : ids) {
            final State s = new State();
            s.id = id;
            states.add(s);
        }

        for (State state : states) {
            String token = null;
            state.instances = new HashSet<>();
            do {
                final DescribeSpotFleetInstancesRequest request = DescribeSpotFleetInstancesRequest.builder()
                        .spotFleetRequestId(state.id)
                        .nextToken(token)
                        .build();
                final DescribeSpotFleetInstancesResponse res = ec2.describeSpotFleetInstances(request);
                for (final ActiveInstance instance : res.activeInstances()) {
                    state.instances.add(instance.instanceId());
                }

                token = res.nextToken();
            } while (token != null);
        }

        final DescribeSpotFleetRequestsRequest request = DescribeSpotFleetRequestsRequest.builder()
                .spotFleetRequestIds(ids)
                .build();
        final DescribeSpotFleetRequestsResponse fleet = ec2.describeSpotFleetRequests(request);
        for (SpotFleetRequestConfig c : fleet.spotFleetRequestConfigs()) {
            for (State state : states) {
                if (state.id.equals(c.spotFleetRequestId())) state.config = c;
            }
        }

        Map<String, FleetStateStats> r = new HashMap<>();
        for (State state : states) {
            r.put(state.id, new FleetStateStats(state.id,
                    state.config.spotFleetRequestConfig().targetCapacity(),
                    new FleetStateStats.State(
                            isActive(state.config),
                            isModifying(state.config),
                            String.valueOf(state.config.spotFleetRequestState())),
                    state.instances,
                    Collections.<String, Double>emptyMap()));
        }

        // todo add weight
        // todo replace single with multiple but just one id less code
        return r;
    }

    @Override
    public Boolean isAutoScalingGroup() {
        return false;
    }

}
