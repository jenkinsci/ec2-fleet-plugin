package com.amazon.jenkins.ec2fleet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.SlaveComputer;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @see EC2FleetCloud
 */
public class IdleRetentionStrategy extends RetentionStrategy<SlaveComputer> {

    private static final int RE_CHECK_IN_MINUTE = 1;

    private static final Logger LOGGER = Logger.getLogger(IdleRetentionStrategy.class.getName());

    /**
     * Will be called under {@link hudson.model.Queue#withLock(Runnable)}
     *
     * @param computer computer
     * @return delay in min before next run
     */
    @SuppressFBWarnings(
            value = "BC_UNCONFIRMED_CAST",
            justification = "to ignore EC2FleetNodeComputer cast")
    @Override
    public long check(final SlaveComputer computer) {
        final EC2FleetNodeComputer fc = (EC2FleetNodeComputer) computer;
        final AbstractEC2FleetCloud cloud = fc.getCloud();

        LOGGER.log(Level.INFO, "Check if node idle " + computer.getName());

        // in some multi-thread edge cases cloud could be null for some time, just be ok with that
        if (cloud == null) {
            LOGGER.warning("Edge case cloud is null for computer " + fc.getDisplayName()
                    + " should be autofixed in a few minutes, if no please create issue for plugin");
            return RE_CHECK_IN_MINUTE;
        }

        // Ensure that the EC2FleetCloud cannot be mutated from under us while
        // we're doing this check
        // Ensure nobody provisions onto this node until we've done
        // checking
        boolean shouldAcceptTasks = fc.isAcceptingTasks();
        boolean justTerminated = false;
        fc.setAcceptingTasks(false);
        try {
            if(fc.isIdle() && (cloud.hasExcessCapacity() || isIdleForTooLong(cloud, fc))) {
                // Find instance ID
                Node compNode = fc.getNode();
                if (compNode == null) {
                    return 0;
                }

                final String instanceId = compNode.getNodeName();
                if (cloud.scheduleToTerminate(instanceId)) {
                    // Instance successfully scheduled for termination, so no longer accept tasks
                    shouldAcceptTasks = false;
                    justTerminated = true;
                }
            }

            if (cloud.isAlwaysReconnect() && !justTerminated && fc.isOffline() && !fc.isConnecting() && fc.isLaunchSupported()) {
                LOGGER.log(Level.INFO, "Reconnecting to instance: " + fc.getDisplayName());
                fc.tryReconnect();
            }
        } finally {
            fc.setAcceptingTasks(shouldAcceptTasks);
        }

        return RE_CHECK_IN_MINUTE;
    }

    @Override
    public void start(SlaveComputer c) {
        LOGGER.log(Level.INFO, "Connecting to instance: " + c.getDisplayName());
        c.connect(false);
    }

    private boolean isIdleForTooLong(final AbstractEC2FleetCloud cloud, final Computer computer) {
        final int idleMinutes = cloud.getIdleMinutes();
        if (idleMinutes <= 0) return false;
        final long idleTime = System.currentTimeMillis() - computer.getIdleStartMilliseconds();
        final long maxIdle = TimeUnit.MINUTES.toMillis(idleMinutes);
        LOGGER.log(Level.INFO, "Instance: " + computer.getDisplayName() + " Age: " + idleTime + " Max Age:" + maxIdle);
        return idleTime > maxIdle;
    }
}
