package com.amazon.jenkins.ec2fleet;

import hudson.model.Label;
import hudson.slaves.ComputerConnector;
import hudson.slaves.NodeProvisioner;

import java.util.Collection;


public class EC2FleetCloudWithMeter extends EC2FleetCloud {

    public final Meter updateMeter = new Meter("update");
    public final Meter provisionMeter = new Meter("provision");
    public final Meter removeMeter = new Meter("remove");

    public EC2FleetCloudWithMeter(
            String name, String oldId, String awsCredentialsId, String credentialsId, String region,
            String endpoint, String fleet, String labelString, String fsRoot, ComputerConnector computerConnector,
            boolean privateIpUsed, boolean alwaysReconnect, Integer idleMinutes, Integer minSize, Integer maxSize,
            Integer numExecutors, boolean addNodeOnlyIfRunning, boolean restrictUsage, boolean disableTaskResubmit,
            Integer initOnlineTimeoutSec, Integer initOnlineCheckIntervalSec, boolean scaleExecutorsByWeight,
            Integer cloudStatusIntervalSec, boolean immediatelyProvision) {
        super(name, oldId, awsCredentialsId, credentialsId, region, endpoint, fleet, labelString, fsRoot,
                computerConnector, privateIpUsed, alwaysReconnect, idleMinutes, minSize, maxSize, numExecutors,
                addNodeOnlyIfRunning, restrictUsage, "-1", disableTaskResubmit, initOnlineTimeoutSec, initOnlineCheckIntervalSec,
                scaleExecutorsByWeight, cloudStatusIntervalSec, immediatelyProvision);
    }

    @Override
    public Collection<NodeProvisioner.PlannedNode> provision(
            final Label label, final int excessWorkload) {
        try (Meter.Shot s = provisionMeter.start()) {
            return super.provision(label, excessWorkload);
        }
    }

    @Override
    public FleetStateStats update() {
        try (Meter.Shot s = updateMeter.start()) {
            return super.update();
        }
    }

    @Override
    public boolean scheduleToTerminate(final String instanceId, boolean force) {
        try (Meter.Shot s = removeMeter.start()) {
            return super.scheduleToTerminate(instanceId, force);
        }
    }

}
