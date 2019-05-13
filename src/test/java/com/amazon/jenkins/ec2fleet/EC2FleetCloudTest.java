package com.amazon.jenkins.ec2fleet;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.BatchState;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsResult;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfig;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class, AmazonEC2Client.class, EC2FleetCloud.class, EC2FleetCloud.DescriptorImpl.class})
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

}
