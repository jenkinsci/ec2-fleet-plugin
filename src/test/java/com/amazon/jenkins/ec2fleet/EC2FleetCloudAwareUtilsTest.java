package com.amazon.jenkins.ec2fleet;

import hudson.model.Computer;
import hudson.model.LabelFinder;
import hudson.model.Node;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class, LabelFinder.class})
public class EC2FleetCloudAwareUtilsTest {

    @Mock
    private Jenkins jenkins;

    @Mock
    private EC2FleetCloud cloud;

    @Mock
    private EC2FleetCloud oldCloud;

    @Mock
    private EC2FleetCloud otherCloud;

    @Mock
    private EC2FleetNodeComputer computer;

    @Mock
    private EC2FleetNode node;

    @Before
    public void before() {
        PowerMockito.mockStatic(LabelFinder.class);

        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.when(Jenkins.getActiveInstance()).thenReturn(jenkins);

        when(oldCloud.getOldId()).thenReturn("cloud");
        when(computer.getCloud()).thenReturn(oldCloud);
        when(node.getCloud()).thenReturn(oldCloud);

        when(cloud.getOldId()).thenReturn("cloud");
        when(otherCloud.getOldId()).thenReturn("other");

        when(jenkins.getNodes()).thenReturn(Collections.<Node>emptyList());
        when(jenkins.getComputers()).thenReturn(new Computer[0]);
    }

    @Test
    public void reassign_nothing_if_no_nodes_or_computers() {
        EC2FleetCloudAwareUtils.reassign("cloud", cloud);
    }

    @Test
    public void reassign_nothing_if_computers_belong_to_diff_cloud_id() {
        when(jenkins.getNodes()).thenReturn(Collections.<Node>emptyList());
        when(computer.getCloud()).thenReturn(otherCloud);
        when(jenkins.getComputers()).thenReturn(new Computer[]{computer});

        EC2FleetCloudAwareUtils.reassign("cloud", cloud);

        verify(computer, times(0)).setCloud(any(EC2FleetCloud.class));
    }

    @Test
    public void reassign_nothing_if_computer_cloud_is_null() {
        when(computer.getCloud()).thenReturn(null);
        when(jenkins.getComputers()).thenReturn(new Computer[]{computer});

        EC2FleetCloudAwareUtils.reassign("cloud", cloud);

        verify(computer, times(0)).setCloud(any(EC2FleetCloud.class));
    }

    @Test
    public void reassign_if_computer_belong_to_old_cloud() {
        when(jenkins.getComputers()).thenReturn(new Computer[]{computer});

        EC2FleetCloudAwareUtils.reassign("cloud", cloud);

        verify(computer, times(1)).setCloud(cloud);
    }

    @Test
    public void reassign_if_node_belong_to_same_cloud() {
        when(computer.getCloud()).thenReturn(cloud);
        when(jenkins.getNodes()).thenReturn(Arrays.asList((Node) node));

        EC2FleetCloudAwareUtils.reassign("cloud", cloud);

        verify(node, times(1)).setCloud(cloud);
    }

    @Test
    public void reassign_nothing_if_node_belong_to_other_cloud_id() {
        when(computer.getCloud()).thenReturn(cloud);
        when(node.getCloud()).thenReturn(otherCloud);
        when(jenkins.getNodes()).thenReturn(Arrays.asList((Node) node));

        EC2FleetCloudAwareUtils.reassign("cloud", cloud);

        verify(node, times(0)).setCloud(cloud);
    }

}
