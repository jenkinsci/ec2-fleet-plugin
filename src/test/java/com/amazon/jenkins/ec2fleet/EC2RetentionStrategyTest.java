package com.amazon.jenkins.ec2fleet;

import hudson.model.Executor;
import hudson.model.Queue;
import hudson.slaves.SlaveComputer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
        PowerMockito.when(computer.getIdleStartMilliseconds()).thenReturn(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(11));
        when(computer.getNode()).thenReturn(node);
        PowerMockito.when(computer.isIdle()).thenReturn(true);
        when(node.getNodeName()).thenReturn("n-a");
        when(computer.isAcceptingTasks()).thenReturn(true);
        when(computer.getCloud()).thenReturn(cloud);
        when(executor.getOwner()).thenReturn(computer);
    }

    @Test
    public void if_idle_time_not_configured_should_do_nothing() {
        when(cloud.getIdleMinutes()).thenReturn(0);

        new EC2RetentionStrategy().check(computer);

        verify(computer, times(0)).getNode();
        verify(cloud, times(0)).scheduleToTerminate(anyString(), anyBoolean(), eq(EC2AgentTerminationReason.IDLE_FOR_TOO_LONG));
        verify(computer).setAcceptingTasks(false);
        verify(computer).setAcceptingTasks(true);
    }

    @Test
    public void shouldScheduleExcessCapacityForTerminationIfIdle() {
        when(computer.isIdle()).thenReturn(Boolean.TRUE);
        when(cloud.hasExcessCapacity()).thenReturn(Boolean.TRUE);
        when(cloud.getIdleMinutes()).thenReturn(20);

        new EC2RetentionStrategy().check(computer);

        verify(computer, times(1)).getNode();
        verify(cloud, times(1)).scheduleToTerminate(anyString(), anyBoolean(), eq(EC2AgentTerminationReason.EXCESS_CAPACITY));
        verify(computer).setAcceptingTasks(false);
    }

    @Test
    public void shouldNotScheduleExcessCapacityForTerminationIfBusy() {
        when(cloud.hasExcessCapacity()).thenReturn(Boolean.TRUE);
        when(computer.isIdle()).thenReturn(Boolean.FALSE);

        new EC2RetentionStrategy().check(computer);

        verify(computer, times(0)).getNode();
        verify(cloud, times(0)).scheduleToTerminate(anyString(), anyBoolean(), any(EC2AgentTerminationReason.class));
        verify(computer).setAcceptingTasks(true);
    }

    @Test
    public void shouldNotScheduleTerminationIfNotExcessCapacity() {
        when(cloud.hasExcessCapacity()).thenReturn(Boolean.FALSE);
        when(computer.isIdle()).thenReturn(Boolean.TRUE);
        when(cloud.getIdleMinutes()).thenReturn(0);

        new EC2RetentionStrategy().check(computer);

        verify(computer, times(0)).getNode();
        verify(cloud, times(0)).scheduleToTerminate(anyString(), anyBoolean(), any(EC2AgentTerminationReason.class));
        verify(computer).setAcceptingTasks(true);
    }

    @Test
    public void should_do_nothing_if_node_is_null() {
        when(cloud.hasExcessCapacity()).thenReturn(Boolean.FALSE);
        when(computer.isIdle()).thenReturn(Boolean.TRUE);
        when(cloud.getIdleMinutes()).thenReturn(1);

        when(computer.getNode()).thenReturn(null);

        new EC2RetentionStrategy().check(computer);

        verify(computer, times(1)).getNode();
        verify(cloud, never()).scheduleToTerminate(anyString(), anyBoolean(), any(EC2AgentTerminationReason.class));
    }

    @Test
    public void if_idle_time_configured_should_do_nothing_if_node_idle_less_time() {
        when(computer.getIdleStartMilliseconds()).thenReturn(System.currentTimeMillis());

        new EC2RetentionStrategy().check(computer);

        verify(computer, never()).getNode();
        verify(cloud, never()).scheduleToTerminate(anyString(), anyBoolean(), any(EC2AgentTerminationReason.class));
        verify(computer).setAcceptingTasks(false);
        verify(computer).setAcceptingTasks(true);
    }

    @Test
    public void if_node_not_execute_anything_yet_idle_time_negative_do_nothing() {
        when(computer.getIdleStartMilliseconds()).thenReturn(Long.MIN_VALUE);

        new EC2RetentionStrategy().check(computer);

        verify(computer, times(0)).getNode();
        verify(cloud, times(0)).scheduleToTerminate(anyString(), anyBoolean(), any(EC2AgentTerminationReason.class));
        verify(computer).setAcceptingTasks(false);
        verify(computer).setAcceptingTasks(true);
    }

    @Test
    public void if_idle_time_configured_should_terminate_node_if_idle_time_more_then_allowed() {
        new EC2RetentionStrategy().check(computer);

        verify(cloud, times(1)).scheduleToTerminate("n-a", false, EC2AgentTerminationReason.IDLE_FOR_TOO_LONG);
        verify(computer, times(1)).setAcceptingTasks(true);
        verify(computer, times(1)).setAcceptingTasks(false);
    }

    @Test
    public void if_computer_has_no_cloud_should_do_nothing() {
        when(computer.getCloud()).thenReturn(null);

        new EC2RetentionStrategy().check(computer);

        verify(cloud, times(0)).scheduleToTerminate(anyString(), anyBoolean(), any(EC2AgentTerminationReason.class));
        verify(computer, times(0)).setAcceptingTasks(true);
        verify(computer, times(0)).setAcceptingTasks(false);
    }

    @Test
    public void if_node_not_idle_should_do_nothing() {
        when(computer.getIdleStartMilliseconds()).thenReturn(0L);
        when(computer.isIdle()).thenReturn(false);

        new EC2RetentionStrategy().check(computer);

        verify(cloud, never()).scheduleToTerminate(eq("n-a"), eq(false), any(EC2AgentTerminationReason.class));
        verify(computer, times(1)).setAcceptingTasks(true);
        verify(computer, times(1)).setAcceptingTasks(false);
    }

    @Test
    public void if_node_idle_time_more_them_allowed_but_not_idle_should_do_nothing() {
        when(computer.isIdle()).thenReturn(false);

        new EC2RetentionStrategy().check(computer);

        verify(cloud, never()).scheduleToTerminate(eq("n-a"), eq(false), any(EC2AgentTerminationReason.class));
        verify(computer, times(1)).setAcceptingTasks(true);
        verify(computer, times(1)).setAcceptingTasks(false);
    }

    @Test
    public void if_exception_happen_during_termination_should_throw_it_and_restore_task_accepting() {
        when(cloud.scheduleToTerminate(anyString(), anyBoolean(), any(EC2AgentTerminationReason.class))).thenThrow(new IllegalArgumentException("test"));

        try {
            new EC2RetentionStrategy().check(computer);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("test", e.getMessage());
            verify(cloud, times(1)).scheduleToTerminate(eq("n-a"), eq(false), any(EC2AgentTerminationReason.class));
            verify(computer).setAcceptingTasks(false);
            verify(computer).setAcceptingTasks(true);
        }
    }

    @Test
    public void when_unlimited_maxTotalUses_should_not_decrement() {
        when(node.getMaxTotalUses()).thenReturn(-1);

        new EC2RetentionStrategy().taskAccepted(executor, task);

        verify(computer, never()).setAcceptingTasks(false);
    }

    @Test
    public void when_maxTotalUses_greater_than_1_should_decrement() {
        when(node.getMaxTotalUses()).thenReturn(2);

        new EC2RetentionStrategy().taskAccepted(executor, task);

        verify(node).setMaxTotalUses(1);
        verify(computer, never()).setAcceptingTasks(false);
    }

    @Test
    public void when_maxTotalUses_is_1_should_stop_accepting_tasks_and_not_decrement() {
        when(node.getMaxTotalUses()).thenReturn(1);

        new EC2RetentionStrategy().taskAccepted(executor, task);

        verify(computer, times(1)).setAcceptingTasks(false);
        verify(node, never()).setMaxTotalUses(anyInt());
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

        verify(cloud, times(1)).scheduleToTerminate("n-a", true, EC2AgentTerminationReason.MAX_TOTAL_USES_EXHAUSTED);
    }
}
