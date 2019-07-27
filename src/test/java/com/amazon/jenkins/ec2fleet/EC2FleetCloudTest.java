package com.amazon.jenkins.ec2fleet;

import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.BatchState;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeRegionsResult;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfig;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.nullable;
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
        spotFleetRequestConfig2 = new SpotFleetRequestConfig();
        spotFleetRequestConfig2.setSpotFleetRequestState(BatchState.Submitted);
        spotFleetRequestConfig3 = new SpotFleetRequestConfig();
        spotFleetRequestConfig3.setSpotFleetRequestState(BatchState.Modifying);
        spotFleetRequestConfig4 = new SpotFleetRequestConfig();
        spotFleetRequestConfig4.setSpotFleetRequestState(BatchState.Cancelled);
        spotFleetRequestConfig5 = new SpotFleetRequestConfig();
        spotFleetRequestConfig5.setSpotFleetRequestState(BatchState.Cancelled_running);
        spotFleetRequestConfig6 = new SpotFleetRequestConfig();
        spotFleetRequestConfig6.setSpotFleetRequestState(BatchState.Cancelled_terminating);
        spotFleetRequestConfig7 = new SpotFleetRequestConfig();
        spotFleetRequestConfig7.setSpotFleetRequestState(BatchState.Failed);

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
    public void provision_fleetIsEmpty() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("", 0, "active",
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "", "", null, null, false,
                false, 0, 0, 1, 1, false,
                false, false, 0, 0, false);

        // when
        Collection<NodeProvisioner.PlannedNode> r = fleetCloud.provision(null, 1);

        // then
        assertEquals(1, r.size());
    }

    @Test
    public void updateStatus_doNothingWhenFleetIsEmpty() {
        // given
        when(ec2Api.connect(any(String.class), any(String.class), anyString())).thenReturn(amazonEC2);

        PowerMockito.when(FleetStateStats.readClusterState(any(AmazonEC2.class), anyString(), anyString()))
                .thenReturn(new FleetStateStats("fleetId", 0, "active",
                        Collections.<String>emptySet(), Collections.<String, Double>emptyMap()));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, null, "credId", null, "region",
                "", "fleetId", "", null, null, false,
                false, 0, 0, 1, 1,
                false, false, false, 0,
                0, false);

        // when
        FleetStateStats stats = fleetCloud.updateStatus();

        // then
        assertEquals(0, stats.getNumDesired());
        assertEquals(0, stats.getNumActive());
        assertEquals("fleetId", stats.getFleetId());
    }

    @Test
    public void updateStatus_shouldAddNodeIfAnyNewDescribed() throws IOException {
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
                0, 0, false);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        FleetStateStats stats = fleetCloud.updateStatus();

        // then
        assertEquals(0, stats.getNumDesired());
        assertEquals(1, stats.getNumActive());
        assertEquals("fleetId", stats.getFleetId());

        // and
        Node actualFleetNode = nodeCaptor.getValue();
        assertEquals(Node.Mode.NORMAL, actualFleetNode.getMode());
    }

    @Test
    public void updateStatus_shouldAddNodeIfAnyNewDescribed_restrictUsage() throws IOException {
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
                0, 0, false);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        FleetStateStats stats = fleetCloud.updateStatus();

        // then
        assertEquals(0, stats.getNumDesired());
        assertEquals(1, stats.getNumActive());
        assertEquals("fleetId", stats.getFleetId());

        // and
        Node actualFleetNode = nodeCaptor.getValue();
        assertEquals(Node.Mode.EXCLUSIVE, actualFleetNode.getMode());
    }

    @Test
    public void updateStatus_shouldAddNodeWithNumExecutors_whenWeightProvidedButNotEnabled() throws IOException {
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
                0, 0, false);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        fleetCloud.updateStatus();

        // then
        Node actualFleetNode = nodeCaptor.getValue();
        assertEquals(1, actualFleetNode.getNumExecutors());
    }

    @Test
    public void updateStatus_shouldAddNodeWithScaledNumExecutors_whenWeightPresentAndEnabled() throws IOException {
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
                0, 0, true);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        fleetCloud.updateStatus();

        // then
        Node actualFleetNode = nodeCaptor.getValue();
        assertEquals(2, actualFleetNode.getNumExecutors());
    }

    @Test
    public void updateStatus_shouldAddNodeWithNumExecutors_whenWeightPresentAndEnabledButForDiffType() throws IOException {
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
                0, 0, true);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        fleetCloud.updateStatus();

        // then
        Node actualFleetNode = nodeCaptor.getValue();
        assertEquals(1, actualFleetNode.getNumExecutors());
    }

    @Test
    public void updateStatus_shouldAddNodeWithRoundToLowScaledNumExecutors_whenWeightPresentAndEnabled() throws IOException {
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
                0, 0, true);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        fleetCloud.updateStatus();

        // then
        Node actualFleetNode = nodeCaptor.getValue();
        assertEquals(1, actualFleetNode.getNumExecutors());
    }

    @Test
    public void updateStatus_shouldAddNodeWithRoundToLowScaledNumExecutors_whenWeightPresentAndEnabled1() throws IOException {
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
                0, 0, true);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        fleetCloud.updateStatus();

        // then
        Node actualFleetNode = nodeCaptor.getValue();
        assertEquals(2, actualFleetNode.getNumExecutors());
    }

    @Test
    public void updateStatus_shouldAddNodeWithScaledToOneNumExecutors_whenWeightPresentButLessOneAndEnabled() throws IOException {
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
                0, 0, true);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        fleetCloud.updateStatus();

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
    public void descriptorImpl_doFillFleetItems_returnActiveFleets() {
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        when(ec2Api.connect(anyString(), anyString(), anyString())).thenReturn(amazonEC2Client);

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = mock(DescribeSpotFleetRequestsResult.class);
        when(amazonEC2Client.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        when(describeSpotFleetRequestsResult.getSpotFleetRequestConfigs())
                .thenReturn(Arrays.asList(spotFleetRequestConfig1, spotFleetRequestConfig2,
                        spotFleetRequestConfig3, spotFleetRequestConfig4, spotFleetRequestConfig5,
                        spotFleetRequestConfig6, spotFleetRequestConfig7));

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                false, "", "", "", "");

        assertEquals(3, r.size());
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnAllFleetsIfShowNonActiveIsEnabled() {
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        when(ec2Api.connect(anyString(), anyString(), anyString())).thenReturn(amazonEC2Client);

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = mock(DescribeSpotFleetRequestsResult.class);
        when(amazonEC2Client.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        when(describeSpotFleetRequestsResult.getSpotFleetRequestConfigs())
                .thenReturn(Arrays.asList(spotFleetRequestConfig1, spotFleetRequestConfig2,
                        spotFleetRequestConfig3, spotFleetRequestConfig4, spotFleetRequestConfig5,
                        spotFleetRequestConfig6, spotFleetRequestConfig7));

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                true, "", "", "", "");

        assertEquals(7, r.size());
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnFleetInfo() {
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

        assertEquals("fleet-id (active)", r.get(0).name);
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
                , 0, 0, false);
        assertEquals(ec2FleetCloud.getDisplayName(), EC2FleetCloud.FLEET_CLOUD_ID);
    }

    @Test
    public void getDisplayName_returnDisplayName() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                "CloudName", null, null, null, null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, false, false, false
                , 0, 0, false);
        assertEquals(ec2FleetCloud.getDisplayName(), "CloudName");
    }

    @Test
    public void getAwsCredentialsId_returnNull_whenNoCredentialsIdOrAwsCredentialsId() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                null, null, null, null, null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, false, false, false
                , 0, 0, false);
        Assert.assertNull(ec2FleetCloud.getAwsCredentialsId());
    }

    @Test
    public void getAwsCredentialsId_returnValue_whenCredentialsIdPresent() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                null, null, null, "Opa", null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, false, false, false
                , 0, 0, false);
        assertEquals("Opa", ec2FleetCloud.getAwsCredentialsId());
    }

    @Test
    public void getAwsCredentialsId_returnValue_whenAwsCredentialsIdPresent() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                null, null, "Opa", null, null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, false, false, false
                , 0, 0, false);
        assertEquals("Opa", ec2FleetCloud.getAwsCredentialsId());
    }

    @Test
    public void getAwsCredentialsId_returnAwsCredentialsId_whenAwsCredentialsIdAndCredentialsIdPresent() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                null, null, "A", "B", null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, false, false, false
                , 0, 0, false);
        assertEquals("A", ec2FleetCloud.getAwsCredentialsId());
    }

    private void mockNodeCreatingPart() {
        when(jenkins.getNodesObject()).thenReturn(mock(Nodes.class));

        ExtensionList labelFinder = mock(ExtensionList.class);
        when(labelFinder.iterator()).thenReturn(Collections.emptyIterator());
        PowerMockito.when(LabelFinder.all()).thenReturn(labelFinder);

        // mocking part of node creation process Jenkins.getInstance().getLabelAtom(l)
        when(jenkins.getLabelAtom(anyString())).thenReturn(new LabelAtom("mock-label"));
    }

    private void mockDescribeInstances(String instanceId, String instanceType) {
        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult();
        describeInstancesResult.withReservations(
                new Reservation().withInstances(new Instance()
                        .withPublicIpAddress("p-ip")
                        .withInstanceType(instanceType)
                        .withInstanceId(instanceId)));

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(describeInstancesResult);
    }

}
