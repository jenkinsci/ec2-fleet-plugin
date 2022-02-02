package com.amazon.jenkins.ec2fleet;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DeleteAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.LaunchTemplateSpecification;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.CreateLaunchTemplateRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsRequest;
import com.amazonaws.services.ec2.model.FleetType;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.RequestLaunchTemplateData;
import com.amazonaws.services.ec2.model.RequestSpotFleetRequest;
import com.amazonaws.services.ec2.model.RequestSpotFleetResult;
import com.amazonaws.services.ec2.model.SpotFleetLaunchSpecification;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfig;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfigData;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.AttachRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.CreateRoleRequest;
import com.amazonaws.services.identitymanagement.model.CreateServiceLinkedRoleRequest;
import com.amazonaws.services.identitymanagement.model.EntityAlreadyExistsException;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.InvalidInputException;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsImpl;
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.sshslaves.SSHConnector;
import hudson.plugins.sshslaves.verifiers.NonVerifyingKeyVerificationStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RealTest extends IntegrationTest {

    private static final String USER_DATA_INSTALL_JAVA8 = Base64.getEncoder().encodeToString(
            "#!/bin/bash\nyum install java-1.8.0 -y && yum remove java-1.7.0-openjdk -y && java -version"
                    .getBytes(StandardCharsets.UTF_8));

    @BeforeClass
    public static void beforeClass() {
        turnOffJenkinsTestTimout();
    }

    private List<String> credentialLines;
    private String privateKeyName;
    private AWSCredentialsProvider awsCredentialsProvider;

    @Before
    public void before() throws IOException {
        credentialLines = FileUtils.readLines(new File("credentials.txt"));
        privateKeyName = getPrivateKeyName(credentialLines);
        awsCredentialsProvider = getAwsCredentialsProvider(credentialLines);
    }

    @Ignore("for manual run as you need to provide real AWS credentials")
    @Test
    public void givenAutoScalingGroup_shouldScaleUpExecuteTaskAndScaleDown() throws IOException {
        final AmazonEC2 amazonEC2 = AmazonEC2Client.builder().withCredentials(awsCredentialsProvider).build();

        final AmazonAutoScaling autoScalingClient = AmazonAutoScalingClient.builder().withCredentials(awsCredentialsProvider).build();

        final String ltName = getOrCreateLaunchTemplate(amazonEC2, privateKeyName);

        final List<String> azs = new ArrayList<>();
        final DescribeAvailabilityZonesResult describeAvailabilityZonesResult = amazonEC2.describeAvailabilityZones();
        for (AvailabilityZone az : describeAvailabilityZonesResult.getAvailabilityZones()) {
            azs.add(az.getZoneName());
        }

        final String autoScalingGroupName = "ec2-fleet-plugin-real-test";
        try {
            autoScalingClient.deleteAutoScalingGroup(new DeleteAutoScalingGroupRequest()
                    .withAutoScalingGroupName(autoScalingGroupName)
                    .withForceDelete(true));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        System.out.println("waiting until group will be deleted");
        tryUntil(new Runnable() {
            @Override
            public void run() {
                Assert.assertEquals(0, autoScalingClient.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest()
                        .withAutoScalingGroupNames(autoScalingGroupName)).getAutoScalingGroups().size());
            }
        }, TimeUnit.MINUTES.toMillis(3));

        autoScalingClient.createAutoScalingGroup(
                new CreateAutoScalingGroupRequest()
                        .withAutoScalingGroupName(autoScalingGroupName)
                        .withDesiredCapacity(0)
                        .withMinSize(0)
                        .withMaxSize(2)
                        .withAvailabilityZones(azs)
                        .withLaunchTemplate(new LaunchTemplateSpecification()
                                .withLaunchTemplateName(ltName)
                                .withVersion("$Default")));

        final String credentialId = setupJenkinsAwsCredentials(awsCredentialsProvider);
        final String sshCredentialId = setupJenkinsSshCredentials(credentialLines);
        final SSHConnector computerConnector = new SSHConnector(
                22, sshCredentialId, null, null, null,
                null, null, null, null, new NonVerifyingKeyVerificationStrategy());
        final EC2FleetCloud cloud = new EC2FleetCloud(
                "", null, credentialId, null, null, null,
                autoScalingGroupName,
                "momo", null, computerConnector, false, false,
                1, 0, 5, 0, 1, true, false,
                "-1", false, 180, 15, false,
                10, true);
        j.jenkins.clouds.add(cloud);

        final List<QueueTaskFuture> tasks = enqueTask(2);

        waitJobSuccessfulExecution(tasks);
        waitZeroNodes();

        System.out.println("wait until EC2 spot fleet will be zero size");
        tryUntil(new Runnable() {
            @Override
            public void run() {
                final DescribeAutoScalingGroupsResult r = autoScalingClient.describeAutoScalingGroups(
                        new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(autoScalingGroupName));
                Assert.assertEquals(1, r.getAutoScalingGroups().size());
                Assert.assertEquals(new Integer(0), r.getAutoScalingGroups().get(0).getDesiredCapacity());
            }
        }, TimeUnit.MINUTES.toMillis(3));
    }

    @Ignore("for manual run as you need to provide real AWS credentials")
    @Test
    public void givenEc2SpotFleet_shouldScaleUpExecuteTaskAndScaleDown() throws Exception {
        final String ec2SpotFleetRoleArn = getOrCreateEc2SpotFleetIamRoleArn(awsCredentialsProvider);

        final AmazonEC2 amazonEC2 = AmazonEC2Client.builder().withCredentials(awsCredentialsProvider).build();

        final RequestSpotFleetResult requestSpotFleetResult = amazonEC2.requestSpotFleet(new RequestSpotFleetRequest()
                .withSpotFleetRequestConfig(new SpotFleetRequestConfigData()
                        .withOnDemandTargetCapacity(0)
                        .withLaunchSpecifications(new SpotFleetLaunchSpecification()
                                .withImageId("ami-5e8c9625")
                                .withKeyName(privateKeyName)
                                .withUserData(USER_DATA_INSTALL_JAVA8)
                                .withInstanceType(InstanceType.T2Small))
                        .withIamFleetRole(ec2SpotFleetRoleArn)
                        .withTerminateInstancesWithExpiration(true)
                        .withValidUntil(new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)))
                        .withType(FleetType.Maintain)));

        final String credentialId = setupJenkinsAwsCredentials(awsCredentialsProvider);

        final String sshCredentialId = setupJenkinsSshCredentials(credentialLines);
        final SSHConnector computerConnector = new SSHConnector(
                22, sshCredentialId, null, null, null,
                null, null, null, null, new NonVerifyingKeyVerificationStrategy());
        final EC2FleetCloud cloud = new EC2FleetCloud(
                "", null, credentialId, null, null, null,
                requestSpotFleetResult.getSpotFleetRequestId(),
                "momo", null, computerConnector, false, false,
                1, 0, 5, 0, 1, true, false,
                "-1", false, 180, 15, false,
                10, true);
        j.jenkins.clouds.add(cloud);

        final List<QueueTaskFuture> tasks = enqueTask(2);

        waitJobSuccessfulExecution(tasks);
        waitZeroNodes();

        System.out.println("wait until EC2 spot fleet will be zero size");
        tryUntil(new Runnable() {
            @Override
            public void run() {
                final List<SpotFleetRequestConfig> r = amazonEC2.describeSpotFleetRequests(new DescribeSpotFleetRequestsRequest()
                        .withSpotFleetRequestIds(requestSpotFleetResult.getSpotFleetRequestId()))
                        .getSpotFleetRequestConfigs();
                Assert.assertEquals(1, r.size());
                Assert.assertEquals(new Integer(0), r.get(0).getSpotFleetRequestConfig().getTargetCapacity());
            }
        }, TimeUnit.MINUTES.toMillis(3));
    }

    private String getPrivateKeyName(List<String> credentialLines) {
        final int privateKeyNameIndex = credentialLines.indexOf("privateKeyName") + 1;
        return credentialLines.get(privateKeyNameIndex);
    }

    private String setupJenkinsAwsCredentials(final AWSCredentialsProvider awsCredentialsProvider) {
        final String credentialId = "credId";
        SystemCredentialsProvider.getInstance().getCredentials().add(
                new AWSCredentialsImpl(CredentialsScope.GLOBAL, credentialId,
                        awsCredentialsProvider.getCredentials().getAWSAccessKeyId(),
                        awsCredentialsProvider.getCredentials().getAWSSecretKey(), "d"));
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

    private String getOrCreateEc2SpotFleetIamRoleArn(AWSCredentialsProvider awsCredentialsProvider) {
        final AmazonIdentityManagement amazonIdentityManagement = AmazonIdentityManagementClient.builder()
                .withCredentials(awsCredentialsProvider).build();
        final String EC2_SPOT_FLEET_IAM_ROLE_NAME = "AmazonEC2SpotFleetRole";
        String ec2SpotFleetRoleArn;
        try {
            ec2SpotFleetRoleArn = amazonIdentityManagement.createRole(new CreateRoleRequest()
                    .withAssumeRolePolicyDocument("{\"Version\":\"2012-10-17\",\"Statement\":[{\"Sid\":\"\",\"Effect\":\"Allow\",\"Principal\":{\"Service\":\"spotfleet.amazonaws.com\"},\"Action\":\"sts:AssumeRole\"}]}")
                    .withRoleName(EC2_SPOT_FLEET_IAM_ROLE_NAME))
                    .getRole().getArn();
        } catch (EntityAlreadyExistsException e) {
            // already exist
            ec2SpotFleetRoleArn = amazonIdentityManagement.getRole(new GetRoleRequest()
                    .withRoleName(EC2_SPOT_FLEET_IAM_ROLE_NAME))
                    .getRole().getArn();
        }

        amazonIdentityManagement.attachRolePolicy(new AttachRolePolicyRequest()
                .withPolicyArn("arn:aws:iam::aws:policy/service-role/AmazonEC2SpotFleetTaggingRole")
                .withRoleName(EC2_SPOT_FLEET_IAM_ROLE_NAME));

        try {
            amazonIdentityManagement.createServiceLinkedRole(new CreateServiceLinkedRoleRequest()
                    .withAWSServiceName("spotfleet.amazonaws.com"));
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

    private String getOrCreateLaunchTemplate(AmazonEC2 amazonEC2, final String keyName) {
        final String LT_NAME = "ec2-fleet-plugin-real-test";
        try {
            amazonEC2.createLaunchTemplate(new CreateLaunchTemplateRequest()
                    .withLaunchTemplateData(new RequestLaunchTemplateData()
                            .withKeyName(keyName)
                            .withUserData(USER_DATA_INSTALL_JAVA8)
                            .withImageId("ami-5e8c9625")
                            .withInstanceType(InstanceType.T2Small))
                    .withLaunchTemplateName(LT_NAME)).getLaunchTemplate();
        } catch (AmazonEC2Exception e) {
            if (e.getMessage().contains("Launch template name already in use")) {
                // all good
            } else {
                throw e;
            }
        }
        return LT_NAME;
    }

    private AWSCredentialsProvider getAwsCredentialsProvider(final List<String> lines) {
        final String accessKey = lines.get(1);
        final String secretKey = lines.get(4);

        if (StringUtils.isBlank(accessKey) || StringUtils.isBlank(secretKey)) {
            throw new IllegalArgumentException("AWS_ACCESS_KEY or AWS_SECRET_KEY is not specified in system properties, -D");
        }

        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
    }

}
