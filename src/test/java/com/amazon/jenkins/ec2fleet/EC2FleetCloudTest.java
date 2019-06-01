package com.amazon.jenkins.ec2fleet;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.ActiveInstance;
import com.amazonaws.services.ec2.model.BatchState;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSpotFleetInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfig;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfigData;
import hudson.ExtensionList;
import hudson.model.LabelFinder;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.slaves.ComputerConnector;
import hudson.slaves.NodeProvisioner;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.model.Nodes;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class, AmazonEC2Client.class, EC2Api.class, EC2FleetCloud.class,
        EC2FleetCloud.DescriptorImpl.class, LabelFinder.class})
public class EC2FleetCloudTest {

    private SpotFleetRequestConfig spotFleetRequestConfig1;
    private SpotFleetRequestConfig spotFleetRequestConfig2;
    private SpotFleetRequestConfig spotFleetRequestConfig3;
    private SpotFleetRequestConfig spotFleetRequestConfig4;
    private SpotFleetRequestConfig spotFleetRequestConfig5;
    private SpotFleetRequestConfig spotFleetRequestConfig6;
    private SpotFleetRequestConfig spotFleetRequestConfig7;

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
    }

    @After
    public void after() {
        Registry.setEc2Api(new EC2Api());
    }

    @Test
    public void provision_fleetIsEmpty() {
        // given
        PowerMockito.mockStatic(Jenkins.class, EC2Api.class);

        AmazonEC2 amazonEC2 = mock(AmazonEC2.class);
        EC2Api ec2Api = mock(EC2Api.class);
        Registry.setEc2Api(ec2Api);
        when(ec2Api.connect(any(String.class), any(String.class))).thenReturn(amazonEC2);

        DescribeSpotFleetInstancesResult describeSpotFleetInstancesResult = new DescribeSpotFleetInstancesResult();
        when(amazonEC2.describeSpotFleetInstances(any(DescribeSpotFleetInstancesRequest.class)))
                .thenReturn(describeSpotFleetInstancesResult);

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = new DescribeSpotFleetRequestsResult();
        describeSpotFleetRequestsResult.setSpotFleetRequestConfigs(Arrays.asList(
                new SpotFleetRequestConfig()
                        .withSpotFleetRequestState("active")
                        .withSpotFleetRequestConfig(
                                new SpotFleetRequestConfigData().withTargetCapacity(0))));
        when(amazonEC2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        Jenkins jenkins = mock(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, "credId", null, "region",
                null, null, null, null, false,
                false, 0, 0, 1, 1, false, false);

        // when
        Collection<NodeProvisioner.PlannedNode> r = fleetCloud.provision(null, 1);

        // then
        Assert.assertEquals(1, r.size());
    }

    @Test
    public void updateStatus_doNothingWhenFleetIsEmpty() {
        // given
        PowerMockito.mockStatic(Jenkins.class, EC2Api.class);

        AmazonEC2 amazonEC2 = mock(AmazonEC2.class);
        EC2Api ec2Api = mock(EC2Api.class);
        Registry.setEc2Api(ec2Api);
        when(ec2Api.connect(any(String.class), any(String.class))).thenReturn(amazonEC2);

        DescribeSpotFleetInstancesResult describeSpotFleetInstancesResult = new DescribeSpotFleetInstancesResult();
        when(amazonEC2.describeSpotFleetInstances(any(DescribeSpotFleetInstancesRequest.class)))
                .thenReturn(describeSpotFleetInstancesResult);

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = new DescribeSpotFleetRequestsResult();
        describeSpotFleetRequestsResult.setSpotFleetRequestConfigs(Arrays.asList(
                new SpotFleetRequestConfig()
                        .withSpotFleetRequestState("active")
                        .withSpotFleetRequestConfig(
                                new SpotFleetRequestConfigData().withTargetCapacity(0))));
        when(amazonEC2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        Jenkins jenkins = mock(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, "credId", null, "region",
                "fleetId", null, null, null, false,
                false, 0, 0, 1, 1, false, false);

        // when
        FleetStateStats stats = fleetCloud.updateStatus();

        // then
        Assert.assertEquals(0, stats.getNumDesired());
        Assert.assertEquals(0, stats.getNumActive());
        Assert.assertEquals("fleetId", stats.getFleetId());
    }

    @Test
    public void updateStatus_shouldAddNodeIfAnyNewDescribed() throws IOException {
        // given
        PowerMockito.mockStatic(Jenkins.class, EC2Api.class, LabelFinder.class);

        AmazonEC2 amazonEC2 = mock(AmazonEC2.class);
        EC2Api ec2Api = mock(EC2Api.class);
        Registry.setEc2Api(ec2Api);
        when(ec2Api.connect(any(String.class), any(String.class))).thenReturn(amazonEC2);

        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult();
        describeInstancesResult.withReservations(
                new Reservation().withInstances(new Instance()
                        .withPublicIpAddress("p-ip")
                        .withInstanceId("i-0")));

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(describeInstancesResult);

        DescribeSpotFleetInstancesResult describeSpotFleetInstancesResult = new DescribeSpotFleetInstancesResult();
        describeSpotFleetInstancesResult.setActiveInstances(Arrays.asList(
                new ActiveInstance().withInstanceId("i-0")
        ));

        when(amazonEC2.describeSpotFleetInstances(any(DescribeSpotFleetInstancesRequest.class)))
                .thenReturn(describeSpotFleetInstancesResult);

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = new DescribeSpotFleetRequestsResult();
        describeSpotFleetRequestsResult.setSpotFleetRequestConfigs(Arrays.asList(
                new SpotFleetRequestConfig()
                        .withSpotFleetRequestState("active")
                        .withSpotFleetRequestConfig(
                                new SpotFleetRequestConfigData().withTargetCapacity(0))));
        when(amazonEC2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        Jenkins jenkins = mock(Jenkins.class);
        Mockito.when(jenkins.getNodesObject()).thenReturn(mock(Nodes.class));
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);

        // mock
        ExtensionList labelFinder = mock(ExtensionList.class);
        when(labelFinder.iterator()).thenReturn(Collections.emptyIterator());
        PowerMockito.when(LabelFinder.all()).thenReturn(labelFinder);

        // mocking part of node creation process Jenkins.getInstance().getLabelAtom(l)
        Mockito.when(jenkins.getLabelAtom(anyString())).thenReturn(new LabelAtom("mock-label"));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, "credId", null, "region",
                "fleetId", null, null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 1, 1, false, false);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        Mockito.doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        FleetStateStats stats = fleetCloud.updateStatus();

        // then
        Assert.assertEquals(0, stats.getNumDesired());
        Assert.assertEquals(1, stats.getNumActive());
        Assert.assertEquals("fleetId", stats.getFleetId());

        // and
        Node actualFleetNode = nodeCaptor.getValue();
        Assert.assertEquals(Node.Mode.NORMAL, actualFleetNode.getMode());
    }

    @Test
    public void updateStatus_shouldAddNodeIfAnyNewDescribed_restrictUsage() throws IOException {
        // given
        PowerMockito.mockStatic(Jenkins.class, EC2Api.class, LabelFinder.class);

        AmazonEC2 amazonEC2 = mock(AmazonEC2.class);
        EC2Api ec2Api = mock(EC2Api.class);
        Registry.setEc2Api(ec2Api);
        when(ec2Api.connect(any(String.class), any(String.class))).thenReturn(amazonEC2);

        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult();
        describeInstancesResult.withReservations(
                new Reservation().withInstances(new Instance()
                        .withPublicIpAddress("p-ip")
                        .withInstanceId("i-0")));

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(describeInstancesResult);

        DescribeSpotFleetInstancesResult describeSpotFleetInstancesResult = new DescribeSpotFleetInstancesResult();
        describeSpotFleetInstancesResult.setActiveInstances(Arrays.asList(
                new ActiveInstance().withInstanceId("i-0")
        ));

        when(amazonEC2.describeSpotFleetInstances(any(DescribeSpotFleetInstancesRequest.class)))
                .thenReturn(describeSpotFleetInstancesResult);

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = new DescribeSpotFleetRequestsResult();
        describeSpotFleetRequestsResult.setSpotFleetRequestConfigs(Arrays.asList(
                new SpotFleetRequestConfig()
                        .withSpotFleetRequestState("active")
                        .withSpotFleetRequestConfig(
                                new SpotFleetRequestConfigData().withTargetCapacity(0))));
        when(amazonEC2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        Jenkins jenkins = mock(Jenkins.class);
        Mockito.when(jenkins.getNodesObject()).thenReturn(mock(Nodes.class));
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);

        // mock
        ExtensionList labelFinder = mock(ExtensionList.class);
        when(labelFinder.iterator()).thenReturn(Collections.emptyIterator());
        PowerMockito.when(LabelFinder.all()).thenReturn(labelFinder);

        // mocking part of node creation process Jenkins.getInstance().getLabelAtom(l)
        Mockito.when(jenkins.getLabelAtom(anyString())).thenReturn(new LabelAtom("mock-label"));

        EC2FleetCloud fleetCloud = new EC2FleetCloud(null, "credId", null, "region",
                "fleetId", null, null, PowerMockito.mock(ComputerConnector.class), false,
                false, 0, 0, 1, 1, false, true);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        Mockito.doNothing().when(jenkins).addNode(nodeCaptor.capture());

        // when
        FleetStateStats stats = fleetCloud.updateStatus();

        // then
        Assert.assertEquals(0, stats.getNumDesired());
        Assert.assertEquals(1, stats.getNumActive());
        Assert.assertEquals("fleetId", stats.getFleetId());

        // and
        Node actualFleetNode = nodeCaptor.getValue();
        Assert.assertEquals(Node.Mode.EXCLUSIVE, actualFleetNode.getMode());
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnEmptyListIfNoFleetInAccount() {
        PowerMockito.mockStatic(Jenkins.class);

        Jenkins jenkins = mock(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                false, "", "", "");

        Assert.assertEquals(0, r.size());
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnEmptyListIfNoActiveFleets() throws Exception {
        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.mockStatic(AmazonEC2Client.class);

        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        PowerMockito.whenNew(AmazonEC2Client.class).withNoArguments().thenReturn(amazonEC2Client);

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = mock(DescribeSpotFleetRequestsResult.class);
        when(amazonEC2Client.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        when(describeSpotFleetRequestsResult.getSpotFleetRequestConfigs())
                .thenReturn(Arrays.asList(spotFleetRequestConfig4, spotFleetRequestConfig5,
                        spotFleetRequestConfig6, spotFleetRequestConfig7));

        Jenkins jenkins = mock(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                false, "", "", "");

        Assert.assertEquals(0, r.size());
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnActiveFleets() throws Exception {
        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.mockStatic(AmazonEC2Client.class);

        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        PowerMockito.whenNew(AmazonEC2Client.class).withNoArguments().thenReturn(amazonEC2Client);

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = mock(DescribeSpotFleetRequestsResult.class);
        when(amazonEC2Client.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        when(describeSpotFleetRequestsResult.getSpotFleetRequestConfigs())
                .thenReturn(Arrays.asList(spotFleetRequestConfig1, spotFleetRequestConfig2,
                        spotFleetRequestConfig3, spotFleetRequestConfig4, spotFleetRequestConfig5,
                        spotFleetRequestConfig6, spotFleetRequestConfig7));

        Jenkins jenkins = mock(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                false, "", "", "");

        Assert.assertEquals(3, r.size());
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnAllFleetsIfShowNonActiveIsEnabled() throws Exception {
        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.mockStatic(AmazonEC2Client.class);

        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        PowerMockito.whenNew(AmazonEC2Client.class).withNoArguments().thenReturn(amazonEC2Client);

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = mock(DescribeSpotFleetRequestsResult.class);
        when(amazonEC2Client.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        when(describeSpotFleetRequestsResult.getSpotFleetRequestConfigs())
                .thenReturn(Arrays.asList(spotFleetRequestConfig1, spotFleetRequestConfig2,
                        spotFleetRequestConfig3, spotFleetRequestConfig4, spotFleetRequestConfig5,
                        spotFleetRequestConfig6, spotFleetRequestConfig7));

        Jenkins jenkins = mock(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                true, "", "", "");

        Assert.assertEquals(7, r.size());
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnFleetInfo() throws Exception {
        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.mockStatic(AmazonEC2Client.class);

        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        PowerMockito.whenNew(AmazonEC2Client.class).withNoArguments().thenReturn(amazonEC2Client);

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = mock(DescribeSpotFleetRequestsResult.class);
        when(amazonEC2Client.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        spotFleetRequestConfig1.setSpotFleetRequestId("fleet-id");

        when(describeSpotFleetRequestsResult.getSpotFleetRequestConfigs())
                .thenReturn(Arrays.asList(spotFleetRequestConfig1));

        Jenkins jenkins = mock(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                false, "", "", "");

        Assert.assertEquals("fleet-id (active)", r.get(0).name);
        Assert.assertEquals("fleet-id", r.get(0).value);
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnFleetsCrossPages() throws Exception {
        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.mockStatic(AmazonEC2Client.class);

        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        PowerMockito.whenNew(AmazonEC2Client.class).withNoArguments().thenReturn(amazonEC2Client);

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

        Jenkins jenkins = mock(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                false, "", "", "");

        Assert.assertEquals("a", r.get(0).value);
        Assert.assertEquals("b", r.get(1).value);
        Assert.assertEquals("c", r.get(2).value);
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnSelectedFleetInAnyState() throws Exception {
        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.mockStatic(AmazonEC2Client.class);

        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        PowerMockito.whenNew(AmazonEC2Client.class).withNoArguments().thenReturn(amazonEC2Client);

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = mock(DescribeSpotFleetRequestsResult.class);
        when(amazonEC2Client.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        spotFleetRequestConfig1.setSpotFleetRequestId("a");
        spotFleetRequestConfig2.setSpotFleetRequestId("failed_selected");
        spotFleetRequestConfig2.setSpotFleetRequestState(BatchState.Failed);

        when(describeSpotFleetRequestsResult.getSpotFleetRequestConfigs())
                .thenReturn(Arrays.asList(spotFleetRequestConfig1, spotFleetRequestConfig2));

        Jenkins jenkins = mock(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                false, "", "", "failed_selected");

        Assert.assertEquals("a", r.get(0).value);
        Assert.assertEquals("failed_selected", r.get(1).value);
    }

    @Test
    public void descriptorImpl_doFillFleetItems_returnEmptyListIfAnyException() throws Exception {
        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.mockStatic(AmazonEC2Client.class);

        PowerMockito.whenNew(AmazonEC2Client.class).withNoArguments().thenThrow(new IllegalArgumentException("test"));

        Jenkins jenkins = mock(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);

        ListBoxModel r = new EC2FleetCloud.DescriptorImpl().doFillFleetItems(
                false, "", "", "");

        Assert.assertEquals(0, r.size());
    }

    @Test
    public void getDisplayName_returnDefaultWhenNull() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                null, null, null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, false, false);
        Assert.assertEquals(ec2FleetCloud.getDisplayName(), EC2FleetCloud.FLEET_CLOUD_ID);
    }

    @Test
    public void getDisplayName_returnDisplayName() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                "CloudName", null, null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, false, false);
        Assert.assertEquals(ec2FleetCloud.getDisplayName(), "CloudName");
    }

    @Test
    public void getAwsCredentialsId_returnNull_whenNoCredentialsIdOrAwsCredentialsId() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                null, null, null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, false, false);
        Assert.assertNull(ec2FleetCloud.getAwsCredentialsId());
    }

    @Test
    public void getAwsCredentialsId_returnValue_whenCredentialsIdPresent() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                null, null, "Opa", null, null,
                null, null, null, false,
                false, null, null, null,
                null, false, false);
        Assert.assertEquals("Opa", ec2FleetCloud.getAwsCredentialsId());
    }

    @Test
    public void getAwsCredentialsId_returnValue_whenAwsCredentialsIdPresent() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                null, "Opa", null, null, null,
                null, null, null, false,
                false, null, null, null,
                null, false, false);
        Assert.assertEquals("Opa", ec2FleetCloud.getAwsCredentialsId());
    }

    @Test
    public void getAwsCredentialsId_returnAwsCredentialsId_whenAwsCredentialsIdAndCredentialsIdPresent() {
        EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
                null, "A", "B", null, null,
                null, null, null, false,
                false, null, null, null,
                null, false, false);
        Assert.assertEquals("A", ec2FleetCloud.getAwsCredentialsId());
    }

}
