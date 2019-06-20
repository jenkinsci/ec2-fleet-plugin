package com.amazon.jenkins.ec2fleet;

import hudson.model.Executor;
import hudson.model.Queue;
import hudson.model.Slave;
import hudson.slaves.OfflineCause;
import hudson.slaves.SlaveComputer;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.logging.Logger;

/**
 * @see EC2FleetNode
 */
public class EC2FleetNodeComputer extends SlaveComputer {

    /**
     * Delay which will be applied when job {@link Queue#scheduleInternal(Queue.Task, int, List)}
     * rescheduled after offline
     */
    private static final int RESCHEDULE_QUIET_PERIOD_SEC = 10;

    private static final Logger LOGGER = Logger.getLogger(EC2FleetNodeComputer.class.getName());

    public EC2FleetNodeComputer(final Slave slave) {
        super(slave);
    }

    @Override
    public EC2FleetNode getNode() {
        return (EC2FleetNode) super.getNode();
    }

    /**
     * Return label which will represent executor in "Build Executor Status"
     * section of Jenkins UI. After reconfiguration actual {@link EC2FleetNode} could
     * be removed before this, so name will be just predefined static.
     *
     * @return node display name or if node is <code>null</code> predefined text about that
     */
    @Nonnull
    @Override
    public String getDisplayName() {
        // getNode() hit map to find node by name
        final EC2FleetNode node = getNode();
        return node == null ? "removing fleet node" : node.getDisplayName();
    }

    /**
     * Will be called when task execution finished by slave, detailed documentation
     * {@link SlaveComputer#taskCompleted(Executor, Queue.Task, long)}
     * <p>
     * EC2 Fleet plugin overrides this method to detect jobs which were failed because of
     * EC2 instance was terminated/stopped. It could be manual stop or because of Spot marked.
     * In all cases as soon as job aborted because of broken connection and slave is offline
     * it will try to resubmit aborted job back to the queue, so user doesn't need to do that manually
     * and another slave could take it.
     * <p>
     * Implementation details
     * <p>
     * There is no official recommendation about way how to resubmit job according to
     * https://issues.jenkins-ci.org/browse/JENKINS-49707 moreover some of Jenkins code says it impossible.
     * <p>
     * method checks {@link SlaveComputer#getOfflineCause()} for disconnect because of EC2 instance termination
     * it returns
     * <code>
     * result = {OfflineCause$ChannelTermination@13708} "Connection was broken: java.io.IOException:
     * Unexpected termination of the channel\n\tat hudson.remoting.SynchronousCommandTransport$ReaderThread...
     * cause = {IOException@13721} "java.io.IOException: Unexpected termination of the channel"
     * timestamp = 1561067177837
     * </code>
     *
     * @param executor   executor
     * @param task       task
     * @param durationMS duration
     */
    @Override
    public void taskCompleted(final Executor executor, final Queue.Task task, final long durationMS) {
        super.taskCompleted(executor, task, durationMS);
        if (isOffline() && getOfflineCause() instanceof OfflineCause.ChannelTermination) {
            LOGGER.info(" job " + task.getName() + " on " + getDisplayName() + " execution aborted because of slave EC2 instance was terminated, resubmitting");
            Queue.getInstance().schedule(task, RESCHEDULE_QUIET_PERIOD_SEC);
        }
    }

}
