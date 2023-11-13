package com.amazon.jenkins.ec2fleet.fleet;

import com.amazon.jenkins.ec2fleet.aws.EC2Api;
import com.amazon.jenkins.ec2fleet.FleetStateStats;
import com.amazon.jenkins.ec2fleet.Registry;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
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
    private AmazonEC2 ec2;

    @Mock
    private EC2Api ec2Api;

    @Before
    public void before() {
        Registry.setEc2Api(ec2Api);

        when(ec2Api.connect(anyString(), anyString(), anyString())).thenReturn(ec2);

        when(ec2.describeFleetInstances(any(DescribeFleetInstancesRequest.class)))
                .thenReturn(new DescribeFleetInstancesResult());

        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(new DescribeFleetsResult()
                        .withFleets(
                                new FleetData()
                                        .withTargetCapacitySpecification(
                                                new TargetCapacitySpecification()
                                                        .withTotalTargetCapacity(0))));
    }

    @After
    public void after() {
        Registry.setEc2Api(new EC2Api());
    }

    @Test(expected = IllegalStateException.class)
    public void getState_failIfNoFleet() {
        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(new DescribeFleetsResult());

        new EC2EC2Fleet().getState("cred", "region", "", "f");
    }

    @Test
    public void getState_returnFleetInfo() {
        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(new DescribeFleetsResult()
                        .withFleets(
                                new FleetData()
                                        .withFleetState(String.valueOf(BatchState.Active))
                                        .withTargetCapacitySpecification(
                                                new TargetCapacitySpecification()
                                                        .withTotalTargetCapacity(12))));

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
                .thenReturn(new DescribeFleetInstancesResult()
                        .withActiveInstances(
                                new ActiveInstance().withInstanceId("i-1"),
                                new ActiveInstance().withInstanceId("i-2")));

        FleetStateStats stats = new EC2EC2Fleet().getState("cred", "region", "", "f");

        Assert.assertEquals(new HashSet<>(Arrays.asList("i-1", "i-2")), stats.getInstances());
        Assert.assertEquals(2, stats.getNumActive());
        verify(ec2).describeFleetInstances(new DescribeFleetInstancesRequest()
                .withFleetId("f"));
    }



    @Test
    public void getState_returnAllPagesDescribedInstancesForFleet() {
        when(ec2.describeFleetInstances(any(DescribeFleetInstancesRequest.class)))
                .thenReturn(new DescribeFleetInstancesResult()
                        .withNextToken("p1")
                        .withActiveInstances(new ActiveInstance().withInstanceId("i-1")))
                .thenReturn(new DescribeFleetInstancesResult()
                        .withActiveInstances(new ActiveInstance().withInstanceId("i-2")));

        FleetStateStats stats = new EC2EC2Fleet().getState("cred", "region", "", "f");

        Assert.assertEquals(new HashSet<>(Arrays.asList("i-1", "i-2")), stats.getInstances());
        Assert.assertEquals(2, stats.getNumActive());
        verify(ec2).describeFleetInstances(new DescribeFleetInstancesRequest()
                .withFleetId("f").withNextToken("p1"));
        verify(ec2).describeFleetInstances(new DescribeFleetInstancesRequest()
                .withFleetId("f"));
    }

    @Test
    public void getState_returnEmptyInstanceTypeWeightsIfNoInformation() {
        FleetStateStats stats = new EC2EC2Fleet().getState("cred", "region", "", "f");

        Assert.assertEquals(Collections.emptyMap(), stats.getInstanceTypeWeights());
    }

    @Test
    public void getState_returnInstanceTypeWeightsFromLaunchSpecification() {
        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(new DescribeFleetsResult()
                        .withFleets(new FleetData()
                                .withFleetState(String.valueOf(BatchState.Active))
                                .withTargetCapacitySpecification(new TargetCapacitySpecification()
                                        .withTotalTargetCapacity(1))
                                .withLaunchTemplateConfigs(new FleetLaunchTemplateConfig()
                                        .withOverrides(
                                                new FleetLaunchTemplateOverrides().withInstanceType("t1").withWeightedCapacity(0.1),
                                                new FleetLaunchTemplateOverrides().withInstanceType("t2").withWeightedCapacity(12.0)))));

        FleetStateStats stats = new EC2EC2Fleet().getState("cred", "region", "", "f");

        Map<String, Double> expected = new HashMap<>();
        expected.put("t1", 0.1);
        expected.put("t2", 12.0);
        Assert.assertEquals(expected, stats.getInstanceTypeWeights());
    }

    @Test
    public void getState_returnInstanceTypeWeightsForLaunchSpecificationIfItHasIt() {
        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(new DescribeFleetsResult()
                        .withFleets(new FleetData()
                                .withFleetState(String.valueOf(BatchState.Active))
                                .withTargetCapacitySpecification(new TargetCapacitySpecification()
                                        .withTotalTargetCapacity(1))
                                .withLaunchTemplateConfigs(new FleetLaunchTemplateConfig()
                                        .withOverrides(
                                                new FleetLaunchTemplateOverrides().withInstanceType("t1"),
                                                new FleetLaunchTemplateOverrides().withWeightedCapacity(12.0)))));

        FleetStateStats stats = new EC2EC2Fleet().getState("cred", "region", "", "f");

        Assert.assertEquals(Collections.emptyMap(), stats.getInstanceTypeWeights());
    }

    @Test
    public void getStateBatch_withNoFleetIdsAndNoFleets_returnsAnEmptyMap() {
        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(new DescribeFleetsResult());

        Collection<String> fleetIds = new ArrayList<>();

        Map<String, FleetStateStats> fleetStateStatsMap = new EC2EC2Fleet().getStateBatch("cred", "region", "", fleetIds);

        Assert.assertTrue("FleetStateStats Map is expected to be empty when no Fleet Ids are given", fleetStateStatsMap.isEmpty());
    }

    @Test
    public void getStateBatch_withFleetIdsAndNoFleets_returnsMapWithNoInstances() {
        when(ec2.describeFleetInstances(any(DescribeFleetInstancesRequest.class)))
                .thenReturn(new DescribeFleetInstancesResult());

        Collection<String> fleetIds = new ArrayList<>();
        fleetIds.add("f1");
        fleetIds.add("f2");

        Map<String, FleetStateStats> fleetStateStatsMap = new EC2EC2Fleet().getStateBatch("cred", "region", "", fleetIds);

        Assert.assertTrue(fleetStateStatsMap.isEmpty());
    }

    @Test
    public void getBatchState_withFleetsAndActiveInstances_returnsDescribedInstancesForFleets() {
        when(ec2.describeFleetInstances(any(DescribeFleetInstancesRequest.class)))
                .thenReturn(new DescribeFleetInstancesResult()
                                .withFleetId("f1")
                                .withActiveInstances(
                                        new ActiveInstance().withInstanceId("i-1"),
                                        new ActiveInstance().withInstanceId("i-2")),
                        new DescribeFleetInstancesResult()
                                .withFleetId("f2")
                                .withActiveInstances(
                                        new ActiveInstance().withInstanceId("i-3")
                                ));

        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(new DescribeFleetsResult()
                        .withFleets(
                                new FleetData()
                                        .withFleetId("f1")
                                        .withFleetState(String.valueOf(BatchState.Active))
                                        .withTargetCapacitySpecification(
                                                new TargetCapacitySpecification()
                                                        .withTotalTargetCapacity(4)),
                                new FleetData()
                                        .withFleetId("f2")
                                        .withFleetState(String.valueOf(BatchState.Active))
                                        .withTargetCapacitySpecification(
                                                new TargetCapacitySpecification()
                                                        .withTotalTargetCapacity(8))));

        Collection<String> fleetIds = new ArrayList<>();
        fleetIds.add("f1");
        fleetIds.add("f2");

        Map<String, FleetStateStats> statsMap = new EC2EC2Fleet().getStateBatch("cred", "region", "", fleetIds);

        Assert.assertEquals(new HashSet<>(Arrays.asList("i-1", "i-2")), statsMap.get("f1").getInstances());
        Assert.assertEquals(new HashSet<>(Collections.singletonList("i-3")), statsMap.get("f2").getInstances());
        Assert.assertEquals(2, statsMap.get("f1").getNumActive());
        Assert.assertEquals(1, statsMap.get("f2").getNumActive());
        verify(ec2).describeFleetInstances(new DescribeFleetInstancesRequest()
                .withFleetId("f1"));
        verify(ec2).describeFleetInstances(new DescribeFleetInstancesRequest()
                .withFleetId("f2"));
    }

    @Test
    public void getBatchState_withFleets_returnsDescribedFleetStats() {
        when(ec2.describeFleets(any(DescribeFleetsRequest.class)))
                .thenReturn(new DescribeFleetsResult()
                        .withFleets(
                                new FleetData()
                                        .withFleetId("f1")
                                        .withFleetState(String.valueOf(BatchState.Active))
                                        .withTargetCapacitySpecification(
                                                new TargetCapacitySpecification()
                                                        .withTotalTargetCapacity(2)),
                                new FleetData()
                                        .withFleetId("f2")
                                        .withFleetState(String.valueOf(BatchState.Modifying))
                                        .withTargetCapacitySpecification(
                                                new TargetCapacitySpecification()
                                                        .withTotalTargetCapacity(6))));

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
                .thenReturn(new DescribeFleetsResult().withFleets(
                        new FleetData()
                                .withFleetId("f1")
                                .withFleetState(String.valueOf(BatchState.Active))
                                .withType(FleetType.Maintain),
                        new FleetData()
                                .withFleetId("f2")
                                .withFleetState(String.valueOf(BatchState.Modifying))
                                .withType(FleetType.Request)));
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
                .thenReturn(new DescribeFleetsResult().withFleets(
                        new FleetData()
                                .withFleetId("f1")
                                .withFleetState(String.valueOf(BatchState.Active))
                                .withType(FleetType.Maintain),
                        new FleetData()
                                .withFleetId("f2")
                                .withFleetState(String.valueOf(BatchState.Active))
                                .withType(FleetType.Request)));
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
                .thenReturn(new DescribeFleetsResult().withFleets(
                        new FleetData()
                                .withFleetId("f1")
                                .withFleetState(String.valueOf(BatchState.Active))
                                .withType(FleetType.Maintain),
                        new FleetData()
                                .withFleetId("f2")
                                .withFleetState(String.valueOf(BatchState.Cancelled_running))
                                .withType(FleetType.Maintain),
                        new FleetData()
                                .withFleetId("f3")
                                .withFleetState(String.valueOf(BatchState.Cancelled_terminating))
                                .withType(FleetType.Maintain),
                        new FleetData()
                                .withFleetId("f3")
                                .withFleetState(String.valueOf(BatchState.Failed))
                                .withType(FleetType.Maintain)));
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
                .thenReturn(new DescribeFleetsResult().withFleets(
                        new FleetData()
                                .withFleetId("f1")
                                .withFleetState(String.valueOf(BatchState.Active))
                                .withType(FleetType.Maintain),
                        new FleetData()
                                .withFleetId("f2")
                                .withFleetState(String.valueOf(BatchState.Submitted))
                                .withType(FleetType.Maintain),
                        new FleetData()
                                .withFleetId("f3")
                                .withFleetState(String.valueOf(BatchState.Modifying))
                                .withType(FleetType.Maintain)));
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