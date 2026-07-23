package com.amazon.jenkins.ec2fleet.fleet;

import com.amazon.jenkins.ec2fleet.EC2AgentTerminationReason;
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
     * Terminates the given instances of this Auto Scaling Group.
     *
     * <p>When the ASG has a warm pool with instance reuse ({@code ReuseOnScaleIn=true}), instances
     * being scaled down are simply handed back to the ASG by removing their scale-in protection, so
     * the ASG moves them to the warm pool for reuse (faster, cache-warm builds) instead of
     * terminating them. Instances that must not be reused (e.g. {@code MAX_TOTAL_USES_EXHAUSTED}), or
     * any instance when no such warm pool is configured, are terminated directly so the ASG replaces
     * them with a fresh instance.
     */
    public void terminateInstances(final String awsCredentialsId, final String regionName, final String endpoint,
                                   final String autoScalingGroupName,
                                   final Map<String, EC2AgentTerminationReason> instances) {
        if (instances == null || instances.isEmpty()) {
            return;
        }

        final AutoScalingClient client = createClient(awsCredentialsId, regionName, endpoint);

        // The warm pool is only queried when there is actually something to terminate.
        final boolean reuseFromWarmPool = hasWarmPoolWithInstanceReuse(client, autoScalingGroupName);

        final List<String> instancesForWarmPool = new ArrayList<>();
        final List<String> instancesToTerminate = new ArrayList<>();
        for (final Map.Entry<String, EC2AgentTerminationReason> entry : instances.entrySet()) {
            if (StringUtils.isBlank(entry.getKey())) {
                continue;
            }
            if (reuseFromWarmPool && entry.getValue() != EC2AgentTerminationReason.MAX_TOTAL_USES_EXHAUSTED) {
                instancesForWarmPool.add(entry.getKey());
            } else {
                instancesToTerminate.add(entry.getKey());
            }
        }

        // Scale-down: drop protection in one call and let the ASG move the instances to the warm pool.
        if (!instancesForWarmPool.isEmpty()) {
            try {
                setScaleInProtection(client, autoScalingGroupName, instancesForWarmPool);
                LOGGER.info(String.format("Removed scale-in protection so ASG %s can move instances to warm pool: %s",
                        autoScalingGroupName, instancesForWarmPool));
            } catch (Exception e) {
                LOGGER.warning(String.format("Failed to remove scale-in protection from instances %s: %s",
                        instancesForWarmPool, e.getMessage()));
            }
        }

        // Terminate directly so the ASG launches a fresh replacement.
        for (final String instanceId : instancesToTerminate) {
            try {
                setScaleInProtection(client, autoScalingGroupName, Collections.singletonList(instanceId));
                client.terminateInstanceInAutoScalingGroup(TerminateInstanceInAutoScalingGroupRequest.builder()
                        .instanceId(instanceId)
                        .shouldDecrementDesiredCapacity(false)
                        .build());
                LOGGER.info(String.format("Terminated instance %s in Auto Scaling group %s", instanceId, autoScalingGroupName));
            } catch (Exception e) {
                LOGGER.warning(String.format("Failed to terminate instance %s in Auto Scaling group %s: %s",
                        instanceId, autoScalingGroupName, e.getMessage()));
            }
        }
    }

    private boolean hasWarmPoolWithInstanceReuse(final AutoScalingClient client, final String autoScalingGroupName) {
        try {
            final WarmPoolConfiguration warmPool = client.describeWarmPool(DescribeWarmPoolRequest.builder()
                    .autoScalingGroupName(autoScalingGroupName)
                    .build()).warmPoolConfiguration();
            return warmPool != null
                    && warmPool.instanceReusePolicy() != null
                    && Boolean.TRUE.equals(warmPool.instanceReusePolicy().reuseOnScaleIn());
        } catch (Exception e) {
            LOGGER.warning(String.format(
                    "Failed to describe warm pool for Auto Scaling group %s, assuming no warm pool: %s",
                    autoScalingGroupName, e.getMessage()));
            return false;
        }
    }

    private void setScaleInProtection(final AutoScalingClient client, final String autoScalingGroupName,
                                      final Collection<String> instanceIds) {
        client.setInstanceProtection(SetInstanceProtectionRequest.builder()
                .autoScalingGroupName(autoScalingGroupName)
                .instanceIds(instanceIds)
                .protectedFromScaleIn(false)
                .build());
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
