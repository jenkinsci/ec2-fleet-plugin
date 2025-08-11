package com.amazon.jenkins.ec2fleet.fleet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.amazon.jenkins.ec2fleet.FleetStateStats;
import com.amazon.jenkins.ec2fleet.aws.AWSUtils;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsHelper;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.AutoScalingClientBuilder;
import software.amazon.awssdk.services.autoscaling.model.*;
import software.amazon.awssdk.services.autoscaling.paginators.DescribeAutoScalingGroupsIterable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutoScalingGroupFleetTest {

    private MockedStatic<Jenkins> mockedJenkins;

    private MockedStatic<AWSCredentialsHelper> mockedAWSCredentialsHelper;

    private MockedStatic<AWSUtils> mockedAWSUtils;

    private static final String ENDPOINT = "fake-endpoint";
    private static final String REGION = "fake-region";
    private static final String CREDS_ID = "cred-Id";
    private static final String ASG_NAME = "asg-name";

    @Mock
    private Jenkins jenkins;
    @Mock
    private AmazonWebServicesCredentials amazonWebServicesCredentials;
    @Mock
    private ClientOverrideConfiguration clientConfiguration;

    @BeforeEach
    void before() {
        mockedJenkins = mockStatic(Jenkins.class);
        mockedJenkins.when(Jenkins::get).thenReturn(jenkins);

        mockedAWSUtils = mockStatic(AWSUtils.class);
        mockedAWSUtils.when(AWSUtils::getClientConfiguration).thenReturn(clientConfiguration);

        mockedAWSCredentialsHelper = mockStatic(AWSCredentialsHelper.class);
    }

    @AfterEach
    void after() {
        mockedAWSCredentialsHelper.close();
        mockedAWSUtils.close();
        mockedJenkins.close();
    }

    @Test
    void createAsgClientWithInstanceProfileWhenCredsNull() {
        try (MockedStatic<AutoScalingClient> mockedStatic = mockStatic(AutoScalingClient.class)) {
            AutoScalingClientBuilder builderMock = Mockito.mock(AutoScalingClientBuilder.class, Mockito.RETURNS_SELF);
            mockedStatic.when(AutoScalingClient::builder).thenReturn(builderMock);
            new AutoScalingGroupFleet().createClient(null, REGION, ENDPOINT);
            verify(builderMock, times(1)).overrideConfiguration((ClientOverrideConfiguration) any());
        }
    }

    @Test
    void createAsgClientWithAWSCredentialsWhenCredentialIdExists() {
        mockedAWSCredentialsHelper.when(() -> AWSCredentialsHelper.getCredentials(CREDS_ID, jenkins)).thenReturn(amazonWebServicesCredentials);

        try (MockedStatic<AutoScalingClient> mockedStatic = mockStatic(AutoScalingClient.class)) {
            AutoScalingClientBuilder builderMock = Mockito.mock(AutoScalingClientBuilder.class, Mockito.RETURNS_SELF);
            mockedStatic.when(AutoScalingClient::builder).thenReturn(builderMock);
            // Call your method that creates the client
            new AutoScalingGroupFleet().createClient(CREDS_ID, REGION, ENDPOINT);

            // Verify that credentialsProvider was called on the builder
            verify(builderMock, times(1)).credentialsProvider(any());
        }
    }

    @Test
    void describeAutoScalingGroupsWithNoASG() {
        final ListBoxModel listBoxModel = new ListBoxModel();
        mockedAWSCredentialsHelper.when(() -> AWSCredentialsHelper.getCredentials(CREDS_ID, jenkins)).thenReturn(amazonWebServicesCredentials);
        try (MockedStatic<AutoScalingClient> mockedStatic = mockStatic(AutoScalingClient.class)) {
            AutoScalingClientBuilder builderMock = Mockito.mock(AutoScalingClientBuilder.class, Mockito.RETURNS_SELF);
            AutoScalingClient clientMock = Mockito.mock(AutoScalingClient.class);
            mockedStatic.when(AutoScalingClient::builder).thenReturn(builderMock);
            when(builderMock.build()).thenReturn(clientMock);

            final DescribeAutoScalingGroupsResponse result = DescribeAutoScalingGroupsResponse.builder().autoScalingGroups(new ArrayList<>())
                    .build();
            DescribeAutoScalingGroupsIterable iterableMock = Mockito.mock(DescribeAutoScalingGroupsIterable.class);
            when(iterableMock.iterator()).thenReturn(Collections.singleton(result).iterator());
            when(clientMock.describeAutoScalingGroupsPaginator(any(DescribeAutoScalingGroupsRequest.class)))
                .thenReturn(iterableMock);

            new AutoScalingGroupFleet().describe(CREDS_ID, REGION, ENDPOINT, listBoxModel, ASG_NAME, true);
        }

        // No ASG is displayed
        assertEquals(0, listBoxModel.size());
    }

    @Test
    void describeAutoScalingGroupsWithSingleASG() {
        final String selectedAsgName = "selected-asg";
        mockedAWSCredentialsHelper.when(() -> AWSCredentialsHelper.getCredentials(CREDS_ID, jenkins)).thenReturn(amazonWebServicesCredentials);
        final ListBoxModel listBoxModel = new ListBoxModel();
        try (MockedStatic<AutoScalingClient> mockedStatic = mockStatic(AutoScalingClient.class)) {
            AutoScalingClientBuilder builderMock = Mockito.mock(AutoScalingClientBuilder.class, Mockito.RETURNS_SELF);
            AutoScalingClient clientMock = Mockito.mock(AutoScalingClient.class);
            mockedStatic.when(AutoScalingClient::builder).thenReturn(builderMock);
            when(builderMock.build()).thenReturn(clientMock);

            final AutoScalingGroup asg = AutoScalingGroup.builder().autoScalingGroupName(selectedAsgName)
                    .build();
            final DescribeAutoScalingGroupsResponse result = DescribeAutoScalingGroupsResponse.builder().autoScalingGroups(Collections.singleton(asg))
                    .build();
            DescribeAutoScalingGroupsIterable iterableMock = Mockito.mock(DescribeAutoScalingGroupsIterable.class);
            when(iterableMock.iterator()).thenReturn(Collections.singleton(result).iterator());
            when(clientMock.describeAutoScalingGroupsPaginator(any(DescribeAutoScalingGroupsRequest.class)))
                .thenReturn(iterableMock);

            new AutoScalingGroupFleet().describe(CREDS_ID, REGION, ENDPOINT, listBoxModel, selectedAsgName, true);
        }
        assertEquals(1, listBoxModel.size());
        assertEquals(selectedAsgName, listBoxModel.get(0).value);
        assertTrue(listBoxModel.get(0).selected);
    }

    @Test
    void describeAutoScalingGroupsWithMultipleASG() {
        final String selectedAsgName = "selected-asg";
        mockedAWSCredentialsHelper.when(() -> AWSCredentialsHelper.getCredentials(CREDS_ID, jenkins)).thenReturn(amazonWebServicesCredentials);
        ListBoxModel listBoxModel = new ListBoxModel();
        try (MockedStatic<AutoScalingClient> mockedStatic = mockStatic(AutoScalingClient.class)) {
            AutoScalingClientBuilder builderMock = Mockito.mock(AutoScalingClientBuilder.class, Mockito.RETURNS_SELF);
            AutoScalingClient clientMock = Mockito.mock(AutoScalingClient.class);
            mockedStatic.when(AutoScalingClient::builder).thenReturn(builderMock);
            when(builderMock.build()).thenReturn(clientMock);

            final AutoScalingGroup selectedAsg = AutoScalingGroup.builder().autoScalingGroupName(selectedAsgName)
                    .build();
            final AutoScalingGroup asg = AutoScalingGroup.builder().autoScalingGroupName(ASG_NAME)
                    .build();
            final List<AutoScalingGroup> asgs = Arrays.asList(selectedAsg, asg);
            final DescribeAutoScalingGroupsResponse result = DescribeAutoScalingGroupsResponse.builder().autoScalingGroups(asgs)
                    .build();
            DescribeAutoScalingGroupsIterable iterableMock = Mockito.mock(DescribeAutoScalingGroupsIterable.class);
            when(iterableMock.iterator()).thenReturn(Collections.singleton(result).iterator());
            when(clientMock.describeAutoScalingGroupsPaginator(any(DescribeAutoScalingGroupsRequest.class)))
                .thenReturn(iterableMock);

            new AutoScalingGroupFleet().describe(CREDS_ID, REGION, ENDPOINT, listBoxModel, selectedAsgName, true);
        }
        assertEquals(2, listBoxModel.size());

        // Verify selected ASG is marked correctly
        for (ListBoxModel.Option listBoxOptions : listBoxModel) {
            if(listBoxOptions.value.equals(selectedAsgName)) {
                assertTrue(listBoxOptions.selected);
            } else {
                assertEquals(ASG_NAME, listBoxOptions.value);
                assertFalse(listBoxOptions.selected);
            }
        }
    }

    @Test
    void modifyAutoScalingGroupsShouldContainInstanceProtectedFromScaleIn() {
        final int targetCapacity = 3;
        final int min = 1;
        final int max = 5;
        mockedJenkins.when(Jenkins::get).thenReturn(jenkins);
        mockedAWSCredentialsHelper.when(() -> AWSCredentialsHelper.getCredentials(CREDS_ID, jenkins)).thenReturn(amazonWebServicesCredentials);
        try (MockedStatic<AutoScalingClient> mockedStatic = mockStatic(AutoScalingClient.class)) {
            AutoScalingClientBuilder builderMock = Mockito.mock(AutoScalingClientBuilder.class, Mockito.RETURNS_SELF);
            AutoScalingClient clientMock = Mockito.mock(AutoScalingClient.class);
            mockedStatic.when(AutoScalingClient::builder).thenReturn(builderMock);
            when(builderMock.build()).thenReturn(clientMock);

            final UpdateAutoScalingGroupRequest requestWithoutScaleIn = UpdateAutoScalingGroupRequest.builder()
                    .autoScalingGroupName(ASG_NAME)
                    .minSize(min).maxSize(max)
                    .desiredCapacity(targetCapacity)
                    .build();
            final UpdateAutoScalingGroupRequest requestWithScaleIn = UpdateAutoScalingGroupRequest.builder()
                    .autoScalingGroupName(ASG_NAME)
                    .minSize(min).maxSize(max)
                    .desiredCapacity(targetCapacity)
                    .newInstancesProtectedFromScaleIn(Boolean.TRUE)
                    .build();

            new AutoScalingGroupFleet().modify(CREDS_ID, REGION, ENDPOINT, ASG_NAME, targetCapacity, min, max);
            verify(clientMock, times(0)).updateAutoScalingGroup(requestWithoutScaleIn);
            verify(clientMock, times(1)).updateAutoScalingGroup(requestWithScaleIn);
        }
    }

    @Test
    void getFleetStateStatesWithEmptyASGs() {
        mockedJenkins.when(Jenkins::get).thenReturn(jenkins);
        mockedAWSCredentialsHelper.when(() -> AWSCredentialsHelper.getCredentials(CREDS_ID, jenkins)).thenReturn(amazonWebServicesCredentials);
        try (MockedStatic<AutoScalingClient> mockedStatic = mockStatic(AutoScalingClient.class)) {
                AutoScalingClientBuilder builderMock = Mockito.mock(AutoScalingClientBuilder.class, Mockito.RETURNS_SELF);
                AutoScalingClient clientMock = Mockito.mock(AutoScalingClient.class);
                mockedStatic.when(AutoScalingClient::builder).thenReturn(builderMock);
                when(builderMock.build()).thenReturn(clientMock);

                final DescribeAutoScalingGroupsRequest describeAutoScalingGroupsRequest = DescribeAutoScalingGroupsRequest.builder().autoScalingGroupNames(ASG_NAME)
                        .build();
                final DescribeAutoScalingGroupsResponse result = DescribeAutoScalingGroupsResponse.builder().autoScalingGroups(new ArrayList<>())
                        .build();
                when(clientMock.describeAutoScalingGroups(describeAutoScalingGroupsRequest)).thenReturn(result);

                assertThrows(IllegalArgumentException.class, () -> new AutoScalingGroupFleet().getState(CREDS_ID, REGION, ENDPOINT, ASG_NAME));
            }
    }

    @Test
    void getFleetStateStates() {
        final int desiredCapacity = 5;
        mockedJenkins.when(Jenkins::get).thenReturn(jenkins);
        mockedAWSCredentialsHelper.when(() -> AWSCredentialsHelper.getCredentials(CREDS_ID, jenkins)).thenReturn(amazonWebServicesCredentials);
        try (MockedStatic<AutoScalingClient> mockedStatic = mockStatic(AutoScalingClient.class)) {
            AutoScalingClientBuilder builderMock = Mockito.mock(AutoScalingClientBuilder.class, Mockito.RETURNS_SELF);
            AutoScalingClient clientMock = Mockito.mock(AutoScalingClient.class);
            mockedStatic.when(AutoScalingClient::builder).thenReturn(builderMock);
            when(builderMock.build()).thenReturn(clientMock);

            final DescribeAutoScalingGroupsRequest describeAutoScalingGroupsRequest = DescribeAutoScalingGroupsRequest.builder().autoScalingGroupNames(ASG_NAME)
                    .build();
            final AutoScalingGroup asg = AutoScalingGroup.builder()
                    .autoScalingGroupName(ASG_NAME)
                    .desiredCapacity(desiredCapacity)
                    .instances(Collections.singleton(Instance.builder().instanceId("i-123")
                            .build()))

                    .build();
            final DescribeAutoScalingGroupsResponse describeAutoScalingGroupsResult = DescribeAutoScalingGroupsResponse.builder().autoScalingGroups(asg)
                    .build();
            when(clientMock.describeAutoScalingGroups(describeAutoScalingGroupsRequest)).thenReturn(describeAutoScalingGroupsResult);

            final FleetStateStats result = new AutoScalingGroupFleet().getState(CREDS_ID, REGION, ENDPOINT, ASG_NAME);

            assertEquals(desiredCapacity, result.getNumDesired());
            assertEquals(ASG_NAME, result.getFleetId());
            assertEquals(FleetStateStats.State.active(), result.getState());
            assertEquals(1, result.getInstances().size());
        }
    }

    @Test
    void getFleetStatesWithASGInstanceWeights() {
        final int desiredCapacity = 5;
        mockedJenkins.when(Jenkins::get).thenReturn(jenkins);
        mockedAWSCredentialsHelper.when(() -> AWSCredentialsHelper.getCredentials(CREDS_ID, jenkins)).thenReturn(amazonWebServicesCredentials);
        try (MockedStatic<AutoScalingClient> mockedStatic = mockStatic(AutoScalingClient.class)) {
            AutoScalingClientBuilder builderMock = Mockito.mock(AutoScalingClientBuilder.class, Mockito.RETURNS_SELF);
            AutoScalingClient clientMock = Mockito.mock(AutoScalingClient.class);
            mockedStatic.when(AutoScalingClient::builder).thenReturn(builderMock);
            when(builderMock.build()).thenReturn(clientMock);

            final DescribeAutoScalingGroupsRequest describeAutoScalingGroupsRequest = DescribeAutoScalingGroupsRequest.builder().autoScalingGroupNames(ASG_NAME)
                    .build();
            final AutoScalingGroup asg = AutoScalingGroup.builder()
                    .autoScalingGroupName(ASG_NAME)
                    .desiredCapacity(desiredCapacity)
                    .mixedInstancesPolicy(MixedInstancesPolicy.builder()
                            .launchTemplate(LaunchTemplate.builder()
                                    .overrides(
                                            LaunchTemplateOverrides.builder()
                                                    .instanceType("t3.small")
                                                    .weightedCapacity("1")
                                            .build(), 
                                            LaunchTemplateOverrides.builder()
                                                    .instanceType("t3.large")
                                                    .weightedCapacity("2")
                                            .build(), 
                                            LaunchTemplateOverrides.builder()
                                                    .instanceType("t3.xlarge")
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .instances(Collections.singleton(Instance.builder().instanceId("i-123")
                            .build()))
                    .build();

            final DescribeAutoScalingGroupsResponse describeAutoScalingGroupsResult = DescribeAutoScalingGroupsResponse.builder().autoScalingGroups(asg)
                    .build();
            when(clientMock.describeAutoScalingGroups(describeAutoScalingGroupsRequest)).thenReturn(describeAutoScalingGroupsResult);

            final Map<String, Double> expectedWeights = new LinkedHashMap<>();
            expectedWeights.put("t3.small", 1d);
            expectedWeights.put("t3.large", 2d);

            final FleetStateStats result = new AutoScalingGroupFleet().getState(CREDS_ID, REGION, ENDPOINT, ASG_NAME);
            assertEquals(desiredCapacity, result.getNumDesired());
            assertEquals(ASG_NAME, result.getFleetId());
            assertEquals(FleetStateStats.State.active(), result.getState());
            assertEquals(1, result.getInstances().size());
            assertEquals(expectedWeights, result.getInstanceTypeWeights());
        }
    }
}
