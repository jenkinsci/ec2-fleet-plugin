package com.amazon.jenkins.ec2fleet.aws;

import com.amazon.jenkins.ec2fleet.aws.EC2Api;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
@RunWith(MockitoJUnitRunner.class)
public class EC2ApiTest {

    @Mock
    private Ec2Client amazonEC2;

    @Test
    public void describeInstances_shouldReturnEmptyResultAndNoCallIfEmptyListOfInstances() {
        Map<String, Instance> described = new EC2Api().describeInstances(amazonEC2, Collections.<String>emptySet());

        Assert.assertEquals(Collections.<String, Instance>emptyMap(), described);
        verifyNoInteractions(amazonEC2);
    }

    @Test
    public void describeInstances_shouldReturnAllInstancesIfStillActive() {
        // given
        Set<String> instanceIds = new HashSet<>();
        instanceIds.add("i-1");
        instanceIds.add("i-2");

        DescribeInstancesResponse describeInstancesResult = DescribeInstancesResponse.builder()
                .build();
        Reservation reservation = Reservation.builder()
                .build();
        Instance instance1 = Instance.builder()
                .instanceId("i-1")
                .state(InstanceState.builder().name(InstanceStateName.RUNNING)
                        .build())
                .build();
        Instance instance2 = Instance.builder()
                .instanceId("i-2")
                .state(InstanceState.builder().name(InstanceStateName.RUNNING)
                        .build())
                .build();
        reservation = reservation.toBuilder().instances(Arrays.asList(instance1, instance2)).build();
        describeInstancesResult = describeInstancesResult.toBuilder().reservations(Arrays.asList(reservation)).build();

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResult);

        // when
        Map<String, Instance> described = new EC2Api().describeInstances(amazonEC2, instanceIds);

        // then
        Map<String, Instance> expected = new HashMap<>();
        expected.put("i-1", instance1);
        expected.put("i-2", instance2);
        Assert.assertEquals(expected, described);
        verify(amazonEC2, times(1))
                .describeInstances(any(DescribeInstancesRequest.class));
    }

    @Test
    public void describeInstances_shouldThrowExceptionIfNotInstanceNotFound() {
        // given
        Set<String> instanceIds = new HashSet<>();
        instanceIds.add("i-1");

        final Ec2Exception exception = (Ec2Exception) Ec2Exception.builder().message("NOT INSTANCE NOT FOUND").awsErrorDetails(AwsErrorDetails.builder().errorCode("NOT INSTANCE_NOT_FOUND_ERROR_CODE").build()).build();
        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .thenThrow(exception);

        // when
        try {
            new EC2Api().describeInstances(amazonEC2, instanceIds);
            Assert.fail();
        } catch (Ec2Exception e) {
            Assert.assertEquals("NOT INSTANCE NOT FOUND", e.awsErrorDetails().errorMessage());
            Assert.assertEquals(Ec2Exception.class, e.getClass());
        }
    }

    @Test
    public void describeInstances_shouldProcessAllPagesUntilNextTokenIsAvailable() {
        // given
        Set<String> instanceIds = new HashSet<>();
        instanceIds.add("i-1");
        instanceIds.add("i-2");
        instanceIds.add("i-3");

        final Instance instance1 = Instance.builder()
                .instanceId("i-1")
                .state(InstanceState.builder().name(InstanceStateName.RUNNING)
                        .build())
                .build();
        DescribeInstancesResponse describeInstancesResult1 =
                DescribeInstancesResponse.builder()
                        .reservations(
                                Reservation.builder().instances(instance1)
                                .build())
                        .nextToken("a")
                .build();

        final Instance instance2 = Instance.builder()
                .instanceId("i-2")
                .state(InstanceState.builder().name(InstanceStateName.RUNNING)
                        .build())
                .build();
        DescribeInstancesResponse describeInstancesResult2 =
                DescribeInstancesResponse.builder()
                        .reservations(Reservation.builder().instances(
                                instance2, 
                                Instance.builder()
                                        .instanceId("i-3")
                                        .state(InstanceState.builder().name(InstanceStateName.TERMINATED)
                                                .build())
                                .build()
                        )
                        .build())
                .build();

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(describeInstancesResult1)
                .thenReturn(describeInstancesResult2);

        // when
        Map<String, Instance> described = new EC2Api().describeInstances(amazonEC2, instanceIds);

        // then
        Map<String, Instance> expected = new HashMap<>();
        expected.put("i-1", instance1);
        expected.put("i-2", instance2);
        Assert.assertEquals(expected, described);
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

        DescribeInstancesResponse describeInstancesResult1 =
                DescribeInstancesResponse.builder()
                        .reservations(
                                Reservation.builder().instances(Instance.builder()
                                        .instanceId("stopped")
                                        .state(InstanceState.builder().name(InstanceStateName.STOPPED)
                                                .build())
                                        .build(), 
                                        Instance.builder()
                                                .instanceId("stopping")
                                                .state(InstanceState.builder().name(InstanceStateName.STOPPING)
                                                        .build())
                                        .build(), 
                                        Instance.builder()
                                                .instanceId("shutting-down")
                                                .state(InstanceState.builder().name(InstanceStateName.SHUTTING_DOWN)
                                                        .build())
                                        .build(), 
                                        Instance.builder()
                                                .instanceId("terminated")
                                                .state(InstanceState.builder().name(InstanceStateName.TERMINATED)
                                                        .build())
                                        .build()
                                )
                                .build())
                .build();


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

        DescribeInstancesResponse describeInstancesResult1 =
                DescribeInstancesResponse.builder()
                        .reservations(
                                Reservation.builder().instances(Instance.builder()
                                        .instanceId("stopped")
                                        .state(InstanceState.builder().name(InstanceStateName.RUNNING)
                                                .build())
                                        .build(), 
                                        Instance.builder()
                                                .instanceId("stopping")
                                                .state(InstanceState.builder().name(InstanceStateName.RUNNING)
                                                        .build())
                                        .build()
                                )
                                .build())
                .build();

        DescribeInstancesResponse describeInstancesResult2 = DescribeInstancesResponse.builder()
                .build();

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(describeInstancesResult1)
                .thenReturn(describeInstancesResult2);

        // when
        new EC2Api().describeInstances(amazonEC2, instanceIds, 2);

        // then
        verify(amazonEC2).describeInstances(DescribeInstancesRequest.builder().instanceIds(Arrays.asList("i1", "i2"))
                .build());
        verify(amazonEC2).describeInstances(DescribeInstancesRequest.builder().instanceIds(Arrays.asList("i3"))
                .build());
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

        Ec2Exception notFoundException = (Ec2Exception) Ec2Exception.builder().message("The instance IDs 'i-1, i-f' do not exist").awsErrorDetails(AwsErrorDetails.builder().errorCode("InvalidInstanceID.NotFound").build()).build();

        final Instance instance3 = Instance.builder().instanceId("i-3")
                .state(InstanceState.builder().name(InstanceStateName.RUNNING)
                        .build())
                .build();
        DescribeInstancesResponse describeInstancesResult2 = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().instances(
                        instance3)
                        .build())
                .build();

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .thenThrow(notFoundException)
                .thenReturn(describeInstancesResult2);

        // when
        final Map<String, Instance> described = new EC2Api().describeInstances(amazonEC2, instanceIds);

        // then
        Assert.assertEquals(Collections.singletonMap("i-3", instance3), described);
        verify(amazonEC2).describeInstances(DescribeInstancesRequest.builder().instanceIds(Arrays.asList("i-1", "i-3", "i-f"))
                .build());
        verify(amazonEC2).describeInstances(DescribeInstancesRequest.builder().instanceIds(Arrays.asList("i-3"))
                .build());
        verifyNoMoreInteractions(amazonEC2);
    }

    @Test
    public void describeInstances_shouldFailIfNotAbleToParseNotFoundExceptionFromEc2Api() {
        // given
        Set<String> instanceIds = new HashSet<>();
        instanceIds.add("i-1");
        instanceIds.add("i-f");
        instanceIds.add("i-3");

        Ec2Exception notFoundException = (Ec2Exception) Ec2Exception.builder().message("unparseable").awsErrorDetails(AwsErrorDetails.builder().errorCode("InvalidInstanceID.NotFound").build()).build();

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .thenThrow(notFoundException);

        // when
        try {
            new EC2Api().describeInstances(amazonEC2, instanceIds);
            Assert.fail();
        } catch (Ec2Exception exception) {
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
        verifyNoInteractions(amazonEC2);
    }

    @Test
    public void tagInstances_shouldTag() {
        // when
        new EC2Api().tagInstances(amazonEC2, new HashSet<>(Arrays.asList("i-1", "i-2")), "opa", "v");

        // then
        verify(amazonEC2).createTags(CreateTagsRequest.builder()
                .resources(new HashSet<>(Arrays.asList("i-1", "i-2")))
                .tags(Tag.builder().key("opa").value("v")
                        .build())
                .build());
        verifyNoMoreInteractions(amazonEC2);
    }

    @Test
    public void tagInstances_givenNullValueShouldTagWithEmptyValue() {
        // when
        new EC2Api().tagInstances(amazonEC2, new HashSet<>(Arrays.asList("i-1", "i-2")), "opa", null);

        // then
        verify(amazonEC2).createTags(CreateTagsRequest.builder()
                .resources(new HashSet<>(Arrays.asList("i-1", "i-2")))
                .tags(Tag.builder().key("opa").value("")
                        .build())
                .build());
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

    @Test
    public void testTerminateInstance() {
        final EC2Api client = new EC2Api();

        client.terminateInstances(amazonEC2, Arrays.asList("i-123"));

        verify(amazonEC2, times(1)).terminateInstances(
                Mockito.any(TerminateInstancesRequest.class));
    }

    @Test (expected = Ec2Exception.class)
    public void testTerminateInstanceRethrowException() {
        final Ec2Exception exception = (Ec2Exception) Ec2Exception.builder().message("You are not authorized to perform this operation").statusCode(403).build();
        when(amazonEC2.terminateInstances(TerminateInstancesRequest.builder().instanceIds(Arrays.asList("i-123"))
                        .build()))
                .thenThrow(exception);
        final EC2Api client = new EC2Api();

        client.terminateInstances(amazonEC2, Arrays.asList("i-123"));

        verify(amazonEC2, times(1)).terminateInstances(
                Mockito.any(TerminateInstancesRequest.class));
    }

}
