package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.aws.EC2Api;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleet;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleets;
import hudson.model.TaskListener;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.Cloud;
import hudson.slaves.ComputerConnector;
import hudson.slaves.ComputerLauncher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProvisionIntegrationTest extends IntegrationTest {

    private final EC2FleetCloud.ExecutorScaler noScaling = new EC2FleetCloud.NoScaler();

    @BeforeAll
    static void beforeClass() {
        System.setProperty("jenkins.test.timeout", "720");
    }

    @Test
    void dont_provide_any_planned_if_empty_and_reached_max_capacity() throws Exception {
        ComputerLauncher computerLauncher = mock(ComputerLauncher.class);
        ComputerConnector computerConnector = mock(ComputerConnector.class);
        when(computerConnector.launch(anyString(), any(TaskListener.class))).thenReturn(computerLauncher);
        final EC2Fleet ec2Fleet = mock(EC2Fleet.class);
        EC2Fleets.setGet(ec2Fleet);
        when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString())).thenReturn(
                new FleetStateStats("", 0, FleetStateStats.State.active(), Collections.emptySet(),
                        Collections.emptyMap()));

        EC2FleetCloud cloud = new EC2FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, computerConnector, false, false,
                0, 0, 0, 0, 1, true, false,
                "-1", false, 0, 0,
                2, false, false, noScaling);
        j.jenkins.clouds.add(cloud);

        final EC2Api ec2Api = spy(EC2Api.class);
        Registry.setEc2Api(ec2Api);

        Ec2Client amazonEC2 = mock(Ec2Client.class);
        when(ec2Api.connect(anyString(), anyString(), Mockito.nullable(String.class))).thenReturn(amazonEC2);

        List<QueueTaskFuture> rs = enqueTask(5);

        assertEquals(0, j.jenkins.getNodes().size());

        triggerSuggestReviewNow("momo");

        Thread.sleep(TimeUnit.SECONDS.toMillis(30));

        assertEquals(0, j.jenkins.getNodes().size());

        cancelTasks(rs);
    }

    @Test
    void should_add_planned_if_capacity_required_but_not_described_yet() throws Exception {
        ComputerLauncher computerLauncher = mock(ComputerLauncher.class);
        ComputerConnector computerConnector = mock(ComputerConnector.class);
        when(computerConnector.launch(anyString(), any(TaskListener.class))).thenReturn(computerLauncher);

        mockEc2FleetApi();

        EC2FleetCloud cloud = new EC2FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, computerConnector, false, false,
                0, 0, 10, 0, 1, true, false,
                "-1", false, 0, 0,
                2, false, false, noScaling);
        j.jenkins.clouds.add(cloud);

        List<QueueTaskFuture> rs = enqueTask(1);

        triggerSuggestReviewNow("momo");

        assertEquals(0, j.jenkins.getNodes().size());

        tryUntil(() -> {
            assertEquals(0, j.jenkins.getNodes().size());
            assertEquals(2, j.jenkins.getLabels().size());
            assertEquals(1, j.jenkins.getLabelAtom("momo").nodeProvisioner.getPendingLaunches().size());
        });

        cancelTasks(rs);
    }

    @Test
    void should_keep_planned_node_until_node_will_not_be_online_so_jenkins_will_not_request_overprovision() throws Exception {
        ComputerLauncher computerLauncher = mock(ComputerLauncher.class);
        ComputerConnector computerConnector = mock(ComputerConnector.class);
        when(computerConnector.launch(anyString(), any(TaskListener.class))).thenReturn(computerLauncher);
        final EC2Fleet ec2Fleet = mock(EC2Fleet.class);
        EC2Fleets.setGet(ec2Fleet);
        when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString())).thenReturn(
                new FleetStateStats("", 0, FleetStateStats.State.active(),
                        Collections.emptySet(), Collections.emptyMap()));
        EC2FleetCloud cloud = spy(new EC2FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, computerConnector, false, false,
                0, 0, 10, 0, 1, true, false,
                "-1", false, 300, 15,
                2, false, false, noScaling));

        j.jenkins.clouds.add(cloud);

        mockEc2FleetApiToEc2SpotFleet(InstanceStateName.RUNNING);

        List<QueueTaskFuture> rs = enqueTask(1);

        final String labelString = "momo";
        // Allow Jenkins to sync `stats` object. Removing this calls provision twice as stats remains null on first try
        Thread.sleep(TimeUnit.SECONDS.toMillis(2));
        triggerSuggestReviewNow(labelString);

        Thread.sleep(TimeUnit.MINUTES.toMillis(2));

        verify(cloud, times(1)).provision(any(Cloud.CloudState.class), anyInt());

        cancelTasks(rs);
    }

    @Test
    void should_not_keep_planned_node_if_configured_so_jenkins_will_overprovision() throws Exception {
        ComputerLauncher computerLauncher = mock(ComputerLauncher.class);
        ComputerConnector computerConnector = mock(ComputerConnector.class);
        when(computerConnector.launch(anyString(), any(TaskListener.class))).thenReturn(computerLauncher);
        final EC2Fleet ec2Fleet = mock(EC2Fleet.class);
        EC2Fleets.setGet(ec2Fleet);
        when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString())).thenReturn(
                new FleetStateStats("", 0, FleetStateStats.State.active(),
                        Collections.emptySet(), Collections.emptyMap()));
        final EC2FleetCloud cloud = spy(new EC2FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, computerConnector, false, false,
                0, 0, 10, 0, 1, true, false,
                "-1", false, 0, 0,
                10, false, false, noScaling));
        j.jenkins.clouds.add(cloud);

        mockEc2FleetApiToEc2SpotFleet(InstanceStateName.RUNNING);

        enqueTask(1);

        tryUntil(() -> {
            j.jenkins.getLabelAtom("momo").nodeProvisioner.suggestReviewNow();
            verify(cloud, atLeast(2)).provision(any(Cloud.CloudState.class), anyInt());
        });
    }

    @Test
    void should_not_allow_jenkins_to_provision_if_address_not_available() throws Exception {
        mockEc2FleetApiToEc2SpotFleet(InstanceStateName.RUNNING);

        ComputerLauncher computerLauncher = mock(ComputerLauncher.class);
        ComputerConnector computerConnector = mock(ComputerConnector.class);
        when(computerConnector.launch(anyString(), any(TaskListener.class))).thenReturn(computerLauncher);

        EC2FleetCloud cloud = spy(new EC2FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, computerConnector, false, false,
                0, 0, 10, 0, 1, true, false,
                "-1", false, 0, 0,
                10, false, false, noScaling));

        cloud.setStats(new FleetStateStats("", 0, FleetStateStats.State.active(),
                Collections.emptySet(), Collections.emptyMap()));

        j.jenkins.clouds.add(cloud);

        EC2Api ec2Api = spy(EC2Api.class);
        Registry.setEc2Api(ec2Api);

        Ec2Client amazonEC2 = mock(Ec2Client.class);
        when(ec2Api.connect(anyString(), anyString(), Mockito.nullable(String.class))).thenReturn(amazonEC2);

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(
                DescribeInstancesResponse.builder().reservations(
                        Reservation.builder().instances(
                                Instance.builder()
                                        .state(InstanceState.builder().name(InstanceStateName.RUNNING)
                                                .build())
//                                        .withPublicIpAddress("public-io")
                                        .instanceId("i-1")
                                .build()
                        )
                        .build())
                .build());

        when(amazonEC2.describeSpotFleetInstances(any(DescribeSpotFleetInstancesRequest.class))).thenReturn(
                DescribeSpotFleetInstancesResponse.builder().activeInstances(ActiveInstance.builder().instanceId("i-1")
                        .build())
                .build());

        DescribeSpotFleetRequestsResponse describeSpotFleetRequestsResult = DescribeSpotFleetRequestsResponse.builder()
                .build();
        describeSpotFleetRequestsResult = describeSpotFleetRequestsResult.toBuilder().spotFleetRequestConfigs(Arrays.asList(
                SpotFleetRequestConfig.builder()
                        .spotFleetRequestState("active")
                        .spotFleetRequestConfig(
                                SpotFleetRequestConfigData.builder().targetCapacity(1)
                                .build())
                .build())).build();
        when(amazonEC2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        List<QueueTaskFuture> rs = enqueTask(1);

        j.jenkins.getLabelAtom("momo").nodeProvisioner.suggestReviewNow();

        assertEquals(0, j.jenkins.getNodes().size());

        Thread.sleep(TimeUnit.MINUTES.toMillis(2));

        cancelTasks(rs);

        verify(cloud, times(1)).provision(any(Cloud.CloudState.class), anyInt());
    }

    @Test
    void should_not_convert_planned_to_node_if_state_is_not_running_and_check_state_enabled() throws Exception {
        ComputerLauncher computerLauncher = mock(ComputerLauncher.class);
        ComputerConnector computerConnector = mock(ComputerConnector.class);
        when(computerConnector.launch(anyString(), any(TaskListener.class))).thenReturn(computerLauncher);
        final EC2Fleet ec2Fleet = mock(EC2Fleet.class);
        EC2Fleets.setGet(ec2Fleet);
        when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString())).thenReturn(
                new FleetStateStats("", 0, FleetStateStats.State.active(),
                        Collections.emptySet(), Collections.emptyMap()));
        EC2FleetCloud cloud = new EC2FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, computerConnector, false, false,
                0, 0, 10, 0, 1, true, false,
                "-1", false, 0, 0,
                2, false, false, noScaling);
        j.jenkins.clouds.add(cloud);

        mockEc2FleetApiToEc2SpotFleet(InstanceStateName.PENDING);

        final List<QueueTaskFuture> rs = enqueTask(1);

        triggerSuggestReviewNow("momo");

        assertEquals(0, j.jenkins.getNodes().size());

        tryUntil(() -> {
            assertEquals(new HashSet<>(Arrays.asList("built-in", "momo")), labelsToNames(j.jenkins.getLabels()));
            assertEquals(1, j.jenkins.getLabelAtom("momo").nodeProvisioner.getPendingLaunches().size());
            assertEquals(0, j.jenkins.getNodes().size());
        });

        cancelTasks(rs);
    }

    @Test
    void should_successfully_create_nodes() throws Exception {
        ComputerLauncher computerLauncher = mock(ComputerLauncher.class);
        ComputerConnector computerConnector = mock(ComputerConnector.class);
        when(computerConnector.launch(anyString(), any(TaskListener.class))).thenReturn(computerLauncher);
        final EC2Fleet ec2Fleet = mock(EC2Fleet.class);
        EC2Fleets.setGet(ec2Fleet);
        when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString())).thenReturn(
                new FleetStateStats("", 0, FleetStateStats.State.active(),
                        Collections.emptySet(), Collections.emptyMap()));
        EC2FleetCloud cloud = new EC2FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, computerConnector, false, false,
                0, 0, 2, 0, 1, true, false,
                "-1", false, 0, 0,
                2, false, false, noScaling);
        j.jenkins.clouds.add(cloud);

        mockEc2FleetApiToEc2SpotFleet(InstanceStateName.RUNNING);

        final List<QueueTaskFuture> rs = enqueTask(2);

        triggerSuggestReviewNow("momo");

        tryUntil(() -> {
            assertEquals(new HashSet<>(Arrays.asList("built-in", "momo", "i-0", "i-1")), labelsToNames(j.jenkins.getLabels()));
            assertEquals(2, j.jenkins.getLabelAtom("momo").getNodes().size());
            // node name should be instance name
            assertEquals(new HashSet<>(Arrays.asList("i-0", "i-1")), nodeToNames(j.jenkins.getLabelAtom("momo").getNodes()));
        });

        cancelTasks(rs);
    }

    @Test
    void should_continue_update_after_termination() throws IOException {
        mockEc2FleetApiToEc2SpotFleet(InstanceStateName.RUNNING, 5);

        final ComputerConnector computerConnector = new LocalComputerConnector(j);
        final EC2FleetCloud cloud = new EC2FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, computerConnector, false, false,
                1, 0, 5, 0, 1, true, false,
                "-1", false, 0, 0,
                10, false, false, noScaling);
        j.jenkins.clouds.add(cloud);

        waitFirstStats(cloud);

        final List<QueueTaskFuture> tasks = new ArrayList<>(enqueTask(5));
        j.jenkins.getLabelAtom("momo").nodeProvisioner.suggestReviewNow();
        System.out.println("tasks submitted");

        // wait ful l execution
        waitJobSuccessfulExecution(tasks);

        // wait until downscale happens
        tryUntil(() -> {
            System.out.println("Inside " + j.jenkins.getLabel("momo").getNodes().size());
            // defect in termination logic, that why 1
            assertThat(j.jenkins.getLabel("momo").getNodes().size(), lessThanOrEqualTo(1));
        }, TimeUnit.MINUTES.toMillis(3));

        final FleetStateStats oldStats = cloud.getStats();
        tryUntil(() -> {
            System.out.println("stats should be updated");
            assertNotSame(oldStats, cloud.getStats());
        });
    }

}
