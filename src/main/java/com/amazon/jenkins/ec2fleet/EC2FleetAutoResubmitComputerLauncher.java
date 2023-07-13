package com.amazon.jenkins.ec2fleet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.Executor;
import hudson.model.ParametersAction;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.queue.SubTask;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.DelegatingComputerLauncher;
import hudson.slaves.SlaveComputer;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@link EC2FleetAutoResubmitComputerLauncher} is responsible for controlling:
 *  * how {@link EC2FleetNodeComputer}s are launched
 *  * how {@link EC2FleetNodeComputer}s connect to agents {@link EC2FleetNode}
 *
 * This is wrapper for {@link ComputerLauncher} to get notification when agent was disconnected
 * and automatically resubmit {@link hudson.model.Queue.Task} if reason is unexpected termination
 * which usually means EC2 instance was interrupted.
 * <p>
 * This is optional feature, it's enabled by default, but could be disabled by
 * {@link EC2FleetCloud#isDisableTaskResubmit()}
 */
@SuppressWarnings("WeakerAccess")
@ThreadSafe
public class EC2FleetAutoResubmitComputerLauncher extends DelegatingComputerLauncher {

    private static final Level LOG_LEVEL = Level.INFO;
    private static final Logger LOGGER = Logger.getLogger(EC2FleetAutoResubmitComputerLauncher.class.getName());

    /**
     * Delay which will be applied when job {@link Queue#scheduleInternal(Queue.Task, int, List)}
     * rescheduled after offline
     */
    private static final int RESCHEDULE_QUIET_PERIOD_SEC = 10;

    public EC2FleetAutoResubmitComputerLauncher(final ComputerLauncher launcher) {
        super(launcher);
    }

    /**
     * {@link ComputerLauncher#afterDisconnect(SlaveComputer, TaskListener)}
     * <p>
     * EC2 Fleet plugin overrides this method to detect jobs which were failed because of
     * EC2 instance was terminated/stopped. It could be manual stop or because of Spot marked.
     * In all cases as soon as job aborted because of broken connection and agent is offline
     * it will try to resubmit aborted job back to the queue, so user doesn't need to do that manually
     * and another agent could take it.
     * <p>
     * Implementation details
     * <p>
     * There is no official recommendation about way how to resubmit job according to
     * https://issues.jenkins-ci.org/browse/JENKINS-49707 moreover some of Jenkins code says it impossible.
     * <p>
     * We resubmit any active executables that were being processed by the disconnected node, regardless of
     * why the node disconnected.
     *
     * @param computer computer
     * @param listener listener
     */
    @SuppressFBWarnings(
            value = "BC_UNCONFIRMED_CAST",
            justification = "to ignore EC2FleetNodeComputer cast")
    @Override
    public void afterDisconnect(final SlaveComputer computer, final TaskListener listener) {
        // according to jenkins docs could be null in edge cases, check ComputerLauncher.afterDisconnect
        if (computer == null) return;

        // in some multi-thread edge cases cloud could be null for some time, just be ok with that
        final AbstractEC2FleetCloud cloud = ((EC2FleetNodeComputer) computer).getCloud();
        if (cloud == null) {
            LOGGER.warning("Cloud is null for computer " + computer.getDisplayName()
                    + ". This should be autofixed in a few minutes, if not please create an issue for the plugin");
            return;
        }

        LOGGER.log(LOG_LEVEL, "DISCONNECTED: " + computer.getDisplayName());

        if (!cloud.isDisableTaskResubmit() && computer.isOffline()) {
            final List<Executor> executors = computer.getExecutors();
            LOGGER.log(LOG_LEVEL, "Start retriggering executors for " + computer.getDisplayName());

            for (Executor executor : executors) {
                final Queue.Executable executable = executor.getCurrentExecutable();
                if (executable != null) {
                    executor.interrupt(Result.ABORTED, new EC2ExecutorInterruptionCause(computer.getDisplayName()));

                    final SubTask subTask = executable.getParent();
                    final Queue.Task task = subTask.getOwnerTask();

                    final List<Action> actions = new ArrayList<>();
                    if (task instanceof WorkflowJob) {
                        final WorkflowRun failedBuild = ((WorkflowJob) task).getLastBuild();
                        actions.addAll(failedBuild.getActions(ParametersAction.class));
                    }
                    if (executable instanceof Actionable) {
                        actions.addAll(((Actionable) executable).getAllActions());
                    }
                    LOGGER.log(LOG_LEVEL, "RETRIGGERING: " + task + " - WITH ACTIONS: " + actions);
                    Queue.getInstance().schedule2(task, RESCHEDULE_QUIET_PERIOD_SEC, actions);
                }
            }
            LOGGER.log(LOG_LEVEL, "Finished retriggering executors for " + computer.getDisplayName());
        } else {
            LOGGER.log(LOG_LEVEL, "Skipping executable resubmission for " + computer.getDisplayName()
                    + " - disableTaskResubmit: " + cloud.isDisableTaskResubmit() + " - offline: " + computer.isOffline());
        }

        // call parent
        super.afterDisconnect(computer, listener);
    }

}
