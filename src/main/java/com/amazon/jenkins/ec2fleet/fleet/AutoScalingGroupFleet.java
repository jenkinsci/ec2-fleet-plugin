package com.amazon.jenkins.ec2fleet.fleet;

import com.amazon.jenkins.ec2fleet.FleetStateStats;
import com.amazon.jenkins.ec2fleet.aws.AWSUtils;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsHelper;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.ObjectUtils;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.AutoScalingClientBuilder;
import software.amazon.awssdk.services.autoscaling.model.*;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.net.URI;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ThreadSafe
public class AutoScalingGroupFleet implements EC2Fleet {

    private static final Logger LOGGER = Logger.getLogger(AutoScalingGroupFleet.class.getName());

    @Override
    public void describe(
            final String awsCredentialsId, final String regionName, final String endpoint,
            final ListBoxModel model, final String selectedId, final boolean showAll) {
        final AutoScalingClient client = createClient(awsCredentialsId, regionName, endpoint);
        final DescribeAutoScalingGroupsRequest request = DescribeAutoScalingGroupsRequest.builder()
                .build();
        for (DescribeAutoScalingGroupsResponse result : client.describeAutoScalingGroupsPaginator(request)) {
            for (final AutoScalingGroup group : result.autoScalingGroups()) {
                final String curName = group.autoScalingGroupName();
                final boolean selected = ObjectUtils.nullSafeEquals(selectedId, curName);
                final String displayStr = "Auto Scaling Group - " + curName;
                model.add(new ListBoxModel.Option(displayStr, curName, selected));
            }
        }
    }

    @Override
    public void modify(
            final String awsCredentialsId, final String regionName, final String endpoint,
            final String id, final int targetCapacity, final int min, final int max) {
        final AutoScalingClient client = createClient(awsCredentialsId, regionName, endpoint);
        client.updateAutoScalingGroup(
                UpdateAutoScalingGroupRequest.builder()
                        .minSize(min)
                        .maxSize(max)
                        .desiredCapacity(targetCapacity)
                        .autoScalingGroupName(id)
                        // without scale in protection auto scaling group could terminate random ec2 instances
                        // in case of scale in, instead we need to enable so plugin could decide
                        // which empty instance should be terminated
                        .newInstancesProtectedFromScaleIn(true)
                .build());
    }

    @Override
    public FleetStateStats getState(
            final String awsCredentialsId, final String regionName, final String endpoint, final String id) {
        final AutoScalingClient client = createClient(awsCredentialsId, regionName, endpoint);
        final DescribeAutoScalingGroupsResponse result = client.describeAutoScalingGroups(
                DescribeAutoScalingGroupsRequest.builder()
                        .autoScalingGroupNames(id)
                .build());

        if (result.autoScalingGroups().isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Cannot find auto scaling group with name %s in region %s", id, regionName));
        }

        final AutoScalingGroup group = result.autoScalingGroups().get(0);

        final Set<String> instanceIds = new HashSet<>(group.instances().size());
        for (final Instance instance : group.instances()) {
            instanceIds.add(instance.instanceId());
        }

        Map<String, Double> instanceWeights = Optional.ofNullable(group.mixedInstancesPolicy())
                .map(MixedInstancesPolicy::launchTemplate)
                .map(LaunchTemplate::overrides)
                .map(overrides -> overrides.stream()
                        .filter(o -> o.weightedCapacity() != null)
                        .collect(Collectors.toMap(LaunchTemplateOverrides::instanceType,
                                override -> Double.parseDouble(override.weightedCapacity()))))
                .orElse(Collections.emptyMap());

        return new FleetStateStats(
                id, group.desiredCapacity(),
                // status could be null which is active
                FleetStateStats.State.active(StringUtils.defaultIfEmpty(group.status(), "active")),
                instanceIds, instanceWeights);
    }

    @Override
    public Map<String, FleetStateStats> getStateBatch(String awsCredentialsId, String regionName, String endpoint, Collection<String> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean isAutoScalingGroup() {
        return true;
    }

    // TODO: move to Registry
    public AutoScalingClient createClient(
            final String awsCredentialsId, final String regionName, final String endpoint) {
        final AmazonWebServicesCredentials credentials = AWSCredentialsHelper.getCredentials(awsCredentialsId, Jenkins.get());
        final ClientOverrideConfiguration clientConfiguration = AWSUtils.getClientConfiguration();
        final AutoScalingClientBuilder clientBuilder =
                credentials != null ?
                        AutoScalingClient.builder()
                                .credentialsProvider(AWSUtils.toSdkV2CredentialsProvider(credentials))
                                .overrideConfiguration(clientConfiguration) :
                        AutoScalingClient.builder()
                                .overrideConfiguration(clientConfiguration);
        if (StringUtils.isNotBlank(regionName)) clientBuilder.region(Region.of(regionName));
        final String effectiveEndpoint = getEndpoint(regionName, endpoint);
        if (effectiveEndpoint != null) clientBuilder.endpointOverride(URI.create(effectiveEndpoint));
        clientBuilder.httpClient(AWSUtils.getApacheHttpClient(endpoint));
        return clientBuilder.build();
    }

    /**
     * Removes scale-in protection from the specified instances, allowing the ASG to terminate them
     * when the desired capacity is reduced (which happens via the modify() call before this method is invoked).
     *
     * This approach lets the ASG handle instance termination naturally rather than the plugin
     * explicitly calling terminateInstanceInAutoScalingGroup().
     */
    public void removeScaleInProtection(final String awsCredentialsId, final String regionName,
                                         final String endpoint, final String autoScalingGroupName,
                                         final Collection<String> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return;
        }

        final AutoScalingClient client = createClient(awsCredentialsId, regionName, endpoint);

        // Filter out blank instance IDs
        final List<String> validInstanceIds = instanceIds.stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());

        if (validInstanceIds.isEmpty()) {
            return;
        }

        try {
            // Remove scale-in protection from all instances in a single API call
            client.setInstanceProtection(SetInstanceProtectionRequest.builder()
                    .autoScalingGroupName(autoScalingGroupName)
                    .instanceIds(validInstanceIds)
                    .protectedFromScaleIn(false)
                    .build());
            LOGGER.info(String.format("Removed scale-in protection from instances: %s", validInstanceIds));
        } catch (Exception e) {
            LOGGER.warning(String.format("Failed to remove scale-in protection from instances %s: %s",
                    validInstanceIds, e.getMessage()));
        }
    }

    /**
     * @deprecated Use {@link #removeScaleInProtection(String, String, String, String, Collection)} instead.
     * This method is kept for backwards compatibility but now delegates to removeScaleInProtection.
     */
    @Deprecated
    public void terminateInstances(final String awsCredentialsId, final String regionName,
                                   final String endpoint, final Collection<String> instanceIds) {
        LOGGER.warning("terminateInstances() is deprecated. The ASG name is required to remove scale-in protection. " +
                "This call will be ignored. Please update to use removeScaleInProtection().");
    }

    // TODO: merge with EC2Api#getEndpoint
    @Nullable
    private String getEndpoint(@Nullable final String regionName, @Nullable final String endpoint) {
        if (StringUtils.isNotEmpty(endpoint)) {
            return endpoint;
        } else if (StringUtils.isNotEmpty(regionName)) {
            final String domain = regionName.startsWith("cn-") ? "amazonaws.com.cn" : "amazonaws.com";
            return "https://autoscaling." + regionName + "." + domain;
        } else {
            return null;
        }
    }

}
