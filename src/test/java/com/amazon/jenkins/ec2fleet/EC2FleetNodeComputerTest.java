package com.amazon.jenkins.ec2fleet;

import hudson.model.Queue;
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
@PrepareForTest({Jenkins.class, Queue.class})
public class EC2FleetNodeComputerTest {

    @Mock
    private EC2FleetNode agent;

    @Mock
    private Jenkins jenkins;

    @Mock
    private Queue queue;

    @Before
    public void before() {
        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.mockStatic(Queue.class);
        when(Jenkins.get()).thenReturn(jenkins);
        when(Queue.getInstance()).thenReturn(queue);

        when(agent.getNumExecutors()).thenReturn(1);
    }

    @Test
    public void getDisplayName_returns_node_display_name_for_default_maxTotalUses() {
        when(agent.getDisplayName()).thenReturn("a n");
        when(agent.getMaxTotalUses()).thenReturn(-1);

        EC2FleetNodeComputer computer = spy(new EC2FleetNodeComputer(agent));
        doReturn(agent).when(computer).getNode();

        Assert.assertEquals("a n", computer.getDisplayName());
    }

    @Test
    public void getDisplayName_returns_builds_left_for_non_default_maxTotalUses() {
        when(agent.getDisplayName()).thenReturn("a n");
        when(agent.getMaxTotalUses()).thenReturn(1);

        EC2FleetNodeComputer computer = spy(new EC2FleetNodeComputer(agent));
        doReturn(agent).when(computer).getNode();

        Assert.assertEquals("a n Builds left: 1 ", computer.getDisplayName());
    }

}
