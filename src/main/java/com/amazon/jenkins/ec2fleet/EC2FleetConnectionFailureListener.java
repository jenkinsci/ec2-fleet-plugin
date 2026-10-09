package com.amazon.jenkins.ec2fleet;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.ComputerListener;
import java.util.logging.Logger;

@Extension
public class EC2FleetConnectionFailureListener extends ComputerListener {

    private static final Logger LOGGER = Logger.getLogger(EC2FleetConnectionFailureListener.class.getName());

    @Override
    public void onOnline(final Computer computer) {
        if (computer instanceof EC2FleetNodeComputer) {
            ((EC2FleetNodeComputer) computer).markConnectedSuccessfully();
        }
    }

    @Override
    public void onLaunchFailure(final Computer computer, final TaskListener listener) {
        if (!(computer instanceof EC2FleetNodeComputer)) {
            return;
        }

        final EC2FleetNodeComputer fleetComputer = (EC2FleetNodeComputer) computer;
        if (fleetComputer.hasConnectedSuccessfully()) {
            return;
        }

        final EC2FleetNode node = fleetComputer.getNode();
        if (node == null) {
            return;
        }

        final AbstractEC2FleetCloud cloud = node.getCloud();
        if (cloud == null || !cloud.isTerminateOnConnectionFailure()) {
            return;
        }

        final String instanceId = node.getInstanceId();
        if (cloud.scheduleToTerminate(instanceId, true, EC2AgentTerminationReason.CONNECTION_FAILURE)) {
            fleetComputer.setAcceptingTasks(false);
            LOGGER.info(String.format(
                    "Scheduled EC2 Fleet agent '%s' for termination after its initial connection failed",
                    fleetComputer.getDisplayName()));
        }
    }
}
