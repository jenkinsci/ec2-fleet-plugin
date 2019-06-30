package com.amazon.jenkins.ec2fleet;

import hudson.model.Computer;
import hudson.model.Node;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.SlaveComputer;

import javax.annotation.concurrent.GuardedBy;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @see EC2FleetCloud
 */
public class IdleRetentionStrategy extends RetentionStrategy<SlaveComputer> {

    private static final int RE_CHECK_IN_MINUTE = 1;

    private static final Logger LOGGER = Logger.getLogger(IdleRetentionStrategy.class.getName());

    private final int maxIdleMinutes;
    private final boolean alwaysReconnect;
    private final EC2FleetCloud cloud;

    @SuppressWarnings("WeakerAccess")
    public IdleRetentionStrategy(final EC2FleetCloud cloud) {
        this.maxIdleMinutes = cloud.getIdleMinutes();
        this.alwaysReconnect = cloud.isAlwaysReconnect();
        this.cloud = cloud;
    }

    /**
     * Will be called under {@link hudson.model.Queue#withLock(Runnable)}
     *
     * @param c computer
     * @return delay in min before next run
     */
    @GuardedBy("Queue.withLock")
    @Override
    public long check(final SlaveComputer c) {
        // Ensure that the EC2FleetCloud cannot be mutated from under us while
        // we're doing this check
        synchronized (cloud) {
            // Ensure nobody provisions onto this node until we've done
            // checking
            boolean shouldAcceptTasks = c.isAcceptingTasks();
            boolean justTerminated = false;
            c.setAcceptingTasks(false);
            try {
                if (c.isIdle() && isIdleForTooLong(c)) {
                    // Find instance ID
                    Node compNode = c.getNode();
                    if (compNode == null) {
                        return 0;
                    }

                    final String nodeId = compNode.getNodeName();
                    if (cloud.terminateInstance(nodeId)) {
                        // Instance successfully terminated, so no longer accept tasks
                        shouldAcceptTasks = false;
                        justTerminated = true;
                    }
                }

                if (alwaysReconnect && !justTerminated && c.isOffline() && !c.isConnecting() && c.isLaunchSupported()) {
                    LOGGER.log(Level.INFO, "Reconnecting to instance: " + c.getDisplayName());
                    c.tryReconnect();
                }
            } finally {
                c.setAcceptingTasks(shouldAcceptTasks);
            }
        }

        return RE_CHECK_IN_MINUTE;
    }

    @Override
    public void start(SlaveComputer c) {
        LOGGER.log(Level.INFO, "Connecting to instance: " + c.getDisplayName());
        c.connect(false);
    }

    private boolean isIdleForTooLong(final Computer c) {
        if (maxIdleMinutes <= 0) return false;
        final long idleTime = System.currentTimeMillis() - c.getIdleStartMilliseconds();
        final long maxIdle = TimeUnit.MINUTES.toMillis(maxIdleMinutes);
        LOGGER.log(Level.FINE, "Instance: " + c.getDisplayName() + " Age: " + idleTime + " Max Age:" + maxIdle);
        return idleTime > maxIdle;
    }

}
