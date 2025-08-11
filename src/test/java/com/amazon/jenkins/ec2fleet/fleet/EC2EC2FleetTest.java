package com.amazon.jenkins.ec2fleet.fleet;

import com.amazon.jenkins.ec2fleet.aws.EC2Api;
import com.amazon.jenkins.ec2fleet.FleetStateStats;
import com.amazon.jenkins.ec2fleet.Registry;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import hudson.util.ListBoxModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EC2EC2FleetTest {

    @Mock
    private Ec2Client ec2;

    @Mock
    private EC2Api ec2Api;

    @BeforeEach
    void before() {
        Registry.setEc2Api(ec2Api);

        when(ec2Api.connect(anyString(), anyString(), anyString())).thenReturn(ec2);

        when(ec2.describeFleetInstances(any(DescribeFleetInstancesRequest.class)))
                .thenReturn(DescribeFleetInstancesResponse.builder()
                .build());

        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(DescribeFleetsResponse.builder()
                .fleets(
                        FleetData.builder()
                                .targetCapacitySpecification(
                                        TargetCapacitySpecification.builder()
                                                .totalTargetCapacity(0)
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
        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                    .thenReturn(DescribeFleetsResponse.builder()
                            .build());
        assertThrows(IllegalStateException.class, () ->

            new EC2EC2Fleet().getState("cred", "region", "", "f"));
    }

    @Test
    void getState_returnFleetInfo() {
        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(DescribeFleetsResponse.builder()
                .fleets(
                        FleetData.builder()
                                .fleetState(String.valueOf(BatchState.ACTIVE))
                                .targetCapacitySpecification(
                                        TargetCapacitySpecification.builder()
                                                .totalTargetCapacity(12)
                                        .build())
                        .build())
                .build());

        FleetStateStats stats = new EC2EC2Fleet().getState("cred", "region", "", "f-id");

        assertEquals("f-id", stats.getFleetId());
        assertEquals(FleetStateStats.State.active(), stats.getState());
        assertEquals(12, stats.getNumDesired());
    }

    @Test
    void getState_returnEmptyIfNoInstancesForFleet() {
        FleetStateStats stats = new EC2EC2Fleet().getState("cred", "region", "", "f");

        assertEquals(Collections.emptySet(), stats.getInstances());
        assertEquals(0, stats.getNumActive());
    }

    @Test
    void getState_returnAllDescribedInstancesForFleet() {
        when(ec2.describeFleetInstances(any(DescribeFleetInstancesRequest.class)))
                .thenReturn(DescribeFleetInstancesResponse.builder()
                .activeInstances(
                        ActiveInstance.builder().instanceId("i-1")
                        .build(), 
                        ActiveInstance.builder().instanceId("i-2")
                        .build())
                .build());

        FleetStateStats stats = new EC2EC2Fleet().getState("cred", "region", "", "f");

        assertEquals(new HashSet<>(Arrays.asList("i-1", "i-2")), stats.getInstances());
        assertEquals(2, stats.getNumActive());
        verify(ec2).describeFleetInstances(DescribeFleetInstancesRequest.builder()
                .fleetId("f")
                .build());
    }


    @Test
    void getState_returnAllPagesDescribedInstancesForFleet() {
        when(ec2.describeFleetInstances(any(DescribeFleetInstancesRequest.class)))
                .thenReturn(DescribeFleetInstancesResponse.builder()
                        .nextToken("p1")
                        .activeInstances(ActiveInstance.builder().instanceId("i-1")
                                .build())
                        .build())
                .thenReturn(DescribeFleetInstancesResponse.builder()
                .activeInstances(ActiveInstance.builder().instanceId("i-2")
                        .build())
                .build());

        FleetStateStats stats = new EC2EC2Fleet().getState("cred", "region", "", "f");

        assertEquals(new HashSet<>(Arrays.asList("i-1", "i-2")), stats.getInstances());
        assertEquals(2, stats.getNumActive());
        verify(ec2).describeFleetInstances(DescribeFleetInstancesRequest.builder()
                .fleetId("f").nextToken("p1")
                .build());
        verify(ec2).describeFleetInstances(DescribeFleetInstancesRequest.builder()
                .fleetId("f")
                .build());
    }

    @Test
    void getState_returnEmptyInstanceTypeWeightsIfNoInformation() {
        FleetStateStats stats = new EC2EC2Fleet().getState("cred", "region", "", "f");

        assertEquals(Collections.emptyMap(), stats.getInstanceTypeWeights());
    }

    @Test
    void getState_returnInstanceTypeWeightsFromLaunchSpecification() {
        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(DescribeFleetsResponse.builder()
                .fleets(FleetData.builder()
                        .fleetState(String.valueOf(BatchState.ACTIVE))
                        .targetCapacitySpecification(TargetCapacitySpecification.builder()
                                .totalTargetCapacity(1)
                                .build())
                        .launchTemplateConfigs(FleetLaunchTemplateConfig.builder()
                                .overrides(
                                        FleetLaunchTemplateOverrides.builder().instanceType("t3a.small").weightedCapacity(0.1)
                                        .build(), 
                                        FleetLaunchTemplateOverrides.builder().instanceType("t3a.medium").weightedCapacity(12.0)
                                        .build())
                                .build())
                        .build())
                .build());

        FleetStateStats stats = new EC2EC2Fleet().getState("cred", "region", "", "f");

        Map<String, Double> expected = new HashMap<>();
        expected.put("t3a.small", 0.1);
        expected.put("t3a.medium", 12.0);
        assertEquals(expected, stats.getInstanceTypeWeights());
    }

    @Test
    void getState_returnInstanceTypeWeightsForLaunchSpecificationIfItHasIt() {
        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(DescribeFleetsResponse.builder()
                .fleets(FleetData.builder()
                        .fleetState(String.valueOf(BatchState.ACTIVE))
                        .targetCapacitySpecification(TargetCapacitySpecification.builder()
                                .totalTargetCapacity(1)
                                .build())
                        .launchTemplateConfigs(FleetLaunchTemplateConfig.builder()
                                .overrides(
                                        FleetLaunchTemplateOverrides.builder().instanceType("t1")
                                        .build(), 
                                        FleetLaunchTemplateOverrides.builder().weightedCapacity(12.0)
                                        .build())
                                .build())
                        .build())
                .build());

        FleetStateStats stats = new EC2EC2Fleet().getState("cred", "region", "", "f");

        assertEquals(Collections.emptyMap(), stats.getInstanceTypeWeights());
    }

    @Test
    void getStateBatch_withNoFleetIdsAndNoFleets_returnsAnEmptyMap() {
        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(DescribeFleetsResponse.builder()
                .build());

        Collection<String> fleetIds = new ArrayList<>();

        Map<String, FleetStateStats> fleetStateStatsMap = new EC2EC2Fleet().getStateBatch("cred", "region", "", fleetIds);

        assertTrue(fleetStateStatsMap.isEmpty(), "FleetStateStats Map is expected to be empty when no Fleet Ids are given");
    }

    @Test
    void getStateBatch_withFleetIdsAndNoFleets_returnsMapWithNoInstances() {
        when(ec2.describeFleetInstances(any(DescribeFleetInstancesRequest.class)))
                .thenReturn(DescribeFleetInstancesResponse.builder()
                .build());

        Collection<String> fleetIds = new ArrayList<>();
        fleetIds.add("f1");
        fleetIds.add("f2");

        Map<String, FleetStateStats> fleetStateStatsMap = new EC2EC2Fleet().getStateBatch("cred", "region", "", fleetIds);

        assertTrue(fleetStateStatsMap.isEmpty());
    }

    @Test
    void getBatchState_withFleetsAndActiveInstances_returnsDescribedInstancesForFleets() {
        when(ec2.describeFleetInstances(any(DescribeFleetInstancesRequest.class)))
                .thenReturn(DescribeFleetInstancesResponse.builder()
                .fleetId("f1")
                .activeInstances(
                        ActiveInstance.builder().instanceId("i-1")
                        .build(), 
                        ActiveInstance.builder().instanceId("i-2")
                        .build())
                .build(),
                DescribeFleetInstancesResponse.builder()
                        .fleetId("f2")
                        .activeInstances(
                                ActiveInstance.builder().instanceId("i-3")
                                .build()
                        )
                        .build());

        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(DescribeFleetsResponse.builder()
                .fleets(
                        FleetData.builder()
                                .fleetId("f1")
                                .fleetState(String.valueOf(BatchState.ACTIVE))
                                .targetCapacitySpecification(
                                        TargetCapacitySpecification.builder()
                                                .totalTargetCapacity(4)
                                        .build())
                        .build(), 
                        FleetData.builder()
                                .fleetId("f2")
                                .fleetState(String.valueOf(BatchState.ACTIVE))
                                .targetCapacitySpecification(
                                        TargetCapacitySpecification.builder()
                                                .totalTargetCapacity(8)
                                        .build())
                        .build())
                .build());

        Collection<String> fleetIds = new ArrayList<>();
        fleetIds.add("f1");
        fleetIds.add("f2");

        Map<String, FleetStateStats> statsMap = new EC2EC2Fleet().getStateBatch("cred", "region", "", fleetIds);

        assertEquals(new HashSet<>(Arrays.asList("i-1", "i-2")), statsMap.get("f1").getInstances());
        assertEquals(new HashSet<>(Collections.singletonList("i-3")), statsMap.get("f2").getInstances());
        assertEquals(2, statsMap.get("f1").getNumActive());
        assertEquals(1, statsMap.get("f2").getNumActive());
        verify(ec2).describeFleetInstances(DescribeFleetInstancesRequest.builder()
                .fleetId("f1")
                .build());
        verify(ec2).describeFleetInstances(DescribeFleetInstancesRequest.builder()
                .fleetId("f2")
                .build());
    }

    @Test
    void getBatchState_withFleets_returnsDescribedFleetStats() {
        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(DescribeFleetsResponse.builder()
                .fleets(
                        FleetData.builder()
                                .fleetId("f1")
                                .fleetState(String.valueOf(BatchState.ACTIVE))
                                .targetCapacitySpecification(
                                        TargetCapacitySpecification.builder()
                                                .totalTargetCapacity(2)
                                        .build())
                        .build(), 
                        FleetData.builder()
                                .fleetId("f2")
                                .fleetState(String.valueOf(BatchState.MODIFYING))
                                .targetCapacitySpecification(
                                        TargetCapacitySpecification.builder()
                                                .totalTargetCapacity(6)
                                        .build())
                        .build())
                .build());

        Collection<String> fleetIds = new ArrayList<>();
        fleetIds.add("f1");
        fleetIds.add("f2");

        Map<String, FleetStateStats> statsMap = new EC2EC2Fleet().getStateBatch("cred", "region", "", fleetIds);

        assertTrue(statsMap.get("f1").getState().isActive());
        assertTrue(statsMap.get("f2").getState().isModifying());
        assertEquals(2, statsMap.get("f1").getNumDesired());
        assertEquals(6, statsMap.get("f2").getNumDesired());
    }


    @Test
    void describe_whenAllFleetsEnabled_shouldIncludeAllFleetsInAllStates() {
        // given
        DescribeFleetsResponse response = DescribeFleetsResponse.builder().fleets(
                FleetData.builder()
                        .fleetId("f1")
                        .fleetState(String.valueOf(BatchState.ACTIVE))
                        .type(FleetType.MAINTAIN)
                .build(), 
                FleetData.builder()
                        .fleetId("f2")
                        .fleetState(String.valueOf(BatchState.MODIFYING))
                        .type(FleetType.REQUEST)
                .build())
                .build();
        lenient().when(ec2.describeFleets(any(DescribeFleetsRequest.class))).thenReturn(response);
        // paginator mock
        software.amazon.awssdk.services.ec2.paginators.DescribeFleetsIterable paginator = Mockito.mock(software.amazon.awssdk.services.ec2.paginators.DescribeFleetsIterable.class);
        when(paginator.iterator()).thenReturn(Collections.singleton(response).iterator());
        when(ec2.describeFleetsPaginator(any(DescribeFleetsRequest.class))).thenReturn(paginator);
        // when
        ListBoxModel model = new ListBoxModel();
        new EC2EC2Fleet().describe("cred", "region", "", model, "selected", true);
        // then
        assertEquals(
                "[EC2 Fleet - f1 (active) (maintain)=f1, EC2 Fleet - f2 (modifying) (request)=f2]",
                model.toString());
    }

    @Test
    void describe_whenAllFleetsDisabled_shouldSkipNonMaintain() {
        // given
        DescribeFleetsResponse response = DescribeFleetsResponse.builder().fleets(
                FleetData.builder()
                        .fleetId("f1")
                        .fleetState(String.valueOf(BatchState.ACTIVE))
                        .type(FleetType.MAINTAIN)
                .build(), 
                FleetData.builder()
                        .fleetId("f2")
                        .fleetState(String.valueOf(BatchState.ACTIVE))
                        .type(FleetType.REQUEST)
                .build())
                .build();
        lenient().when(ec2.describeFleets(any(DescribeFleetsRequest.class))).thenReturn(response);
        // paginator mock
        software.amazon.awssdk.services.ec2.paginators.DescribeFleetsIterable paginator = Mockito.mock(software.amazon.awssdk.services.ec2.paginators.DescribeFleetsIterable.class);
        when(paginator.iterator()).thenReturn(Collections.singleton(response).iterator());
        when(ec2.describeFleetsPaginator(any(DescribeFleetsRequest.class))).thenReturn(paginator);
        // when
        ListBoxModel model = new ListBoxModel();
        new EC2EC2Fleet().describe("cred", "region", "", model, "selected", false);
        // then
        assertEquals(
                "[EC2 Fleet - f1 (active) (maintain)=f1]",
                model.toString());
    }

    @Test
    void describe_whenAllFleetsDisabled_shouldSkipNonCancelledOrFailed() {
        // given
        DescribeFleetsResponse response = DescribeFleetsResponse.builder().fleets(
                FleetData.builder()
                        .fleetId("f1")
                        .fleetState(String.valueOf(BatchState.ACTIVE))
                        .type(FleetType.MAINTAIN)
                .build(), 
                FleetData.builder()
                        .fleetId("f2")
                        .fleetState(String.valueOf(BatchState.CANCELLED_RUNNING))
                        .type(FleetType.MAINTAIN)
                .build(), 
                FleetData.builder()
                        .fleetId("f3")
                        .fleetState(String.valueOf(BatchState.CANCELLED_TERMINATING))
                        .type(FleetType.MAINTAIN)
                .build(), 
                FleetData.builder()
                        .fleetId("f3")
                        .fleetState(String.valueOf(BatchState.FAILED))
                        .type(FleetType.MAINTAIN)
                .build())
                .build();
        lenient().when(ec2.describeFleets(any(DescribeFleetsRequest.class))).thenReturn(response);
        // paginator mock
        software.amazon.awssdk.services.ec2.paginators.DescribeFleetsIterable paginator = Mockito.mock(software.amazon.awssdk.services.ec2.paginators.DescribeFleetsIterable.class);
        when(paginator.iterator()).thenReturn(Collections.singleton(response).iterator());
        when(ec2.describeFleetsPaginator(any(DescribeFleetsRequest.class))).thenReturn(paginator);
        // when
        ListBoxModel model = new ListBoxModel();
        new EC2EC2Fleet().describe("cred", "region", "", model, "selected", false);
        // then
        assertEquals(
                "[EC2 Fleet - f1 (active) (maintain)=f1]",
                model.toString());
    }

    @Test
    void describe_whenAllFleetsDisabled_shouldIncludeSubmittedModifiedActive() {
        // given
        DescribeFleetsResponse response = DescribeFleetsResponse.builder().fleets(
                FleetData.builder()
                        .fleetId("f1")
                        .fleetState(String.valueOf(BatchState.ACTIVE))
                        .type(FleetType.MAINTAIN)
                .build(), 
                FleetData.builder()
                        .fleetId("f2")
                        .fleetState(String.valueOf(BatchState.SUBMITTED))
                        .type(FleetType.MAINTAIN)
                .build(), 
                FleetData.builder()
                        .fleetId("f3")
                        .fleetState(String.valueOf(BatchState.MODIFYING))
                        .type(FleetType.MAINTAIN)
                .build())
                .build();
        lenient().when(ec2.describeFleets(any(DescribeFleetsRequest.class))).thenReturn(response);
        // paginator mock
        software.amazon.awssdk.services.ec2.paginators.DescribeFleetsIterable paginator = Mockito.mock(software.amazon.awssdk.services.ec2.paginators.DescribeFleetsIterable.class);
        when(paginator.iterator()).thenReturn(Collections.singleton(response).iterator());
        when(ec2.describeFleetsPaginator(any(DescribeFleetsRequest.class))).thenReturn(paginator);
        // when
        ListBoxModel model = new ListBoxModel();
        new EC2EC2Fleet().describe("cred", "region", "", model, "selected", false);
        // then
        assertEquals(
                "[EC2 Fleet - f1 (active) (maintain)=f1, EC2 Fleet - f2 (submitted) (maintain)=f2, EC2 Fleet - f3 (modifying) (maintain)=f3]",
                model.toString());
    }

    @Test
    void isEC2EC2Fleet_withFleetId_returnsTrue() {
        String fleetId = "fleet-123456";
        boolean isEC2EC2Fleet = EC2Fleets.isEC2EC2Fleet(fleetId);

        assertTrue(isEC2EC2Fleet);
    }

    @Test
    void isEC2EC2Fleet_withNonFleetId_returnsFalse() {
        String fleetId = "sfr-123456";
        boolean isEC2EC2Fleet = EC2Fleets.isEC2EC2Fleet(fleetId);

        assertFalse(isEC2EC2Fleet);
    }
}

