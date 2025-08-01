package com.amazon.jenkins.ec2fleet.fleet;

import com.amazon.jenkins.ec2fleet.aws.EC2Api;
import com.amazon.jenkins.ec2fleet.FleetStateStats;
import com.amazon.jenkins.ec2fleet.Registry;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import hudson.util.ListBoxModel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EC2EC2FleetTest {

    @Mock
    private Ec2Client ec2;

    @Mock
    private EC2Api ec2Api;

    @Before
    public void before() {
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

    @After
    public void after() {
        Registry.setEc2Api(new EC2Api());
    }

    @Test(expected = IllegalStateException.class)
    public void getState_failIfNoFleet() {
        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(DescribeFleetsResponse.builder()
                .build());

        new EC2EC2Fleet().getState("cred", "region", "", "f");
    }

    @Test
    public void getState_returnFleetInfo() {
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

        Assert.assertEquals("f-id", stats.getFleetId());
        Assert.assertEquals(FleetStateStats.State.active(), stats.getState());
        Assert.assertEquals(12, stats.getNumDesired());
    }

    @Test
    public void getState_returnEmptyIfNoInstancesForFleet() {
        FleetStateStats stats = new EC2EC2Fleet().getState("cred", "region", "", "f");

        Assert.assertEquals(Collections.emptySet(), stats.getInstances());
        Assert.assertEquals(0, stats.getNumActive());
    }

    @Test
    public void getState_returnAllDescribedInstancesForFleet() {
        when(ec2.describeFleetInstances(any(DescribeFleetInstancesRequest.class)))
                .thenReturn(DescribeFleetInstancesResponse.builder()
                .activeInstances(
                        ActiveInstance.builder().instanceId("i-1")
                        .build(), 
                        ActiveInstance.builder().instanceId("i-2")
                        .build())
                .build());

        FleetStateStats stats = new EC2EC2Fleet().getState("cred", "region", "", "f");

        Assert.assertEquals(new HashSet<>(Arrays.asList("i-1", "i-2")), stats.getInstances());
        Assert.assertEquals(2, stats.getNumActive());
        verify(ec2).describeFleetInstances(DescribeFleetInstancesRequest.builder()
                .fleetId("f")
                .build());
    }



    @Test
    public void getState_returnAllPagesDescribedInstancesForFleet() {
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

        Assert.assertEquals(new HashSet<>(Arrays.asList("i-1", "i-2")), stats.getInstances());
        Assert.assertEquals(2, stats.getNumActive());
        verify(ec2).describeFleetInstances(DescribeFleetInstancesRequest.builder()
                .fleetId("f").nextToken("p1")
                .build());
        verify(ec2).describeFleetInstances(DescribeFleetInstancesRequest.builder()
                .fleetId("f")
                .build());
    }

    @Test
    public void getState_returnEmptyInstanceTypeWeightsIfNoInformation() {
        FleetStateStats stats = new EC2EC2Fleet().getState("cred", "region", "", "f");

        Assert.assertEquals(Collections.emptyMap(), stats.getInstanceTypeWeights());
    }

    @Test
    public void getState_returnInstanceTypeWeightsFromLaunchSpecification() {
        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(DescribeFleetsResponse.builder()
                .fleets(FleetData.builder()
                        .fleetState(String.valueOf(BatchState.ACTIVE))
                        .targetCapacitySpecification(TargetCapacitySpecification.builder()
                                .totalTargetCapacity(1)
                                .build())
                        .launchTemplateConfigs(FleetLaunchTemplateConfig.builder()
                                .overrides(
                                        FleetLaunchTemplateOverrides.builder().instanceType("t1").weightedCapacity(0.1)
                                        .build(), 
                                        FleetLaunchTemplateOverrides.builder().instanceType("t2").weightedCapacity(12.0)
                                        .build())
                                .build())
                        .build())
                .build());

        FleetStateStats stats = new EC2EC2Fleet().getState("cred", "region", "", "f");

        Map<String, Double> expected = new HashMap<>();
        expected.put("t1", 0.1);
        expected.put("t2", 12.0);
        Assert.assertEquals(expected, stats.getInstanceTypeWeights());
    }

    @Test
    public void getState_returnInstanceTypeWeightsForLaunchSpecificationIfItHasIt() {
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

        Assert.assertEquals(Collections.emptyMap(), stats.getInstanceTypeWeights());
    }

    @Test
    public void getStateBatch_withNoFleetIdsAndNoFleets_returnsAnEmptyMap() {
        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(DescribeFleetsResponse.builder()
                .build());

        Collection<String> fleetIds = new ArrayList<>();

        Map<String, FleetStateStats> fleetStateStatsMap = new EC2EC2Fleet().getStateBatch("cred", "region", "", fleetIds);

        Assert.assertTrue("FleetStateStats Map is expected to be empty when no Fleet Ids are given", fleetStateStatsMap.isEmpty());
    }

    @Test
    public void getStateBatch_withFleetIdsAndNoFleets_returnsMapWithNoInstances() {
        when(ec2.describeFleetInstances(any(DescribeFleetInstancesRequest.class)))
                .thenReturn(DescribeFleetInstancesResponse.builder()
                .build());

        Collection<String> fleetIds = new ArrayList<>();
        fleetIds.add("f1");
        fleetIds.add("f2");

        Map<String, FleetStateStats> fleetStateStatsMap = new EC2EC2Fleet().getStateBatch("cred", "region", "", fleetIds);

        Assert.assertTrue(fleetStateStatsMap.isEmpty());
    }

    @Test
    public void getBatchState_withFleetsAndActiveInstances_returnsDescribedInstancesForFleets() {
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

        Assert.assertEquals(new HashSet<>(Arrays.asList("i-1", "i-2")), statsMap.get("f1").getInstances());
        Assert.assertEquals(new HashSet<>(Collections.singletonList("i-3")), statsMap.get("f2").getInstances());
        Assert.assertEquals(2, statsMap.get("f1").getNumActive());
        Assert.assertEquals(1, statsMap.get("f2").getNumActive());
        verify(ec2).describeFleetInstances(DescribeFleetInstancesRequest.builder()
                .fleetId("f1")
                .build());
        verify(ec2).describeFleetInstances(DescribeFleetInstancesRequest.builder()
                .fleetId("f2")
                .build());
    }

    @Test
    public void getBatchState_withFleets_returnsDescribedFleetStats() {
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

        Assert.assertTrue(statsMap.get("f1").getState().isActive());
        Assert.assertTrue(statsMap.get("f2").getState().isModifying());
        Assert.assertEquals(2, statsMap.get("f1").getNumDesired());
        Assert.assertEquals(6, statsMap.get("f2").getNumDesired());
    }


    @Test
    public void describe_whenAllFleetsEnabled_shouldIncludeAllFleetsInAllStates() {
        // given
        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(DescribeFleetsResponse.builder().fleets(
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
                .build());
        // when
        ListBoxModel model = new ListBoxModel();
        new EC2EC2Fleet().describe("cred", "region", "", model, "selected", true);
        // then
        Assert.assertEquals(
                "[EC2 Fleet - f1 (active) (maintain)=f1, EC2 Fleet - f2 (modifying) (request)=f2]",
                model.toString());
    }

    @Test
    public void describe_whenAllFleetsDisabled_shouldSkipNonMaintain() {
        // given
        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(DescribeFleetsResponse.builder().fleets(
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
                .build());
        // when
        ListBoxModel model = new ListBoxModel();
        new EC2EC2Fleet().describe("cred", "region", "", model, "selected", false);
        // then
        Assert.assertEquals(
                "[EC2 Fleet - f1 (active) (maintain)=f1]",
                model.toString());
    }

    @Test
    public void describe_whenAllFleetsDisabled_shouldSkipNonCancelledOrFailed() {
        // given
        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(DescribeFleetsResponse.builder().fleets(
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
                .build());
        // when
        ListBoxModel model = new ListBoxModel();
        new EC2EC2Fleet().describe("cred", "region", "", model, "selected", false);
        // then
        Assert.assertEquals(
                "[EC2 Fleet - f1 (active) (maintain)=f1]",
                model.toString());
    }

    @Test
    public void describe_whenAllFleetsDisabled_shouldIncludeSubmittedModifiedActive() {
        // given
        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(DescribeFleetsResponse.builder().fleets(
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
                .build());
        // when
        ListBoxModel model = new ListBoxModel();
        new EC2EC2Fleet().describe("cred", "region", "", model, "selected", false);
        // then
        Assert.assertEquals(
                "[EC2 Fleet - f1 (active) (maintain)=f1, EC2 Fleet - f2 (submitted) (maintain)=f2, EC2 Fleet - f3 (modifying) (maintain)=f3]",
                model.toString());
    }

    @Test
    public void isEC2EC2Fleet_withFleetId_returnsTrue() {
        String fleetId = "fleet-123456";
        boolean isEC2EC2Fleet = EC2Fleets.isEC2EC2Fleet(fleetId);

        Assert.assertTrue(isEC2EC2Fleet);
    }
    @Test
    public void isEC2EC2Fleet_withNonFleetId_returnsFalse() {
        String fleetId = "sfr-123456";
        boolean isEC2EC2Fleet = EC2Fleets.isEC2EC2Fleet(fleetId);

        Assert.assertFalse(isEC2EC2Fleet);
    }
}