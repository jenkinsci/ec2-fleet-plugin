package com.amazon.jenkins.ec2fleet;

import hudson.model.Label;
import hudson.model.LoadStatistics;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NodeProvisioner.StrategyState.class})
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
        when(strategy.getClouds()).thenReturn(clouds);
        when(state.getSnapshot()).thenReturn(snapshot);
    }

    @Test
    public void givenNoRequiredCapacity_shouldDoNotScale() {
        final EC2FleetCloud ec2FleetCloud = mock(EC2FleetCloud.class);
        clouds.add(ec2FleetCloud);

        strategy.apply(state);

        verify(ec2FleetCloud, never()).canProvision(any(Label.class));
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

        verify(ec2FleetCloud, never()).canProvision(any(Label.class));
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
        when(ec2FleetCloud.canProvision(any(Label.class))).thenReturn(true);
        when(ec2FleetCloud.isNoDelayProvision()).thenReturn(false);

        Assert.assertEquals(
                NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES,
                strategy.apply(state));
        verify(ec2FleetCloud, never()).provision(any(Label.class), anyInt());
    }

    @Test
    public void givenEC2CloudWhichCannotProvision_shouldDoNotScale() {
        when(snapshot.getQueueLength()).thenReturn(10);
        when(state.getLabel()).thenReturn(label);

        final EC2FleetCloud ec2FleetCloud = mock(EC2FleetCloud.class);
        clouds.add(ec2FleetCloud);
        when(ec2FleetCloud.canProvision(any(Label.class))).thenReturn(false);

        Assert.assertEquals(
                NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES,
                strategy.apply(state));
        verify(ec2FleetCloud, never()).provision(any(Label.class), anyInt());
    }

    @Test
    public void givenEC2CloudsWithEnabledNoDelayAndWithout_shouldDoScalingForOne() {
        when(snapshot.getQueueLength()).thenReturn(10);
        when(state.getLabel()).thenReturn(label);

        final EC2FleetCloud ec2FleetCloud1 = mock(EC2FleetCloud.class);
        clouds.add(ec2FleetCloud1);
        final EC2FleetCloud ec2FleetCloud2 = mock(EC2FleetCloud.class);
        clouds.add(ec2FleetCloud2);
        when(ec2FleetCloud1.canProvision(any(Label.class))).thenReturn(true);
        when(ec2FleetCloud2.canProvision(any(Label.class))).thenReturn(true);
        when(ec2FleetCloud1.isNoDelayProvision()).thenReturn(true);
        when(ec2FleetCloud2.isNoDelayProvision()).thenReturn(false);

        Assert.assertEquals(
                NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES,
                strategy.apply(state));
        verify(ec2FleetCloud1, times(1)).provision(label, 10);
        verify(ec2FleetCloud2, never()).provision(any(Label.class), anyInt());
    }

    @Test
    public void givenEC2CloudsWhenOneCanCoverCapacity_shouldDoScalingForFirstOnly() {
        when(snapshot.getQueueLength()).thenReturn(2);
        when(state.getLabel()).thenReturn(label);

        final EC2FleetCloud ec2FleetCloud1 = mock(EC2FleetCloud.class);
        clouds.add(ec2FleetCloud1);
        final EC2FleetCloud ec2FleetCloud2 = mock(EC2FleetCloud.class);
        clouds.add(ec2FleetCloud2);
        when(ec2FleetCloud1.canProvision(any(Label.class))).thenReturn(true);
        when(ec2FleetCloud2.canProvision(any(Label.class))).thenReturn(true);
        when(ec2FleetCloud1.isNoDelayProvision()).thenReturn(true);
        when(ec2FleetCloud2.isNoDelayProvision()).thenReturn(true);
        when(ec2FleetCloud1.provision(any(Label.class), anyInt())).thenReturn(Arrays.asList(
                mock(NodeProvisioner.PlannedNode.class),
                mock(NodeProvisioner.PlannedNode.class)
        ));

        Assert.assertEquals(
                NodeProvisioner.StrategyDecision.PROVISIONING_COMPLETED,
                strategy.apply(state));
        verify(ec2FleetCloud1, times(1)).provision(label, 2);
        verify(ec2FleetCloud2, never()).provision(any(Label.class), anyInt());
    }

    @Test
    public void givenEC2Clouds_shouldDoScalingAndReduceForNextOne() {
        when(snapshot.getQueueLength()).thenReturn(5);
        when(state.getLabel()).thenReturn(label);

        final EC2FleetCloud ec2FleetCloud1 = mock(EC2FleetCloud.class);
        clouds.add(ec2FleetCloud1);
        final EC2FleetCloud ec2FleetCloud2 = mock(EC2FleetCloud.class);
        clouds.add(ec2FleetCloud2);
        when(ec2FleetCloud1.canProvision(any(Label.class))).thenReturn(true);
        when(ec2FleetCloud2.canProvision(any(Label.class))).thenReturn(true);
        when(ec2FleetCloud1.isNoDelayProvision()).thenReturn(true);
        when(ec2FleetCloud2.isNoDelayProvision()).thenReturn(true);
        when(ec2FleetCloud1.provision(any(Label.class), anyInt())).thenReturn(Arrays.asList(
                mock(NodeProvisioner.PlannedNode.class),
                mock(NodeProvisioner.PlannedNode.class)
        ));

        Assert.assertEquals(
                NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES,
                strategy.apply(state));
        verify(ec2FleetCloud1, times(1)).provision(label, 5);
        verify(ec2FleetCloud2, times(1)).provision(label, 3);
    }

}
