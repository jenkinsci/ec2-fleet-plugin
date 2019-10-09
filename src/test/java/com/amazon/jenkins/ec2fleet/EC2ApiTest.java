package com.amazon.jenkins.ec2fleet;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
@RunWith(MockitoJUnitRunner.class)
public class EC2ApiTest {

    @Mock
    private AmazonEC2 amazonEC2;

    @Test
    public void describeInstances_shouldReturnEmptyResultAndNoCallIfEmptyListOfInstances() {
        Map<String, Instance> described = new EC2Api().describeInstances(amazonEC2, Collections.<String>emptySet());

        Assert.assertEquals(Collections.<String, Instance>emptyMap(), described);
        verifyZeroInteractions(amazonEC2);
    }

    @Test
    public void describeInstances_shouldReturnAllInstancesIfStillActive() {
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
        Map<String, Instance> described = new EC2Api().describeInstances(amazonEC2, instanceIds);

        // then
        Assert.assertEquals(ImmutableMap.of("i-1", instance1, "i-2", instance2), described);
        verify(amazonEC2, times(1))
                .describeInstances(any(DescribeInstancesRequest.class));
    }

    @Test
    public void describeInstances_shouldProcessAllPagesUntilNextTokenIsAvailable() {
        // given
        Set<String> instanceIds = new HashSet<>();
        instanceIds.add("i-1");
        instanceIds.add("i-2");
        instanceIds.add("i-3");

        final Instance instance1 = new Instance()
                .withInstanceId("i-1")
                .withState(new InstanceState().withName(InstanceStateName.Running));
        DescribeInstancesResult describeInstancesResult1 =
                new DescribeInstancesResult()
                        .withReservations(
                                new Reservation().withInstances(instance1))
                        .withNextToken("a");

        final Instance instance2 = new Instance()
                .withInstanceId("i-2")
                .withState(new InstanceState().withName(InstanceStateName.Running));
        DescribeInstancesResult describeInstancesResult2 =
                new DescribeInstancesResult()
                        .withReservations(new Reservation().withInstances(
                                instance2,
                                new Instance()
                                        .withInstanceId("i-3")
                                        .withState(new InstanceState().withName(InstanceStateName.Terminated))
                        ));

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(describeInstancesResult1)
                .thenReturn(describeInstancesResult2);

        // when
        Map<String, Instance> described = new EC2Api().describeInstances(amazonEC2, instanceIds);

        // then
        Assert.assertEquals(ImmutableMap.of("i-1", instance1, "i-2", instance2), described);
        verify(amazonEC2, times(2))
                .describeInstances(any(DescribeInstancesRequest.class));
    }

    @Test
    public void describeInstances_shouldNotDescribeMissedInResultInstanceOrTerminatedOrStoppedOrStoppingOrShuttingDownAs() {
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
        Map<String, Instance> described = new EC2Api().describeInstances(amazonEC2, instanceIds);

        // then
        Assert.assertEquals(Collections.<String, Instance>emptyMap(), described);
        verify(amazonEC2, times(1))
                .describeInstances(any(DescribeInstancesRequest.class));
    }

    @Test
    public void describeInstances_shouldSendInOneCallNoMoreThenBatchSizeOfInstance() {
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
        new EC2Api().describeInstances(amazonEC2, instanceIds, 2);

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
    public void describeInstances_shouldHandleAmazonEc2NotFoundErrorAsTerminatedInstancesAndRetry() {
        // given
        Set<String> instanceIds = new HashSet<>();
        instanceIds.add("i-1");
        instanceIds.add("i-f");
        instanceIds.add("i-3");

        AmazonEC2Exception notFoundException = new AmazonEC2Exception(
                "The instance IDs 'i-1, i-f' do not exist");
        notFoundException.setErrorCode("InvalidInstanceID.NotFound");

        final Instance instance3 = new Instance().withInstanceId("i-3")
                .withState(new InstanceState().withName(InstanceStateName.Running));
        DescribeInstancesResult describeInstancesResult2 = new DescribeInstancesResult()
                .withReservations(new Reservation().withInstances(
                        instance3));

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .thenThrow(notFoundException)
                .thenReturn(describeInstancesResult2);

        // when
        final Map<String, Instance> described = new EC2Api().describeInstances(amazonEC2, instanceIds);

        // then
        Assert.assertEquals(ImmutableMap.of("i-3", instance3), described);
        verify(amazonEC2).describeInstances(new DescribeInstancesRequest().withInstanceIds(Arrays.asList("i-1", "i-3", "i-f")));
        verify(amazonEC2).describeInstances(new DescribeInstancesRequest().withInstanceIds(Arrays.asList("i-3")));
        verifyNoMoreInteractions(amazonEC2);
    }

    @Test
    public void describeInstances_shouldFailIfNotAbleToParseNotFoundExceptionFromEc2Api() {
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
            new EC2Api().describeInstances(amazonEC2, instanceIds);
            Assert.fail();
        } catch (AmazonEC2Exception exception) {
            Assert.assertSame(notFoundException, exception);
        }
    }

    @Test
    public void describeInstances_shouldThrowExceptionIfEc2DescribeFailsWithException() {
        // given
        Set<String> instanceIds = new HashSet<>();
        instanceIds.add("a");

        UnsupportedOperationException exception = new UnsupportedOperationException("test");
        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .thenThrow(exception);

        // when
        try {
            new EC2Api().describeInstances(amazonEC2, instanceIds);
            Assert.fail();
        } catch (UnsupportedOperationException e) {
            Assert.assertSame(exception, e);
        }
    }

    @Test
    public void tagInstances_shouldDoNothingIfNoInstancesPassed() {
        // when
        new EC2Api().tagInstances(amazonEC2, Collections.<String>emptySet(), "opa", "v");

        // then
        verifyZeroInteractions(amazonEC2);
    }

    @Test
    public void tagInstances_shouldTag() {
        // when
        new EC2Api().tagInstances(amazonEC2, ImmutableSet.of("i-1", "i-2"), "opa", "v");

        // then
        verify(amazonEC2).createTags(new CreateTagsRequest()
                .withResources(ImmutableSet.of("i-1", "i-2"))
                .withTags(new Tag().withKey("opa").withValue("v")));
        verifyNoMoreInteractions(amazonEC2);
    }

    @Test
    public void tagInstances_givenNullValueShouldTagWithEmptyValue() {
        // when
        new EC2Api().tagInstances(amazonEC2, ImmutableSet.of("i-1", "i-2"), "opa", null);

        // then
        verify(amazonEC2).createTags(new CreateTagsRequest()
                .withResources(ImmutableSet.of("i-1", "i-2"))
                .withTags(new Tag().withKey("opa").withValue("")));
        verifyNoMoreInteractions(amazonEC2);
    }

    @Test
    public void getEndpoint_returnNullIfRegionNameOrEndpointAreEmpty() {
        Assert.assertNull(new EC2Api().getEndpoint(null, null));
    }

    @Test
    public void getEndpoint_returnEnpointAsIsIfProvided() {
        Assert.assertEquals("mymy", new EC2Api().getEndpoint(null, "mymy"));
    }

    @Test
    public void getEndpoint_returnCraftedIfRegionNotInStatic() {
        Assert.assertEquals("https://ec2.non-real-region.amazonaws.com",
                new EC2Api().getEndpoint("non-real-region", null));
    }

    @Test
    public void getEndpoint_returnCraftedChinaIfRegionNotInStatic() {
        Assert.assertEquals("https://ec2.cn-non-real.amazonaws.com.cn",
                new EC2Api().getEndpoint("cn-non-real", null));
    }

    @Test
    public void getEndpoint_returnStaticRegionEndpoint() {
        Assert.assertEquals("https://ec2.cn-north-1.amazonaws.com.cn",
                new EC2Api().getEndpoint("cn-north-1", null));
    }

}
