package com.amazon.jenkins.ec2fleet.fleet;

import com.amazon.jenkins.ec2fleet.EC2Api;
import com.amazon.jenkins.ec2fleet.FleetStateStats;
import com.amazon.jenkins.ec2fleet.Registry;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.ActiveInstance;
import com.amazonaws.services.ec2.model.BatchState;
import com.amazonaws.services.ec2.model.DescribeSpotFleetInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsResult;
import com.amazonaws.services.ec2.model.FleetType;
import com.amazonaws.services.ec2.model.SpotFleetLaunchSpecification;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfig;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfigData;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import hudson.util.ListBoxModel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EC2SpotFleetTest {

    @Mock
    private AmazonEC2 ec2;

    @Mock
    private EC2Api ec2Api;

    @Before
    public void before() {
        Registry.setEc2Api(ec2Api);

        when(ec2Api.connect(anyString(), anyString(), anyString())).thenReturn(ec2);

        when(ec2.describeSpotFleetInstances(any(DescribeSpotFleetInstancesRequest.class)))
                .thenReturn(new DescribeSpotFleetInstancesResult());

        when(ec2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(new DescribeSpotFleetRequestsResult()
                        .withSpotFleetRequestConfigs(
                                new SpotFleetRequestConfig()
                                        .withSpotFleetRequestConfig(
                                                new SpotFleetRequestConfigData()
                                                        .withTargetCapacity(0))));
    }

    @After
    public void after() {
        Registry.setEc2Api(new EC2Api());
    }

    @Test(expected = IllegalStateException.class)
    public void getState_failIfNoFleet() {
        when(ec2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(new DescribeSpotFleetRequestsResult());

        new EC2SpotFleet().getState("cred", "region", "", "f");
    }

    @Test
    public void getState_returnFleetInfo() {
        when(ec2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(new DescribeSpotFleetRequestsResult()
                        .withSpotFleetRequestConfigs(
                                new SpotFleetRequestConfig()
                                        .withSpotFleetRequestState(BatchState.Active)
                                        .withSpotFleetRequestConfig(
                                                new SpotFleetRequestConfigData()
                                                        .withTargetCapacity(12))));

        FleetStateStats stats = new EC2SpotFleet().getState("cred", "region", "", "f-id");

        Assert.assertEquals("f-id", stats.getFleetId());
        Assert.assertEquals(FleetStateStats.State.active(), stats.getState());
        Assert.assertEquals(12, stats.getNumDesired());
    }

    @Test
    public void getState_returnEmptyIfNoInstancesForFleet() {
        FleetStateStats stats = new EC2SpotFleet().getState("cred", "region", "", "f");

        Assert.assertEquals(Collections.emptySet(), stats.getInstances());
        Assert.assertEquals(0, stats.getNumActive());
    }

    @Test
    public void getState_returnAllDescribedInstancesForFleet() {
        when(ec2.describeSpotFleetInstances(any(DescribeSpotFleetInstancesRequest.class)))
                .thenReturn(new DescribeSpotFleetInstancesResult()
                        .withActiveInstances(
                                new ActiveInstance().withInstanceId("i-1"),
                                new ActiveInstance().withInstanceId("i-2")));

        FleetStateStats stats = new EC2SpotFleet().getState("cred", "region", "", "f");

        Assert.assertEquals(ImmutableSet.of("i-1", "i-2"), stats.getInstances());
        Assert.assertEquals(2, stats.getNumActive());
        verify(ec2).describeSpotFleetInstances(new DescribeSpotFleetInstancesRequest()
                .withSpotFleetRequestId("f"));
    }

    @Test
    public void getState_returnAllPagesDescribedInstancesForFleet() {
        when(ec2.describeSpotFleetInstances(any(DescribeSpotFleetInstancesRequest.class)))
                .thenReturn(new DescribeSpotFleetInstancesResult()
                        .withNextToken("p1")
                        .withActiveInstances(new ActiveInstance().withInstanceId("i-1")))
                .thenReturn(new DescribeSpotFleetInstancesResult()
                        .withActiveInstances(new ActiveInstance().withInstanceId("i-2")));

        FleetStateStats stats = new EC2SpotFleet().getState("cred", "region", "", "f");

        Assert.assertEquals(ImmutableSet.of("i-1", "i-2"), stats.getInstances());
        Assert.assertEquals(2, stats.getNumActive());
        verify(ec2).describeSpotFleetInstances(new DescribeSpotFleetInstancesRequest()
                .withSpotFleetRequestId("f").withNextToken("p1"));
        verify(ec2).describeSpotFleetInstances(new DescribeSpotFleetInstancesRequest()
                .withSpotFleetRequestId("f"));
    }

    @Test
    public void getState_returnEmptyInstanceTypeWeightsIfNoInformation() {
        FleetStateStats stats = new EC2SpotFleet().getState("cred", "region", "", "f");

        Assert.assertEquals(Collections.emptyMap(), stats.getInstanceTypeWeights());
    }

    @Test
    public void getState_returnInstanceTypeWeightsFromLaunchSpecification() {
        when(ec2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(new DescribeSpotFleetRequestsResult()
                        .withSpotFleetRequestConfigs(new SpotFleetRequestConfig()
                                .withSpotFleetRequestState(BatchState.Active)
                                .withSpotFleetRequestConfig(new SpotFleetRequestConfigData()
                                        .withTargetCapacity(1)
                                        .withLaunchSpecifications(
                                                new SpotFleetLaunchSpecification().withInstanceType("t1").withWeightedCapacity(0.1),
                                                new SpotFleetLaunchSpecification().withInstanceType("t2").withWeightedCapacity(12.0)))));

        FleetStateStats stats = new EC2SpotFleet().getState("cred", "region", "", "f");

        Assert.assertEquals(ImmutableMap.of("t1", 0.1, "t2", 12.0), stats.getInstanceTypeWeights());
    }

    @Test
    public void getState_returnInstanceTypeWeightsForLaunchSpecificationIfItHasIt() {
        when(ec2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(new DescribeSpotFleetRequestsResult()
                        .withSpotFleetRequestConfigs(new SpotFleetRequestConfig()
                                .withSpotFleetRequestState(BatchState.Active)
                                .withSpotFleetRequestConfig(new SpotFleetRequestConfigData()
                                        .withTargetCapacity(1)
                                        .withLaunchSpecifications(
                                                new SpotFleetLaunchSpecification().withInstanceType("t1"),
                                                new SpotFleetLaunchSpecification().withWeightedCapacity(12.0)))));

        FleetStateStats stats = new EC2SpotFleet().getState("cred", "region", "", "f");

        Assert.assertEquals(Collections.emptyMap(), stats.getInstanceTypeWeights());
    }

    @Test
    public void describe_whenAllFleetsEnabled_shouldIncludeAllFleetsInAllStates() {
        // given
        when(ec2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(new DescribeSpotFleetRequestsResult().withSpotFleetRequestConfigs(
                        new SpotFleetRequestConfig()
                                .withSpotFleetRequestId("f1")
                                .withSpotFleetRequestState(BatchState.Active)
                                .withSpotFleetRequestConfig(new SpotFleetRequestConfigData()
                                        .withType(FleetType.Maintain)),
                        new SpotFleetRequestConfig()
                                .withSpotFleetRequestId("f2")
                                .withSpotFleetRequestState(BatchState.Modifying)
                                .withSpotFleetRequestConfig(new SpotFleetRequestConfigData()
                                        .withType(FleetType.Request))
                ));
        // when
        ListBoxModel model = new ListBoxModel();
        new EC2SpotFleet().describe("cred", "region", "", model, "selected", true);
        // then
        Assert.assertEquals(
                "[EC2 Spot Fleet - f1 (active) (maintain)=f1, EC2 Spot Fleet - f2 (modifying) (request)=f2]",
                model.toString());
    }

    @Test
    public void describe_whenAllFleetsDisabled_shouldSkipNonMaintain() {
        // given
        when(ec2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(new DescribeSpotFleetRequestsResult().withSpotFleetRequestConfigs(
                        new SpotFleetRequestConfig()
                                .withSpotFleetRequestId("f1")
                                .withSpotFleetRequestState(BatchState.Active)
                                .withSpotFleetRequestConfig(new SpotFleetRequestConfigData()
                                        .withType(FleetType.Maintain)),
                        new SpotFleetRequestConfig()
                                .withSpotFleetRequestId("f2")
                                .withSpotFleetRequestState(BatchState.Active)
                                .withSpotFleetRequestConfig(new SpotFleetRequestConfigData()
                                        .withType(FleetType.Request))
                ));
        // when
        ListBoxModel model = new ListBoxModel();
        new EC2SpotFleet().describe("cred", "region", "", model, "selected", false);
        // then
        Assert.assertEquals(
                "[EC2 Spot Fleet - f1 (active) (maintain)=f1]",
                model.toString());
    }

    @Test
    public void describe_whenAllFleetsDisabled_shouldSkipNonCancelledOrFailed() {
        // given
        when(ec2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(new DescribeSpotFleetRequestsResult().withSpotFleetRequestConfigs(
                        new SpotFleetRequestConfig()
                                .withSpotFleetRequestId("f1")
                                .withSpotFleetRequestState(BatchState.Active)
                                .withSpotFleetRequestConfig(new SpotFleetRequestConfigData()
                                        .withType(FleetType.Maintain)),
                        new SpotFleetRequestConfig()
                                .withSpotFleetRequestId("f2")
                                .withSpotFleetRequestState(BatchState.Cancelled_running)
                                .withSpotFleetRequestConfig(new SpotFleetRequestConfigData()
                                        .withType(FleetType.Maintain)),
                        new SpotFleetRequestConfig()
                                .withSpotFleetRequestId("f3")
                                .withSpotFleetRequestState(BatchState.Cancelled_terminating)
                                .withSpotFleetRequestConfig(new SpotFleetRequestConfigData()
                                        .withType(FleetType.Maintain)),
                        new SpotFleetRequestConfig()
                                .withSpotFleetRequestId("f3")
                                .withSpotFleetRequestState(BatchState.Failed)
                                .withSpotFleetRequestConfig(new SpotFleetRequestConfigData()
                                        .withType(FleetType.Maintain))
                ));
        // when
        ListBoxModel model = new ListBoxModel();
        new EC2SpotFleet().describe("cred", "region", "", model, "selected", false);
        // then
        Assert.assertEquals(
                "[EC2 Spot Fleet - f1 (active) (maintain)=f1]",
                model.toString());
    }

    @Test
    public void describe_whenAllFleetsDisabled_shouldIncludeSubmittedModifiedActive() {
        // given
        when(ec2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(new DescribeSpotFleetRequestsResult().withSpotFleetRequestConfigs(
                        new SpotFleetRequestConfig()
                                .withSpotFleetRequestId("f1")
                                .withSpotFleetRequestState(BatchState.Active)
                                .withSpotFleetRequestConfig(new SpotFleetRequestConfigData()
                                        .withType(FleetType.Maintain)),
                        new SpotFleetRequestConfig()
                                .withSpotFleetRequestId("f2")
                                .withSpotFleetRequestState(BatchState.Submitted)
                                .withSpotFleetRequestConfig(new SpotFleetRequestConfigData()
                                        .withType(FleetType.Maintain)),
                        new SpotFleetRequestConfig()
                                .withSpotFleetRequestId("f3")
                                .withSpotFleetRequestState(BatchState.Modifying)
                                .withSpotFleetRequestConfig(new SpotFleetRequestConfigData()
                                        .withType(FleetType.Maintain))
                ));
        // when
        ListBoxModel model = new ListBoxModel();
        new EC2SpotFleet().describe("cred", "region", "", model, "selected", false);
        // then
        Assert.assertEquals(
                "[EC2 Spot Fleet - f1 (active) (maintain)=f1, EC2 Spot Fleet - f2 (submitted) (maintain)=f2, EC2 Spot Fleet - f3 (modifying) (maintain)=f3]",
                model.toString());
    }

}
