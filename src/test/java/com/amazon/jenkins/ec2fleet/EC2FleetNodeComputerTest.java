package com.amazon.jenkins.ec2fleet;

import hudson.model.Queue;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EC2FleetNodeComputerTest {

    private MockedStatic<Jenkins> mockedJenkins;

    private MockedStatic<Queue> mockedQueue;

    @Mock
    private EC2FleetNode agent;

    @Mock
    private Jenkins jenkins;

    @Mock
    private Queue queue;

    @BeforeEach
    void before() {
        mockedJenkins = Mockito.mockStatic(Jenkins.class);
        mockedJenkins.when(Jenkins::get).thenReturn(jenkins);

        mockedQueue = Mockito.mockStatic(Queue.class);
        mockedQueue.when(Queue::getInstance).thenReturn(queue);

        when(agent.getNumExecutors()).thenReturn(1);
    }

    @AfterEach
    void after() {
        mockedQueue.close();
        mockedJenkins.close();
    }

    @Test
    void getDisplayName_returns_node_display_name_for_default_maxTotalUses() {
        when(agent.getDisplayName()).thenReturn("a n");
        when(agent.getUsesRemaining()).thenReturn(-1);

        EC2FleetNodeComputer computer = spy(new EC2FleetNodeComputer(agent));
        doReturn(agent).when(computer).getNode();

        assertEquals("a n", computer.getDisplayName());
    }

    @Test
    void getDisplayName_returns_builds_left_for_non_default_maxTotalUses() {
        when(agent.getDisplayName()).thenReturn("a n");
        when(agent.getUsesRemaining()).thenReturn(1);

        EC2FleetNodeComputer computer = spy(new EC2FleetNodeComputer(agent));
        doReturn(agent).when(computer).getNode();

        assertEquals("a n Builds left: 1 ", computer.getDisplayName());
    }

}
