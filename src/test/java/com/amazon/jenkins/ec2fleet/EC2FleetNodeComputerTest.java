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
    private EC2FleetCloud cloud;

    @Before
    public void before() {
        PowerMockito.mockStatic(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(jenkins);
        when(Queue.getInstance()).thenReturn(queue);

        when(slave.getNumExecutors()).thenReturn(1);
    }

    @Test
    public void getDisplayName_should_be_ok_with_init_null_cloud() {
        EC2FleetNodeComputer computer = spy(new EC2FleetNodeComputer(slave, "a", null));
        Assert.assertEquals("unknown fleet a", computer.getDisplayName());
    }

    @Test
    public void getDisplayName_should_be_ok_with_set_null_cloud() {
        EC2FleetNodeComputer computer = spy(new EC2FleetNodeComputer(slave, "a", cloud));
        computer.setCloud(null);
        Assert.assertEquals("unknown fleet a", computer.getDisplayName());
    }

    @Test
    public void getDisplayName_returns_node_display_name() {
        when(cloud.getDisplayName()).thenReturn("a");
        EC2FleetNodeComputer computer = spy(new EC2FleetNodeComputer(slave, "n", cloud));
        Assert.assertEquals("a n", computer.getDisplayName());
    }

}
