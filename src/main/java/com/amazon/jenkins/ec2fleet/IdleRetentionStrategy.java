package com.amazon.jenkins.ec2fleet;

import hudson.model.Computer;
import hudson.model.Node;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.SlaveComputer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: cyberax
 * Date: 1/12/16
 * Time: 02:56
 */
public class IdleRetentionStrategy extends RetentionStrategy<SlaveComputer>
{
    private final int maxIdleMinutes;
    private final EC2FleetCloud parent;

    private static final Logger LOGGER = Logger.getLogger(EC2FleetCloud.class.getName());

    public IdleRetentionStrategy(final int maxIdleMinutes, final EC2FleetCloud parent) {
        this.maxIdleMinutes = maxIdleMinutes;
        this.parent = parent;
        LOGGER.log(Level.INFO, "Idle Retention initiated");
    }

    protected boolean isIdleForTooLong(final Computer c) {
        long age = System.currentTimeMillis()-c.getIdleStartMilliseconds();
        long maxAge = maxIdleMinutes*60*1000;
        LOGGER.log(Level.INFO, "Instance: " + c.getDisplayName() + " Age: " + age + " Max Age:" + maxAge);
        return System.currentTimeMillis()-c.getIdleStartMilliseconds() > (maxIdleMinutes*60*1000);
    }

    @Override public long check(final SlaveComputer c) {
        // Ensure that the EC2FleetCloud cannot be mutated from under us while
        // we're doing this check
        synchronized(parent) {
            // Ensure nobody provisions onto this node until we've done
            // checking
            boolean shouldAcceptTasks = c.isAcceptingTasks();
            c.setAcceptingTasks(false);
            try {
                if (isIdleForTooLong(c)) {
                    // Find instance ID
                    Node compNode = c.getNode();
                    if (compNode == null) {
                        return 0;
                    }

                    final String nodeId = compNode.getNodeName();
                    if (parent.terminateInstance(nodeId)) {
                        // Instance successfully terminated, so no longer accept tasks
                        shouldAcceptTasks = false;
                    }
                } else {
                    if (c.isOffline() && !c.isConnecting() && c.isLaunchSupported())
                        c.tryReconnect();
                }
            } finally {
                c.setAcceptingTasks(shouldAcceptTasks);
            }
        }

        return 1;
    }
}
