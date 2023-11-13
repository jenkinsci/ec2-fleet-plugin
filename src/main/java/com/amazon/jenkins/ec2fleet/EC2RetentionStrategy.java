package com.amazon.jenkins.ec2fleet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.ExecutorListener;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.slaves.RetentionStrategy;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@link EC2RetentionStrategy} controls when to take {@link EC2FleetNodeComputer} offline, bring it back online, or even to destroy it.
 */
public class EC2RetentionStrategy extends RetentionStrategy<EC2FleetNodeComputer> implements ExecutorListener {
    private static final Logger LOGGER = Logger.getLogger(EC2RetentionStrategy.class.getName());
    private static final int RE_CHECK_IN_A_MINUTE = 1;

    /**
     * Will be called under {@link hudson.model.Queue#withLock(Runnable)}
     *
     * @param fc EC2FleetNodeComputer
     * @return delay in min before next run
     */
    @SuppressFBWarnings(
            value = "BC_UNCONFIRMED_CAST",
            justification = "to ignore EC2FleetNodeComputer cast")
    @Override
    public long check(final EC2FleetNodeComputer fc) {
        final AbstractEC2FleetCloud cloud = fc.getCloud();

        LOGGER.fine(String.format("Checking if node '%s' is idle ", fc.getName()));

        // in some multi-thread edge cases cloud could be null for some time, just be ok with that
        if (cloud == null) {
            LOGGER.warning("Cloud is null for computer " + fc.getDisplayName()
                    + ". This should be autofixed in a few minutes, if not please create an issue for the plugin");
            return RE_CHECK_IN_A_MINUTE;
        }

        // Ensure that the EC2FleetCloud cannot be mutated from under us while
        // we're doing this check
        // Ensure nobody provisions onto this node until we've done
        // checking
        boolean shouldAcceptTasks = fc.isAcceptingTasks();
        boolean markedForTermination = false;
        fc.setAcceptingTasks(false);
        try {
            if(fc.isIdle()) {
                Node node = fc.getNode();
                if (node == null) {
                    return RE_CHECK_IN_A_MINUTE;
                }

                EC2AgentTerminationReason reason;
                // Determine the reason for termination from specific to generic use cases.
                // Reasoning for checking all cases of termination initiated by the plugin:
                //  A user-initiated change to cloud configuration creates a new EC2FleetCloud object, erasing class fields containing data like instance IDs to terminate.
                //  Hence, determine the reasons for termination here using persisted fields for accurate handling of termination.
                if (fc.isMarkedForDeletion()) {
                    reason = EC2AgentTerminationReason.AGENT_DELETED;
                } else if (cloud.hasExcessCapacity()) {
                    reason = EC2AgentTerminationReason.EXCESS_CAPACITY;
                } else if (cloud instanceof EC2FleetCloud && !((EC2FleetCloud) cloud).hasUnlimitedUsesForNodes()
                        && ((EC2FleetNode)node).getUsesRemaining() <= 0) {
                    reason = EC2AgentTerminationReason.MAX_TOTAL_USES_EXHAUSTED;
                } else if (isIdleForTooLong(cloud, fc)) {
                    reason = EC2AgentTerminationReason.IDLE_FOR_TOO_LONG;
                } else {
                    return RE_CHECK_IN_A_MINUTE;
                }

                final String instanceId = node.getNodeName();
                final boolean ignoreMinConstraints = reason.equals(EC2AgentTerminationReason.MAX_TOTAL_USES_EXHAUSTED);
                if (cloud.scheduleToTerminate(instanceId, ignoreMinConstraints, reason)) {
                    // Instance successfully scheduled for termination, so no longer accept tasks (i.e. suspended)
                    shouldAcceptTasks = false;
                    LOGGER.fine(String.format("Suspended node %s after scheduling instance for termination, reason: %s.",
                            node.getDisplayName(), instanceId, reason));
                    markedForTermination = true;
                }
            }

            // if connection to the computer is lost for some reason, try to reconnect if configured to do so.
            if (cloud.isAlwaysReconnect() && !markedForTermination && fc.isOffline() && !fc.isConnecting() && fc.isLaunchSupported()) {
                LOGGER.log(Level.INFO, "Reconnecting to instance: " + fc.getDisplayName());
                fc.tryReconnect();
            }
        } finally {
            fc.setAcceptingTasks(shouldAcceptTasks);
        }

        return RE_CHECK_IN_A_MINUTE;
    }

    @Override
    public void start(EC2FleetNodeComputer c) {
        LOGGER.log(Level.INFO, "Connecting to instance: " + c.getDisplayName());
        c.connect(false);
    }

    private boolean isIdleForTooLong(final AbstractEC2FleetCloud cloud, final Computer computer) {
        final int idleMinutes = cloud.getIdleMinutes();
        if (idleMinutes <= 0) return false;

        final long idleTime = System.currentTimeMillis() - computer.getIdleStartMilliseconds();
        final long maxIdle = TimeUnit.MINUTES.toMillis(idleMinutes);
        final boolean isIdleForTooLong = idleTime > maxIdle;

        // TODO: use Jenkins terminology in logs
        if (isIdleForTooLong) {
            LOGGER.log(Level.INFO, "Instance {0} has been idle for too long (Age: {1}, Max Age: {2}).", new Object[]{computer.getDisplayName(), String.valueOf(idleTime), String.valueOf(maxIdle)});
        } else {
            LOGGER.log(Level.INFO, "Instance:" + computer.getDisplayName() + " Age: " + idleTime + " Max Age:" + maxIdle);
        }

        return isIdleForTooLong;
    }

    @Override
    public void taskAccepted(Executor executor, Queue.Task task) {
        final EC2FleetNodeComputer computer = (EC2FleetNodeComputer) executor.getOwner();
        if (computer != null) {
            final EC2FleetNode ec2FleetNode = computer.getNode();
            if (ec2FleetNode != null) {
                final int maxTotalUses = ec2FleetNode.getMaxTotalUses();
                if (maxTotalUses <= -1) { // unlimited uses
                    LOGGER.fine("maxTotalUses set to unlimited (" + maxTotalUses + ") for agent " + computer.getName());
                } else { // limited uses
                    if (ec2FleetNode.getUsesRemaining() > 1) {
                        ec2FleetNode.decrementUsesRemaining();
                        LOGGER.info("Agent " + computer.getName() + " has " + ec2FleetNode.getUsesRemaining() + " builds left");
                    } else if (ec2FleetNode.getUsesRemaining() == 1) { // current task should be the last task for this agent
                        LOGGER.info(String.format("maxTotalUses drained - suspending agent %s after current build", computer.getName()));
                        computer.setAcceptingTasks(false);
                        ec2FleetNode.decrementUsesRemaining();
                    } else {
                        // don't decrement when usesRemaining=0, as -1 has a special meaning.
                        LOGGER.warning(String.format("Agent %s accepted a task after being suspended!!! MaxTotalUses: %d, uses remaining: %d",
                                computer.getName(), ec2FleetNode.getMaxTotalUses(), ec2FleetNode.getUsesRemaining()));
                    }
                }
            }
        }
    }

    @Override
    public void taskCompleted(Executor executor, Queue.Task task, long l) {
        postJobAction(executor, null);
    }

    @Override
    public void taskCompletedWithProblems(Executor executor, Queue.Task task, long l, Throwable throwable) {
        postJobAction(executor, throwable);
    }

    private void postJobAction(final Executor executor, final Throwable throwable) {
        if (throwable != null) {
            LOGGER.warning(String.format("Build %s completed with problems on agent %s. TimeSpentInQueue: %ds, duration: %ds, problems: %s",
                    executor.getCurrentExecutable(), executor.getOwner().getName(),
                    TimeUnit.MILLISECONDS.toSeconds(executor.getTimeSpentInQueue()),
                    TimeUnit.MILLISECONDS.toSeconds(executor.getElapsedTime()), throwable.getMessage()));
        } else {
            LOGGER.info(String.format("Build %s completed successfully on agent %s. TimeSpentInQueue: %ds, duration: %ds.",
                    executor.getCurrentExecutable(), executor.getOwner().getName(),
                    TimeUnit.MILLISECONDS.toSeconds(executor.getTimeSpentInQueue()),
                    TimeUnit.MILLISECONDS.toSeconds(executor.getElapsedTime())));
        }

        final EC2FleetNodeComputer computer = (EC2FleetNodeComputer) executor.getOwner();
        if (computer != null) {
            final EC2FleetNode ec2FleetNode = computer.getNode();
            if (ec2FleetNode != null) {
                final AbstractEC2FleetCloud cloud = ec2FleetNode.getCloud();
                if (computer.countBusy() <= 1 && !computer.isAcceptingTasks()) {
                    LOGGER.info("Calling scheduleToTerminate for node " + ec2FleetNode.getNodeName() + " due to exhausted maxTotalUses.");
                    // Schedule instance for termination even if it breaches minSize and minSpareSize constraints
                    cloud.scheduleToTerminate(ec2FleetNode.getNodeName(), true, EC2AgentTerminationReason.MAX_TOTAL_USES_EXHAUSTED);
                }
            }
        }
    }
}
