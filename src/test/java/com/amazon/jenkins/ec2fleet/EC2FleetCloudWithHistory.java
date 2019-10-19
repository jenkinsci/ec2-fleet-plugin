package com.amazon.jenkins.ec2fleet;

import hudson.model.Label;
import hudson.slaves.ComputerConnector;
import hudson.slaves.NodeProvisioner;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;


public class EC2FleetCloudWithHistory extends EC2FleetCloud {

    public CopyOnWriteArrayList<Long> provisionTimes = new CopyOnWriteArrayList<>();

    public EC2FleetCloudWithHistory(
            String name, String oldId, String awsCredentialsId, String credentialsId, String region,
            String endpoint, String fleet, String labelString, String fsRoot, ComputerConnector computerConnector,
            boolean privateIpUsed, boolean alwaysReconnect, Integer idleMinutes, Integer minSize, Integer maxSize,
            Integer numExecutors, boolean addNodeOnlyIfRunning, boolean restrictUsage, boolean disableTaskResubmit,
            Integer initOnlineTimeoutSec, Integer initOnlineCheckIntervalSec, boolean scaleExecutorsByWeight,
            Integer cloudStatusIntervalSec, boolean immediatelyProvision) {
        super(name, oldId, awsCredentialsId, credentialsId, region, endpoint, fleet, labelString, fsRoot,
                computerConnector, privateIpUsed, alwaysReconnect, idleMinutes, minSize, maxSize, numExecutors,
                addNodeOnlyIfRunning, restrictUsage, disableTaskResubmit, initOnlineTimeoutSec,
                initOnlineCheckIntervalSec, scaleExecutorsByWeight, cloudStatusIntervalSec, immediatelyProvision);
    }

    @Override
    public Collection<NodeProvisioner.PlannedNode> provision(
            final Label label, final int excessWorkload) {
        final Collection<NodeProvisioner.PlannedNode> r = super.provision(label, excessWorkload);
        for (NodeProvisioner.PlannedNode ignore : r) provisionTimes.add(System.currentTimeMillis());
        return r;
    }

//    @Override
//    public FleetStateStats update() {
//        try (Meter.Shot s = updateMeter.start()) {
//            return super.update();
//        }
//    }

//    @Override
//    public boolean scheduleToTerminate(final String instanceId) {
//        try (Meter.Shot s = removeMeter.start()) {
//            return super.scheduleToTerminate(instanceId);
//        }
//    }

}
