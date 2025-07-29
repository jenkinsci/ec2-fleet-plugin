package com.amazon.jenkins.ec2fleet;

import hudson.model.Label;
import hudson.model.LoadStatistics;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@RunWith(MockitoJUnitRunner.class)
public class NoDelayProvisionStrategyTest {

    @Mock
    private NodeProvisioner.StrategyState state;

    @Mock
    private LoadStatistics.LoadStatisticsSnapshot snapshot;

    @Mock
    private Label label;

    private NoDelayProvisionStrategy strategy;

    private List<Cloud> clouds = new ArrayList<>();

    @Before
    public void before() {
        strategy = spy(new NoDelayProvisionStrategy());
        doReturn(clouds).when(strategy).getClouds();
        when(state.getSnapshot()).thenReturn(snapshot);
    }

    @Test
    public void givenNoRequiredCapacity_shouldDoNotScale() {
        final EC2FleetCloud ec2FleetCloud = mock(EC2FleetCloud.class);
        clouds.add(ec2FleetCloud);

        strategy.apply(state);

        verify(ec2FleetCloud, never()).canProvision(any(Cloud.CloudState.class));
    }

    @Test
    public void givenAvailableSameAsRequiredCapacity_shouldDoNotScale() {
        final EC2FleetCloud ec2FleetCloud = mock(EC2FleetCloud.class);
        clouds.add(ec2FleetCloud);
        when(snapshot.getQueueLength()).thenReturn(10);
        when(snapshot.getAvailableExecutors()).thenReturn(10);

        Assert.assertEquals(
                NodeProvisioner.StrategyDecision.PROVISIONING_COMPLETED,
                strategy.apply(state));

        verify(ec2FleetCloud, never()).canProvision(any(Cloud.CloudState.class));
    }

    @Test
    public void givenNoEC2Cloud_shouldDoNotScale() {
        when(snapshot.getQueueLength()).thenReturn(10);

        Assert.assertEquals(
                NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES,
                strategy.apply(state));
    }

    @Test
    public void givenNonEC2Cloud_shouldDoNotScale() {
        when(snapshot.getQueueLength()).thenReturn(10);
        clouds.add(mock(Cloud.class));

        Assert.assertEquals(
                NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES,
                strategy.apply(state));
    }

    @Test
    public void givenEC2CloudWithDisabledNoDelay_shouldDoNotScale() {
        when(snapshot.getQueueLength()).thenReturn(10);
        when(state.getLabel()).thenReturn(label);

        final EC2FleetCloud ec2FleetCloud = mock(EC2FleetCloud.class);
        clouds.add(ec2FleetCloud);
        when(ec2FleetCloud.canProvision(any(Cloud.CloudState.class))).thenReturn(true);
        when(ec2FleetCloud.isNoDelayProvision()).thenReturn(false);

        Assert.assertEquals(
                NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES,
                strategy.apply(state));
        verify(ec2FleetCloud, never()).provision(any(Cloud.CloudState.class), anyInt());
    }

    @Test
    public void givenEC2CloudWhichCannotProvision_shouldDoNotScale() {
        when(snapshot.getQueueLength()).thenReturn(10);
        when(state.getLabel()).thenReturn(label);

        final EC2FleetCloud ec2FleetCloud = mock(EC2FleetCloud.class);
        clouds.add(ec2FleetCloud);
        when(ec2FleetCloud.canProvision(any(Cloud.CloudState.class))).thenReturn(false);

        Assert.assertEquals(
                NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES,
                strategy.apply(state));
        verify(ec2FleetCloud, never()).provision(any(Cloud.CloudState.class), anyInt());
    }

    @Test
    public void givenEC2CloudsWithEnabledNoDelayAndWithout_shouldDoScalingForOne() {
        when(snapshot.getQueueLength()).thenReturn(10);
        when(state.getLabel()).thenReturn(label);
        when(state.getAdditionalPlannedCapacity()).thenReturn(0);

        final EC2FleetCloud ec2FleetCloud1 = mock(EC2FleetCloud.class);
        clouds.add(ec2FleetCloud1);
        final EC2FleetCloud ec2FleetCloud2 = mock(EC2FleetCloud.class);
        clouds.add(ec2FleetCloud2);
        when(ec2FleetCloud1.canProvision(any(Cloud.CloudState.class))).thenReturn(true);
        when(ec2FleetCloud2.canProvision(any(Cloud.CloudState.class))).thenReturn(true);
        when(ec2FleetCloud1.isNoDelayProvision()).thenReturn(true);
        when(ec2FleetCloud2.isNoDelayProvision()).thenReturn(false);

        Assert.assertEquals(
                NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES,
                strategy.apply(state));

        ArgumentCaptor<Cloud.CloudState> cloudStateArgCaptor = ArgumentCaptor.forClass(Cloud.CloudState.class);
        verify(ec2FleetCloud1, times(1)).provision(cloudStateArgCaptor.capture(), eq(10));
        Assert.assertEquals(label, cloudStateArgCaptor.getValue().getLabel());
        Assert.assertEquals(0, cloudStateArgCaptor.getValue().getAdditionalPlannedCapacity());
        verify(ec2FleetCloud2, never()).provision(any(Cloud.CloudState.class), anyInt());
    }

    @Test
    public void givenEC2CloudsWhenOneCanCoverCapacity_shouldDoScalingForFirstOnly() {
        when(snapshot.getQueueLength()).thenReturn(2);
        when(state.getLabel()).thenReturn(label);

        final EC2FleetCloud ec2FleetCloud1 = mock(EC2FleetCloud.class);
        clouds.add(ec2FleetCloud1);
        final EC2FleetCloud ec2FleetCloud2 = mock(EC2FleetCloud.class, withSettings().strictness(Strictness.LENIENT));
        clouds.add(ec2FleetCloud2);
        when(ec2FleetCloud1.canProvision(any(Cloud.CloudState.class))).thenReturn(true);
        when(ec2FleetCloud2.canProvision(any(Cloud.CloudState.class))).thenReturn(true);
        when(ec2FleetCloud1.isNoDelayProvision()).thenReturn(true);
        when(ec2FleetCloud2.isNoDelayProvision()).thenReturn(true);
        when(ec2FleetCloud1.provision(any(Cloud.CloudState.class), anyInt())).thenReturn(Arrays.asList(
                new NodeProvisioner.PlannedNode("", new CompletableFuture<>(), 1),
                new NodeProvisioner.PlannedNode("", new CompletableFuture<>(), 1)
        ));

        Assert.assertEquals(
                NodeProvisioner.StrategyDecision.PROVISIONING_COMPLETED,
                strategy.apply(state));
        verify(ec2FleetCloud1, times(1)).provision(any(Cloud.CloudState.class), eq(2));
        verify(ec2FleetCloud2, never()).provision(any(Cloud.CloudState.class), anyInt());
    }

    @Test
    public void givenEC2Clouds_shouldDoScalingAndReduceForNextOne() {
        when(snapshot.getQueueLength()).thenReturn(5);
        when(state.getLabel()).thenReturn(label);

        final EC2FleetCloud ec2FleetCloud1 = mock(EC2FleetCloud.class);
        clouds.add(ec2FleetCloud1);
        final EC2FleetCloud ec2FleetCloud2 = mock(EC2FleetCloud.class);
        clouds.add(ec2FleetCloud2);
        when(ec2FleetCloud1.canProvision(any(Cloud.CloudState.class))).thenReturn(true);
        when(ec2FleetCloud2.canProvision(any(Cloud.CloudState.class))).thenReturn(true);
        when(ec2FleetCloud1.isNoDelayProvision()).thenReturn(true);
        when(ec2FleetCloud2.isNoDelayProvision()).thenReturn(true);
        when(ec2FleetCloud1.provision(any(Cloud.CloudState.class), anyInt())).thenReturn(Arrays.asList(
                new NodeProvisioner.PlannedNode("", new CompletableFuture<>(), 1),
                new NodeProvisioner.PlannedNode("", new CompletableFuture<>(), 1)
        ));

        Assert.assertEquals(
                NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES,
                strategy.apply(state));
        verify(ec2FleetCloud1, times(1)).provision(any(Cloud.CloudState.class), eq(5));
        verify(ec2FleetCloud2, times(1)).provision(any(Cloud.CloudState.class), eq(3));
    }

    @Test
    public void givenEC2Clouds_shouldReduceAsAmountOfExecutors() {
        when(snapshot.getQueueLength()).thenReturn(2);
        when(state.getLabel()).thenReturn(label);

        final EC2FleetCloud ec2FleetCloud1 = mock(EC2FleetCloud.class);
        clouds.add(ec2FleetCloud1);
        when(ec2FleetCloud1.canProvision(any(Cloud.CloudState.class))).thenReturn(true);
        when(ec2FleetCloud1.isNoDelayProvision()).thenReturn(true);
        final NodeProvisioner.PlannedNode plannedNode = new NodeProvisioner.PlannedNode("", new CompletableFuture<>(), 2);
        when(ec2FleetCloud1.provision(any(Cloud.CloudState.class), anyInt())).thenReturn(Arrays.asList(plannedNode));
        // then
        final NodeProvisioner.StrategyDecision decision = strategy.apply(state);
        // when
        Assert.assertEquals(NodeProvisioner.StrategyDecision.PROVISIONING_COMPLETED, decision);
        verify(ec2FleetCloud1, times(1)).provision(any(Cloud.CloudState.class), eq(2));
    }

    @Test
    public void givenPlannedCapacity_shouldComputeExcessWorkloadCorrectly() {
        when(snapshot.getQueueLength()).thenReturn(6);
        when(snapshot.getAvailableExecutors()).thenReturn(0);
        when(state.getPlannedCapacitySnapshot()).thenReturn(3);
        when(state.getLabel()).thenReturn(label);

        final EC2FleetCloud ec2FleetCloud1 = mock(EC2FleetCloud.class);
        clouds.add(ec2FleetCloud1);
        when(ec2FleetCloud1.canProvision(any(Cloud.CloudState.class))).thenReturn(true);
        when(ec2FleetCloud1.isNoDelayProvision()).thenReturn(true);
        final NodeProvisioner.PlannedNode plannedNode1 = new NodeProvisioner.PlannedNode("fc1-0", new CompletableFuture<>(), 2);
        when(ec2FleetCloud1.provision(any(Cloud.CloudState.class), anyInt())).thenReturn(Arrays.asList(plannedNode1));

        final EC2FleetCloud ec2FleetCloud2 = mock(EC2FleetCloud.class);
        clouds.add(ec2FleetCloud2);
        when(ec2FleetCloud2.canProvision(any(Cloud.CloudState.class))).thenReturn(true);
        when(ec2FleetCloud2.isNoDelayProvision()).thenReturn(true);
        final NodeProvisioner.PlannedNode plannedNode2 = new NodeProvisioner.PlannedNode("fc2-0", new CompletableFuture<>(), 1);
        when(ec2FleetCloud2.provision(any(Cloud.CloudState.class), anyInt())).thenReturn(Arrays.asList(plannedNode2));

        final NodeProvisioner.StrategyDecision decision = strategy.apply(state);

        Assert.assertEquals(NodeProvisioner.StrategyDecision.PROVISIONING_COMPLETED, decision);
        verify(ec2FleetCloud1, times(1)).provision(any(Cloud.CloudState.class), eq(3));
        verify(ec2FleetCloud2, times(1)).provision(any(Cloud.CloudState.class), eq(1));
    }

}
