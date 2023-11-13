package com.amazon.jenkins.ec2fleet;

import hudson.model.Executor;
import hudson.model.Queue;
import hudson.slaves.SlaveComputer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SlaveComputer.class)
public class EC2RetentionStrategyTest {

    @Mock
    private EC2FleetCloud cloud;

    @Mock
    private EC2FleetNodeComputer computer;

    @Mock
    private EC2FleetNode node;

    @Mock
    private Queue.Task task;

    @Mock
    private Executor executor;

    @Before
    public void before() {
        when(cloud.getIdleMinutes()).thenReturn(10);
        when(cloud.hasUnlimitedUsesForNodes()).thenReturn(true);
        when(cloud.isAlwaysReconnect()).thenReturn(false);

        PowerMockito.when(computer.getIdleStartMilliseconds()).thenReturn(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(11));
        when(computer.getNode()).thenReturn(node);
        PowerMockito.when(computer.isIdle()).thenReturn(true);
        when(computer.isAcceptingTasks()).thenReturn(true);
        when(computer.getCloud()).thenReturn(cloud);
        when(computer.isMarkedForDeletion()).thenReturn(false);

        when(node.getNodeName()).thenReturn("n-a");

        when(executor.getOwner()).thenReturn(computer);
    }

    @Test
    public void if_idle_time_not_configured_should_do_nothing() {
        when(cloud.getIdleMinutes()).thenReturn(0);

        new EC2RetentionStrategy().check(computer);

        verify(computer).getNode();
        verify(cloud, never()).scheduleToTerminate(anyString(), anyBoolean(), eq(EC2AgentTerminationReason.IDLE_FOR_TOO_LONG));

        InOrder inOrder = inOrder(computer);
        inOrder.verify(computer).setAcceptingTasks(false);
        inOrder.verify(computer).setAcceptingTasks(true);
    }

    @Test
    public void should_schedule_agent_for_termination_if_marked() {
        when(computer.isIdle()).thenReturn(Boolean.TRUE);
        when(computer.isMarkedForDeletion()).thenReturn(Boolean.TRUE);

        new EC2RetentionStrategy().check(computer);

        verify(computer).getNode();
        verify(cloud).scheduleToTerminate(anyString(), eq(false), eq(EC2AgentTerminationReason.AGENT_DELETED));
        verify(computer).setAcceptingTasks(false);
    }

    @Test
    public void should_schedule_for_termination_if_excess_capacity() {
        when(computer.isIdle()).thenReturn(Boolean.TRUE);
        when(computer.isMarkedForDeletion()).thenReturn(Boolean.FALSE);
        when(cloud.hasExcessCapacity()).thenReturn(Boolean.TRUE);

        new EC2RetentionStrategy().check(computer);

        verify(computer).getNode();
        verify(cloud).scheduleToTerminate(anyString(), eq(false), eq(EC2AgentTerminationReason.EXCESS_CAPACITY));
        verify(computer).setAcceptingTasks(false);
    }

    @Test
    public void should_schedule_for_termination_if_node_has_exhausted_maxTotalUses() {
        when(computer.isIdle()).thenReturn(Boolean.TRUE);
        when(computer.isMarkedForDeletion()).thenReturn(Boolean.FALSE);
        when(cloud.hasExcessCapacity()).thenReturn(Boolean.FALSE);
        when(cloud.hasUnlimitedUsesForNodes()).thenReturn(Boolean.FALSE);
        when(node.getUsesRemaining()).thenReturn(0);

        new EC2RetentionStrategy().check(computer);

        verify(computer).getNode();
        verify(computer).isMarkedForDeletion();
        verify(cloud).hasExcessCapacity();
        verify(cloud).hasUnlimitedUsesForNodes();
        verify(node).getUsesRemaining();
        verify(cloud, never()).getIdleMinutes();
        verify(cloud).scheduleToTerminate(anyString(), eq(true), eq(EC2AgentTerminationReason.MAX_TOTAL_USES_EXHAUSTED));
        verify(computer).setAcceptingTasks(false);
    }

    @Test
    public void should_schedule_for_termination_if_idle_for_too_long() {
        when(computer.isIdle()).thenReturn(Boolean.TRUE);
        when(computer.isMarkedForDeletion()).thenReturn(Boolean.FALSE);
        when(cloud.hasExcessCapacity()).thenReturn(Boolean.FALSE);
        when(cloud.hasUnlimitedUsesForNodes()).thenReturn(Boolean.FALSE);
        when(node.getUsesRemaining()).thenReturn(1);
        when(cloud.getIdleMinutes()).thenReturn(10);

        new EC2RetentionStrategy().check(computer);

        verify(computer).getNode();
        verify(cloud).scheduleToTerminate(anyString(), eq(false), eq(EC2AgentTerminationReason.IDLE_FOR_TOO_LONG));
        verify(computer).setAcceptingTasks(false);
    }

    @Test
    public void should_do_nothing_if_node_is_null() {
        when(cloud.hasExcessCapacity()).thenReturn(Boolean.FALSE);
        when(computer.isIdle()).thenReturn(Boolean.TRUE);
        when(cloud.getIdleMinutes()).thenReturn(1);

        when(computer.getNode()).thenReturn(null);

        new EC2RetentionStrategy().check(computer);

        verify(computer).getNode();
        verify(cloud, never()).scheduleToTerminate(anyString(), anyBoolean(), any(EC2AgentTerminationReason.class));
    }

    @Test
    public void should_NOT_schedule_for_termination_if_busy() {
        when(computer.isIdle()).thenReturn(Boolean.FALSE);

        new EC2RetentionStrategy().check(computer);

        verify(computer, never()).getNode();
        verify(cloud, never()).scheduleToTerminate(anyString(), anyBoolean(), any(EC2AgentTerminationReason.class));

        InOrder inOrder = inOrder(computer);
        inOrder.verify(computer).setAcceptingTasks(false);
        inOrder.verify(computer).setAcceptingTasks(true);
    }
    
    @Test
    public void should_NOT_schedule_for_termination_if_no_conditions_apply() {
        when(computer.isIdle()).thenReturn(Boolean.TRUE);
        when(computer.isMarkedForDeletion()).thenReturn(Boolean.FALSE);
        when(cloud.hasExcessCapacity()).thenReturn(Boolean.FALSE);
        when(cloud.hasUnlimitedUsesForNodes()).thenReturn(Boolean.TRUE);
        when(cloud.getIdleMinutes()).thenReturn(0);

        new EC2RetentionStrategy().check(computer);


        verify(computer).getNode();
        verify(cloud, never()).scheduleToTerminate(anyString(), anyBoolean(), any(EC2AgentTerminationReason.class));

        InOrder inOrder = inOrder(computer);
        inOrder.verify(computer).setAcceptingTasks(false);
        inOrder.verify(computer).setAcceptingTasks(true);
    }

    @Test
    public void if_idle_time_configured_should_do_nothing_if_node_not_idle_for_too_long() {
        when(computer.getIdleStartMilliseconds()).thenReturn(System.currentTimeMillis());

        new EC2RetentionStrategy().check(computer);

        verify(computer).isIdle();
        verify(computer).getNode();
        verify(cloud, never()).scheduleToTerminate(anyString(), anyBoolean(), any(EC2AgentTerminationReason.class));

        InOrder inOrder = inOrder(computer);
        inOrder.verify(computer).setAcceptingTasks(false);
        inOrder.verify(computer).setAcceptingTasks(true);
    }

    @Test
    public void if_node_not_execute_anything_yet_idle_time_negative_do_nothing() {
        when(computer.getIdleStartMilliseconds()).thenReturn(Long.MIN_VALUE);

        new EC2RetentionStrategy().check(computer);

        verify(computer).getNode();
        verify(cloud, never()).scheduleToTerminate(anyString(), anyBoolean(), any(EC2AgentTerminationReason.class));

        InOrder inOrder = inOrder(computer);
        inOrder.verify(computer).setAcceptingTasks(false);
        inOrder.verify(computer).setAcceptingTasks(true);
    }

    @Test
    public void if_idle_time_configured_should_terminate_node_if_idle_time_more_then_allowed() {
        new EC2RetentionStrategy().check(computer);

        verify(cloud).scheduleToTerminate("n-a", false, EC2AgentTerminationReason.IDLE_FOR_TOO_LONG);

        InOrder inOrder = inOrder(computer);
        inOrder.verify(computer).setAcceptingTasks(false);
        inOrder.verify(computer).setAcceptingTasks(true);
    }

    @Test
    public void if_computer_has_no_cloud_should_do_nothing() {
        when(computer.getCloud()).thenReturn(null);

        new EC2RetentionStrategy().check(computer);

        verify(cloud, never()).scheduleToTerminate(anyString(), anyBoolean(), any(EC2AgentTerminationReason.class));
        verify(computer, never()).setAcceptingTasks(anyBoolean());
    }

    @Test
    public void if_node_not_idle_should_do_nothing() {
        when(computer.getIdleStartMilliseconds()).thenReturn(0L);
        when(computer.isIdle()).thenReturn(false);

        new EC2RetentionStrategy().check(computer);

        verify(cloud, never()).scheduleToTerminate(eq("n-a"), eq(false), any(EC2AgentTerminationReason.class));

        InOrder inOrder = inOrder(computer);
        inOrder.verify(computer).setAcceptingTasks(false);
        inOrder.verify(computer).setAcceptingTasks(true);
    }

    @Test
    public void if_node_idle_time_more_them_allowed_but_not_idle_should_do_nothing() {
        when(computer.isIdle()).thenReturn(false);

        new EC2RetentionStrategy().check(computer);

        verify(cloud, never()).scheduleToTerminate(eq("n-a"), eq(false), any(EC2AgentTerminationReason.class));
        InOrder inOrder = inOrder(computer);
        inOrder.verify(computer).setAcceptingTasks(false);
        inOrder.verify(computer).setAcceptingTasks(true);
    }

    @Test
    public void if_exception_happen_during_termination_should_throw_it_and_restore_task_accepting() {
        when(cloud.scheduleToTerminate(anyString(), anyBoolean(), any(EC2AgentTerminationReason.class))).thenThrow(new IllegalArgumentException("test"));

        try {
            new EC2RetentionStrategy().check(computer);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("test", e.getMessage());
            verify(cloud).scheduleToTerminate(eq("n-a"), eq(false), any(EC2AgentTerminationReason.class));
            InOrder inOrder = inOrder(computer);
            inOrder.verify(computer).setAcceptingTasks(false);
            inOrder.verify(computer).setAcceptingTasks(true);
        }
    }

    @Test
    public void when_unlimited_maxTotalUses_should_not_decrement_nor_suspend_computer() {
        when(node.getMaxTotalUses()).thenReturn(-1);

        new EC2RetentionStrategy().taskAccepted(executor, task);

        verify(computer).getNode();
        verify(computer, never()).setAcceptingTasks(false);
    }

    @Test
    public void when_limited_maxTotalUses_and_more_than_one_remainingUses_should_decrement_only() {
        when(node.getMaxTotalUses()).thenReturn(2);
        when(node.getUsesRemaining()).thenReturn(2);

        new EC2RetentionStrategy().taskAccepted(executor, task);

        verify(computer).getNode();
        verify(node, times(2)).getUsesRemaining();
        verify(node).decrementUsesRemaining();
        verify(computer, never()).setAcceptingTasks(false);
    }

    @Test
    public void when_limited_maxTotalUses_and_usesRemaining_is_1_should_decrement_and_stop_accepting_tasks() {
        when(node.getMaxTotalUses()).thenReturn(2);
        when(node.getUsesRemaining()).thenReturn(1);

        new EC2RetentionStrategy().taskAccepted(executor, task);

        verify(computer).getNode();
        verify(computer).setAcceptingTasks(false);
        verify(node).decrementUsesRemaining();
    }

    @Test
    public void when_limited_maxTotalUses_and_usesRemaining_is_0_do_nothing() {
        when(node.getMaxTotalUses()).thenReturn(5);
        when(node.getUsesRemaining()).thenReturn(0);

        new EC2RetentionStrategy().taskAccepted(executor, task);

        verify(computer).getNode();
        verify(node, never()).decrementUsesRemaining();
        verify(computer, never()).setAcceptingTasks(false);
    }

    @Test
    public void when_otherBusyExecutors_should_not_scheduleToTerminate() {
        when(node.getCloud()).thenReturn(cloud);
        when(computer.countBusy()).thenReturn(2);

        new EC2RetentionStrategy().taskCompleted(executor, task, 0L);

        verify(cloud, never()).scheduleToTerminate(anyString(), anyBoolean(), any(EC2AgentTerminationReason.class));
    }

    @Test
    public void when_noOtherBusyExecutors_and_accepting_tasks_should_not_scheduleToTerminate() {
        when(node.getCloud()).thenReturn(cloud);
        when(computer.countBusy()).thenReturn(1);
        when(computer.isAcceptingTasks()).thenReturn(true);

        new EC2RetentionStrategy().taskCompleted(executor, task, 0L);

        verify(cloud, never()).scheduleToTerminate(anyString(), anyBoolean(), any(EC2AgentTerminationReason.class));
    }

    @Test
    public void when_noOtherBusyExecutors_and_not_accepting_tasks_should_scheduleToTerminate() {
        when(node.getCloud()).thenReturn(cloud);
        when(computer.countBusy()).thenReturn(1);
        when(computer.isAcceptingTasks()).thenReturn(false);

        new EC2RetentionStrategy().taskCompleted(executor, task, 0L);

        verify(cloud).scheduleToTerminate("n-a", true, EC2AgentTerminationReason.MAX_TOTAL_USES_EXHAUSTED);
    }
}
