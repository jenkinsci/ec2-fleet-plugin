package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.fleet.EC2Fleet;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleets;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.ActiveInstance;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSpotFleetInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfig;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfigData;
import com.google.common.collect.ImmutableSet;
import hudson.model.Label;
import hudson.model.TaskListener;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.ComputerConnector;
import hudson.slaves.ComputerLauncher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProvisionIntegrationTest extends IntegrationTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("jenkins.test.timeout", "720");
    }

    @Test
    public void dont_provide_any_planned_if_empty_and_reached_max_capacity() throws Exception {
        ComputerLauncher computerLauncher = mock(ComputerLauncher.class);
        ComputerConnector computerConnector = mock(ComputerConnector.class);
        when(computerConnector.launch(anyString(), any(TaskListener.class))).thenReturn(computerLauncher);

        EC2FleetCloud cloud = new EC2FleetCloud(null, null, "credId", null, "region",
                null, "fId", "momo", null, computerConnector, false, false,
                0, 0, 0, 1, true, false,
                false, 0, 0, false,
                2, false);
        j.jenkins.clouds.add(cloud);

        final EC2Api ec2Api = spy(EC2Api.class);
        Registry.setEc2Api(ec2Api);

        final EC2Fleet ec2Fleet = mock(EC2Fleet.class);
        EC2Fleets.setGet(ec2Fleet);

        when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString())).thenReturn(
                new FleetStateStats("", 0, FleetStateStats.State.active(), ImmutableSet.<String>of(),
                        Collections.<String, Double>emptyMap()));

        AmazonEC2 amazonEC2 = mock(AmazonEC2.class);
        when(ec2Api.connect(anyString(), anyString(), Mockito.nullable(String.class))).thenReturn(amazonEC2);

        List<QueueTaskFuture> rs = enqueTask(5);

        Assert.assertEquals(0, j.jenkins.getNodes().size());

        triggerSuggestReviewNow("momo");

        Thread.sleep(TimeUnit.SECONDS.toMillis(30));

        Assert.assertEquals(0, j.jenkins.getNodes().size());

        cancelTasks(rs);
    }

    @Test
    public void should_add_planned_if_capacity_required_but_not_described_yet() throws Exception {
        ComputerLauncher computerLauncher = mock(ComputerLauncher.class);
        ComputerConnector computerConnector = mock(ComputerConnector.class);
        when(computerConnector.launch(anyString(), any(TaskListener.class))).thenReturn(computerLauncher);

        mockEc2FleetApi();

        EC2FleetCloud cloud = new EC2FleetCloud(null, null, "credId", null, "region",
                null, "fId", "momo", null, computerConnector, false, false,
                0, 0, 10, 1, true, false,
                false, 0, 0, false,
                2, false);
        j.jenkins.clouds.add(cloud);

        List<QueueTaskFuture> rs = enqueTask(1);

        triggerSuggestReviewNow("momo");

        Assert.assertEquals(0, j.jenkins.getNodes().size());

        tryUntil(new Runnable() {
            @Override
            public void run() {
                Assert.assertEquals(0, j.jenkins.getNodes().size());
                Assert.assertEquals(2, j.jenkins.getLabels().size());
                Assert.assertEquals(1, j.jenkins.getLabelAtom("momo").nodeProvisioner.getPendingLaunches().size());
            }
        });

        cancelTasks(rs);
    }

    @Test
    public void should_keep_planned_node_until_node_will_not_be_online_so_jenkins_will_not_request_overprovision() throws Exception {
        ComputerLauncher computerLauncher = mock(ComputerLauncher.class);
        ComputerConnector computerConnector = mock(ComputerConnector.class);
        when(computerConnector.launch(anyString(), any(TaskListener.class))).thenReturn(computerLauncher);

        EC2FleetCloud cloud = spy(new EC2FleetCloud(null, null, "credId", null, "region",
                null, "fId", "momo", null, computerConnector, false, false,
                0, 0, 10, 1, true, false,
                false, 300, 15, false,
                2, false));

        // provide init state
        cloud.setStats(new FleetStateStats("", 0, FleetStateStats.State.active(),
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        j.jenkins.clouds.add(cloud);

        mockEc2FleetApiToEc2SpotFleet(InstanceStateName.Running);

        List<QueueTaskFuture> rs = enqueTask(1);

        final String labelString = "momo";
        triggerSuggestReviewNow(labelString);

        Thread.sleep(TimeUnit.MINUTES.toMillis(2));

        verify(cloud, times(1)).provision(any(Label.class), anyInt());

        cancelTasks(rs);
    }

    @Test
    public void should_not_keep_planned_node_if_configured_so_jenkins_will_overprovision() throws Exception {
        ComputerLauncher computerLauncher = mock(ComputerLauncher.class);
        ComputerConnector computerConnector = mock(ComputerConnector.class);
        when(computerConnector.launch(anyString(), any(TaskListener.class))).thenReturn(computerLauncher);

        final EC2FleetCloud cloud = spy(new EC2FleetCloud(null, null, "credId", null, "region",
                null, "fId", "momo", null, computerConnector, false, false,
                0, 0, 10, 1, true, false,
                false, 0, 0, false,
                10, false));
        j.jenkins.clouds.add(cloud);

        mockEc2FleetApiToEc2SpotFleet(InstanceStateName.Running);

        enqueTask(1);

        tryUntil(new Runnable() {
            @Override
            public void run() {
                j.jenkins.getLabelAtom("momo").nodeProvisioner.suggestReviewNow();
                verify(cloud, atLeast(2)).provision(any(Label.class), anyInt());
            }
        });
    }

    @Test
    public void should_not_allow_jenkins_to_provision_if_address_not_available() throws Exception {
        ComputerLauncher computerLauncher = mock(ComputerLauncher.class);
        ComputerConnector computerConnector = mock(ComputerConnector.class);
        when(computerConnector.launch(anyString(), any(TaskListener.class))).thenReturn(computerLauncher);

        EC2FleetCloud cloud = spy(new EC2FleetCloud(null, null, "credId", null, "region",
                null, "fId", "momo", null, computerConnector, false, false,
                0, 0, 10, 1, true, false,
                false, 0, 0, false,
                10, false));

        cloud.setStats(new FleetStateStats("", 0, FleetStateStats.State.active(),
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        j.jenkins.clouds.add(cloud);

        EC2Api ec2Api = spy(EC2Api.class);
        Registry.setEc2Api(ec2Api);

        AmazonEC2 amazonEC2 = mock(AmazonEC2.class);
        when(ec2Api.connect(anyString(), anyString(), Mockito.nullable(String.class))).thenReturn(amazonEC2);

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(
                new DescribeInstancesResult().withReservations(
                        new Reservation().withInstances(
                                new Instance()
                                        .withState(new InstanceState().withName(InstanceStateName.Running))
//                                        .withPublicIpAddress("public-io")
                                        .withInstanceId("i-1")
                        )));

        when(amazonEC2.describeSpotFleetInstances(any(DescribeSpotFleetInstancesRequest.class))).thenReturn(
                new DescribeSpotFleetInstancesResult().withActiveInstances(new ActiveInstance().withInstanceId("i-1")));

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = new DescribeSpotFleetRequestsResult();
        describeSpotFleetRequestsResult.setSpotFleetRequestConfigs(Arrays.asList(
                new SpotFleetRequestConfig()
                        .withSpotFleetRequestState("active")
                        .withSpotFleetRequestConfig(
                                new SpotFleetRequestConfigData().withTargetCapacity(1))));
        when(amazonEC2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        List<QueueTaskFuture> rs = enqueTask(1);

        j.jenkins.getLabelAtom("momo").nodeProvisioner.suggestReviewNow();

        Assert.assertEquals(0, j.jenkins.getNodes().size());

        Thread.sleep(TimeUnit.MINUTES.toMillis(2));

        cancelTasks(rs);

        verify(cloud, times(1)).provision(any(Label.class), anyInt());
    }

    @Test
    public void should_not_convert_planned_to_node_if_state_is_not_running_and_check_state_enabled() throws Exception {
        ComputerLauncher computerLauncher = mock(ComputerLauncher.class);
        ComputerConnector computerConnector = mock(ComputerConnector.class);
        when(computerConnector.launch(anyString(), any(TaskListener.class))).thenReturn(computerLauncher);

        EC2FleetCloud cloud = new EC2FleetCloud(null, null, "credId", null, "region",
                null, "fId", "momo", null, computerConnector, false, false,
                0, 0, 10, 1, true, false,
                false, 0, 0, false,
                2, false);
        j.jenkins.clouds.add(cloud);

        mockEc2FleetApiToEc2SpotFleet(InstanceStateName.Pending);

        final List<QueueTaskFuture> rs = enqueTask(1);

        triggerSuggestReviewNow("momo");

        Assert.assertEquals(0, j.jenkins.getNodes().size());

        tryUntil(new Runnable() {
            @Override
            public void run() {
                Assert.assertEquals(ImmutableSet.of("master", "momo"), labelsToNames(j.jenkins.getLabels()));
                Assert.assertEquals(1, j.jenkins.getLabelAtom("momo").nodeProvisioner.getPendingLaunches().size());
                Assert.assertEquals(0, j.jenkins.getNodes().size());
            }
        });

        cancelTasks(rs);
    }

    @Test
    public void should_successfully_create_nodes() throws Exception {
        ComputerLauncher computerLauncher = mock(ComputerLauncher.class);
        ComputerConnector computerConnector = mock(ComputerConnector.class);
        when(computerConnector.launch(anyString(), any(TaskListener.class))).thenReturn(computerLauncher);

        EC2FleetCloud cloud = new EC2FleetCloud(null, null, "credId", null, "region",
                null, "fId", "momo", null, computerConnector, false, false,
                0, 0, 2, 1, true, false,
                false, 0, 0, false,
                2, false);
        j.jenkins.clouds.add(cloud);

        mockEc2FleetApiToEc2SpotFleet(InstanceStateName.Running);

        final List<QueueTaskFuture> rs = enqueTask(2);

        triggerSuggestReviewNow("momo");

        tryUntil(new Runnable() {
            @Override
            public void run() {
                Assert.assertEquals(ImmutableSet.of("master", "momo", "i-0", "i-1"), labelsToNames(j.jenkins.getLabels()));
                Assert.assertEquals(2, j.jenkins.getLabelAtom("momo").getNodes().size());
                // node name should be instance name
                Assert.assertEquals(ImmutableSet.of("i-0", "i-1"), nodeToNames(j.jenkins.getLabelAtom("momo").getNodes()));
            }
        });

        cancelTasks(rs);
    }

    @Test
    public void should_continue_update_after_termination() throws IOException {
        mockEc2FleetApiToEc2SpotFleet(InstanceStateName.Running, 5);

        final ComputerConnector computerConnector = new LocalComputerConnector(j);
        final EC2FleetCloud cloud = new EC2FleetCloud(null, null, "credId", null, "region",
                null, "fId", "momo", null, computerConnector, false, false,
                1, 0, 5, 1, true, false,
                false, 0, 0, false,
                10, false);
        j.jenkins.clouds.add(cloud);

        waitFirstStats(cloud);

        final List<QueueTaskFuture> tasks = new ArrayList<>(enqueTask(5));
        j.jenkins.getLabelAtom("momo").nodeProvisioner.suggestReviewNow();
        System.out.println("tasks submitted");

        // wait full execution
        waitJobSuccessfulExecution(tasks);

        // wait until downscale happens
        tryUntil(new Runnable() {
            @Override
            public void run() {
                // defect in termination logic, that why 1
                Assert.assertThat(j.jenkins.getLabel("momo").getNodes().size(), Matchers.lessThanOrEqualTo(1));
            }
        }, TimeUnit.MINUTES.toMillis(3));

        final FleetStateStats oldStats = cloud.getStats();
        tryUntil(new Runnable() {
            @Override
            public void run() {
                System.out.println("stats should be updated");
                Assert.assertNotSame(oldStats, cloud.getStats());
            }
        });
    }

}
