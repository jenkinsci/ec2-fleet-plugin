package com.amazon.jenkins.ec2fleet;

import hudson.model.Queue;
import jenkins.model.Jenkins;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EC2FleetNodeComputerTest {

    private MockedStatic<Jenkins> mockedJenkins;

    private MockedStatic<Queue> mockedQueue;

    @Mock
    private EC2FleetNode agent;

    @Mock
    private Jenkins jenkins;

    @Mock
    private Queue queue;

    @Before
    public void before() {
        mockedJenkins = Mockito.mockStatic(Jenkins.class);
        mockedJenkins.when(Jenkins::get).thenReturn(jenkins);

        mockedQueue = Mockito.mockStatic(Queue.class);
        mockedQueue.when(Queue::getInstance).thenReturn(queue);

        when(agent.getNumExecutors()).thenReturn(1);
    }

    @After
    public void after() {
        mockedQueue.close();
        mockedJenkins.close();
    }

    @Test
    public void getDisplayName_returns_node_display_name_for_default_maxTotalUses() {
        when(agent.getDisplayName()).thenReturn("a n");
        when(agent.getUsesRemaining()).thenReturn(-1);

        EC2FleetNodeComputer computer = spy(new EC2FleetNodeComputer(agent));
        doReturn(agent).when(computer).getNode();

        Assert.assertEquals("a n", computer.getDisplayName());
    }

    @Test
    public void getDisplayName_returns_builds_left_for_non_default_maxTotalUses() {
        when(agent.getDisplayName()).thenReturn("a n");
        when(agent.getUsesRemaining()).thenReturn(1);

        EC2FleetNodeComputer computer = spy(new EC2FleetNodeComputer(agent));
        doReturn(agent).when(computer).getNode();

        Assert.assertEquals("a n Builds left: 1 ", computer.getDisplayName());
    }

}
