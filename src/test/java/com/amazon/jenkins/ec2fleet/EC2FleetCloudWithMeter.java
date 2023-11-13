package com.amazon.jenkins.ec2fleet;

import hudson.slaves.Cloud;
import hudson.slaves.ComputerConnector;
import hudson.slaves.NodeProvisioner;

import java.util.Collection;


public class EC2FleetCloudWithMeter extends EC2FleetCloud {

    public final Meter updateMeter = new Meter("update");
    public final Meter provisionMeter = new Meter("provision");
    public final Meter removeMeter = new Meter("remove");

    public EC2FleetCloudWithMeter(
            String name, String awsCredentialsId, String credentialsId, String region,
            String endpoint, String fleet, String labelString, String fsRoot, ComputerConnector computerConnector,
            boolean privateIpUsed, boolean alwaysReconnect, Integer idleMinutes, Integer minSize, Integer maxSize,
            Integer minSpareSize, Integer numExecutors, boolean addNodeOnlyIfRunning, boolean restrictUsage, boolean disableTaskResubmit,
            Integer initOnlineTimeoutSec, Integer initOnlineCheckIntervalSec, Integer cloudStatusIntervalSec,
            boolean immediatelyProvision, ExecutorScaler executorScaler) {
        super(name, awsCredentialsId, credentialsId, region, endpoint, fleet, labelString, fsRoot,
                computerConnector, privateIpUsed, alwaysReconnect, idleMinutes, minSize, maxSize, minSpareSize, numExecutors,
                addNodeOnlyIfRunning, restrictUsage, "-1", disableTaskResubmit, initOnlineTimeoutSec, initOnlineCheckIntervalSec,
                cloudStatusIntervalSec, immediatelyProvision, executorScaler);
    }

    @Override
    public Collection<NodeProvisioner.PlannedNode> provision(
            final Cloud.CloudState cloudState, final int excessWorkload) {
        try (Meter.Shot s = provisionMeter.start()) {
            return super.provision(cloudState, excessWorkload);
        }
    }

    @Override
    public FleetStateStats update() {
        try (Meter.Shot s = updateMeter.start()) {
            return super.update();
        }
    }

    @Override
    public boolean scheduleToTerminate(final String instanceId, boolean ignoreMinConstraints, EC2AgentTerminationReason reason) {
        try (Meter.Shot s = removeMeter.start()) {
            return super.scheduleToTerminate(instanceId, ignoreMinConstraints, reason);
        }
    }

}
