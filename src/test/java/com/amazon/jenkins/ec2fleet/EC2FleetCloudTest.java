package com.amazon.jenkins.ec2fleet;

import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.BatchState;
import com.amazonaws.services.ec2.model.DescribeRegionsResult;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsResult;
import com.amazonaws.services.ec2.model.FleetType;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.ModifySpotFleetRequestRequest;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfig;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfigData;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import hudson.ExtensionList;
import hudson.model.LabelFinder;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.slaves.ComputerConnector;
import hudson.slaves.NodeProvisioner;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.model.Nodes;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class, EC2FleetCloud.class, EC2FleetCloud.DescriptorImpl.class,
        LabelFinder.class, FleetStateStats.class})
public class EC2FleetCloudTest {

    private SpotFleetRequestConfig spotFleetRequestConfig1;
    private SpotFleetRequestConfig spotFleetRequestConfig2;
    private SpotFleetRequestConfig spotFleetRequestConfig3;
    private SpotFleetRequestConfig spotFleetRequestConfig4;
    private SpotFleetRequestConfig spotFleetRequestConfig5;
    private SpotFleetRequestConfig spotFleetRequestConfig6;
    private SpotFleetRequestConfig spotFleetRequestConfig7;
    private SpotFleetRequestConfig spotFleetRequestConfig8;

    @Mock
    private Jenkins jenkins;

    @Mock
    private EC2Api ec2Api;

    @Mock
    private AmazonEC2 amazonEC2;

    @Before
    public void before() {
        spotFleetRequestConfig1 = new SpotFleetRequestConfig();
        spotFleetRequestConfig1.setSpotFleetRequestState(BatchState.Active);
        spotFleetRequestConfig1.setSpotFleetRequestConfig(new SpotFleetRequestConfigData().withType(FleetType.Maintain));
        spotFleetRequestConfig2 = new SpotFleetRequestConfig();
        spotFleetRequestConfig2.setSpotFleetRequestState(BatchState.Submitted);
        spotFleetRequestConfig2.setSpotFleetRequestConfig(new SpotFleetRequestConfigData().withType(FleetType.Maintain));
        spotFleetRequestConfig3 = new SpotFleetRequestConfig();
        spotFleetRequestConfig3.setSpotFleetRequestState(BatchState.Modifying);
        spotFleetRequestConfig3.setSpotFleetRequestConfig(new SpotFleetRequestConfigData().withType(FleetType.Maintain));
        spotFleetRequestConfig4 = new SpotFleetRequestConfig();
        spotFleetRequestConfig4.setSpotFleetRequestState(BatchState.Cancelled);
        spotFleetRequestConfig4.setSpotFleetRequestConfig(new SpotFleetRequestConfigData().withType(FleetType.Maintain));
        spotFleetRequestConfig5 = new SpotFleetRequestConfig();
        spotFleetRequestConfig5.setSpotFleetRequestState(BatchState.Cancelled_running);
        spotFleetRequestConfig5.setSpotFleetRequestConfig(new SpotFleetRequestConfigData().withType(FleetType.Maintain));
        spotFleetRequestConfig6 = new SpotFleetRequestConfig();
        spotFleetRequestConfig6.setSpotFleetRequestState(BatchState.Cancelled_terminating);
        spotFleetRequestConfig6.setSpotFleetRequestConfig(new SpotFleetRequestConfigData().withType(FleetType.Maintain));
        spotFleetRequestConfig7 = new SpotFleetRequestConfig();
        spotFleetRequestConfig7.setSpotFleetRequestState(BatchState.Failed);
        spotFleetRequestConfig7.setSpotFleetRequestConfig(new SpotFleetRequestConfigData().withType(FleetType.Maintain));
        spotFleetRequestConfig8 = new SpotFleetRequestConfig();
        spotFleetRequestConfig8.setSpotFleetRequestState(BatchState.Active);
        spotFleetRequestConfig8.setSpotFleetRequestConfig(new SpotFleetRequestConfigData().withType(FleetType.Request));

        Registry.setEc2Api(ec2Api);

        PowerMockito.mockStatic(LabelFinder.class);

        PowerMockito.mockStatic(FleetStateStats.class);

        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);
    }

    @After
    public void after() {
        Registry.setEc2Api(new EC2Api());
    }

    @Test
    public void provision_shouldProvisionNoneWhenMaxReached() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, "active",
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 0, 10, 1, false,
                false, false, 0, 0, false, 10);

        fleetCloud.setStats(new FleetStateStats("", 10, "active",
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        // when
        Collection<NodeProvisioner.PlannedNode> r = fleetCloud.provision(null, 1);

        // then
        assertEquals(0, r.size());
        assertEquals(0, fleetCloud.getToAdd());
    }

    @Test
    public void provision_shouldProvisionNoneWhenExceedMax() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, "active",
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 0, 9, 1, false,
                false, false, 0, 0, false, 10);

        fleetCloud.setStats(new FleetStateStats("", 10, "active",
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        // when
        Collection<NodeProvisioner.PlannedNode> r = fleetCloud.provision(null, 1);

        // then
        assertEquals(0, r.size());
        assertEquals(0, fleetCloud.getToAdd());
    }

    @Test
    public void provision_shouldProvisionIfBelowMax() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, "active",
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 0, 10, 1, false,
                false, false, 0, 0, false, 10);

        fleetCloud.setStats(new FleetStateStats("", 5, "active",
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        // when
        Collection<NodeProvisioner.PlannedNode> r = fleetCloud.provision(null, 1);

        // then
        assertEquals(1, r.size());
        assertEquals(1, fleetCloud.getToAdd());
    }

    @Test
    public void provision_shouldProvisionNoMoreMax() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, "active",
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 0, 10, 1, false,
                false, false, 0, 0, false, 10);

        fleetCloud.setStats(new FleetStateStats("", 5, "active",
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        // when
        Collection<NodeProvisioner.PlannedNode> r = fleetCloud.provision(null, 10);

        // then
        assertEquals(5, r.size());
        assertEquals(5, fleetCloud.getToAdd());
    }

    @Test
    public void provision_shouldProvisionNoMoreMaxWhenMultipleCallBeforeUpdate() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, "active",
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 0, 10, 1, false,
                false, false, 0, 0, false, 10);

        fleetCloud.setStats(new FleetStateStats("", 5, "active",
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        // when
        Collection<NodeProvisioner.PlannedNode> r1 = fleetCloud.provision(null, 2);
        Collection<NodeProvisioner.PlannedNode> r2 = fleetCloud.provision(null, 2);
        Collection<NodeProvisioner.PlannedNode> r3 = fleetCloud.provision(null, 5);

        // then
        assertEquals(2, r1.size());
        assertEquals(2, r2.size());
        assertEquals(1, r3.size());
        assertEquals(5, fleetCloud.getToAdd());
    }

    @Test
    public void provision_shouldProvisionNoneIfNotYetUpdated() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, "active",
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 0, 1, 1, false,
                false, false, 0, 0, false, 10);

        // when
        Collection<NodeProvisioner.PlannedNode> r = fleetCloud.provision(null, 1);

        // then
        assertEquals(0, r.size());
        assertEquals(0, fleetCloud.getToAdd());
    }

    @Test
    public void scheduleToTerminate_shouldNotRemoveIfStatsNotUpdated() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, "active",
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 0, 1, 1, false,
                false, false, 0, 0, false, 10);

        // when
        boolean r = fleetCloud.scheduleToTerminate("z");

        // then
        assertFalse(r);
    }

    @Test
    public void scheduleToTerminate_notRemoveIfBelowMin() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, "active",
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 1, 1, 1, false,
                false, false, 0, 0, false, 10);

        fleetCloud.setStats(new FleetStateStats("", 0, "active",
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        // when
        boolean r = fleetCloud.scheduleToTerminate("z");

        // then
        assertFalse(r);
    }

    @Test
    public void scheduleToTerminate_notRemoveIfEqualMin() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, "active",
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 1, 1, 1, false,
                false, false, 0, 0, false, 10);

        fleetCloud.setStats(new FleetStateStats("", 1, "active",
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        // when
        boolean r = fleetCloud.scheduleToTerminate("z");

        // then
        assertFalse(r);
    }

    @Test
    public void scheduleToTerminate_remove() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, "active",
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 1, 1, 1, false,
                false, false, 0, 0, false, 10);

        fleetCloud.setStats(new FleetStateStats("", 2, "active",
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        // when
        boolean r = fleetCloud.scheduleToTerminate("z");

        // then
        assertTrue(r);
        assertEquals(ImmutableSet.of("z"), fleetCloud.getInstanceIdsToTerminate());
    }

    @Test
    public void scheduleToTerminate_upToZeroNodes() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, "active",
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 0, 1, 1, false,
                false, false, 0, 0, false, 10);

        fleetCloud.setStats(new FleetStateStats("", 2, "active",
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        // when
        boolean r1 = fleetCloud.scheduleToTerminate("z-1");
        boolean r2 = fleetCloud.scheduleToTerminate("z-2");

        // then
        assertTrue(r1);
        assertTrue(r2);
        assertEquals(ImmutableSet.of("z-1", "z-2"), fleetCloud.getInstanceIdsToTerminate());
    }

    @Test
    public void scheduleToTerminate_removeNoMoreMinIfCalledMultipleBeforeUpdate() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, "active",
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 1, 1, 1, false,
                false, false, 0, 0, false, 10);

        fleetCloud.setStats(new FleetStateStats("", 3, "active",
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        // when
        boolean r1 = fleetCloud.scheduleToTerminate("z1");
        boolean r2 = fleetCloud.scheduleToTerminate("z2");
        boolean r3 = fleetCloud.scheduleToTerminate("z3");

        // then
        assertTrue(r1);
        assertTrue(r2);
        assertFalse(r3);
        assertEquals(ImmutableSet.of("z1", "z2"), fleetCloud.getInstanceIdsToTerminate());
    }

    @Test
    public void update_shouldDoNothingIfNoTerminationOrProvisionAndFleetIsEmpty() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, "active",
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, null, false,
                false, 0, 0, 1, 1,
                false, false, false, 0,
                0, false, 10);

        // when
        FleetStateStats stats = fleetCloud.update();

        // then
        assertEquals(0, stats.getNumDesired());
        assertEquals(0, stats.getNumActive());
        assertEquals("fleetId", stats.getFleetId());
    }

    @Test
    public void update_shouldIncreaseTargetCapacityWhenProvisioned() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, "active",
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, null, false,
                false, 0, 0, 10, 1,
                false, false, false, 0,
                0, false, 10);

        fleetCloud.setStats(new FleetStateStats("", 0, "active",
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        fleetCloud.provision(null, 2);

        // when
        fleetCloud.update();

        // then
        verify(amazonEC2).modifySpotFleetRequest(
                new ModifySpotFleetRequestRequest()
                        .withExcessCapacityTerminationPolicy("NoTermination")
                        .withSpotFleetRequestId("fleetId")
                        .withTargetCapacity(2));
    }

    @Test
    public void update_shouldResetTerminateAndProvision() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, "active",
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, null, false,
                false, 0, 0, 10, 1,
                false, false, false, 0,
                0, false, 10);

        fleetCloud.setStats(new FleetStateStats("", 5, "active",
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        fleetCloud.provision(null, 2);
        fleetCloud.scheduleToTerminate("i-1");

        // when
        fleetCloud.update();

        // then
        verify(amazonEC2).modifySpotFleetRequest(
                new ModifySpotFleetRequestRequest()
                        .withExcessCapacityTerminationPolicy("NoTermination")
                        .withSpotFleetRequestId("fleetId")
                        .withTargetCapacity(6));
        assertEquals(0, fleetCloud.getInstanceIdsToTerminate().size());
        assertEquals(0, fleetCloud.getToAdd());
    }

    @Test
    public void update_shouldNotIncreaseMoreThenMax() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, "active",
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, null, false,
                false, 0, 0, 10, 1,
                false, false, false, 0,
                0, false, 10);

        fleetCloud.setStats(new FleetStateStats("", 5, "active",
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        for (int i = 0; i < 10; i++) fleetCloud.provision(null, 1);
        for (int i = 0; i < 10; i++) fleetCloud.scheduleToTerminate("i-" + i);
        for (int i = 0; i < 10; i++) fleetCloud.provision(null, 1);

        // when
        fleetCloud.update();

        // then
        verify(amazonEC2).modifySpotFleetRequest(
                new ModifySpotFleetRequestRequest()
                        .withExcessCapacityTerminationPolicy("NoTermination")
                        .withSpotFleetRequestId("fleetId")
                        .withTargetCapacity(0));
        assertEquals(0, fleetCloud.getInstanceIdsToTerminate().size());
        assertEquals(0, fleetCloud.getToAdd());
    }

    @Test
    public void update_shouldNotCountScheduledToTerminateWhenScaleUp() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, "active",
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, null, false,
                false, 0, 0, 10, 1,
                false, false, false, 0,
                0, false, 10);

        fleetCloud.setStats(new FleetStateStats("", 5, "active",
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        for (int i = 0; i < 10; i++) fleetCloud.provision(null, 1);
        for (int i = 0; i < 5; i++) fleetCloud.scheduleToTerminate("i-" + i);

        // when
        fleetCloud.update();

        // then
        verify(amazonEC2).modifySpotFleetRequest(
                new ModifySpotFleetRequestRequest()
                        .withExcessCapacityTerminationPolicy("NoTermination")
                        .withSpotFleetRequestId("fleetId")
                        .withTargetCapacity(5));
        assertEquals(0, fleetCloud.getInstanceIdsToTerminate().size());
        assertEquals(0, fleetCloud.getToAdd());
    }

    @Test
    public void update_shouldDecreaseTargetCapacityAndTerminateInstancesIfScheduled() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, "active",
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, null, false,
                false, 0, 0, 10, 1,
                false, false, false, 0,
                0, false, 10);

        fleetCloud.setStats(new FleetStateStats("", 4, "active",
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        fleetCloud.scheduleToTerminate("i-1");
        fleetCloud.scheduleToTerminate("i-2");

        // when
        fleetCloud.update();

        // then
        verify(amazonEC2).modifySpotFleetRequest(
                new ModifySpotFleetRequestRequest()
                        .withExcessCapacityTerminationPolicy("NoTermination")
                        .withSpotFleetRequestId("fleetId")
                        .withTargetCapacity(2));

        verify(ec2Api).terminateInstances(amazonEC2, ImmutableSet.<String>of("i-1", "i-2"));
    }

    @Test
    public void update_shouldAddNodeIfAnyNewDescribed() throws IOException {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        final Instance instance = new Instance()
                .withPublicIpAddress("p-ip")
                .withInstanceId("i-0");

        when(ec2Api.describeInstances(any(AmazonEC2.class), any(Set.class))).thenReturn(
                ImmutableMap.of("i-0", instance));

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, "active",
                        ImmutableSet.of("i-0"), Collections.<String, Double>emptyMap()));

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 1, 1,
                false, false, false,
                0, 0, false, 10);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        FleetStateStats stats = fleetCloud.update();

        // then
        assertEquals(0, stats.getNumDesired());
        assertEquals(1, stats.getNumActive());
        assertEquals("fleetId", stats.getFleetId());

        // and
        Node actualFleetNode = nodeCaptor.getValue();
        assertEquals(Node.Mode.NORMAL, actualFleetNode.getMode());
    }

    @Test
    public void update_shouldAddNodeIfAnyNewDescribed_restrictUsage() throws IOException {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, "active",
                        ImmutableSet.of("i-0"), Collections.<String, Double>emptyMap()));

        final Instance instance = new Instance()
                .withPublicIpAddress("p-ip")
                .withInstanceId("i-0");

        when(ec2Api.describeInstances(any(AmazonEC2.class), any(Set.class))).thenReturn(
                ImmutableMap.of("i-0", instance));

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 1, 1, false,
                true, false,
                0, 0, false, 10);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        FleetStateStats stats = fleetCloud.update();

        // then
        assertEquals(0, stats.getNumDesired());
        assertEquals(1, stats.getNumActive());
        assertEquals("fleetId", stats.getFleetId());

        // and
        Node actualFleetNode = nodeCaptor.getValue();
        assertEquals(Node.Mode.EXCLUSIVE, actualFleetNode.getMode());
    }

    @Test
    public void update_shouldAddNodeWithNumExecutors_whenWeightProvidedButNotEnabled() throws IOException {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        final String instanceType = "t";
        final Instance instance = new Instance()
                .withPublicIpAddress("p-ip")
                .withInstanceType(instanceType)
                .withInstanceId("i-0");

        when(ec2Api.describeInstances(any(AmazonEC2.class), any(Set.class))).thenReturn(
                ImmutableMap.of("i-0", instance));

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, "active",
                        ImmutableSet.of("i-0"),
                        ImmutableMap.of(instanceType, 1.1)));

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 1, 1, false,
                true, false,
                0, 0, false, 10);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        fleetCloud.update();

        // then
        Node actualFleetNode = nodeCaptor.getValue();
        assertEquals(1, actualFleetNode.getNumExecutors());
    }

    @Test
    public void update_shouldAddNodeWithScaledNumExecutors_whenWeightPresentAndEnabled() throws IOException {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        final String instanceType = "t";
        final String instanceId = "i-0";
        final Instance instance = new Instance()
                .withPublicIpAddress("p-ip")
                .withInstanceType(instanceType)
                .withInstanceId(instanceId);

        when(ec2Api.describeInstances(any(AmazonEC2.class), any(Set.class))).thenReturn(
                ImmutableMap.of(instanceId, instance));

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, "active",
                        ImmutableSet.of(instanceId),
                        ImmutableMap.of(instanceType, 2.0)));

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 1, 1, false,
                true, false,
                0, 0, true, 10);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        fleetCloud.update();

        // then
        Node actualFleetNode = nodeCaptor.getValue();
        assertEquals(2, actualFleetNode.getNumExecutors());
    }

    @Test
    public void update_shouldAddNodeWithNumExecutors_whenWeightPresentAndEnabledButForDiffType() throws IOException {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        final String instanceType = "t";
        final String instanceId = "i-0";
        final Instance instance = new Instance()
                .withPublicIpAddress("p-ip")
                .withInstanceType(instanceType)
                .withInstanceId(instanceId);

        when(ec2Api.describeInstances(any(AmazonEC2.class), any(Set.class))).thenReturn(
                ImmutableMap.of(instanceId, instance));

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, "active",
                        ImmutableSet.of(instanceId),
                        ImmutableMap.of("diff-t", 2.0)));

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 1, 1, false,
                true, false,
                0, 0, true, 10);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        fleetCloud.update();

        // then
        Node actualFleetNode = nodeCaptor.getValue();
        assertEquals(1, actualFleetNode.getNumExecutors());
    }

    @Test
    public void update_shouldAddNodeWithRoundToLowScaledNumExecutors_whenWeightPresentAndEnabled() throws IOException {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        final String instanceType = "t";
        final String instanceId = "i-0";
        final Instance instance = new Instance()
                .withPublicIpAddress("p-ip")
                .withInstanceType(instanceType)
                .withInstanceId(instanceId);

        when(ec2Api.describeInstances(any(AmazonEC2.class), any(Set.class))).thenReturn(
                ImmutableMap.of(instanceId, instance));

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, "active",
                        ImmutableSet.of(instanceId),
                        ImmutableMap.of(instanceType, 1.44)));

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 1, 1, false,
                true, false,
                0, 0, true, 10);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        fleetCloud.update();

        // then
        Node actualFleetNode = nodeCaptor.getValue();
        assertEquals(1, actualFleetNode.getNumExecutors());
    }

    @Test
    public void update_shouldAddNodeWithRoundToLowScaledNumExecutors_whenWeightPresentAndEnabled1() throws IOException {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        final String instanceType = "t";
        final String instanceId = "i-0";
        final Instance instance = new Instance()
                .withPublicIpAddress("p-ip")
                .withInstanceType(instanceType)
                .withInstanceId(instanceId);

        when(ec2Api.describeInstances(any(AmazonEC2.class), any(Set.class))).thenReturn(
                ImmutableMap.of(instanceId, instance));

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, "active",
                        ImmutableSet.of(instanceId),
                        ImmutableMap.of(instanceType, 1.5)));

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 1, 1, false,
                true, false,
                0, 0, true, 10);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        fleetCloud.update();

        // then
        Node actualFleetNode = nodeCaptor.getValue();
        assertEquals(2, actualFleetNode.getNumExecutors());
    }

    @Test
    public void update_shouldAddNodeWithScaledToOneNumExecutors_whenWeightPresentButLessOneAndEnabled() throws IOException {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        final String instanceType = "t";
        final String instanceId = "i-0";
        final Instance instance = new Instance()
                .withPublicIpAddress("p-ip")
                .withInstanceType(instanceType)
                .withInstanceId(instanceId);

        when(ec2Api.describeInstances(any(AmazonEC2.class), any(Set.class))).thenReturn(
                ImmutableMap.of(instanceId, instance));

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, "active",
                        ImmutableSet.of(instanceId),
                        ImmutableMap.of(instanceType, .1)));

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 1, 1, false,
                true, false,
                0, 0, true, 10);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        fleetCloud.update();

        // then
        Node actualFleetNode = nodeCaptor.getValue();
        assertEquals(1, actualFleetNode.getNumExecutors());
    }

    @Test
    public void descriptorImpl_doFillRegionItems_returnStaticRegionsIfApiCallFailed() {
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        when(ec2Api.connect(anyString(), anyString(), anyString())).thenReturn(amazonEC2Client);

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillRegionItems("");

        Assert.assertThat(r.size(), Matchers.greaterThan(0));
        assertEquals(RegionUtils.getRegions().size(), r.size());
    }

    @Test
    public void descriptorImpl_doFillRegionItems_returnStaticRegionsAndDynamic() {
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        when(ec2Api.connect(anyString(), nullable(String.class), nullable(String.class))).thenReturn(amazonEC2Client);
        when(amazonEC2Client.describeRegions()).thenReturn(new DescribeRegionsResult().withRegions(new Region().withRegionName("dynamic-region")));

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillRegionItems("");

        Assert.assertThat(r.size(), Matchers.greaterThan(0));
        Assert.assertThat(r.toString(), Matchers.containsString("dynamic-region"));
        assertEquals(RegionUtils.getRegions().size() + 1, r.size());
    }

    @Test
    public void descriptorImpl_doFillRegionItems_returnConsistOrderBetweenCalls() {
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        when(ec2Api.connect(anyString(), nullable(String.class), nullable(String.class))).thenReturn(amazonEC2Client);
        when(amazonEC2Client.describeRegions()).thenReturn(new DescribeRegionsResult().withRegions(new Region().withRegionName("dynamic-region")));

        ListBoxModel r1 = new EC2FleetCloud.DescriptorImpl().doFillRegionItems("");
        ListBoxModel r2 = new EC2FleetCloud.DescriptorImpl().doFillRegionItems("");
        ListBoxModel r3 = new EC2FleetCloud.DescriptorImpl().doFillRegionItems("");

        assertEquals(r1.toString(), r2.toString());
        assertEquals(r2.toString(), r3.toString());
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnEmptyListIfNoFleetInAccount() {
        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                false, "", "", "", "");

        assertEquals(0, r.size());
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnEmptyListIfNoActiveFleets() {
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        when(ec2Api.connect(anyString(), anyString(), anyString())).thenReturn(amazonEC2Client);

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = mock(DescribeSpotFleetRequestsResult.class);
        when(amazonEC2Client.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        when(describeSpotFleetRequestsResult.getSpotFleetRequestConfigs())
                .thenReturn(Arrays.asList(spotFleetRequestConfig4, spotFleetRequestConfig5,
                        spotFleetRequestConfig6, spotFleetRequestConfig7));

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                false, "", "", "", "");

        assertEquals(0, r.size());
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnEmptyListIfNoMaintainFleets() {
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        when(ec2Api.connect(anyString(), anyString(), anyString())).thenReturn(amazonEC2Client);

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = mock(DescribeSpotFleetRequestsResult.class);
        when(amazonEC2Client.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        when(describeSpotFleetRequestsResult.getSpotFleetRequestConfigs())
                .thenReturn(Arrays.asList(spotFleetRequestConfig4, spotFleetRequestConfig5,
                        spotFleetRequestConfig6, spotFleetRequestConfig7, spotFleetRequestConfig8));

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                false, "", "", "", "");

        assertEquals(0, r.size());
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnEmptyIfFleetTypeIsNull() {
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        when(ec2Api.connect(anyString(), anyString(), anyString())).thenReturn(amazonEC2Client);

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = mock(DescribeSpotFleetRequestsResult.class);
        when(amazonEC2Client.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        spotFleetRequestConfig8.getSpotFleetRequestConfig().setType((String) null);
        when(describeSpotFleetRequestsResult.getSpotFleetRequestConfigs())
                .thenReturn(Arrays.asList(spotFleetRequestConfig8));

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                false, "", "", "", "");

        assertEquals(0, r.size());
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnActiveAndMaintainFleets() {
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        when(ec2Api.connect(anyString(), anyString(), anyString())).thenReturn(amazonEC2Client);

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = mock(DescribeSpotFleetRequestsResult.class);
        when(amazonEC2Client.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        when(describeSpotFleetRequestsResult.getSpotFleetRequestConfigs())
                .thenReturn(Arrays.asList(spotFleetRequestConfig1, spotFleetRequestConfig2,
                        spotFleetRequestConfig3, spotFleetRequestConfig4, spotFleetRequestConfig5,
                        spotFleetRequestConfig6, spotFleetRequestConfig7, spotFleetRequestConfig8));

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                false, "", "", "", "");

        assertEquals(3, r.size());
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnAllFleetsIfShowAllIsEnabled() {
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        when(ec2Api.connect(anyString(), anyString(), anyString())).thenReturn(amazonEC2Client);

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = mock(DescribeSpotFleetRequestsResult.class);
        when(amazonEC2Client.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        when(describeSpotFleetRequestsResult.getSpotFleetRequestConfigs())
                .thenReturn(Arrays.asList(spotFleetRequestConfig1, spotFleetRequestConfig2,
                        spotFleetRequestConfig3, spotFleetRequestConfig4, spotFleetRequestConfig5,
                        spotFleetRequestConfig6, spotFleetRequestConfig7, spotFleetRequestConfig8));

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                true, "", "", "", "");

        assertEquals(8, r.size());
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnFleetIdAndStatusType() {
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        when(ec2Api.connect(anyString(), anyString(), anyString())).thenReturn(amazonEC2Client);

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = mock(DescribeSpotFleetRequestsResult.class);
        when(amazonEC2Client.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        spotFleetRequestConfig1.setSpotFleetRequestId("fleet-id");

        when(describeSpotFleetRequestsResult.getSpotFleetRequestConfigs())
                .thenReturn(Arrays.asList(spotFleetRequestConfig1));

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                false, "", "", "", "");

        assertEquals("fleet-id (active) (maintain)", r.get(0).name);
        assertEquals("fleet-id", r.get(0).value);
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnFleetsCrossPages() {
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        when(ec2Api.connect(anyString(), anyString(), anyString())).thenReturn(amazonEC2Client);

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = mock(DescribeSpotFleetRequestsResult.class);
        when(amazonEC2Client.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        when(describeSpotFleetRequestsResult.getNextToken())
                .thenReturn("a")
                .thenReturn("b")
                .thenReturn(null);

        spotFleetRequestConfig1.setSpotFleetRequestId("a");
        spotFleetRequestConfig2.setSpotFleetRequestId("b");
        spotFleetRequestConfig3.setSpotFleetRequestId("c");

        when(describeSpotFleetRequestsResult.getSpotFleetRequestConfigs())
                .thenReturn(Arrays.asList(spotFleetRequestConfig1))
                .thenReturn(Arrays.asList(spotFleetRequestConfig2))
                .thenReturn(Arrays.asList(spotFleetRequestConfig3));

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                false, "", "", "", "");

        assertEquals("a", r.get(0).value);
        assertEquals("b", r.get(1).value);
        assertEquals("c", r.get(2).value);
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnSelectedFleetInAnyState() {
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        when(ec2Api.connect(anyString(), anyString(), anyString())).thenReturn(amazonEC2Client);

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = mock(DescribeSpotFleetRequestsResult.class);
        when(amazonEC2Client.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        spotFleetRequestConfig1.setSpotFleetRequestId("a");
        spotFleetRequestConfig2.setSpotFleetRequestId("failed_selected");
        spotFleetRequestConfig2.setSpotFleetRequestState(BatchState.Failed);

        when(describeSpotFleetRequestsResult.getSpotFleetRequestConfigs())
                .thenReturn(Arrays.asList(spotFleetRequestConfig1, spotFleetRequestConfig2));

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                false, "", "", "", "failed_selected");

        assertEquals("a", r.get(0).value);
        assertEquals("failed_selected", r.get(1).value);
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnEmptyListIfAnyException() {
        when(ec2Api.connect(anyString(), anyString(), anyString())).thenThrow(new IllegalArgumentException("test"));

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                false, "", "", "", "");

        assertEquals(0, r.size());
    }

    @Test
    public void getDisplayName_returnDefaultWhenNull() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                null, null, null, null, null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, false, false, false
                , 0, 0, false, 10);
        assertEquals(ec2FleetCloud.getDisplayName(), EC2FleetCloud.FLEET_CLOUD_ID);
    }

    @Test
    public void getDisplayName_returnDisplayName() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                "CloudName", null, null, null, null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, false, false, false
                , 0, 0, false, 10);
        assertEquals(ec2FleetCloud.getDisplayName(), "CloudName");
    }

    @Test
    public void getAwsCredentialsId_returnNull_whenNoCredentialsIdOrAwsCredentialsId() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                null, null, null, null, null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, false, false, false
                , 0, 0, false, 10);
        Assert.assertNull(ec2FleetCloud.getAwsCredentialsId());
    }

    @Test
    public void getAwsCredentialsId_returnValue_whenCredentialsIdPresent() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                null, null, null, "Opa", null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, false, false, false
                , 0, 0, false, 10);
        assertEquals("Opa", ec2FleetCloud.getAwsCredentialsId());
    }

    @Test
    public void getAwsCredentialsId_returnValue_whenAwsCredentialsIdPresent() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                null, null, "Opa", null, null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, false, false, false
                , 0, 0, false, 10);
        assertEquals("Opa", ec2FleetCloud.getAwsCredentialsId());
    }

    @Test
    public void getAwsCredentialsId_returnAwsCredentialsId_whenAwsCredentialsIdAndCredentialsIdPresent() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                null, null, "A", "B", null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, false, false, false
                , 0, 0, false, 10);
        assertEquals("A", ec2FleetCloud.getAwsCredentialsId());
    }

    // todo create test cases update failed to modify fleet

    @Test
    public void getCloudStatusInterval_returnCloudStatusInterval() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                "CloudName", null, null, null, null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, false, false, false
                , 0, 0, false, 45);
        assertEquals(45, ec2FleetCloud.getCloudStatusIntervalSec());
    }

    private void mockNodeCreatingPart() {
        when(jenkins.getNodesObject()).thenReturn(mock(Nodes.class));

        ExtensionList labelFinder = mock(ExtensionList.class);
        when(labelFinder.iterator()).thenReturn(Collections.emptyIterator());
        PowerMockito.when(LabelFinder.all()).thenReturn(labelFinder);

        // mocking part of node creation process Jenkins.getInstance().getLabelAtom(l)
        when(jenkins.getLabelAtom(anyString())).thenReturn(new LabelAtom("mock-label"));
    }

}
