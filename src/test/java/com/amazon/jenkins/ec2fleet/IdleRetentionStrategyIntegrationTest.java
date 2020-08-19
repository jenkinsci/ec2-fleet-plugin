package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.fleet.EC2Fleet;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleets;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.google.common.collect.ImmutableSet;
import hudson.model.queue.QueueTaskFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
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

public class IdleRetentionStrategyIntegrationTest extends IntegrationTest {

    private AmazonEC2 amazonEC2;

    @Before
    public void before() {
        final EC2Fleet ec2Fleet = mock(EC2Fleet.class);
        EC2Fleets.setGet(ec2Fleet);
        final EC2Api ec2Api = spy(EC2Api.class);
        Registry.setEc2Api(ec2Api);
        amazonEC2 = mock(AmazonEC2.class);

        when(ec2Fleet.getState(anyString(), anyString(), nullable(String.class), anyString()))
                .thenReturn(new FleetStateStats("", 2, FleetStateStats.State.active(), ImmutableSet.of("i-1", "i-2"), Collections.emptyMap()));
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
    public void shouldTerminateExcessCapacity() throws Exception {
        final EC2FleetCloud cloud = new EC2FleetCloud(null, null, "credId", null, "region",
                null, "fId", "momo", null, new LocalComputerConnector(j), false, false,
                1, 0, 0, 1, false, true, false, 0, 0, false, 999, false);
        // Set initial jenkins nodes
        cloud.update();
        j.jenkins.clouds.add(cloud);

        assertAtLeastOneNode();

        final ArgumentCaptor<TerminateInstancesRequest> argument = ArgumentCaptor.forClass(TerminateInstancesRequest.class);

        // IdleRetentionStrategy checks every 60 sections
        Thread.sleep(1000 * 60);

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
        final EC2FleetCloud cloud = new EC2FleetCloud(null, null, "credId", null, "region",
                null, "fId", "momo", null, new LocalComputerConnector(j), false, false,
                1, 0, 0, 1, false, true, false, 0, 0, false, 999, false);
        cloud.update();
        j.jenkins.clouds.add(cloud);
        // Keep a busy queue
        List<QueueTaskFuture> rs = enqueTask(10);
        triggerSuggestReviewNow();

        assertAtLeastOneNode();

        final ArgumentCaptor<TerminateInstancesRequest> argument = ArgumentCaptor.forClass(TerminateInstancesRequest.class);

        // IdleRetentionStrategy checks every 60 sections
        Thread.sleep(1000 * 60);
        cloud.update();

        verify((amazonEC2), times(0)).terminateInstances(any());
        cancelTasks(rs);
    }

    @Test
    public void shouldTerminateIdleNodesAfterIdleTimeout() throws Exception {
        final EC2FleetCloud cloud = new EC2FleetCloud(null, null, "credId", null, "region",
                null, "fId", "momo", null, new LocalComputerConnector(j), false, false,
                1, 0, 2, 1, false, true, false, 0, 0, false, 30, false);
        j.jenkins.clouds.add(cloud);
        cloud.update();

        assertAtLeastOneNode();

        final ArgumentCaptor<TerminateInstancesRequest> argument = ArgumentCaptor.forClass(TerminateInstancesRequest.class);

        // IdleRetentionStrategy checks every 60 sections and idle timeout is 60 seconds.
        Thread.sleep(1000 * 120);
        cloud.update();
        verify((amazonEC2), times(1)).terminateInstances(argument.capture());

        final List<String> instanceIds = new ArrayList<String>();
        instanceIds.add("i-2");
        instanceIds.add("i-1");
        assertTrue(argument.getAllValues().get(0).getInstanceIds().containsAll(instanceIds));
    }

    @Test
    public void shouldNotTerminateBelowMinSize() throws Exception {
        final EC2FleetCloud cloud = new EC2FleetCloud(null, null, "credId", null, "region",
                null, "fId", "momo", null, new LocalComputerConnector(j), false, false,
                1, 2, 5, 1, false, true, false, 0, 0, false, 30, false);
        j.jenkins.clouds.add(cloud);
        cloud.update();

        assertAtLeastOneNode();

        // IdleRetentionStrategy checks every 60 sections and idle timeout is 60 seconds.
        Thread.sleep(1000 * 120);
        cloud.update();
        verify((amazonEC2), times(0)).terminateInstances(any());
    }
}
