package com.amazon.jenkins.ec2fleet;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.Reservation;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EC2ApiTest {

    @Mock
    private AmazonEC2 amazonEC2;

    @Test
    public void shouldReturnEmptyResultAndNoCallIfEmptyListOfInstances() {
        Set<String> terminated = new EC2Api().describeTerminated(amazonEC2, Collections.<String>emptySet());

        Assert.assertEquals(Collections.emptySet(), terminated);
        verifyZeroInteractions(amazonEC2);
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

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResult);

        // when
        Set<String> terminated = new EC2Api().describeTerminated(amazonEC2, instanceIds);

        // then
        Assert.assertEquals(Collections.emptySet(), terminated);
        verify(amazonEC2, times(1))
                .describeInstances(any(DescribeInstancesRequest.class));
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

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(describeInstancesResult1)
                .thenReturn(describeInstancesResult2);

        // when
        Set<String> terminated = new EC2Api().describeTerminated(amazonEC2, instanceIds);

        // then
        Assert.assertEquals(new HashSet<>(Arrays.asList("i-3")), terminated);
        verify(amazonEC2, times(2))
                .describeInstances(any(DescribeInstancesRequest.class));
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


        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(describeInstancesResult1);

        // when
        Set<String> terminated = new EC2Api().describeTerminated(amazonEC2, instanceIds);

        // then
        Assert.assertEquals(new HashSet<>(Arrays.asList(
                "missed", "terminated", "stopped", "shutting-down", "stopping")), terminated);
        verify(amazonEC2, times(1))
                .describeInstances(any(DescribeInstancesRequest.class));
    }

    @Test
    public void shouldSendInOneCallNoMoreThenBatchSizeOfInstance() {
        // given
        Set<String> instanceIds = new HashSet<>();
        instanceIds.add("i1");
        instanceIds.add("i2");
        instanceIds.add("i3");

        DescribeInstancesResult describeInstancesResult1 =
                new DescribeInstancesResult()
                        .withReservations(
                                new Reservation().withInstances(new Instance()
                                                .withInstanceId("stopped")
                                                .withState(new InstanceState().withName(InstanceStateName.Running)),
                                        new Instance()
                                                .withInstanceId("stopping")
                                                .withState(new InstanceState().withName(InstanceStateName.Running))
                                ));

        DescribeInstancesResult describeInstancesResult2 = new DescribeInstancesResult();

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(describeInstancesResult1)
                .thenReturn(describeInstancesResult2);

        // when
        new EC2Api().describeTerminated(amazonEC2, instanceIds, 2);

        // then
        verify(amazonEC2).describeInstances(new DescribeInstancesRequest().withInstanceIds(Arrays.asList("i1", "i2")));
        verify(amazonEC2).describeInstances(new DescribeInstancesRequest().withInstanceIds(Arrays.asList("i3")));
        verifyNoMoreInteractions(amazonEC2);
    }

    /**
     * NotFound exception example data
     * <p>
     * <code>
     * Single instance
     * requestId = "0fd56c54-e11a-4928-843c-9a80a24bedd1"
     * errorCode = "InvalidInstanceID.NotFound"
     * errorType = {AmazonServiceException$ErrorType@11247} "Unknown"
     * errorMessage = "The instance ID 'i-1233f' does not exist"
     * </code>
     * <p>
     * Multiple instances
     * <code>
     * ex = {AmazonEC2Exception@11233} "com.amazonaws.services.ec2.model.AmazonEC2Exception: The instance IDs 'i-1233f, i-ffffff' do not exist (Service: AmazonEC2; Status Code: 400; Error Code: InvalidInstanceID.NotFound; Request ID:)"
     * requestId = "1a353313-ef52-4626-b87b-fd828db6343f"
     * errorCode = "InvalidInstanceID.NotFound"
     * errorType = {AmazonServiceException$ErrorType@11251} "Unknown"
     * errorMessage = "The instance IDs 'i-1233f, i-ffffff' do not exist"
     * </code>
     */
    @Test
    public void shouldHandleAmazonEc2NotFoundErrorAsTerminatedInstancesAndRetry() {
        // given
        Set<String> instanceIds = new HashSet<>();
        instanceIds.add("i-1");
        instanceIds.add("i-f");
        instanceIds.add("i-3");

        AmazonEC2Exception notFoundException = new AmazonEC2Exception(
                "The instance IDs 'i-1, i-f' do not exist");
        notFoundException.setErrorCode("InvalidInstanceID.NotFound");

        DescribeInstancesResult describeInstancesResult2 = new DescribeInstancesResult()
                .withReservations(new Reservation().withInstances(
                        new Instance().withInstanceId("i-3")
                                .withState(new InstanceState().withName(InstanceStateName.Running))));

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .thenThrow(notFoundException)
                .thenReturn(describeInstancesResult2);

        // when
        final Set<String> terminatedIds = new EC2Api().describeTerminated(amazonEC2, instanceIds);

        // then
        Assert.assertEquals(new HashSet<>(Arrays.asList("i-1", "i-f")), terminatedIds);
        verify(amazonEC2).describeInstances(new DescribeInstancesRequest().withInstanceIds(Arrays.asList("i-1", "i-3", "i-f")));
        verify(amazonEC2).describeInstances(new DescribeInstancesRequest().withInstanceIds(Arrays.asList("i-3")));
        verifyNoMoreInteractions(amazonEC2);
    }

    @Test
    public void shouldFailIfNotAbleToParseNotFoundExceptionFromEc2Api() {
        // given
        Set<String> instanceIds = new HashSet<>();
        instanceIds.add("i-1");
        instanceIds.add("i-f");
        instanceIds.add("i-3");

        AmazonEC2Exception notFoundException = new AmazonEC2Exception(
                "unparseable");
        notFoundException.setErrorCode("InvalidInstanceID.NotFound");

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .thenThrow(notFoundException);

        // when
        try {
            new EC2Api().describeTerminated(amazonEC2, instanceIds);
            Assert.fail();
        } catch (AmazonEC2Exception exception) {
            Assert.assertSame(notFoundException, exception);
        }
    }

    @Test
    public void shouldThrowExceptionIfEc2DescribeFailsWithException() {
        // given
        Set<String> instanceIds = new HashSet<>();
        instanceIds.add("a");

        UnsupportedOperationException exception = new UnsupportedOperationException("test");
        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .thenThrow(exception);

        // when
        try {
            new EC2Api().describeTerminated(amazonEC2, instanceIds);
            Assert.fail();
        } catch (UnsupportedOperationException e) {
            Assert.assertSame(exception, e);
        }
    }

    @Ignore("manual test pass credentials if you want to run")
    @Test
    public void realShouldHandleAmazonEc2NotFoundErrorAsTerminatedInstancesAndRetry() {
        // given
        final String accessKey = "...";
        final String secretKey = "...";

        final Set<String> instanceIds = new HashSet<>();
        instanceIds.add("i-1233f");
        instanceIds.add("i-ff");

        final AmazonEC2 amazonEC2 = new AmazonEC2Client(new BasicAWSCredentials(accessKey, secretKey));

        // when
        Set<String> t = new EC2Api().describeTerminated(amazonEC2, instanceIds);
        Assert.assertEquals(instanceIds, t);
    }

}
