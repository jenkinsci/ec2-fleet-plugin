package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.aws.EC2Api;
import com.amazon.jenkins.ec2fleet.fleet.Fleet;
import com.amazon.jenkins.ec2fleet.fleet.Fleets;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import hudson.model.Node;
import hudson.model.queue.QueueTaskFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EC2RetentionStrategyIntegrationTest extends IntegrationTest {

    private AmazonEC2 amazonEC2;

    @Before
    public void before() {
        final Fleet fleet = mock(Fleet.class);
        Fleets.setGet(fleet);
        final EC2Api ec2Api = spy(EC2Api.class);
        Registry.setEc2Api(ec2Api);
        amazonEC2 = mock(AmazonEC2.class);

        when(fleet.getState(anyString(), anyString(), nullable(String.class), anyString()))
                .thenReturn(new FleetStateStats("", 2, FleetStateStats.State.active(), new HashSet<>(Arrays.asList("i-1", "i-2")), Collections.emptyMap()));
        when(ec2Api.connect(anyString(), anyString(), Mockito.nullable(String.class))).thenReturn(amazonEC2);

        final Instance instance = new Instance()
                .withState(new InstanceState().withName(InstanceStateName.Running))
                .withPublicIpAddress("public-io")
                .withInstanceId("i-1");
        final Instance instance1 = new Instance()
                .withState(new InstanceState().withName(InstanceStateName.Running))
                .withPublicIpAddress("public-io")
                .withInstanceId("i-2");

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(
                new DescribeInstancesResult().withReservations(
                        new Reservation().withInstances(
                                instance, instance1
                        )));
        when(amazonEC2.terminateInstances(any(TerminateInstancesRequest.class))).thenReturn(new TerminateInstancesResult());
    }

    @Test
    public void shouldTerminateNodeMarkedForDeletion() throws Exception {
        final FleetCloud cloud = new FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, new LocalComputerConnector(j), false, false,
                1, 0, 0, 0, 1, false, true, "-1", false, 0, 0, false, 999, false);
        // Set initial jenkins nodes
        cloud.update();
        j.jenkins.clouds.add(cloud);

        assertAtLeastOneNode();

        FleetNode node = (FleetNode) j.jenkins.getNode("i-1");
        FleetNodeComputer c = (FleetNodeComputer) node.toComputer();
        c.doDoDelete(); // mark node for termination
        node.getRetentionStrategy().check(c);

        // Make sure the scheduled for termination instances are terminated
        cloud.update();

        final ArgumentCaptor<TerminateInstancesRequest> argument = ArgumentCaptor.forClass(TerminateInstancesRequest.class);
        verify(amazonEC2, times(1)).terminateInstances(argument.capture());
        assertTrue(argument.getAllValues().get(0).getInstanceIds().containsAll(Arrays.asList("i-1")));
    }

    @Test
    public void shouldTerminateExcessCapacity() throws Exception {
        final FleetCloud cloud = new FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, new LocalComputerConnector(j), false, false,
                1, 0, 0, 0, 1, false, true, "-1", false, 0, 0, false, 999, false);
        // Set initial jenkins nodes
        cloud.update();
        j.jenkins.clouds.add(cloud);

        assertAtLeastOneNode();

        final ArgumentCaptor<TerminateInstancesRequest> argument = ArgumentCaptor.forClass(TerminateInstancesRequest.class);

        // Nodes take a minute to become idle
        Thread.sleep(1000 * 61);
        // Manually trigger the retention check because it's super flaky whether it actually gets triggered
        for (final Node node : j.jenkins.getNodes()) {
            if (node instanceof FleetNode && ((FleetNode) node).getCloud() == cloud) {
                FleetNodeComputer computer = (FleetNodeComputer) ((FleetNode) node).getComputer();
                new EC2RetentionStrategy().check(computer);
            }
        }

        // Make sure the scheduled for termination instances are terminated
        cloud.update();

        verify((amazonEC2), times(1)).terminateInstances(argument.capture());

        final List<String> instanceIds = new ArrayList<String>();
        instanceIds.add("i-2");
        instanceIds.add("i-1");

        assertTrue(argument.getAllValues().get(0).getInstanceIds().containsAll(instanceIds));
    }

    @Test
    public void shouldNotTerminateExcessCapacityWhenNodeIsBusy() throws Exception {
        // Keep a busy queue
        List<QueueTaskFuture> rs = enqueTask(10, 90);
        triggerSuggestReviewNow();

        FleetCloud cloud = new FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, new LocalComputerConnector(j), false, false,
                1, 2, 2, 0, 1, false, true, "-1", false, 0, 0, false, 999, false);
        j.jenkins.clouds.add(cloud);
        cloud.update();

        assertAtLeastOneNode();
        cloud = new FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, new LocalComputerConnector(j), false, false,
                1, 0, 0, 0, 1, false, true, "-1", false, 0, 0, false, 99, false);
        j.jenkins.clouds.clear();
        j.jenkins.clouds.add(cloud);
        assertAtLeastOneNode();
        cloud.update();

        // Nodes take a minute to become idle
        Thread.sleep(1000 * 61);
        // Manually trigger the retention check because it's super flaky whether it actually gets triggered
        for (final Node node : j.jenkins.getNodes()) {
            if (node instanceof FleetNode && ((FleetNode) node).getCloud() == cloud) {
                FleetNodeComputer computer = (FleetNodeComputer) ((FleetNode) node).getComputer();
                new EC2RetentionStrategy().check(computer);
            }
        }
        cloud.update();

        verify((amazonEC2), times(0)).terminateInstances(any());
        cancelTasks(rs);
    }

    @Test
    public void shouldTerminateIdleNodesAfterIdleTimeout() throws Exception {
        final FleetCloud cloud = new FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, new LocalComputerConnector(j), false, false,
                1, 0, 2, 0, 1, false, true, "-1", false, 0, 0, false, 99, false);
        j.jenkins.clouds.add(cloud);
        cloud.update();

        assertAtLeastOneNode();

        final ArgumentCaptor<TerminateInstancesRequest> argument = ArgumentCaptor.forClass(TerminateInstancesRequest.class);

        // Nodes take a minute to become idle
        Thread.sleep(1000 * 61);
        // Manually trigger the retention check because it's super flaky whether it actually gets triggered
        for (final Node node : j.jenkins.getNodes()) {
            if (node instanceof FleetNode && ((FleetNode) node).getCloud() == cloud) {
                FleetNodeComputer computer = (FleetNodeComputer) ((FleetNode) node).getComputer();
                new EC2RetentionStrategy().check(computer);
            }
        }
        cloud.update();

        verify((amazonEC2), times(1)).terminateInstances(argument.capture());

        final List<String> instanceIds = new ArrayList<String>();
        instanceIds.add("i-2");
        instanceIds.add("i-1");
        assertTrue(argument.getAllValues().get(0).getInstanceIds().containsAll(instanceIds));
    }

    @Test
    public void shouldNotTerminateBelowMinSize() throws Exception {
        final FleetCloud cloud = new FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, new LocalComputerConnector(j), false, false,
                1, 2, 5, 0, 1, false, true, "-1", false, 0, 0, false, 30, false);
        j.jenkins.clouds.add(cloud);
        cloud.update();

        assertAtLeastOneNode();

        // Nodes take a minute to become idle
        Thread.sleep(1000 * 61);
        // Manually trigger the retention check because it's super flaky whether it actually gets triggered
        for (final Node node : j.jenkins.getNodes()) {
            if (node instanceof FleetNode && ((FleetNode) node).getCloud() == cloud) {
                FleetNodeComputer computer = (FleetNodeComputer) ((FleetNode) node).getComputer();
                new EC2RetentionStrategy().check(computer);
            }
        }
        cloud.update();

        verify((amazonEC2), times(0)).terminateInstances(any());
    }

    @Test
    public void shouldNotTerminateBelowMinSpareSize() throws Exception {
        final FleetCloud cloud = new FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, new LocalComputerConnector(j), false, false,
                1, 0, 5, 2, 1, false, true, "-1", false, 0, 0, false, 30, false);
        j.jenkins.clouds.add(cloud);
        cloud.update();

        assertAtLeastOneNode();

        // Nodes take a minute to become idle
        Thread.sleep(1000 * 61);
        // Manually trigger the retention check because it's super flaky whether it actually gets triggered
        for (final Node node : j.jenkins.getNodes()) {
            if (node instanceof FleetNode && ((FleetNode) node).getCloud() == cloud) {
                FleetNodeComputer computer = (FleetNodeComputer) ((FleetNode) node).getComputer();
                new EC2RetentionStrategy().check(computer);
            }
        }
        cloud.update();

        verify((amazonEC2), times(0)).terminateInstances(any());
    }

    @Test
    public void shouldTerminateWhenMaxTotalUsesIsExhausted() throws Exception {
        final String label = "momo";
        final int numTasks = 4; // schedule a total of 4 tasks, 2 per instance
        final int maxTotalUses = 2;
        final int taskSleepTime = 1;

        FleetCloud cloud = spy(new FleetCloud("testCloud", "credId", null, "region",
                null, "fId", label, null, new LocalComputerConnector(j), false, false,
                0, 0, 10, 0, 1, false, true,
                String.valueOf(maxTotalUses), true, 0, 0, false, 10, false));
        j.jenkins.clouds.add(cloud);
        cloud.update();
        assertAtLeastOneNode();

        System.out.println("*** scheduling tasks ***");
        waitJobSuccessfulExecution(enqueTask(numTasks, taskSleepTime));
        Thread.sleep(3000); // sleep for a bit to make sure post job actions finish and the computers are idle

        // make sure the instances scheduled for termination are terminated
        cloud.update();

        final ArgumentCaptor<TerminateInstancesRequest> argument = ArgumentCaptor.forClass(TerminateInstancesRequest.class);
        verify((amazonEC2)).terminateInstances(argument.capture());
        assertTrue(argument.getAllValues().get(0).getInstanceIds().containsAll(Arrays.asList("i-1", "i-2")));
    }
}
