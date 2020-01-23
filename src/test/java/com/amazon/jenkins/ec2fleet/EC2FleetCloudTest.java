package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.fleet.AutoScalingGroupFleet;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleet;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleets;
import com.amazon.jenkins.ec2fleet.fleet.EC2SpotFleet;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.BatchState;
import com.amazonaws.services.ec2.model.DescribeRegionsResult;
import com.amazonaws.services.ec2.model.FleetType;
import com.amazonaws.services.ec2.model.Instance;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class, EC2FleetCloud.class, EC2FleetCloud.DescriptorImpl.class,
        LabelFinder.class, FleetStateStats.class, EC2Fleets.class})
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
    private EC2Fleet ec2Fleet;

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

        PowerMockito.mockStatic(EC2Fleets.class);
        when(EC2Fleets.get(anyString())).thenReturn(ec2Fleet);

        PowerMockito.mockStatic(FleetStateStats.class);
        PowerMockito.mockStatic(LabelFinder.class);

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

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, FleetStateStats.State.active(),
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 0, 10, 1, true,
                false, false, 0, 0, false,
                10, false);

        fleetCloud.setStats(new FleetStateStats("", 10, FleetStateStats.State.active(),
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

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, FleetStateStats.State.active(),
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 0, 9, 1, true,
                false, false, 0, 0, false,
                10, false);

        fleetCloud.setStats(new FleetStateStats("", 10, FleetStateStats.State.active(),
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

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, FleetStateStats.State.active(),
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 0, 10, 1, true,
                false, false, 0, 0, false,
                10, false);

        fleetCloud.setStats(new FleetStateStats("", 5, FleetStateStats.State.active(),
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

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, FleetStateStats.State.active(),
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 0, 10, 1, true,
                false, false, 0, 0, false,
                10, false);

        fleetCloud.setStats(new FleetStateStats("", 5, FleetStateStats.State.active(),
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

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, FleetStateStats.State.active(),
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 0, 10, 1, true,
                false, false, 0, 0, false,
                10, false);

        fleetCloud.setStats(new FleetStateStats("", 5, FleetStateStats.State.active(),
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

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, FleetStateStats.State.active(),
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 0, 1, 1, true,
                false, false, 0, 0, false,
                10, false);

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

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, FleetStateStats.State.active(),
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 0, 1, 1, true,
                false, false, 0, 0, false,
                10, false);

        // when
        boolean r = fleetCloud.scheduleToTerminate("z");

        // then
        assertFalse(r);
    }

    @Test
    public void scheduleToTerminate_notRemoveIfBelowMin() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, FleetStateStats.State.active(),
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 1, 1, 1, true,
                false, false, 0, 0, false,
                10, false);

        fleetCloud.setStats(new FleetStateStats("", 0, FleetStateStats.State.active(),
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

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, FleetStateStats.State.active(),
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 1, 1, 1, true,
                false, false, 0, 0, false,
                10, false);

        fleetCloud.setStats(new FleetStateStats("", 1, FleetStateStats.State.active(),
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

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, FleetStateStats.State.active(),
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 1, 1, 1, true,
                false, false, 0, 0, false,
                10, false);

        fleetCloud.setStats(new FleetStateStats("", 2, FleetStateStats.State.active(),
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

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, FleetStateStats.State.active(),
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 0, 1, 1, true,
                false, false, 0, 0, false,
                10, false);

        fleetCloud.setStats(new FleetStateStats("", 2, FleetStateStats.State.active(),
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

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, FleetStateStats.State.active(),
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 1, 1, 1, true,
                false, false, 0, 0, false,
                10, false);

        fleetCloud.setStats(new FleetStateStats("", 3, FleetStateStats.State.active(),
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

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, FleetStateStats.State.active(),
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, null, false,
                false, 0, 0, 1, 1, true,
                false, false, 0,
                0, false, 10, false);

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

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, FleetStateStats.State.active(),
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, null, false,
                false, 0, 0, 10, 1, true,
                false, false, 0,
                0, false, 10, false);

        fleetCloud.setStats(new FleetStateStats("", 0, FleetStateStats.State.active(),
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        fleetCloud.provision(null, 2);

        // when
        fleetCloud.update();

        // then
        verify(ec2Fleet).modify(anyString(), anyString(), anyString(), eq("fleetId"), eq(2), eq(0), eq(10));
    }

    @Test
    public void update_shouldResetTerminateAndProvision() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        final FleetStateStats currentState = new FleetStateStats("fleetId", 5, FleetStateStats.State.active(),
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap());
        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(currentState);

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, null, false,
                false, 0, 0, 10, 1, true,
                false, false, 0,
                0, false, 10, false);

        fleetCloud.setStats(currentState);

        fleetCloud.provision(null, 2);
        fleetCloud.scheduleToTerminate("i-1");

        // when
        fleetCloud.update();

        // then
        verify(ec2Fleet).modify(anyString(), anyString(), anyString(), eq("fleetId"), eq(6), eq(0), eq(10));
        assertEquals(0, fleetCloud.getInstanceIdsToTerminate().size());
        assertEquals(0, fleetCloud.getToAdd());
    }

    @Test
    public void update_shouldNotIncreaseMoreThenMax() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, FleetStateStats.State.active(),
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, null, false,
                false, 0, 0, 10, 1, true,
                false, false, 0,
                0, false, 10, false);

        fleetCloud.setStats(new FleetStateStats("", 5, FleetStateStats.State.active(),
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        for (int i = 0; i < 10; i++) fleetCloud.provision(null, 1);
        for (int i = 0; i < 10; i++) fleetCloud.scheduleToTerminate("i-" + i);
        for (int i = 0; i < 10; i++) fleetCloud.provision(null, 1);

        // when
        fleetCloud.update();

        // then
        verify(ec2Fleet).modify(anyString(), anyString(), anyString(), eq("fleetId"), eq(0), eq(0), eq(10));
        assertEquals(0, fleetCloud.getInstanceIdsToTerminate().size());
        assertEquals(0, fleetCloud.getToAdd());
    }

    @Test
    public void update_shouldNotCountScheduledToTerminateWhenScaleUp() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 5, FleetStateStats.State.active(),
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, null, false,
                false, 0, 0, 10, 1, true,
                false, false, 0,
                0, false, 10, false);

        fleetCloud.setStats(new FleetStateStats("", 5, FleetStateStats.State.active(),
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        for (int i = 0; i < 10; i++) fleetCloud.provision(null, 1);
        for (int i = 0; i < 5; i++) fleetCloud.scheduleToTerminate("i-" + i);

        // when
        fleetCloud.update();

        // then
        verify(ec2Fleet).modify(anyString(), anyString(), anyString(), eq("fleetId"), eq(5), eq(0), eq(10));
        assertEquals(0, fleetCloud.getInstanceIdsToTerminate().size());
        assertEquals(0, fleetCloud.getToAdd());
    }

    @Test
    public void update_shouldDecreaseTargetCapacityAndTerminateInstancesIfScheduled() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 4, FleetStateStats.State.active(),
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, null, false,
                false, 0, 0, 10, 1, true,
                false, false, 0,
                0, false, 10, false);

        fleetCloud.setStats(new FleetStateStats("", 4, FleetStateStats.State.active(),
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        fleetCloud.scheduleToTerminate("i-1");
        fleetCloud.scheduleToTerminate("i-2");

        // when
        fleetCloud.update();

        // then
        verify(ec2Fleet).modify(anyString(), anyString(), anyString(), eq("fleetId"), eq(2), eq(0), eq(10));
        verify(ec2Api).terminateInstances(amazonEC2, ImmutableSet.of("i-1", "i-2"));
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

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, FleetStateStats.State.active(),
                        ImmutableSet.of("i-0"), Collections.<String, Double>emptyMap()));

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 1, 1, false,
                false, false,
                0, 0, false, 10, false);

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
    public void update_shouldTagNewNodesBeforeAdding() throws IOException {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        final Instance instance1 = new Instance().withPublicIpAddress("p-ip").withInstanceId("i-0");
        final Instance instance2 = new Instance().withPublicIpAddress("p-ip").withInstanceId("i-1");

        when(ec2Api.describeInstances(any(AmazonEC2.class), any(Set.class))).thenReturn(
                ImmutableMap.of("i-0", instance1, "i-1", instance2));

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, FleetStateStats.State.active(),
                        ImmutableSet.of("i-0", "i-1"), Collections.<String, Double>emptyMap()));

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 2, 1, false,
                false, false,
                0, 0, false, 10, false);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        fleetCloud.update();

        // then
        verify(ec2Api).tagInstances(amazonEC2, ImmutableSet.of("i-0", "i-1"), "ec2-fleet-plugin:cloud-name", "FleetCloud");
        Node actualFleetNode = nodeCaptor.getValue();
        assertEquals(Node.Mode.NORMAL, actualFleetNode.getMode());
    }

    @Test
    public void update_shouldTagNewNodesBeforeAddingWithFleetName() throws IOException {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        final Instance instance1 = new Instance().withPublicIpAddress("p-ip").withInstanceId("i-0");

        when(ec2Api.describeInstances(any(AmazonEC2.class), any(Set.class))).thenReturn(
                ImmutableMap.of("i-0", instance1));

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, FleetStateStats.State.active(),
                        ImmutableSet.of("i-0"), Collections.<String, Double>emptyMap()));

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud("my-fleet", null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 1, 1, false,
                false, false,
                0, 0, false, 10, false);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        fleetCloud.update();

        // then
        verify(ec2Api).tagInstances(amazonEC2, ImmutableSet.of("i-0"), "ec2-fleet-plugin:cloud-name", "my-fleet");
        Node actualFleetNode = nodeCaptor.getValue();
        assertEquals(Node.Mode.NORMAL, actualFleetNode.getMode());
    }

    @Test
    public void update_givenFailedTaggingShouldIgnoreExceptionAndAddNode() throws IOException {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);
        doThrow(new UnsupportedOperationException("testexception"))
                .when(ec2Api).tagInstances(any(AmazonEC2.class), any(Set.class), anyString(), anyString());

        final Instance instance = new Instance()
                .withPublicIpAddress("p-ip")
                .withInstanceId("i-0");

        when(ec2Api.describeInstances(any(AmazonEC2.class), any(Set.class))).thenReturn(
                ImmutableMap.of("i-0", instance));

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, FleetStateStats.State.active(),
                        ImmutableSet.of("i-0"), Collections.<String, Double>emptyMap()));

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 1, 1, false,
                false, false,
                0, 0, false, 10, false);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        fleetCloud.update();

        // then
        verify(ec2Api).tagInstances(amazonEC2, ImmutableSet.of("i-0"), "ec2-fleet-plugin:cloud-name", "FleetCloud");
        Node actualFleetNode = nodeCaptor.getValue();
        assertEquals(Node.Mode.NORMAL, actualFleetNode.getMode());
    }

    @Test
    public void update_shouldAddNodeIfAnyNewDescribed_restrictUsage() throws IOException {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, FleetStateStats.State.active(),
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
                0, 0, false, 10, false);

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

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, FleetStateStats.State.active(),
                        ImmutableSet.of("i-0"),
                        ImmutableMap.of(instanceType, 1.1)));

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 1, 1, false,
                true, false,
                0, 0, false, 10, false);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        fleetCloud.update();

        // then
        Node actualFleetNode = nodeCaptor.getValue();
        assertEquals(1, actualFleetNode.getNumExecutors());
    }

    @Test
    public void update_givenManuallyUpdatedFleetShouldCorrectLocalTargetCapacityToKeepZeroOrPositive() throws IOException {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        when(ec2Api.describeInstances(any(AmazonEC2.class), any(Set.class))).thenReturn(
                Collections.emptyMap());

        final FleetStateStats initState = new FleetStateStats("fleetId", 0,
                FleetStateStats.State.active(),
                ImmutableSet.of("i-0", "i-1"), Collections.<String, Double>emptyMap());
        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(initState);

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 10, 1, false,
                true, false,
                0, 0, false, 10, false);
        fleetCloud.setStats(initState);

        doNothing().when(jenkins).addNode(any(Node.class));

        fleetCloud.scheduleToTerminate("i-0");
        fleetCloud.scheduleToTerminate("i-1");

        // when
        fleetCloud.update();

        // then
        // should reset list to empty
        verify(ec2Fleet).modify(anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt(), anyInt());
        Assert.assertEquals(0, fleetCloud.getPlannedNodesCache().size());
    }

    @Test
    public void update_shouldTrimPlannedNodesIfExceedTargetCapacity() throws IOException {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        when(ec2Api.describeInstances(any(AmazonEC2.class), any(Set.class))).thenReturn(
                Collections.emptyMap());

        final FleetStateStats initState = new FleetStateStats("fleetId", 0,
                FleetStateStats.State.active(),
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap());
        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(initState);

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 10, 1, false,
                true, false,
                0, 0, false, 10, false);
        fleetCloud.setStats(initState);

        doNothing().when(jenkins).addNode(any(Node.class));

        // when
        Collection<NodeProvisioner.PlannedNode> plannedNodes = fleetCloud.provision(null, 10);
        Assert.assertEquals(10, plannedNodes.size());
        for (NodeProvisioner.PlannedNode plannedNode : plannedNodes) {
            Assert.assertFalse(plannedNode.future.isCancelled());
        }

        // try to modify and reset to add
        fleetCloud.update();
        // reset to old empty state
        fleetCloud.setStats(initState);
        fleetCloud.update();

        // then
        // should reset list to empty
        Assert.assertEquals(0, fleetCloud.getPlannedNodesCache().size());
        // make sure all trimmed planned nodes were cancelled
        Assert.assertEquals(10, plannedNodes.size());
        for (NodeProvisioner.PlannedNode plannedNode : plannedNodes) {
            Assert.assertTrue("Planned node should be cancelled", plannedNode.future.isCancelled());
        }
    }

    @Test
    public void update_shouldTrimPlannedNodesBasedOnUpdatedTargetCapacityIfProvisionCalledInBetween() throws IOException {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        when(ec2Api.describeInstances(any(AmazonEC2.class), any(Set.class))).thenReturn(
                Collections.emptyMap());

        final FleetStateStats initState = new FleetStateStats("fleetId", 0,
                FleetStateStats.State.active(),
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap());
        when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(initState);

        mockNodeCreatingPart();

        final EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 10, 1, false,
                true, false,
                0, 0, false, 10, false);
        fleetCloud.setStats(initState);

        doNothing().when(jenkins).addNode(any(Node.class));

        fleetCloud.provision(null, 1);

        // intercept modify operation to emulate call of provision during update method
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                fleetCloud.provision(null, 1);
                return null;
            }
        }).when(ec2Fleet).modify(anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt(), anyInt());

        // when
        fleetCloud.update();

        // then
        // should be two, planned one added before update another during update
        Assert.assertEquals(2, fleetCloud.getPlannedNodesCache().size());
    }

    @Test
    public void update_shouldUpdateStateWithFleetTargetCapacityPlusToAdd() throws IOException {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        final String instanceType = "t";
        final Instance instance = new Instance()
                .withPublicIpAddress("p-ip")
                .withInstanceType(instanceType)
                .withInstanceId("i-0");

        when(ec2Api.describeInstances(any(AmazonEC2.class), any(Set.class))).thenReturn(
                ImmutableMap.of("i-0", instance));

        final FleetStateStats initState = new FleetStateStats("fleetId", 5,
                FleetStateStats.State.active(),
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap());
        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(initState);

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 10, 1, false,
                true, false,
                0, 0, false, 10, false);
        fleetCloud.setStats(initState);

        doNothing().when(jenkins).addNode(any(Node.class));

        fleetCloud.provision(null, 2);

        // when
        fleetCloud.update();

        // then
        Assert.assertEquals(7, fleetCloud.getStats().getNumDesired());
    }

    /**
     * See {@link EC2FleetCloudTest#update_shouldUpdateStateWithFleetTargetCapacityPlusToAdd()}
     */
    @Test
    public void update_shouldUpdateStateWithFleetTargetCapacityMinusToTerminate() throws IOException {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        final String instanceType = "t";
        final Instance instance = new Instance()
                .withPublicIpAddress("p-ip")
                .withInstanceType(instanceType)
                .withInstanceId("i-0");

        when(ec2Api.describeInstances(any(AmazonEC2.class), any(Set.class))).thenReturn(
                ImmutableMap.of("i-0", instance));

        final FleetStateStats initState = new FleetStateStats("fleetId", 5,
                FleetStateStats.State.active(),
                ImmutableSet.<String>of("i-0"), Collections.<String, Double>emptyMap());
        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(initState);

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 10, 1, false,
                true, false,
                0, 0, false, 10, false);
        fleetCloud.setStats(initState);

        doNothing().when(jenkins).addNode(any(Node.class));

        fleetCloud.scheduleToTerminate("i-0");

        // when
        fleetCloud.update();

        // then
        Assert.assertEquals(4, fleetCloud.getStats().getNumDesired());
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

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, FleetStateStats.State.active(),
                        ImmutableSet.of(instanceId),
                        ImmutableMap.of(instanceType, 2.0)));

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 1, 1, false,
                true, false,
                0, 0, true, 10, false);

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

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, FleetStateStats.State.active(),
                        ImmutableSet.of(instanceId),
                        ImmutableMap.of("diff-t", 2.0)));

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 1, 1, false,
                true, false,
                0, 0, true, 10, false);

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

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, FleetStateStats.State.active(),
                        ImmutableSet.of(instanceId),
                        ImmutableMap.of(instanceType, 1.44)));

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 1, 1, false,
                true, false,
                0, 0, true, 10, false);

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

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, FleetStateStats.State.active(),
                        ImmutableSet.of(instanceId),
                        ImmutableMap.of(instanceType, 1.5)));

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 1, 1, false,
                true, false,
                0, 0, true, 10, false);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        fleetCloud.update();

        // then
        Node actualFleetNode = nodeCaptor.getValue();
        assertEquals(2, actualFleetNode.getNumExecutors());
    }

    @Test
    public void update_shouldAddNodeWithScaledToOneNumExecutors_whenWeightPresentButLessOneAndEnabled() {
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

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, FleetStateStats.State.active(),
                        ImmutableSet.of(instanceId),
                        Collections.<String, Double>emptyMap()));

        PowerMockito.doThrow(new UnsupportedOperationException("Test exception")).when(ec2Fleet)
                .modify(anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt(), anyInt());

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 1, 1, false,
                true, false,
                0, 0, true, 10, false);
        // set init state so we can do provision
        fleetCloud.setStats(new FleetStateStats("", 0, FleetStateStats.State.active(),
                Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));
        // run provision
        fleetCloud.provision(null, 1);

        // when
        try {
            fleetCloud.update();
            Assert.fail("Where is my exception?");
        } catch (UnsupportedOperationException e) {
            Assert.assertEquals(1, fleetCloud.getToAdd());
        }
    }

    @Test
    public void update_givenFailedModifyShouldNotUpdateToAddToDelete() throws IOException {
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

        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, FleetStateStats.State.active(),
                        ImmutableSet.of(instanceId),
                        ImmutableMap.of(instanceType, .1)));

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 1, 1, false,
                true, false,
                0, 0, true, 10, false);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        fleetCloud.update();

        // then
        Node actualFleetNode = nodeCaptor.getValue();
        assertEquals(1, actualFleetNode.getNumExecutors());
    }

    @Test
    public void update_givenFleetInModifyingShouldStopAndNotUpdateAddOrModify() throws IOException {
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

        final FleetStateStats stats = new FleetStateStats("fleetId", 0,
                FleetStateStats.State.modifying(""),
                ImmutableSet.of(instanceId),
                ImmutableMap.of(instanceType, .1));
        PowerMockito.when(ec2Fleet.getState(anyString(), anyString(), anyString(), anyString())).thenReturn(stats);

        mockNodeCreatingPart();

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 1, 1, false,
                true, false,
                0, 0, true, 10, false);
        fleetCloud.setStats(stats);

        doNothing().when(jenkins).addNode(any(Node.class));

        // when
        FleetStateStats newStats = fleetCloud.update();

        // then
        Assert.assertSame(stats, newStats);
        verify(ec2Fleet, never()).modify(any(String.class), any(String.class), any(String.class), any(String.class), anyInt(), anyInt(), anyInt());
        verify(jenkins, never()).addNode(any(Node.class));
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
    public void descriptorImpl_doFillFleetItems_returnEmptyListIfNoEmptyEC2Fleet() {
        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                false, "", "", "", "");

        assertEquals(0, r.size());
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnFleetsProvidedByAllEC2Fleets() {
        final EC2Fleet ec2SpotFleet = mock(EC2SpotFleet.class);
        final EC2Fleet autoScalingGroupFleet = mock(AutoScalingGroupFleet.class);
        when(EC2Fleets.all()).thenReturn(Arrays.asList(ec2SpotFleet, autoScalingGroupFleet));

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                false, "", "", "", "");

        assertEquals(0, r.size());
        verify(ec2SpotFleet).describe("", "", "", r, "", false);
        verify(autoScalingGroupFleet).describe("", "", "", r, "", false);
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnEmptyListIfAnyException() {
        final EC2Fleet ec2SpotFleet = mock(EC2SpotFleet.class);
        doThrow(new RuntimeException("test")).when(ec2SpotFleet).describe(
                anyString(), anyString(), anyString(), any(ListBoxModel.class), anyString(), anyBoolean());

        final EC2Fleet autoScalingGroupFleet = mock(AutoScalingGroupFleet.class);
        when(EC2Fleets.all()).thenReturn(Arrays.asList(ec2SpotFleet, autoScalingGroupFleet));

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
                null, true, false, false
                , 0, 0, false, 10, false);
        assertEquals(ec2FleetCloud.getDisplayName(), EC2FleetCloud.FLEET_CLOUD_ID);
    }

    @Test
    public void getDisplayName_returnDisplayName() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                "CloudName", null, null, null, null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, true, false, false
                , 0, 0, false,
                10, false);
        assertEquals(ec2FleetCloud.getDisplayName(), "CloudName");
    }

    @Test
    public void getAwsCredentialsId_returnNull_whenNoCredentialsIdOrAwsCredentialsId() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                null, null, null, null, null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, true, false, false,
                0, 0, false,
                10, false);
        Assert.assertNull(ec2FleetCloud.getAwsCredentialsId());
    }

    @Test
    public void getAwsCredentialsId_returnValue_whenCredentialsIdPresent() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                null, null, null, "Opa", null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, true, false, false
                , 0, 0, false,
                10, false);
        assertEquals("Opa", ec2FleetCloud.getAwsCredentialsId());
    }

    @Test
    public void getAwsCredentialsId_returnValue_whenAwsCredentialsIdPresent() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                null, null, "Opa", null, null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, true, false, false
                , 0, 0, false,
                10, false);
        assertEquals("Opa", ec2FleetCloud.getAwsCredentialsId());
    }

    @Test
    public void getAwsCredentialsId_returnAwsCredentialsId_whenAwsCredentialsIdAndCredentialsIdPresent() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                null, null, "A", "B", null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, true, false, false
                , 0, 0, false,
                10, false);
        assertEquals("A", ec2FleetCloud.getAwsCredentialsId());
    }

    // todo create test cases update failed to modify fleet

    @Test
    public void getCloudStatusInterval_returnCloudStatusInterval() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                "CloudName", null, null, null, null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, true, false, false
                , 0, 0, false,
                45, false);
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
