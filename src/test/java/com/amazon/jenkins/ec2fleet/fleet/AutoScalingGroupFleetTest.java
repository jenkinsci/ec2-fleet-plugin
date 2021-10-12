package com.amazon.jenkins.ec2fleet.fleet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.amazon.jenkins.ec2fleet.FleetStateStats;
import com.amazon.jenkins.ec2fleet.utils.AWSUtils;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.LaunchTemplate;
import com.amazonaws.services.autoscaling.model.LaunchTemplateOverrides;
import com.amazonaws.services.autoscaling.model.MixedInstancesPolicy;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsHelper;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AWSUtils.class, AWSCredentialsHelper.class, AutoScalingGroupFleet.class, Jenkins.class })
public class AutoScalingGroupFleetTest {
    private static final String ENDPOINT = "fake-endpoint";
    private static final String REGION = "fake-region";
    private static final String CREDS_ID = "cred-Id";
    private static final String ASG_NAME = "asg-name";

    @Mock
    private Jenkins jenkins;
    @Mock
    private AmazonWebServicesCredentials amazonWebServicesCredentials;
    @Mock
    private AmazonAutoScalingClient autoScalingClient;
    @Mock
    private ClientConfiguration clientConfiguration;

    @Before
    public void before() {
        mockStatic(AWSUtils.class);
        mockStatic(AWSCredentialsHelper.class);
        mockStatic(Jenkins.class);
        when(Jenkins.get()).thenReturn(jenkins);
        when(AWSUtils.getClientConfiguration(ENDPOINT)).thenReturn(clientConfiguration);
    }

    @Test
    public void createAsgClientWithInstanceProfileWhenCredsNull() throws Exception {
        PowerMockito.whenNew(AmazonAutoScalingClient.class)
                .withArguments(clientConfiguration)
                .thenReturn(autoScalingClient);

        final AmazonAutoScalingClient result = new AutoScalingGroupFleet().createClient(null, REGION, ENDPOINT);
        assertEquals(autoScalingClient, result);
    }

    @Test
    public void createAsgClientWithAWSCredentialsWhenCredentialIdExists() throws Exception {
        when(AWSCredentialsHelper.getCredentials(CREDS_ID, jenkins)).thenReturn(amazonWebServicesCredentials);

        PowerMockito.whenNew(AmazonAutoScalingClient.class)
                .withArguments(amazonWebServicesCredentials, clientConfiguration)
                .thenReturn(autoScalingClient);

        final AmazonAutoScalingClient result = new AutoScalingGroupFleet().createClient(CREDS_ID, REGION, ENDPOINT);
        assertEquals(autoScalingClient, result);
    }

    @Test
    public void describeAutoScalingGroupsWithNoASG() throws Exception {
        final ListBoxModel listBoxModel = new ListBoxModel();

        when(AWSCredentialsHelper.getCredentials(CREDS_ID, jenkins)).thenReturn(amazonWebServicesCredentials);

        PowerMockito.whenNew(AmazonAutoScalingClient.class)
                .withArguments(amazonWebServicesCredentials, clientConfiguration)
                .thenReturn(autoScalingClient);

        final DescribeAutoScalingGroupsResult result = new DescribeAutoScalingGroupsResult().withAutoScalingGroups(new ArrayList<>());
        when(autoScalingClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class))).thenReturn(result);

        new AutoScalingGroupFleet().describe(CREDS_ID, REGION, ENDPOINT, listBoxModel, ASG_NAME, true);

        // No ASG is displayed
        assertEquals(0, listBoxModel.size());
    }

    @Test
    public void describeAutoScalingGroupsWithSingleASG() throws Exception {
        final String selectedAsgName = "selected-asg";
        when(AWSCredentialsHelper.getCredentials(CREDS_ID, jenkins)).thenReturn(amazonWebServicesCredentials);
        final ListBoxModel listBoxModel = new ListBoxModel();

        PowerMockito.whenNew(AmazonAutoScalingClient.class)
                .withArguments(amazonWebServicesCredentials, clientConfiguration)
                .thenReturn(autoScalingClient);

        final AutoScalingGroup asg = new AutoScalingGroup().withAutoScalingGroupName(selectedAsgName);
        final DescribeAutoScalingGroupsResult result = new DescribeAutoScalingGroupsResult().withAutoScalingGroups(Collections.singleton(asg));
        when(autoScalingClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class))).thenReturn(result);

        new AutoScalingGroupFleet().describe(CREDS_ID, REGION, ENDPOINT, listBoxModel, selectedAsgName, true);

        assertEquals(1, listBoxModel.size());
        // verify the selected ASG is returned
        assertEquals(listBoxModel.get(0).value, selectedAsgName);
        assertTrue(listBoxModel.get(0).selected);
    }

    @Test
    public void describeAutoScalingGroupsWithMultipleASG() throws Exception {
        final String selectedAsgName = "selected-asg";

        when(AWSCredentialsHelper.getCredentials(CREDS_ID, jenkins)).thenReturn(amazonWebServicesCredentials);
        ListBoxModel listBoxModel = new ListBoxModel();
        PowerMockito.whenNew(AmazonAutoScalingClient.class)
                .withArguments(amazonWebServicesCredentials, clientConfiguration)
                .thenReturn(autoScalingClient);
        final AutoScalingGroup selectedAsg = new AutoScalingGroup().withAutoScalingGroupName(selectedAsgName);
        final AutoScalingGroup asg = new AutoScalingGroup().withAutoScalingGroupName(ASG_NAME);
        final List<AutoScalingGroup> asgs = Arrays.asList(selectedAsg, asg);
        final DescribeAutoScalingGroupsResult result = new DescribeAutoScalingGroupsResult().withAutoScalingGroups(asgs);
        when(autoScalingClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class))).thenReturn(result);

        new AutoScalingGroupFleet().describe(CREDS_ID, REGION, ENDPOINT, listBoxModel, selectedAsgName, true);

        assertEquals(2, listBoxModel.size());

        // Verify selected ASG is marked correctly
        for (ListBoxModel.Option listBoxOptions : listBoxModel) {
            if(listBoxOptions.value.equals(selectedAsgName)) {
                assertTrue(listBoxOptions.selected);
            } else {
                assertEquals(listBoxOptions.value, ASG_NAME);
                assertFalse(listBoxOptions.selected);
            }
        }
    }

    @Test
    public void modifyAutoScalingGroupsShouldContainInstanceProtectedFromScaleIn() throws Exception {
        final int targetCapacity = 3;
        final int min = 1;
        final int max = 5;

        when(Jenkins.get()).thenReturn(jenkins);

        when(AWSCredentialsHelper.getCredentials(CREDS_ID, jenkins)).thenReturn(amazonWebServicesCredentials);
        ListBoxModel listBoxModel = new ListBoxModel();

        PowerMockito.whenNew(AmazonAutoScalingClient.class)
                .withArguments(amazonWebServicesCredentials, clientConfiguration)
                .thenReturn(autoScalingClient);

        final UpdateAutoScalingGroupRequest requestWithoutScaleIn = new UpdateAutoScalingGroupRequest()
                .withAutoScalingGroupName(ASG_NAME)
                .withMinSize(min).withMaxSize(max)
                .withDesiredCapacity(targetCapacity);
        final UpdateAutoScalingGroupRequest requestWithScaleIn = new UpdateAutoScalingGroupRequest()
                .withAutoScalingGroupName(ASG_NAME)
                .withMinSize(min).withMaxSize(max)
                .withDesiredCapacity(targetCapacity)
                .withNewInstancesProtectedFromScaleIn(Boolean.TRUE);
        new AutoScalingGroupFleet().modify(CREDS_ID, REGION, ENDPOINT, ASG_NAME, targetCapacity, min, max);
        verify(autoScalingClient, times(0)).updateAutoScalingGroup(requestWithoutScaleIn);
        verify(autoScalingClient, times(1)).updateAutoScalingGroup(requestWithScaleIn);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getFleetStateStatesWithEmptyASGs() throws Exception {
        when(Jenkins.get()).thenReturn(jenkins);
        when(AWSCredentialsHelper.getCredentials(CREDS_ID, jenkins)).thenReturn(amazonWebServicesCredentials);
        PowerMockito.whenNew(AmazonAutoScalingClient.class)
                .withArguments(amazonWebServicesCredentials, clientConfiguration)
                .thenReturn(autoScalingClient);

        final DescribeAutoScalingGroupsRequest describeAutoScalingGroupsRequest = new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(ASG_NAME);
        final DescribeAutoScalingGroupsResult result = new DescribeAutoScalingGroupsResult().withAutoScalingGroups(new ArrayList<>());
        when(autoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest)).thenReturn(result);

        final FleetStateStats fleetStateStats = new AutoScalingGroupFleet().getState(CREDS_ID, REGION, ENDPOINT, ASG_NAME);

        // Empty asg list should have thrown exception
        fail("Exception not raised");
    }

    @Test
    public void getFleetStateStates() throws Exception {
        final int desiredCapacity = 5;
        when(Jenkins.get()).thenReturn(jenkins);
        when(AWSCredentialsHelper.getCredentials(CREDS_ID, jenkins)).thenReturn(amazonWebServicesCredentials);
        PowerMockito.whenNew(AmazonAutoScalingClient.class)
                .withArguments(amazonWebServicesCredentials, clientConfiguration)
                .thenReturn(autoScalingClient);


        final DescribeAutoScalingGroupsRequest describeAutoScalingGroupsRequest = new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(ASG_NAME);
        final AutoScalingGroup asg = new AutoScalingGroup()
                .withAutoScalingGroupName(ASG_NAME)
                .withDesiredCapacity(desiredCapacity)
                .withInstances(Collections.singleton(new Instance().withInstanceId("i-123")));

        final DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult().withAutoScalingGroups(asg);
        when(autoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest)).thenReturn(describeAutoScalingGroupsResult);

        final FleetStateStats result = new AutoScalingGroupFleet().getState(CREDS_ID, REGION, ENDPOINT, ASG_NAME);

        assertEquals(desiredCapacity, result.getNumDesired());
        assertEquals(ASG_NAME, result.getFleetId());
        assertEquals(FleetStateStats.State.active(), result.getState());
        assertEquals(1, result.getInstances().size());
    }

    @Test
    public void getFleetStatesWithASGInstanceWeights() throws Exception {
        final int desiredCapacity = 5;
        when(Jenkins.get()).thenReturn(jenkins);
        when(AWSCredentialsHelper.getCredentials(CREDS_ID, jenkins)).thenReturn(amazonWebServicesCredentials);
        PowerMockito.whenNew(AmazonAutoScalingClient.class)
                .withArguments(amazonWebServicesCredentials, clientConfiguration)
                .thenReturn(autoScalingClient);


        final DescribeAutoScalingGroupsRequest describeAutoScalingGroupsRequest = new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(ASG_NAME);
        final AutoScalingGroup asg = new AutoScalingGroup()
                .withAutoScalingGroupName(ASG_NAME)
                .withDesiredCapacity(desiredCapacity)
                .withMixedInstancesPolicy(new MixedInstancesPolicy()
                        .withLaunchTemplate(new LaunchTemplate()
                                .withOverrides(
                                        new LaunchTemplateOverrides()
                                                .withInstanceType("t3.small")
                                                .withWeightedCapacity("1"),
                                        new LaunchTemplateOverrides()
                                                .withInstanceType("t3.large")
                                                .withWeightedCapacity("2"),
                                        new LaunchTemplateOverrides()
                                                .withInstanceType("t3.xlarge")
                                                .withWeightedCapacity("4.3")
                                )
                        )
                )
                .withInstances(Collections.singleton(new Instance().withInstanceId("i-123")));

        final DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult().withAutoScalingGroups(asg);
        when(autoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest)).thenReturn(describeAutoScalingGroupsResult);

        final FleetStateStats result = new AutoScalingGroupFleet().getState(CREDS_ID, REGION, ENDPOINT, ASG_NAME);

        final Map<String, Double> expectedWeights = new LinkedHashMap<>();
        expectedWeights.put("t3.small", 1d);
        expectedWeights.put("t3.large", 2d);
        expectedWeights.put("t3.xlarge", 4.3d);

        assertEquals(desiredCapacity, result.getNumDesired());
        assertEquals(ASG_NAME, result.getFleetId());
        assertEquals(FleetStateStats.State.active(), result.getState());
        assertEquals(1, result.getInstances().size());
        assertEquals(result.getInstanceTypeWeights(), expectedWeights);
    }
}
