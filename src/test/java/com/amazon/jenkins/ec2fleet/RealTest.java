package com.amazon.jenkins.ec2fleet;

import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsImpl;
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.sshslaves.SSHConnector;
import hudson.plugins.sshslaves.verifiers.NonVerifyingKeyVerificationStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.CreateAutoScalingGroupRequest;
import software.amazon.awssdk.services.autoscaling.model.DeleteAutoScalingGroupRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.LaunchTemplateSpecification;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RealTest extends IntegrationTest {

    private static final String USER_DATA_INSTALL_JAVA8 = Base64.getEncoder().encodeToString(
            "#!/bin/bash\nyum install java-1.8.0 -y && yum remove java-1.7.0-openjdk -y && java -version"
                    .getBytes(StandardCharsets.UTF_8));

    @BeforeAll
    static void beforeClass() {
        turnOffJenkinsTestTimout();
    }

    private List<String> credentialLines;
    private String privateKeyName;
    private AwsCredentialsProvider awsCredentialsProvider;
    private EC2FleetCloud.ExecutorScaler noScaling;

    @BeforeEach
    void before() throws IOException {
        credentialLines = FileUtils.readLines(new File("credentials.txt"), StandardCharsets.UTF_8);
        privateKeyName = getPrivateKeyName(credentialLines);
        awsCredentialsProvider = getAwsCredentialsProvider(credentialLines);
        noScaling = new EC2FleetCloud.NoScaler();
    }

    @Disabled("for manual run as you need to provide real AWS credentials")
    @Test
    void givenAutoScalingGroup_shouldScaleUpExecuteTaskAndScaleDown() throws IOException {
        final Ec2Client amazonEC2 = Ec2Client.builder().credentialsProvider(awsCredentialsProvider).build();

        final AutoScalingClient autoScalingClient = AutoScalingClient.builder().credentialsProvider(awsCredentialsProvider).build();

        final String ltName = getOrCreateLaunchTemplate(amazonEC2, privateKeyName);

        final List<String> azs = new ArrayList<>();
        final DescribeAvailabilityZonesResponse describeAvailabilityZonesResult = amazonEC2.describeAvailabilityZones();
        for (AvailabilityZone az : describeAvailabilityZonesResult.availabilityZones()) {
            azs.add(az.zoneName());
        }

        final String autoScalingGroupName = "ec2-fleet-plugin-real-test";
        try {
            autoScalingClient.deleteAutoScalingGroup(DeleteAutoScalingGroupRequest.builder()
                    .autoScalingGroupName(autoScalingGroupName)
                    .forceDelete(true)
                    .build());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        System.out.println("waiting until group will be deleted");
        tryUntil(() -> assertEquals(0, autoScalingClient.describeAutoScalingGroups(DescribeAutoScalingGroupsRequest.builder()
                .autoScalingGroupNames(autoScalingGroupName)
                .build()).autoScalingGroups().size()), TimeUnit.MINUTES.toMillis(3));

        autoScalingClient.createAutoScalingGroup(
                CreateAutoScalingGroupRequest.builder()
                        .autoScalingGroupName(autoScalingGroupName)
                        .desiredCapacity(0)
                        .minSize(0)
                        .maxSize(2)
                        .availabilityZones(azs)
                        .launchTemplate(LaunchTemplateSpecification.builder()
                                .launchTemplateName(ltName)
                                .version("$Default")
                                .build())
                .build());

        final String credentialId = setupJenkinsAwsCredentials(awsCredentialsProvider);
        final String sshCredentialId = setupJenkinsSshCredentials(credentialLines);
        final SSHConnector computerConnector = new SSHConnector(
                22, sshCredentialId, null, null, null,
                null, null, null, null, new NonVerifyingKeyVerificationStrategy());
        final EC2FleetCloud cloud = new EC2FleetCloud(
                "TestCloud", credentialId, null, null, null,
                autoScalingGroupName,
                "momo", null, computerConnector, false, false,
                1, 0, 5, 0, 1, true, false,
                "-1", false, 180, 15,
                10, true, false, noScaling, false, false);
        j.jenkins.clouds.add(cloud);

        final List<QueueTaskFuture> tasks = enqueTask(2);

        waitJobSuccessfulExecution(tasks);
        waitZeroNodes();

        System.out.println("wait until EC2 spot fleet will be zero size");
        tryUntil(() -> {
            final DescribeAutoScalingGroupsResponse r = autoScalingClient.describeAutoScalingGroups(
                    DescribeAutoScalingGroupsRequest.builder().autoScalingGroupNames(autoScalingGroupName)
                    .build());
            assertEquals(1, r.autoScalingGroups().size());
            assertEquals(Integer.valueOf(0), r.autoScalingGroups().get(0).desiredCapacity());
        }, TimeUnit.MINUTES.toMillis(3));
    }

    @Disabled("for manual run as you need to provide real AWS credentials")
    @Test
    void givenEc2SpotFleet_shouldScaleUpExecuteTaskAndScaleDown() throws Exception {
        final String ec2SpotFleetRoleArn = getOrCreateEc2SpotFleetIamRoleArn(awsCredentialsProvider);

        final Ec2Client amazonEC2 = Ec2Client.builder().credentialsProvider(awsCredentialsProvider).build();

        final RequestSpotFleetResponse requestSpotFleetResult = amazonEC2.requestSpotFleet(RequestSpotFleetRequest.builder()
                .spotFleetRequestConfig(SpotFleetRequestConfigData.builder()
                        .onDemandTargetCapacity(0)
                        .launchSpecifications(SpotFleetLaunchSpecification.builder()
                                .imageId("ami-5e8c9625")
                                .keyName(privateKeyName)
                                .userData(USER_DATA_INSTALL_JAVA8)
                                .instanceType(InstanceType.T2_SMALL)
                                .build())
                        .iamFleetRole(ec2SpotFleetRoleArn)
                        .terminateInstancesWithExpiration(true)
                        .validUntil(new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)).toInstant())
                        .type(FleetType.MAINTAIN)
                        .build())
                .build());

        final String credentialId = setupJenkinsAwsCredentials(awsCredentialsProvider);

        final String sshCredentialId = setupJenkinsSshCredentials(credentialLines);
        final SSHConnector computerConnector = new SSHConnector(
                22, sshCredentialId, null, null, null,
                null, null, null, null, new NonVerifyingKeyVerificationStrategy());
        final EC2FleetCloud cloud = new EC2FleetCloud(
                "TestCloud", credentialId, null, null, null,
                requestSpotFleetResult.spotFleetRequestId(),
                "momo", null, computerConnector, false, false,
                1, 0, 5, 0, 1, true, false,
                "-1", false, 180, 15,
                10, true, false, noScaling, false, false);
        j.jenkins.clouds.add(cloud);

        final List<QueueTaskFuture> tasks = enqueTask(2);

        waitJobSuccessfulExecution(tasks);
        waitZeroNodes();

        System.out.println("wait until EC2 spot fleet will be zero size");
        tryUntil(() -> {
            final List<SpotFleetRequestConfig> r = amazonEC2.describeSpotFleetRequests(DescribeSpotFleetRequestsRequest.builder()
                            .spotFleetRequestIds(requestSpotFleetResult.spotFleetRequestId())
                            .build())
                    .spotFleetRequestConfigs();
            assertEquals(1, r.size());
            assertEquals(Integer.valueOf(0), r.get(0).spotFleetRequestConfig().targetCapacity());
        }, TimeUnit.MINUTES.toMillis(3));
    }

    private String getPrivateKeyName(List<String> credentialLines) {
        final int privateKeyNameIndex = credentialLines.indexOf("privateKeyName") + 1;
        return credentialLines.get(privateKeyNameIndex);
    }

    private String setupJenkinsAwsCredentials(final AwsCredentialsProvider awsCredentialsProvider) {
        final String credentialId = "credId";
        SystemCredentialsProvider.getInstance().getCredentials().add(
                new AWSCredentialsImpl(CredentialsScope.GLOBAL, credentialId,
                        awsCredentialsProvider.resolveCredentials().accessKeyId(),
                        awsCredentialsProvider.resolveCredentials().secretAccessKey(), "d"));
        return credentialId;
    }

    private String setupJenkinsSshCredentials(final List<String> credentialLines) {
        final String credentialId = "sshCredentialId";

        final int privateSshKeyStart = credentialLines.indexOf("-----BEGIN RSA PRIVATE KEY-----");
        final String privateKey = StringUtils.join(credentialLines.subList(privateSshKeyStart, credentialLines.size()), "\n");

        SystemCredentialsProvider.getInstance().getCredentials().add(
                new BasicSSHUserPrivateKey(
                        CredentialsScope.GLOBAL, credentialId,
                        "ec2-user",
                        new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(privateKey),
                        "",
                        "my private key to ssh ec2 for jenkins"));
        return credentialId;
    }

    private String getOrCreateEc2SpotFleetIamRoleArn(AwsCredentialsProvider awsCredentialsProvider) {
        final IamClient amazonIdentityManagement = IamClient.builder()
                .credentialsProvider(awsCredentialsProvider).build();
        final String EC2_SPOT_FLEET_IAM_ROLE_NAME = "AmazonEC2SpotFleetRole";
        String ec2SpotFleetRoleArn;
        try {
            ec2SpotFleetRoleArn = amazonIdentityManagement.createRole(CreateRoleRequest.builder()
                            .assumeRolePolicyDocument("{\"Version\":\"2012-10-17\",\"Statement\":[{\"Sid\":\"\",\"Effect\":\"Allow\",\"Principal\":{\"Service\":\"spotfleet.amazonaws.com\"},\"Action\":\"sts:AssumeRole\"}]}")
                            .roleName(EC2_SPOT_FLEET_IAM_ROLE_NAME)
                            .build())
                    .role().arn();
        } catch (EntityAlreadyExistsException e) {
            // already exist
            ec2SpotFleetRoleArn = amazonIdentityManagement.getRole(GetRoleRequest.builder()
                            .roleName(EC2_SPOT_FLEET_IAM_ROLE_NAME)
                            .build())
                    .role().arn();
        }

        amazonIdentityManagement.attachRolePolicy(AttachRolePolicyRequest.builder()
                .policyArn("arn:aws:iam::aws:policy/service-role/AmazonEC2SpotFleetTaggingRole")
                .roleName(EC2_SPOT_FLEET_IAM_ROLE_NAME)
                .build());

        try {
            amazonIdentityManagement.createServiceLinkedRole(CreateServiceLinkedRoleRequest.builder()
                    .awsServiceName("spotfleet.amazonaws.com")
                    .build());
        } catch (InvalidInputException e) {
            if (e.getMessage().contains("Service role name AWSServiceRoleForEC2SpotFleet has been taken in this account")) {
                // all good
            } else {
                throw e;
            }
        }

        System.out.println("EC2 Spot Fleet IAM Role ARN " + ec2SpotFleetRoleArn);
        return ec2SpotFleetRoleArn;
    }

    private String getOrCreateLaunchTemplate(Ec2Client amazonEC2, final String keyName) {
        final String LT_NAME = "ec2-fleet-plugin-real-test";
        try {
            amazonEC2.createLaunchTemplate(CreateLaunchTemplateRequest.builder()
                    .launchTemplateData(RequestLaunchTemplateData.builder()
                            .keyName(keyName)
                            .userData(USER_DATA_INSTALL_JAVA8)
                            .imageId("ami-5e8c9625")
                            .instanceType(InstanceType.T2_SMALL)
                            .build())
                    .launchTemplateName(LT_NAME)
                    .build()).launchTemplate();
        } catch (Ec2Exception e) {
            if (e.getMessage().contains("Launch template name already in use")) {
                // all good
            } else {
                throw e;
            }
        }
        return LT_NAME;
    }

    private AwsCredentialsProvider getAwsCredentialsProvider(final List<String> lines) {
        final String accessKey = lines.get(1);
        final String secretKey = lines.get(4);

        if (StringUtils.isBlank(accessKey) || StringUtils.isBlank(secretKey)) {
            throw new IllegalArgumentException("AWS_ACCESS_KEY or AWS_SECRET_KEY is not specified in system properties, -D");
        }

        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

}
