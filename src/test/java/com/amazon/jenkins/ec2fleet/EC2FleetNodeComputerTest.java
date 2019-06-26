package com.amazon.jenkins.ec2fleet;

import hudson.model.Queue;
import hudson.model.Slave;
import jenkins.model.Jenkins;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class})
public class EC2FleetNodeComputerTest {

    @Mock
    private Slave slave;

    @Mock
    private Jenkins jenkins;

    @Mock
    private Queue queue;

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

}
