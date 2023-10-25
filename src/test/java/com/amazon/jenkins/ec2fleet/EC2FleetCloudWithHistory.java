package com.amazon.jenkins.ec2fleet;

import hudson.slaves.Cloud;
import hudson.slaves.ComputerConnector;
import hudson.slaves.NodeProvisioner;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;


public class EC2FleetCloudWithHistory extends EC2FleetCloud {

    public CopyOnWriteArrayList<Long> provisionTimes = new CopyOnWriteArrayList<>();

    public EC2FleetCloudWithHistory(
            String name, String awsCredentialsId, String credentialsId, String region,
            String endpoint, String fleet, String labelString, String fsRoot, ComputerConnector computerConnector,
            boolean privateIpUsed, boolean alwaysReconnect, Integer idleMinutes, Integer minSize, Integer maxSize, Integer minSpareSize,
            Integer numExecutors, boolean addNodeOnlyIfRunning, boolean restrictUsage, boolean disableTaskResubmit,
            Integer initOnlineTimeoutSec, Integer initOnlineCheckIntervalSec, Integer cloudStatusIntervalSec,
            boolean immediatelyProvision, ExecutorScaler executorScaler) {
        super(name, awsCredentialsId, credentialsId, region, endpoint, fleet, labelString, fsRoot,
                computerConnector, privateIpUsed, alwaysReconnect, idleMinutes, minSize, maxSize, minSpareSize, numExecutors,
                addNodeOnlyIfRunning, restrictUsage, "-1", disableTaskResubmit, initOnlineTimeoutSec,
                initOnlineCheckIntervalSec, cloudStatusIntervalSec, immediatelyProvision, executorScaler);
    }

    @Override
    public Collection<NodeProvisioner.PlannedNode> provision(
            final Cloud.CloudState cloudState, final int excessWorkload) {
        final Collection<NodeProvisioner.PlannedNode> r = super.provision(cloudState, excessWorkload);
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
