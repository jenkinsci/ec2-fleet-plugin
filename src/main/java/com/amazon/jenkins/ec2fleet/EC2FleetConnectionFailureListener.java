package com.amazon.jenkins.ec2fleet;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.ComputerListener;
import hudson.slaves.ComputerLauncher;
import hudson.util.DaemonThreadFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Extension
public class EC2FleetConnectionFailureListener extends ComputerListener {

    private static final Logger LOGGER = Logger.getLogger(EC2FleetConnectionFailureListener.class.getName());
    private static final ScheduledExecutorService EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());

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

        final ComputerLauncher baseLauncher = fleetComputer.getBaseLauncher();
        if (!(baseLauncher instanceof SSHLauncher)) {
            return;
        }

        final SSHLauncher sshLauncher = (SSHLauncher) baseLauncher;
        final long retryWindowMillis = getRetryWindowMillis(sshLauncher);
        final EC2FleetNode node = fleetComputer.getNode();
        if (node == null) {
            return;
        }

        final AbstractEC2FleetCloud cloud = node.getCloud();
        if (cloud == null || !cloud.isTerminateOnConnectionFailure()) {
            return;
        }

        fleetComputer.scheduleConnectionFailureCheck(EXECUTOR, () -> {
            if (fleetComputer.isOnline()) {
                return;
            }
            final String instanceId = node.getInstanceId();
            if (cloud.scheduleToTerminate(instanceId, true, EC2AgentTerminationReason.CONNECTION_FAILURE)) {
                fleetComputer.setAcceptingTasks(false);
                LOGGER.info(String.format(
                        "Scheduled EC2 Fleet agent '%s' for termination after its SSH connection retries failed",
                        fleetComputer.getDisplayName()));
            }
        }, retryWindowMillis);
    }

    static long getRetryWindowMillis(final SSHLauncher launcher) {
        final long retryAttempts = (long) launcher.getMaxNumRetries() + 1;
        final long secondsPerAttempt = (long) launcher.getLaunchTimeoutSeconds() + launcher.getRetryWaitTime();
        if (secondsPerAttempt > Long.MAX_VALUE / retryAttempts / 1000) {
            return Long.MAX_VALUE;
        }
        return secondsPerAttempt * retryAttempts * 1000;
    }
}
