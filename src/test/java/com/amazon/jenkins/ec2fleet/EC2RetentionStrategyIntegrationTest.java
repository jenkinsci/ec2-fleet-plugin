package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.aws.EC2Api;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleet;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleets;
import hudson.model.Node;
import hudson.model.queue.QueueTaskFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class EC2RetentionStrategyIntegrationTest extends IntegrationTest {

    private final EC2FleetCloud.ExecutorScaler noScaling = new EC2FleetCloud.NoScaler();

    private Ec2Client amazonEC2;

    @BeforeEach
    void before() {
        final EC2Fleet ec2Fleet = mock(EC2Fleet.class);
        EC2Fleets.setGet(ec2Fleet);
        final EC2Api ec2Api = spy(EC2Api.class);
        Registry.setEc2Api(ec2Api);
        amazonEC2 = mock(Ec2Client.class);

        when(ec2Fleet.getState(anyString(), anyString(), nullable(String.class), anyString()))
                .thenReturn(new FleetStateStats("", 2, FleetStateStats.State.active(), new HashSet<>(Arrays.asList("i-1", "i-2")), Collections.emptyMap()));
        when(ec2Api.connect(anyString(), anyString(), Mockito.nullable(String.class))).thenReturn(amazonEC2);

        final Instance instance = Instance.builder()
                .state(InstanceState.builder().name(InstanceStateName.RUNNING)
                        .build())
                .publicIpAddress("public-io")
                .instanceId("i-1")
                .build();
        final Instance instance1 = Instance.builder()
                .state(InstanceState.builder().name(InstanceStateName.RUNNING)
                        .build())
                .publicIpAddress("public-io")
                .instanceId("i-2")
                .build();

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(
                DescribeInstancesResponse.builder().reservations(
                        Reservation.builder().instances(
                                instance, instance1
                        )
                        .build())
                .build());
        when(amazonEC2.terminateInstances(any(TerminateInstancesRequest.class))).thenReturn(TerminateInstancesResponse.builder()
                .build());
    }

    @Test
    void shouldTerminateNodeMarkedForDeletion() throws Exception {
        final EC2FleetCloud cloud = new EC2FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, new LocalComputerConnector(j), false, false,
                1, 0, 0, 0, 1, false, true, "-1", false, 0, 0, 999, false, false, noScaling);
        // Set initial jenkins nodes
        cloud.update();
        j.jenkins.clouds.add(cloud);

        assertAtLeastOneNode();

        EC2FleetNode node = (EC2FleetNode) j.jenkins.getNode("i-1");
        EC2FleetNodeComputer c = (EC2FleetNodeComputer) node.toComputer();
        c.doDoDelete(); // mark node for termination
        node.getRetentionStrategy().check(c);

        // Make sure the scheduled for termination instances are terminated
        cloud.update();

        final ArgumentCaptor<TerminateInstancesRequest> argument = ArgumentCaptor.forClass(TerminateInstancesRequest.class);
        verify(amazonEC2, times(1)).terminateInstances(argument.capture());
        assertTrue(argument.getAllValues().get(0).instanceIds().containsAll(Arrays.asList("i-1")));
    }

    @Test
    void shouldTerminateExcessCapacity() throws Exception {
        final EC2FleetCloud cloud = new EC2FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, new LocalComputerConnector(j), false, false,
                1, 0, 0, 0, 1, false, true, "-1", false, 0, 0, 999, false, false, noScaling);
        // Set initial jenkins nodes
        cloud.update();
        j.jenkins.clouds.add(cloud);

        assertAtLeastOneNode();

        final ArgumentCaptor<TerminateInstancesRequest> argument = ArgumentCaptor.forClass(TerminateInstancesRequest.class);

        // Nodes take a minute to become idle
        Thread.sleep(1000 * 61);
        // Manually trigger the retention check because it's super flaky whether it actually gets triggered
        for (final Node node : j.jenkins.getNodes()) {
            if (node instanceof EC2FleetNode && ((EC2FleetNode) node).getCloud() == cloud) {
                EC2FleetNodeComputer computer = (EC2FleetNodeComputer) ((EC2FleetNode) node).getComputer();
                new EC2RetentionStrategy().check(computer);
            }
        }

        // Make sure the scheduled for termination instances are terminated
        cloud.update();

        verify((amazonEC2), times(1)).terminateInstances(argument.capture());

        final List<String> instanceIds = new ArrayList<>();
        instanceIds.add("i-2");
        instanceIds.add("i-1");

        assertTrue(argument.getAllValues().get(0).instanceIds().containsAll(instanceIds));
    }

    @Test
    void shouldNotTerminateExcessCapacityWhenNodeIsBusy() throws Exception {
        // Keep a busy queue
        List<QueueTaskFuture> rs = enqueTask(10, 90);
        triggerSuggestReviewNow();

        EC2FleetCloud cloud = new EC2FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, new LocalComputerConnector(j), false, false,
                1, 2, 2, 0, 1, false, true, "-1", false, 0, 0, 999, false, false, noScaling);
        j.jenkins.clouds.add(cloud);
        cloud.update();

        assertAtLeastOneNode();
        cloud = new EC2FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, new LocalComputerConnector(j), false, false,
                1, 0, 0, 0, 1, false, true, "-1", false, 0, 0, 99, false, false, noScaling);
        j.jenkins.clouds.clear();
        j.jenkins.clouds.add(cloud);
        assertAtLeastOneNode();
        cloud.update();

        // Nodes take a minute to become idle
        Thread.sleep(1000 * 61);
        // Manually trigger the retention check because it's super flaky whether it actually gets triggered
        for (final Node node : j.jenkins.getNodes()) {
            if (node instanceof EC2FleetNode && ((EC2FleetNode) node).getCloud() == cloud) {
                EC2FleetNodeComputer computer = (EC2FleetNodeComputer) ((EC2FleetNode) node).getComputer();
                new EC2RetentionStrategy().check(computer);
            }
        }
        cloud.update();

        verify((amazonEC2), times(0)).terminateInstances((TerminateInstancesRequest) any());
        cancelTasks(rs);
    }

    @Test
    void shouldTerminateIdleNodesAfterIdleTimeout() throws Exception {
        final EC2FleetCloud cloud = new EC2FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, new LocalComputerConnector(j), false, false,
                1, 0, 2, 0, 1, false, true, "-1", false, 0, 0, 99, false, false, noScaling);
        j.jenkins.clouds.add(cloud);
        cloud.update();

        assertAtLeastOneNode();

        final ArgumentCaptor<TerminateInstancesRequest> argument = ArgumentCaptor.forClass(TerminateInstancesRequest.class);

        // Nodes take a minute to become idle
        Thread.sleep(1000 * 61);
        // Manually trigger the retention check because it's super flaky whether it actually gets triggered
        for (final Node node : j.jenkins.getNodes()) {
            if (node instanceof EC2FleetNode && ((EC2FleetNode) node).getCloud() == cloud) {
                EC2FleetNodeComputer computer = (EC2FleetNodeComputer) ((EC2FleetNode) node).getComputer();
                new EC2RetentionStrategy().check(computer);
            }
        }
        cloud.update();

        verify((amazonEC2), times(1)).terminateInstances(argument.capture());

        final List<String> instanceIds = new ArrayList<>();
        instanceIds.add("i-2");
        instanceIds.add("i-1");
        assertTrue(argument.getAllValues().get(0).instanceIds().containsAll(instanceIds));
    }

    @Test
    void shouldNotTerminateBelowMinSize() throws Exception {
        final EC2FleetCloud cloud = new EC2FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, new LocalComputerConnector(j), false, false,
                1, 2, 5, 0, 1, false, true, "-1", false, 0, 0, 30, false, false, noScaling);
        j.jenkins.clouds.add(cloud);
        cloud.update();

        assertAtLeastOneNode();

        // Nodes take a minute to become idle
        Thread.sleep(1000 * 61);
        // Manually trigger the retention check because it's super flaky whether it actually gets triggered
        for (final Node node : j.jenkins.getNodes()) {
            if (node instanceof EC2FleetNode && ((EC2FleetNode) node).getCloud() == cloud) {
                EC2FleetNodeComputer computer = (EC2FleetNodeComputer) ((EC2FleetNode) node).getComputer();
                new EC2RetentionStrategy().check(computer);
            }
        }
        cloud.update();

        verify((amazonEC2), times(0)).terminateInstances((TerminateInstancesRequest) any());
    }

    @Test
    void shouldNotTerminateBelowMinSpareSize() throws Exception {
        final EC2FleetCloud cloud = new EC2FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, new LocalComputerConnector(j), false, false,
                1, 0, 5, 2, 1, false, true, "-1", false, 0, 0, 30, false, false, noScaling);
        j.jenkins.clouds.add(cloud);
        cloud.update();

        assertAtLeastOneNode();

        // Nodes take a minute to become idle
        Thread.sleep(1000 * 61);
        // Manually trigger the retention check because it's super flaky whether it actually gets triggered
        for (final Node node : j.jenkins.getNodes()) {
            if (node instanceof EC2FleetNode && ((EC2FleetNode) node).getCloud() == cloud) {
                EC2FleetNodeComputer computer = (EC2FleetNodeComputer) ((EC2FleetNode) node).getComputer();
                new EC2RetentionStrategy().check(computer);
            }
        }
        cloud.update();

        verify((amazonEC2), times(0)).terminateInstances((TerminateInstancesRequest) any());
    }

    @Test
    void shouldTerminateWhenMaxTotalUsesIsExhausted() throws Exception {
        final String label = "momo";
        final int numTasks = 4; // schedule a total of 4 tasks, 2 per instance
        final int maxTotalUses = 2;
        final int taskSleepTime = 1;

        EC2FleetCloud cloud = spy(new EC2FleetCloud("testCloud", "credId", null, "region",
                null, "fId", label, null, new LocalComputerConnector(j), false, false,
                0, 0, 10, 0, 1, false, true,
                String.valueOf(maxTotalUses), true, 0, 0, 10, false, false, noScaling));
        j.jenkins.clouds.add(cloud);
        cloud.update();
        assertAtLeastOneNode();

        System.out.println("*** scheduling tasks ***");
        waitJobSuccessfulExecution(enqueTask(numTasks, taskSleepTime));
        Thread.sleep(3000); // sleep for a bit to make sure post job actions finish and the computers are idle

        // make sure the instances scheduled for termination are terminated
        cloud.update();

        final ArgumentCaptor<TerminateInstancesRequest> argument = ArgumentCaptor.forClass(TerminateInstancesRequest.class);
        verify((amazonEC2), atLeastOnce()).terminateInstances(argument.capture());
        assertTrue(argument.getAllValues().get(0).instanceIds().containsAll(Arrays.asList("i-1", "i-2")));
    }
}
