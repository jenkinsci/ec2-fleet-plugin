package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.cloud.FleetNode;
import com.amazon.jenkins.ec2fleet.cloud.FleetNodeComputer;
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
public class FleetNodeComputerTest {

    @Mock
    private Slave slave;

    @Mock
    private Jenkins jenkins;

    @Mock
    private FleetNode fleetNode;

    @Before
    public void before() {
        PowerMockito.mockStatic(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(jenkins);

        when(slave.getNumExecutors()).thenReturn(1);

        when(fleetNode.getDisplayName()).thenReturn("fleet node name");
    }

    @Test
    public void get_display_name_computer_node_removed_returns_predefined() {
        FleetNodeComputer computer = spy(new FleetNodeComputer(slave));
        doReturn(null).when(computer).getNode();
        Assert.assertEquals("removing fleet node", computer.getDisplayName());
    }

    @Test
    public void get_display_name_returns_node_display_name() {
        FleetNodeComputer computer = spy(new FleetNodeComputer(slave));
        doReturn(fleetNode).when(computer).getNode();
        Assert.assertEquals("fleet node name", computer.getDisplayName());
    }

}
