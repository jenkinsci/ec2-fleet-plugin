package com.amazon.jenkins.ec2fleet.fleet;

import com.amazon.jenkins.ec2fleet.FleetStateStats;
import com.amazon.jenkins.ec2fleet.aws.AWSUtils;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsHelper;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.ObjectUtils;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.*;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
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
        String token = null;
        do {
            final DescribeAutoScalingGroupsRequest request = DescribeAutoScalingGroupsRequest.builder()
                    .build();
            request.nextToken(token);
            final DescribeAutoScalingGroupsResponse result = client.describeAutoScalingGroups(request);
            for (final AutoScalingGroup group : result.autoScalingGroups()) {
                final String curName = group.autoScalingGroupName();
                final boolean selected = ObjectUtils.nullSafeEquals(selectedId, curName);
                final String displayStr = "Auto Scaling Group - " + curName;
                model.add(new ListBoxModel.Option(displayStr, curName, selected));
            }
            token = result.nextToken();
        } while (token != null);
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
                .map(MixedInstancesPolicy::getLaunchTemplate)
                .map(LaunchTemplate::getOverrides)
                .map(overrides -> overrides.stream()
                        .filter(o -> o.weightedCapacity() != null)
                        .collect(Collectors.toMap(LaunchTemplateOverrides::getInstanceType,
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
        final ClientOverrideConfiguration clientConfiguration = AWSUtils.getClientConfiguration(endpoint);
        final AutoScalingClient client =
                credentials != null ?
                        new AutoScalingClient(credentials, clientConfiguration) :
                        new AutoScalingClient(clientConfiguration);
        final String effectiveEndpoint = getEndpoint(regionName, endpoint);
        if (effectiveEndpoint != null) client.setEndpoint(effectiveEndpoint);
        return client;
    }

    public void terminateInstances(final String awsCredentialsId, final String regionName, final String endpoint, final Collection<String> instanceIds) {
        final AutoScalingClient client = createClient(awsCredentialsId, regionName, endpoint);

        for(String instanceId : instanceIds) {
            if (StringUtils.isBlank(instanceId)) {
                throw new IllegalArgumentException("Instance ID cannot be null or empty");
            }
            try{
                // Attempt to terminate the instance in the Auto Scaling group first
                client.terminateInstanceInAutoScalingGroup(TerminateInstanceInAutoScalingGroupRequest.builder()
                        .instanceId(instanceId)
                        .shouldDecrementDesiredCapacity(false)
                        .build());
            } catch (Exception e) {
                LOGGER.warning(String.format("Failed to terminate instance %s in Auto Scaling group: %s", instanceId, e.getMessage()));
            }
        }
    }

    // TODO: merge with EC2Api#getEndpoint
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
                return "https://autoscaling." + regionName + "." + domain;
            }
        } else {
            return null;
        }
    }

}
