package com.amazon.jenkins.ec2fleet.aws;

import com.amazon.jenkins.ec2fleet.EC2FleetLabelParameters;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.Capability;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.cloudformation.model.Tag;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsHelper;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class CloudFormationApi {

    public AmazonCloudFormation connect(final String awsCredentialsId, final String regionName, final String endpoint) {
        final ClientConfiguration clientConfiguration = AWSUtils.getClientConfiguration(endpoint);
        final AmazonWebServicesCredentials credentials = AWSCredentialsHelper.getCredentials(awsCredentialsId, Jenkins.get());
        final AmazonCloudFormation client =
                credentials != null ?
                        new AmazonCloudFormationClient(credentials, clientConfiguration) :
                        new AmazonCloudFormationClient(clientConfiguration);

        final String effectiveEndpoint = getEndpoint(regionName, endpoint);
        if (effectiveEndpoint != null) client.setEndpoint(effectiveEndpoint);
        return client;
    }

    // todo do we want to merge with EC2Api#getEndpoint
    @Nullable
    private String getEndpoint(@Nullable final String regionName, @Nullable final String endpoint) {
        if (StringUtils.isNotEmpty(endpoint)) {
            return endpoint;
        } else if (StringUtils.isNotEmpty(regionName)) {
            final Region region = RegionUtils.getRegion(regionName);
            if (region != null && region.isServiceSupported(endpoint)) {
                return region.getServiceEndpoint(endpoint);
            } else {
                final String domain = regionName.startsWith("cn-") ? "amazonaws.com.cn" : "amazonaws.com";
                return "https://cloudformation." + regionName + "." + domain;
            }
        } else {
            return null;
        }
    }

    public void delete(final AmazonCloudFormation client, final String stackId) {
        client.deleteStack(new DeleteStackRequest().withStackName(stackId));
    }

    public void create(
            final AmazonCloudFormation client, final String fleetName, final String keyName, final String parametersString) {
        final EC2FleetLabelParameters parameters = new EC2FleetLabelParameters(parametersString);

        try {
            final String type = parameters.getOrDefault("type", "ec2-spot-fleet");
            final String imageId = parameters.get("imageId"); //"ami-0080e4c5bc078760e";
            final int maxSize = parameters.getIntOrDefault("maxSize", 10);
            final int minSize = parameters.getIntOrDefault("minSize", 0);
            final String instanceType = parameters.getOrDefault("instanceType", "m4.large");
            final String spotPrice = parameters.getOrDefault("spotPrice", ""); // "0.04"

            final String template = "/com/amazon/jenkins/ec2fleet/" + (type.equals("asg") ? "auto-scaling-group.yml" : "ec2-spot-fleet.yml");
            client.createStack(
                    new CreateStackRequest()
                            .withStackName(fleetName + "-" + System.currentTimeMillis())
                            .withTags(
                                    new Tag().withKey("ec2-fleet-plugin")
                                            .withValue(parametersString)
                            )
                            .withTemplateBody(IOUtils.toString(CloudFormationApi.class.getResourceAsStream(template)))
                            // to allow some of templates create iam
                            .withCapabilities(Capability.CAPABILITY_IAM)
                            .withParameters(
                                    new Parameter().withParameterKey("ImageId").withParameterValue(imageId),
                                    new Parameter().withParameterKey("InstanceType").withParameterValue(instanceType),
                                    new Parameter().withParameterKey("MaxSize").withParameterValue(Integer.toString(maxSize)),
                                    new Parameter().withParameterKey("MinSize").withParameterValue(Integer.toString(minSize)),
                                    new Parameter().withParameterKey("SpotPrice").withParameterValue(spotPrice),
                                    new Parameter().withParameterKey("KeyName").withParameterValue(keyName)
                            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class StackInfo {
        public final String stackId;
        public final String fleetId;
        public final StackStatus stackStatus;

        public StackInfo(String stackId, String fleetId, StackStatus stackStatus) {
            this.stackId = stackId;
            this.fleetId = fleetId;
            this.stackStatus = stackStatus;
        }
    }

    public Map<String, StackInfo> describe(
            final AmazonCloudFormation client, final String fleetName) {
        Map<String, StackInfo> r = new HashMap<>();

        String nextToken = null;
        do {
            DescribeStacksResult describeStacksResult = client.describeStacks(
                    new DescribeStacksRequest().withNextToken(nextToken));
            for (Stack stack : describeStacksResult.getStacks()) {
                if (stack.getStackName().startsWith(fleetName)) {
                    final String fleetId = stack.getOutputs().isEmpty() ? null : stack.getOutputs().get(0).getOutputValue();
                    r.put(stack.getTags().get(0).getValue(), new StackInfo(
                            stack.getStackId(), fleetId, StackStatus.valueOf(stack.getStackStatus())));
                }
            }
            nextToken = describeStacksResult.getNextToken();
        } while (nextToken != null);

        return r;
    }

}
