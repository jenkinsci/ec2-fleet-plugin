package com.amazon.jenkins.ec2fleet.aws;

import com.amazon.jenkins.ec2fleet.EC2FleetLabelParameters;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsHelper;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.CloudFormationClientBuilder;
import software.amazon.awssdk.services.cloudformation.model.Capability;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.awssdk.services.cloudformation.model.Tag;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class CloudFormationApi {

    public CloudFormationClient connect(final String awsCredentialsId, final String regionName, final String endpoint) {
        final ClientOverrideConfiguration clientConfiguration = AWSUtils.getClientConfiguration();
        final AmazonWebServicesCredentials credentials = AWSCredentialsHelper.getCredentials(awsCredentialsId, Jenkins.get());
        CloudFormationClientBuilder clientBuilder =
                credentials != null ?
                        CloudFormationClient.builder()
                                        .credentialsProvider(AWSUtils.toSdkV2CredentialsProvider(credentials))
                                        .overrideConfiguration(clientConfiguration) :
                        CloudFormationClient.builder()
                                        .overrideConfiguration(clientConfiguration);

        if (StringUtils.isNotBlank(regionName)) clientBuilder.region(Region.of(regionName));
        final String effectiveEndpoint = getEndpoint(regionName, endpoint);
        if (effectiveEndpoint != null) clientBuilder.endpointOverride(URI.create(effectiveEndpoint));
        clientBuilder.httpClient(AWSUtils.getApacheHttpClient(endpoint));
        return clientBuilder.build();
    }

    // todo do we want to merge with EC2Api#getEndpoint
    @Nullable
    private String getEndpoint(@Nullable final String regionName, @Nullable final String endpoint) {
        if (StringUtils.isNotEmpty(endpoint)) {
            return endpoint;
        } else if (StringUtils.isNotEmpty(regionName)) {
            final String domain = regionName.startsWith("cn-") ? "amazonaws.com.cn" : "amazonaws.com";
            return "https://cloudformation." + regionName + "." + domain;
        } else {
            return null;
        }
    }

    public void delete(final CloudFormationClient client, final String stackId) {
        client.deleteStack(DeleteStackRequest.builder().stackName(stackId)
                .build());
    }

    public void create(
            final CloudFormationClient client, final String fleetName, final String keyName, final String parametersString) {
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
                    CreateStackRequest.builder()
                            .stackName(fleetName + "-" + System.currentTimeMillis())
                            .tags(
                                    Tag.builder().key("ec2-fleet-plugin")
                                            .value(parametersString)
                                    .build()
                            )
                            .templateBody(IOUtils.toString(CloudFormationApi.class.getResourceAsStream(template)))
                            // to allow some of templates create iam
                            .capabilities(Capability.CAPABILITY_IAM)
                            .parameters(
                                    Parameter.builder().parameterKey("ImageId").parameterValue(imageId)
                                    .build(), 
                                    Parameter.builder().parameterKey("InstanceType").parameterValue(instanceType)
                                    .build(), 
                                    Parameter.builder().parameterKey("MaxSize").parameterValue(Integer.toString(maxSize))
                                    .build(), 
                                    Parameter.builder().parameterKey("MinSize").parameterValue(Integer.toString(minSize))
                                    .build(), 
                                    Parameter.builder().parameterKey("SpotPrice").parameterValue(spotPrice)
                                    .build(), 
                                    Parameter.builder().parameterKey("KeyName").parameterValue(keyName)
                                    .build()
                            )
                            .build());
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
            final CloudFormationClient client, final String fleetName) {
        Map<String, StackInfo> r = new HashMap<>();

        String nextToken = null;
        do {
            DescribeStacksResponse describeStacksResult = client.describeStacks(
                    DescribeStacksRequest.builder().nextToken(nextToken)
                    .build());
            for (Stack stack : describeStacksResult.stacks()) {
                if (stack.stackName().startsWith(fleetName)) {
                    final String fleetId = stack.outputs().isEmpty() ? null : stack.outputs().get(0).outputValue();
                    r.put(stack.tags().get(0).value(), new StackInfo(
                            stack.stackId(), fleetId, StackStatus.valueOf(String.valueOf(stack.stackStatus()))));
                }
            }
            nextToken = describeStacksResult.nextToken();
        } while (nextToken != null);

        return r;
    }

}
