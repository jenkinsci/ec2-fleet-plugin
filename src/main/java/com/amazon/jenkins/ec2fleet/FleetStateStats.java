package com.amazon.jenkins.ec2fleet;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.ActiveInstance;
import com.amazonaws.services.ec2.model.DescribeSpotFleetInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsResult;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfig;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @see EC2FleetCloud
 */
@SuppressWarnings("unused")
public final class FleetStateStats {

    private @Nonnull
    final String fleetId;
    private @Nonnegative
    final int numActive;
    private @Nonnegative
    final int numDesired;
    private @Nonnull
    final String state;
    private @Nonnull
    final Set<String> instances;
    private @Nonnull
    final String label;

    public FleetStateStats(final @Nonnull String fleetId,
                           final int numDesired, final @Nonnull String state,
                           final @Nonnull Set<String> instances,
                           final @Nonnull String label) {
        this.fleetId = fleetId;
        this.numActive = instances.size();
        this.numDesired = numDesired;
        this.state = state;
        this.instances = instances;
        this.label = label;
    }

    @Nonnull
    public String getFleetId() {
        return fleetId;
    }

    public int getNumActive() {
        return numActive;
    }

    public int getNumDesired() {
        return numDesired;
    }

    @Nonnull
    public String getState() {
        return state;
    }

    @Nonnull
    public Set<String> getInstances() {
        return instances;
    }

    @Nonnull
    public String getLabel() {
        return label;
    }

    public static FleetStateStats readClusterState(final AmazonEC2 ec2, final String fleetId, final String label) {
        String token = null;
        final Set<String> instances = new HashSet<>();
        do {
            final DescribeSpotFleetInstancesRequest request = new DescribeSpotFleetInstancesRequest();
            request.setSpotFleetRequestId(fleetId);
            request.setNextToken(token);
            final DescribeSpotFleetInstancesResult res = ec2.describeSpotFleetInstances(request);
            for (final ActiveInstance instance : res.getActiveInstances()) {
                instances.add(instance.getInstanceId());
            }

            token = res.getNextToken();
        } while (token != null);

        final DescribeSpotFleetRequestsRequest request = new DescribeSpotFleetRequestsRequest();
        request.setSpotFleetRequestIds(Collections.singleton(fleetId));
        final DescribeSpotFleetRequestsResult fleet = ec2.describeSpotFleetRequests(request);
        if (fleet.getSpotFleetRequestConfigs().isEmpty())
            throw new IllegalStateException("Fleet " + fleetId + " can't be described");

        final SpotFleetRequestConfig fleetConfig = fleet.getSpotFleetRequestConfigs().get(0);

        return new FleetStateStats(fleetId,
                fleetConfig.getSpotFleetRequestConfig().getTargetCapacity(),
                fleetConfig.getSpotFleetRequestState(), instances,
                label);
    }
}
