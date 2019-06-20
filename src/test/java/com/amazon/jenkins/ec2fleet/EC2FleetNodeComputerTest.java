package com.amazon.jenkins.ec2fleet;

import hudson.model.Queue;
import hudson.model.Slave;
import hudson.slaves.OfflineCause;
import jenkins.model.Jenkins;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class, Queue.class})
public class EC2FleetNodeComputerTest {

    @Mock
    private Slave slave;

    @Mock
    private Jenkins jenkins;

    @Mock
    private Queue queue;

    @Mock
    private Queue.Task task;

    @Mock
    private EC2FleetNode fleetNode;

    @Before
    public void before() {
        PowerMockito.mockStatic(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(jenkins);
        when(Queue.getInstance()).thenReturn(queue);

        when(slave.getNumExecutors()).thenReturn(1);

        when(fleetNode.getDisplayName()).thenReturn("fleet node name");
    }

    @Test
    public void getDisplayName_computer_node_removed_returns_predefined() {
        EC2FleetNodeComputer computer = spy(new EC2FleetNodeComputer(slave));
        doReturn(null).when(computer).getNode();
        Assert.assertEquals("removing fleet node", computer.getDisplayName());
    }

    @Test
    public void getDisplayName_returns_node_display_name() {
        EC2FleetNodeComputer computer = spy(new EC2FleetNodeComputer(slave));
        doReturn(fleetNode).when(computer).getNode();
        Assert.assertEquals("fleet node name", computer.getDisplayName());
    }

    @Test
    public void taskCompleted_should_do_nothing_if_task_finished_without_cause() {
        EC2FleetNodeComputer computer = spy(new EC2FleetNodeComputer(slave));
        computer.taskCompleted(null, task, 0);
        verifyZeroInteractions(queue);
    }

    @Test
    public void taskCompleted_should_do_nothing_if_task_finished_offline_but_no_cause() {
        EC2FleetNodeComputer computer = spy(new EC2FleetNodeComputer(slave));
        when(computer.isOffline()).thenReturn(true);
        computer.taskCompleted(null, task, 0);
        verifyZeroInteractions(queue);
    }

    @Test
    public void taskCompleted_should_do_nothing_if_task_finished_cause_but_still_online() {
        EC2FleetNodeComputer computer = spy(new EC2FleetNodeComputer(slave));
        when(computer.isOffline()).thenReturn(false);
        when(computer.getOfflineCause()).thenReturn(new OfflineCause.ChannelTermination(null));
        computer.taskCompleted(null, task, 0);
        verifyZeroInteractions(queue);
    }

    @Test
    public void taskCompleted_should_resubmit_task_if_offline_and_cause_disconnect() {
        EC2FleetNodeComputer computer = spy(new EC2FleetNodeComputer(slave));
        when(computer.isOffline()).thenReturn(true);
        when(computer.getOfflineCause()).thenReturn(new OfflineCause.ChannelTermination(null));
        computer.taskCompleted(null, task, 0);
        verify(queue).schedule(eq(task), anyInt());
    }

}
