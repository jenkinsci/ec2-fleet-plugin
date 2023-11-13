package com.amazon.jenkins.ec2fleet.fleet;

import com.amazon.jenkins.ec2fleet.FleetStateStats;
import com.amazon.jenkins.ec2fleet.aws.AWSUtils;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
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
import org.apache.commons.lang.StringUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ThreadSafe
public class AutoScalingGroupFleet implements EC2Fleet {

    @Override
    public void describe(
            final String awsCredentialsId, final String regionName, final String endpoint,
            final ListBoxModel model, final String selectedId, final boolean showAll) {
        final AmazonAutoScalingClient client = createClient(awsCredentialsId, regionName, endpoint);
        String token = null;
        do {
            final DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest();
            request.withNextToken(token);
            final DescribeAutoScalingGroupsResult result = client.describeAutoScalingGroups(request);
            for (final AutoScalingGroup group : result.getAutoScalingGroups()) {
                final String curName = group.getAutoScalingGroupName();
                final boolean selected = ObjectUtils.nullSafeEquals(selectedId, curName);
                final String displayStr = "Auto Scaling Group - " + curName;
                model.add(new ListBoxModel.Option(displayStr, curName, selected));
            }
            token = result.getNextToken();
        } while (token != null);
    }

    @Override
    public void modify(
            final String awsCredentialsId, final String regionName, final String endpoint,
            final String id, final int targetCapacity, final int min, final int max) {
        final AmazonAutoScalingClient client = createClient(awsCredentialsId, regionName, endpoint);
        client.updateAutoScalingGroup(
                new UpdateAutoScalingGroupRequest()
                        .withMinSize(min)
                        .withMaxSize(max)
                        .withDesiredCapacity(targetCapacity)
                        .withAutoScalingGroupName(id)
                        // without scale in protection auto scaling group could terminate random ec2 instances
                        // in case of scale in, instead we need to enable so plugin could decide
                        // which empty instance should be terminated
                        .withNewInstancesProtectedFromScaleIn(true));
    }

    @Override
    public FleetStateStats getState(
            final String awsCredentialsId, final String regionName, final String endpoint, final String id) {
        final AmazonAutoScalingClient client = createClient(awsCredentialsId, regionName, endpoint);
        final DescribeAutoScalingGroupsResult result = client.describeAutoScalingGroups(
                new DescribeAutoScalingGroupsRequest()
                        .withAutoScalingGroupNames(id));

        if (result.getAutoScalingGroups().isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Cannot find auto scaling group with name %s in region %s", id, regionName));
        }

        final AutoScalingGroup group = result.getAutoScalingGroups().get(0);

        final Set<String> instanceIds = new HashSet<>(group.getInstances().size());
        for (final Instance instance : group.getInstances()) {
            instanceIds.add(instance.getInstanceId());
        }

        Map<String, Double> instanceWeights = Optional.ofNullable(group.getMixedInstancesPolicy())
                .map(MixedInstancesPolicy::getLaunchTemplate)
                .map(LaunchTemplate::getOverrides)
                .map(overrides -> overrides.stream()
                        .filter(o -> o.getWeightedCapacity() != null)
                        .collect(Collectors.toMap(LaunchTemplateOverrides::getInstanceType,
                                override -> Double.parseDouble(override.getWeightedCapacity()))))
                .orElse(Collections.emptyMap());

        return new FleetStateStats(
                id, group.getDesiredCapacity(),
                // status could be null which is active
                FleetStateStats.State.active(StringUtils.defaultIfEmpty(group.getStatus(), "active")),
                instanceIds, instanceWeights);
    }

    @Override
    public Map<String, FleetStateStats> getStateBatch(String awsCredentialsId, String regionName, String endpoint, Collection<String> ids) {
        throw new UnsupportedOperationException();
    }

    // TODO: move to Registry
    public AmazonAutoScalingClient createClient(
            final String awsCredentialsId, final String regionName, final String endpoint) {
        final AmazonWebServicesCredentials credentials = AWSCredentialsHelper.getCredentials(awsCredentialsId, Jenkins.get());
        final ClientConfiguration clientConfiguration = AWSUtils.getClientConfiguration(endpoint);
        final AmazonAutoScalingClient client =
                credentials != null ?
                        new AmazonAutoScalingClient(credentials, clientConfiguration) :
                        new AmazonAutoScalingClient(clientConfiguration);
        final String effectiveEndpoint = getEndpoint(regionName, endpoint);
        if (effectiveEndpoint != null) client.setEndpoint(effectiveEndpoint);
        return client;
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
