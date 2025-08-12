package com.amazon.jenkins.ec2fleet.fleet;

import com.amazon.jenkins.ec2fleet.aws.EC2Api;
import com.amazon.jenkins.ec2fleet.FleetStateStats;
import com.amazon.jenkins.ec2fleet.Registry;
import hudson.util.ListBoxModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EC2SpotFleetTest {

    @Mock
    private Ec2Client ec2;

    @Mock
    private EC2Api ec2Api;

    @BeforeEach
    void before() {
        Registry.setEc2Api(ec2Api);

        when(ec2Api.connect(anyString(), anyString(), anyString())).thenReturn(ec2);

        when(ec2.describeSpotFleetInstances(any(DescribeSpotFleetInstancesRequest.class)))
                .thenReturn(DescribeSpotFleetInstancesResponse.builder()
                .build());

        when(ec2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(DescribeSpotFleetRequestsResponse.builder()
                .spotFleetRequestConfigs(
                        SpotFleetRequestConfig.builder()
                                .spotFleetRequestConfig(
                                        SpotFleetRequestConfigData.builder()
                                                .targetCapacity(0)
                                        .build())
                        .build())
                .build());
    }

    @AfterEach
    void after() {
        Registry.setEc2Api(new EC2Api());
    }

    @Test
    void getState_failIfNoFleet() {
        when(ec2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                    .thenReturn(DescribeSpotFleetRequestsResponse.builder()
                            .build());
        assertThrows(IllegalStateException.class, () ->

            new EC2SpotFleet().getState("cred", "region", "", "f"));
    }

    @Test
    void getState_returnFleetInfo() {
        when(ec2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(DescribeSpotFleetRequestsResponse.builder()
                .spotFleetRequestConfigs(
                        SpotFleetRequestConfig.builder()
                                .spotFleetRequestState(BatchState.ACTIVE)
                                .spotFleetRequestConfig(
                                        SpotFleetRequestConfigData.builder()
                                                .targetCapacity(12)
                                        .build())
                        .build())
                .build());

        FleetStateStats stats = new EC2SpotFleet().getState("cred", "region", "", "f-id");

        assertEquals("f-id", stats.getFleetId());
        assertEquals(FleetStateStats.State.active(), stats.getState());
        assertEquals(12, stats.getNumDesired());
    }

    @Test
    void getState_returnEmptyIfNoInstancesForFleet() {
        FleetStateStats stats = new EC2SpotFleet().getState("cred", "region", "", "f");

        assertEquals(Collections.emptySet(), stats.getInstances());
        assertEquals(0, stats.getNumActive());
    }

    @Test
    void getState_returnAllDescribedInstancesForFleet() {
        when(ec2.describeSpotFleetInstances(any(DescribeSpotFleetInstancesRequest.class)))
                .thenReturn(DescribeSpotFleetInstancesResponse.builder()
                .activeInstances(
                        ActiveInstance.builder().instanceId("i-1")
                        .build(), 
                        ActiveInstance.builder().instanceId("i-2")
                        .build())
                .build());

        FleetStateStats stats = new EC2SpotFleet().getState("cred", "region", "", "f");

        assertEquals(new HashSet<>(Arrays.asList("i-1", "i-2")), stats.getInstances());
        assertEquals(2, stats.getNumActive());
        verify(ec2).describeSpotFleetInstances(DescribeSpotFleetInstancesRequest.builder()
                .spotFleetRequestId("f")
                .build());
    }

    @Test
    void getState_returnAllPagesDescribedInstancesForFleet() {
        when(ec2.describeSpotFleetInstances(any(DescribeSpotFleetInstancesRequest.class)))
                .thenReturn(DescribeSpotFleetInstancesResponse.builder()
                        .nextToken("p1")
                        .activeInstances(ActiveInstance.builder().instanceId("i-1")
                                .build())
                        .build())
                .thenReturn(DescribeSpotFleetInstancesResponse.builder()
                .activeInstances(ActiveInstance.builder().instanceId("i-2")
                        .build())
                .build());

        FleetStateStats stats = new EC2SpotFleet().getState("cred", "region", "", "f");

        assertEquals(new HashSet<>(Arrays.asList("i-1", "i-2")), stats.getInstances());
        assertEquals(2, stats.getNumActive());
        verify(ec2).describeSpotFleetInstances(DescribeSpotFleetInstancesRequest.builder()
                .spotFleetRequestId("f").nextToken("p1")
                .build());
        verify(ec2).describeSpotFleetInstances(DescribeSpotFleetInstancesRequest.builder()
                .spotFleetRequestId("f")
                .build());
    }

    @Test
    void getState_returnEmptyInstanceTypeWeightsIfNoInformation() {
        FleetStateStats stats = new EC2SpotFleet().getState("cred", "region", "", "f");

        assertEquals(Collections.emptyMap(), stats.getInstanceTypeWeights());
    }

    @Test
    void getState_returnInstanceTypeWeightsFromLaunchSpecification() {
        when(ec2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(DescribeSpotFleetRequestsResponse.builder()
                .spotFleetRequestConfigs(SpotFleetRequestConfig.builder()
                        .spotFleetRequestState(BatchState.ACTIVE)
                        .spotFleetRequestConfig(SpotFleetRequestConfigData.builder()
                                .targetCapacity(1)
                                .launchSpecifications(
                                        SpotFleetLaunchSpecification.builder().instanceType("t3a.small").weightedCapacity(0.1)
                                        .build(), 
                                        SpotFleetLaunchSpecification.builder().instanceType("t3a.medium").weightedCapacity(12.0)
                                        .build())
                                .build())
                        .build())
                .build());

        FleetStateStats stats = new EC2SpotFleet().getState("cred", "region", "", "f");

        Map<String, Double> expected = new HashMap<>();
        expected.put("t3a.small", 0.1);
        expected.put("t3a.medium", 12.0);
        assertEquals(expected, stats.getInstanceTypeWeights());
    }

    @Test
    void getState_returnInstanceTypeWeightsForLaunchSpecificationIfItHasIt() {
        when(ec2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(DescribeSpotFleetRequestsResponse.builder()
                .spotFleetRequestConfigs(SpotFleetRequestConfig.builder()
                        .spotFleetRequestState(BatchState.ACTIVE)
                        .spotFleetRequestConfig(SpotFleetRequestConfigData.builder()
                                .targetCapacity(1)
                                .launchSpecifications(
                                        SpotFleetLaunchSpecification.builder().instanceType("t1")
                                        .build(), 
                                        SpotFleetLaunchSpecification.builder().weightedCapacity(12.0)
                                        .build())
                                .build())
                        .build())
                .build());

        FleetStateStats stats = new EC2SpotFleet().getState("cred", "region", "", "f");

        assertEquals(Collections.emptyMap(), stats.getInstanceTypeWeights());
    }

    @Test
    void describe_whenAllFleetsEnabled_shouldIncludeAllFleetsInAllStates() {
        // given
        DescribeSpotFleetRequestsResponse response = DescribeSpotFleetRequestsResponse.builder().spotFleetRequestConfigs(
                SpotFleetRequestConfig.builder()
                        .spotFleetRequestId("f1")
                        .spotFleetRequestState(BatchState.ACTIVE)
                        .spotFleetRequestConfig(SpotFleetRequestConfigData.builder()
                                .type(FleetType.MAINTAIN)
                                .build())
                .build(), 
                SpotFleetRequestConfig.builder()
                        .spotFleetRequestId("f2")
                        .spotFleetRequestState(BatchState.MODIFYING)
                        .spotFleetRequestConfig(SpotFleetRequestConfigData.builder()
                                .type(FleetType.REQUEST)
                                .build())
                .build()
        )
        .build();
        lenient().when(ec2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class))).thenReturn(response);
        // paginator mock
        software.amazon.awssdk.services.ec2.paginators.DescribeSpotFleetRequestsIterable paginator = Mockito.mock(software.amazon.awssdk.services.ec2.paginators.DescribeSpotFleetRequestsIterable.class);
        when(paginator.iterator()).thenReturn(Collections.singleton(response).iterator());
        when(ec2.describeSpotFleetRequestsPaginator(any(DescribeSpotFleetRequestsRequest.class))).thenReturn(paginator);
        // when
        ListBoxModel model = new ListBoxModel();
        new EC2SpotFleet().describe("cred", "region", "", model, "selected", true);
        // then
        assertEquals(
                "[EC2 Spot Fleet - f1 (active) (maintain)=f1, EC2 Spot Fleet - f2 (modifying) (request)=f2]",
                model.toString());
    }

    @Test
    void describe_whenAllFleetsDisabled_shouldSkipNonMaintain() {
        // given
        DescribeSpotFleetRequestsResponse response = DescribeSpotFleetRequestsResponse.builder().spotFleetRequestConfigs(
                SpotFleetRequestConfig.builder()
                        .spotFleetRequestId("f1")
                        .spotFleetRequestState(BatchState.ACTIVE)
                        .spotFleetRequestConfig(SpotFleetRequestConfigData.builder()
                                .type(FleetType.MAINTAIN)
                                .build())
                .build(), 
                SpotFleetRequestConfig.builder()
                        .spotFleetRequestId("f2")
                        .spotFleetRequestState(BatchState.ACTIVE)
                        .spotFleetRequestConfig(SpotFleetRequestConfigData.builder()
                                .type(FleetType.REQUEST)
                                .build())
                .build()
        )
        .build();
        lenient().when(ec2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class))).thenReturn(response);
        // paginator mock
        software.amazon.awssdk.services.ec2.paginators.DescribeSpotFleetRequestsIterable paginator = Mockito.mock(software.amazon.awssdk.services.ec2.paginators.DescribeSpotFleetRequestsIterable.class);
        when(paginator.iterator()).thenReturn(Collections.singleton(response).iterator());
        when(ec2.describeSpotFleetRequestsPaginator(any(DescribeSpotFleetRequestsRequest.class))).thenReturn(paginator);
        // when
        ListBoxModel model = new ListBoxModel();
        new EC2SpotFleet().describe("cred", "region", "", model, "selected", false);
        // then
        assertEquals(
                "[EC2 Spot Fleet - f1 (active) (maintain)=f1]",
                model.toString());
    }

    @Test
    void describe_whenAllFleetsDisabled_shouldSkipNonCancelledOrFailed() {
        // given
        DescribeSpotFleetRequestsResponse response = DescribeSpotFleetRequestsResponse.builder().spotFleetRequestConfigs(
                SpotFleetRequestConfig.builder()
                        .spotFleetRequestId("f1")
                        .spotFleetRequestState(BatchState.ACTIVE)
                        .spotFleetRequestConfig(SpotFleetRequestConfigData.builder()
                                .type(FleetType.MAINTAIN)
                                .build())
                .build(), 
                SpotFleetRequestConfig.builder()
                        .spotFleetRequestId("f2")
                        .spotFleetRequestState(BatchState.CANCELLED_RUNNING)
                        .spotFleetRequestConfig(SpotFleetRequestConfigData.builder()
                                .type(FleetType.MAINTAIN)
                                .build())
                .build(), 
                SpotFleetRequestConfig.builder()
                        .spotFleetRequestId("f3")
                        .spotFleetRequestState(BatchState.CANCELLED_TERMINATING)
                        .spotFleetRequestConfig(SpotFleetRequestConfigData.builder()
                                .type(FleetType.MAINTAIN)
                                .build())
                .build(), 
                SpotFleetRequestConfig.builder()
                        .spotFleetRequestId("f3")
                        .spotFleetRequestState(BatchState.FAILED)
                        .spotFleetRequestConfig(SpotFleetRequestConfigData.builder()
                                .type(FleetType.MAINTAIN)
                                .build())
                .build()
        )
        .build();
        lenient().when(ec2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class))).thenReturn(response);
        // paginator mock
        software.amazon.awssdk.services.ec2.paginators.DescribeSpotFleetRequestsIterable paginator = Mockito.mock(software.amazon.awssdk.services.ec2.paginators.DescribeSpotFleetRequestsIterable.class);
        when(paginator.iterator()).thenReturn(Collections.singleton(response).iterator());
        when(ec2.describeSpotFleetRequestsPaginator(any(DescribeSpotFleetRequestsRequest.class))).thenReturn(paginator);
        // when
        ListBoxModel model = new ListBoxModel();
        new EC2SpotFleet().describe("cred", "region", "", model, "selected", false);
        // then
        assertEquals(
                "[EC2 Spot Fleet - f1 (active) (maintain)=f1]",
                model.toString());
    }

    @Test
    void describe_whenAllFleetsDisabled_shouldIncludeSubmittedModifiedActive() {
        // given
        DescribeSpotFleetRequestsResponse response = DescribeSpotFleetRequestsResponse.builder().spotFleetRequestConfigs(
                SpotFleetRequestConfig.builder()
                        .spotFleetRequestId("f1")
                        .spotFleetRequestState(BatchState.ACTIVE)
                        .spotFleetRequestConfig(SpotFleetRequestConfigData.builder()
                                .type(FleetType.MAINTAIN)
                                .build())
                .build(), 
                SpotFleetRequestConfig.builder()
                        .spotFleetRequestId("f2")
                        .spotFleetRequestState(BatchState.SUBMITTED)
                        .spotFleetRequestConfig(SpotFleetRequestConfigData.builder()
                                .type(FleetType.MAINTAIN)
                                .build())
                .build(), 
                SpotFleetRequestConfig.builder()
                        .spotFleetRequestId("f3")
                        .spotFleetRequestState(BatchState.MODIFYING)
                        .spotFleetRequestConfig(SpotFleetRequestConfigData.builder()
                                .type(FleetType.MAINTAIN)
                                .build())
                .build()
        )
        .build();
        lenient().when(ec2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class))).thenReturn(response);
        // paginator mock
        software.amazon.awssdk.services.ec2.paginators.DescribeSpotFleetRequestsIterable paginator = Mockito.mock(software.amazon.awssdk.services.ec2.paginators.DescribeSpotFleetRequestsIterable.class);
        when(paginator.iterator()).thenReturn(Collections.singleton(response).iterator());
        when(ec2.describeSpotFleetRequestsPaginator(any(DescribeSpotFleetRequestsRequest.class))).thenReturn(paginator);
        // when
        ListBoxModel model = new ListBoxModel();
        new EC2SpotFleet().describe("cred", "region", "", model, "selected", false);
        // then
        assertEquals(
                "[EC2 Spot Fleet - f1 (active) (maintain)=f1, EC2 Spot Fleet - f2 (submitted) (maintain)=f2, EC2 Spot Fleet - f3 (modifying) (maintain)=f3]",
                model.toString());
    }

}
