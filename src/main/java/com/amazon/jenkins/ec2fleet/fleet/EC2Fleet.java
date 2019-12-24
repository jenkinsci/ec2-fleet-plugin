package com.amazon.jenkins.ec2fleet.fleet;

import com.amazon.jenkins.ec2fleet.FleetStateStats;
import hudson.util.ListBoxModel;

import java.util.Collection;
import java.util.Map;

/**
 * Hide details of access to EC2 Fleet depending on implementation like EC2 Fleet based on EC2 Spot Fleet
 * or Auto Scaling Group.
 *
 * @see EC2SpotFleet
 * @see AutoScalingGroupFleet
 */
public interface EC2Fleet {

    void describe(
            final String awsCredentialsId, final String regionName, final String endpoint,
            final ListBoxModel model, final String selectedId, final boolean showAll);

    void modify(
            final String awsCredentialsId, final String regionName, final String endpoint,
            final String id, final int targetCapacity, int min, int max);

    FleetStateStats getState(
            final String awsCredentialsId, final String regionName, final String endpoint,
            final String id);

    Map<String, FleetStateStats> getStateBatch(
            final String awsCredentialsId, final String regionName, final String endpoint,
            final Collection<String> ids);

}
