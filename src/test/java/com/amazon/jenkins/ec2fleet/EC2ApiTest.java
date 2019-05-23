package com.amazon.jenkins.ec2fleet;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.Reservation;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class EC2ApiTest {

    @Mock
    private AmazonEC2 amazonEC2;

    @Test
    public void shouldReturnEmptyResultAndNoCallIfEmptyListOfInstances() {
        Set<String> terminated = EC2Api.describeTerminated(amazonEC2, Collections.<String>emptySet());

        Assert.assertEquals(Collections.emptySet(), terminated);
        Mockito.verifyZeroInteractions(amazonEC2);
    }

    @Test
    public void shouldReturnEmptyIfAllInstancesStillActive() {
        // given
        Set<String> instanceIds = new HashSet<>();
        instanceIds.add("i-1");
        instanceIds.add("i-2");

        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult();
        Reservation reservation = new Reservation();
        Instance instance1 = new Instance()
                .withInstanceId("i-1")
                .withState(new InstanceState().withName(InstanceStateName.Running));
        Instance instance2 = new Instance()
                .withInstanceId("i-2")
                .withState(new InstanceState().withName(InstanceStateName.Running));
        reservation.setInstances(Arrays.asList(instance1, instance2));
        describeInstancesResult.setReservations(Arrays.asList(reservation));

        Mockito.when(amazonEC2.describeInstances(Mockito.any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResult);

        // when
        Set<String> terminated = EC2Api.describeTerminated(amazonEC2, instanceIds);

        // then
        Assert.assertEquals(Collections.emptySet(), terminated);
        Mockito.verify(amazonEC2, Mockito.times(1))
                .describeInstances(Mockito.any(DescribeInstancesRequest.class));
    }

    @Test
    public void shouldProcessAllPagesUntilNextTokenIsAvailable() {
        // given
        Set<String> instanceIds = new HashSet<>();
        instanceIds.add("i-1");
        instanceIds.add("i-2");
        instanceIds.add("i-3");

        DescribeInstancesResult describeInstancesResult1 =
                new DescribeInstancesResult()
                        .withReservations(
                                new Reservation().withInstances(new Instance()
                                        .withInstanceId("i-1")
                                        .withState(new InstanceState().withName(InstanceStateName.Running))))
                        .withNextToken("a");

        DescribeInstancesResult describeInstancesResult2 =
                new DescribeInstancesResult()
                        .withReservations(new Reservation().withInstances(
                                new Instance()
                                        .withInstanceId("i-2")
                                        .withState(new InstanceState().withName(InstanceStateName.Running)),
                                new Instance()
                                        .withInstanceId("i-3")
                                        .withState(new InstanceState().withName(InstanceStateName.Terminated))
                        ));

        Mockito.when(amazonEC2.describeInstances(Mockito.any(DescribeInstancesRequest.class)))
                .thenReturn(describeInstancesResult1)
                .thenReturn(describeInstancesResult2);

        // when
        Set<String> terminated = EC2Api.describeTerminated(amazonEC2, instanceIds);

        // then
        Assert.assertEquals(new HashSet<>(Arrays.asList("i-3")), terminated);
        Mockito.verify(amazonEC2, Mockito.times(2))
                .describeInstances(Mockito.any(DescribeInstancesRequest.class));
    }

    @Test
    public void shouldAssumeMissedInResultInstanceOrTerminatedOrStoppedOrStoppingOrShuttingDownAsTermianted() {
        // given
        Set<String> instanceIds = new HashSet<>();
        instanceIds.add("missed");
        instanceIds.add("stopped");
        instanceIds.add("terminated");
        instanceIds.add("stopping");
        instanceIds.add("shutting-down");

        DescribeInstancesResult describeInstancesResult1 =
                new DescribeInstancesResult()
                        .withReservations(
                                new Reservation().withInstances(new Instance()
                                                .withInstanceId("stopped")
                                                .withState(new InstanceState().withName(InstanceStateName.Stopped)),
                                        new Instance()
                                                .withInstanceId("stopping")
                                                .withState(new InstanceState().withName(InstanceStateName.Stopping)),
                                        new Instance()
                                                .withInstanceId("shutting-down")
                                                .withState(new InstanceState().withName(InstanceStateName.ShuttingDown)),
                                        new Instance()
                                                .withInstanceId("terminated")
                                                .withState(new InstanceState().withName(InstanceStateName.Terminated))
                                ));


        Mockito.when(amazonEC2.describeInstances(Mockito.any(DescribeInstancesRequest.class)))
                .thenReturn(describeInstancesResult1);

        // when
        Set<String> terminated = EC2Api.describeTerminated(amazonEC2, instanceIds);

        // then
        Assert.assertEquals(new HashSet<>(Arrays.asList(
                "missed", "terminated", "stopped", "shutting-down", "stopping")), terminated);
        Mockito.verify(amazonEC2, Mockito.times(1))
                .describeInstances(Mockito.any(DescribeInstancesRequest.class));
    }

    @Test
    public void shouldThrowExceptionIfEc2DescribeFailsWithException() {
        // given
        Set<String> instanceIds = new HashSet<>();
        instanceIds.add("a");

        UnsupportedOperationException exception = new UnsupportedOperationException("test");
        Mockito.when(amazonEC2.describeInstances(Mockito.any(DescribeInstancesRequest.class)))
                .thenThrow(exception);

        // when
        try {
            EC2Api.describeTerminated(amazonEC2, instanceIds);
            Assert.fail();
        } catch (UnsupportedOperationException e) {
            Assert.assertSame(exception, e);
        }
    }

}
