package com.amazon.jenkins.ec2fleet;

import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.Executor;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Slave;
import hudson.model.TaskListener;
import hudson.model.queue.SubTask;
import hudson.slaves.ComputerLauncher;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class, Queue.class, WorkflowJob.class, WorkflowRun.class})
public class EC2FleetAutoResubmitComputerLauncherTest {

    @Mock
    private ComputerLauncher baseComputerLauncher;

    @Mock
    private TaskListener taskListener;

    @Mock
    private Action action1;

    @Mock
    private Executor executor1;

    @Mock
    private Executor executor2;

    private Actionable executable1;

    @Mock
    private Queue.Executable executable2;

    @Mock
    private Slave agent;

    @Mock
    private EC2FleetNodeComputer computer;

    @Mock
    private Jenkins jenkins;

    @Mock
    private Queue queue;

    @Mock
    private SubTask subTask1;

    @Mock
    private SubTask subTask2;

    @Mock
    private Queue.Task task1;

    @Mock
    private Queue.Task task2;

    @Mock
    private EC2FleetNode fleetNode;

    @Mock
    private EC2FleetCloud cloud;

    @Mock
    private WorkflowJob workflowJob;

    @Mock
    private WorkflowRun workflowRun;

    @Before
    public void before() {
        executable1 = mock(Actionable.class, withSettings().extraInterfaces(Queue.Executable.class));

        when(computer.getDisplayName()).thenReturn("i-12");

        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.mockStatic(Queue.class);
        when(Jenkins.get()).thenReturn(jenkins);
        when(Queue.getInstance()).thenReturn(queue);

        when(agent.getNumExecutors()).thenReturn(1);

        when(fleetNode.getDisplayName()).thenReturn("fleet node name");

        when(((Queue.Executable) executable1).getParent()).thenReturn(subTask1);
        when(executable2.getParent()).thenReturn(subTask2);

        when(subTask1.getOwnerTask()).thenReturn(task1);
        when(subTask2.getOwnerTask()).thenReturn(task2);

        when(executor1.getCurrentExecutable()).thenReturn((Queue.Executable) executable1);
        when(executor2.getCurrentExecutable()).thenReturn(executable2);

        when(computer.getExecutors()).thenReturn(Arrays.asList(executor1, executor2));
        when(computer.isOffline()).thenReturn(true);

        when(computer.getCloud()).thenReturn(cloud);
    }

    @Test
    public void afterDisconnect_should_do_nothing_if_still_online() {
        when(computer.isOffline()).thenReturn(false);
        new EC2FleetAutoResubmitComputerLauncher(baseComputerLauncher)
                .afterDisconnect(computer, taskListener);
        verifyZeroInteractions(queue);
    }

    @Test
    public void afterDisconnect_should_do_nothing_if_offline_but_no_executable() {
        when(computer.getExecutors()).thenReturn(Arrays.asList(executor1));
        when(executor1.getCurrentExecutable()).thenReturn(null);
        new EC2FleetAutoResubmitComputerLauncher(baseComputerLauncher)
                .afterDisconnect(computer, taskListener);
        verifyZeroInteractions(queue);
    }

    @Test
    public void taskCompleted_should_resubmit_task_if_offline_and_has_executable() {
        when(computer.getExecutors()).thenReturn(Arrays.asList(executor1));
        new EC2FleetAutoResubmitComputerLauncher(baseComputerLauncher)
                .afterDisconnect(computer, taskListener);
        verify(queue).schedule2(eq(task1), anyInt(), eq(Collections.<Action>emptyList()));
        verifyZeroInteractions(queue);
    }

    @Test
    public void taskCompleted_should_not_resubmit_task_if_offline_but_disabled() {
        when(cloud.isDisableTaskResubmit()).thenReturn(true);
        when(computer.getExecutors()).thenReturn(Arrays.asList(executor1));
        new EC2FleetAutoResubmitComputerLauncher(baseComputerLauncher)
                .afterDisconnect(computer, taskListener);
        verifyZeroInteractions(queue);
    }

    @Test
    public void taskCompleted_should_resubmit_task_for_all_executors() {
        new EC2FleetAutoResubmitComputerLauncher(baseComputerLauncher)
                .afterDisconnect(computer, taskListener);
        verify(queue).schedule2(eq(task1), anyInt(), eq(Collections.<Action>emptyList()));
        verify(queue).schedule2(eq(task2), anyInt(), eq(Collections.<Action>emptyList()));
        verifyZeroInteractions(queue);
    }

    @Test
    public void taskCompleted_should_abort_executors_during_resubmit() {
        new EC2FleetAutoResubmitComputerLauncher(baseComputerLauncher)
                .afterDisconnect(computer, taskListener);
        verify(executor1).interrupt(Result.ABORTED, new EC2ExecutorInterruptionCause("i-12"));
        verify(executor2).interrupt(Result.ABORTED, new EC2ExecutorInterruptionCause("i-12"));
    }

    @Test
    public void taskCompleted_should_resubmit_task_with_actions() {
        when(computer.getExecutors()).thenReturn(Arrays.asList(executor1));
        when(executable1.getActions()).thenReturn(Arrays.asList(action1));
        new EC2FleetAutoResubmitComputerLauncher(baseComputerLauncher)
                .afterDisconnect(computer, taskListener);
        verify(queue).schedule2(eq(task1), anyInt(), eq(Arrays.asList(action1)));
        verifyZeroInteractions(queue);
    }

    @Test
    public void taskCompleted_should_resubmit_task_with_failed_build_actions() {
        when(subTask1.getOwnerTask()).thenReturn(workflowJob);
        when(workflowJob.getLastFailedBuild()).thenReturn(workflowRun);
        when(workflowRun.getActions(any())).thenReturn((Collections.singletonList(action1)));
        when(computer.getExecutors()).thenReturn(Arrays.asList(executor1));
        new EC2FleetAutoResubmitComputerLauncher(baseComputerLauncher)
            .afterDisconnect(computer, taskListener);
        verify(queue).schedule2(eq(workflowJob), anyInt(), eq(Arrays.asList(action1)));
        verify(workflowRun, times(1)).getActions(any());
    }

}
