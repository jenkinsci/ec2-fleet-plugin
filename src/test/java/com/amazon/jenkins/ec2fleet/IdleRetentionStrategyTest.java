package com.amazon.jenkins.ec2fleet;

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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SlaveComputer.class)
public class IdleRetentionStrategyTest {

    @Mock
    private EC2FleetCloud cloud;

    @Mock
    private EC2FleetNodeComputer slaveComputer;

    @Mock
    private EC2FleetNode slave;

    @Before
    public void before() {
        when(cloud.getIdleMinutes()).thenReturn(10);
        PowerMockito.when(slaveComputer.getIdleStartMilliseconds()).thenReturn(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(11));
        when(slaveComputer.getNode()).thenReturn(slave);
        PowerMockito.when(slaveComputer.isIdle()).thenReturn(true);
        when(slave.getNodeName()).thenReturn("n-a");
        when(slaveComputer.isAcceptingTasks()).thenReturn(true);
        when(slaveComputer.getCloud()).thenReturn(cloud);
    }

    @Test
    public void if_idle_time_not_configured_should_do_nothing() {
        when(cloud.getIdleMinutes()).thenReturn(0);

        new IdleRetentionStrategy().check(slaveComputer);

        verify(slaveComputer, times(0)).getNode();
        verify(cloud, times(0)).terminateInstance(anyString());
        verify(slaveComputer).setAcceptingTasks(false);
        verify(slaveComputer).setAcceptingTasks(true);
    }

    @Test
    public void if_idle_time_configured_should_do_nothing_if_node_idle_less_time() {
        when(slaveComputer.getIdleStartMilliseconds()).thenReturn(System.currentTimeMillis());

        new IdleRetentionStrategy().check(slaveComputer);

        verify(slaveComputer, never()).getNode();
        verify(cloud, never()).terminateInstance(anyString());
        verify(slaveComputer).setAcceptingTasks(false);
        verify(slaveComputer).setAcceptingTasks(true);
    }

    @Test
    public void if_node_not_execute_anything_yet_idle_time_negative_do_nothing() {
        when(slaveComputer.getIdleStartMilliseconds()).thenReturn(Long.MIN_VALUE);

        new IdleRetentionStrategy().check(slaveComputer);

        verify(slaveComputer, times(0)).getNode();
        verify(cloud, times(0)).terminateInstance(anyString());
        verify(slaveComputer).setAcceptingTasks(false);
        verify(slaveComputer).setAcceptingTasks(true);
    }

    @Test
    public void if_idle_time_configured_should_terminate_node_if_idle_time_more_then_allowed() {
        new IdleRetentionStrategy().check(slaveComputer);

        verify(cloud, times(1)).terminateInstance("n-a");
        verify(slaveComputer, times(1)).setAcceptingTasks(true);
        verify(slaveComputer, times(1)).setAcceptingTasks(false);
    }

    @Test
    public void if_computer_has_no_cloud_should_do_nothing() {
        when(slaveComputer.getCloud()).thenReturn(null);

        new IdleRetentionStrategy().check(slaveComputer);

        verify(cloud, times(0)).terminateInstance(anyString());
        verify(slaveComputer, times(0)).setAcceptingTasks(true);
        verify(slaveComputer, times(0)).setAcceptingTasks(false);
    }

    @Test
    public void if_node_not_idle_should_do_nothing() {
        when(slaveComputer.getIdleStartMilliseconds()).thenReturn(0L);
        when(slaveComputer.isIdle()).thenReturn(false);

        new IdleRetentionStrategy().check(slaveComputer);

        verify(cloud, never()).terminateInstance("n-a");
        verify(slaveComputer, times(1)).setAcceptingTasks(true);
        verify(slaveComputer, times(1)).setAcceptingTasks(false);
    }

    @Test
    public void if_node_idle_time_more_them_allowed_but_not_idle_should_do_nothing() {
        when(slaveComputer.isIdle()).thenReturn(false);

        new IdleRetentionStrategy().check(slaveComputer);

        verify(cloud, never()).terminateInstance("n-a");
        verify(slaveComputer, times(1)).setAcceptingTasks(true);
        verify(slaveComputer, times(1)).setAcceptingTasks(false);
    }

    @Test
    public void if_exception_happen_during_termination_should_throw_it_and_restore_task_accepting() {
        when(cloud.terminateInstance(anyString())).thenThrow(new IllegalArgumentException("test"));

        try {
            new IdleRetentionStrategy().check(slaveComputer);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("test", e.getMessage());
            verify(cloud, times(1)).terminateInstance("n-a");
            verify(slaveComputer).setAcceptingTasks(false);
            verify(slaveComputer).setAcceptingTasks(true);
        }
    }

}
