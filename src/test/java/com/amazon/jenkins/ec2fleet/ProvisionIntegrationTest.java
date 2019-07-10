package com.amazon.jenkins.ec2fleet;

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
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
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

    @Test
    public void dont_provide_any_planned_if_empty_and_reached_max_capacity() throws Exception {
        ComputerLauncher computerLauncher = mock(ComputerLauncher.class);
        ComputerConnector computerConnector = mock(ComputerConnector.class);
        when(computerConnector.launch(anyString(), any(TaskListener.class))).thenReturn(computerLauncher);

        EC2FleetCloud cloud = new EC2FleetCloud(null, null, "credId", null, "region",
                null, "fId", "momo", null, computerConnector, false, false,
                0, 0, 0, 1, false, false,
                false, 0, 0, false);
        j.jenkins.clouds.add(cloud);

        EC2Api ec2Api = mock(EC2Api.class);
        Registry.setEc2Api(ec2Api);

        AmazonEC2 amazonEC2 = mock(AmazonEC2.class);
        when(ec2Api.connect(anyString(), anyString(), Mockito.nullable(String.class))).thenReturn(amazonEC2);

        when(amazonEC2.describeSpotFleetInstances(any(DescribeSpotFleetInstancesRequest.class)))
                .thenReturn(new DescribeSpotFleetInstancesResult());

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = new DescribeSpotFleetRequestsResult();
        describeSpotFleetRequestsResult.setSpotFleetRequestConfigs(Arrays.asList(
                new SpotFleetRequestConfig()
                        .withSpotFleetRequestState("active")
                        .withSpotFleetRequestConfig(
                                new SpotFleetRequestConfigData().withTargetCapacity(0))));
        when(amazonEC2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        List<QueueTaskFuture> rs = getQueueTaskFutures(5);

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

        EC2FleetCloud cloud = new EC2FleetCloud(null, null, "credId", null, "region",
                null, "fId", "momo", null, computerConnector, false, false,
                0, 0, 10, 1, false, false,
                false, 0, 0, false);
        j.jenkins.clouds.add(cloud);

        mockEc2ApiToDescribeFleetNotInstanceWhenModified();

        List<QueueTaskFuture> rs = getQueueTaskFutures(1);

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
                0, 0, 10, 1, false, false,
                false, 300, 15, false));
        j.jenkins.clouds.add(cloud);

        mockEc2ApiToDescribeInstancesWhenModified(InstanceStateName.Running);

        List<QueueTaskFuture> rs = getQueueTaskFutures(1);

        final String labelString = "momo";
        triggerSuggestReviewNow(labelString);

        Thread.sleep(TimeUnit.MINUTES.toMillis(2));

        verify(cloud, times(1)).provisionInternal(any(Label.class), anyInt());

        cancelTasks(rs);
    }

    @Test
    public void should_not_keep_planned_node_if_configured_so_jenkins_will_overprovision() throws Exception {
        ComputerLauncher computerLauncher = mock(ComputerLauncher.class);
        ComputerConnector computerConnector = mock(ComputerConnector.class);
        when(computerConnector.launch(anyString(), any(TaskListener.class))).thenReturn(computerLauncher);

        final EC2FleetCloud cloud = spy(new EC2FleetCloud(null, null, "credId", null, "region",
                null, "fId", "momo", null, computerConnector, false, false,
                0, 0, 10, 1, false, false,
                false, 0, 0, false));
        j.jenkins.clouds.add(cloud);

        mockEc2ApiToDescribeInstancesWhenModified(InstanceStateName.Running);

        getQueueTaskFutures(1);

        tryUntil(new Runnable() {
            @Override
            public void run() {
                j.jenkins.getLabelAtom("momo").nodeProvisioner.suggestReviewNow();
                verify(cloud, atLeast(2)).provisionInternal(any(Label.class), anyInt());
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
                0, 0, 10, 1, false, false,
                false, 0, 0, false));
        j.jenkins.clouds.add(cloud);

        EC2Api ec2Api = mock(EC2Api.class);
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

        List<QueueTaskFuture> rs = getQueueTaskFutures(1);

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
                false, 0, 0, false);
        j.jenkins.clouds.add(cloud);

        mockEc2ApiToDescribeInstancesWhenModified(InstanceStateName.Pending);

        List<QueueTaskFuture> rs = getQueueTaskFutures(1);

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

}
