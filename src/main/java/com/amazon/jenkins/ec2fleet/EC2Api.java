package com.amazon.jenkins.ec2fleet;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.Reservation;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsHelper;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public class EC2Api {

    private static final ImmutableSet<String> TERMINATED_STATES = ImmutableSet.of(
            InstanceStateName.Terminated.toString(),
            InstanceStateName.Stopped.toString(),
            InstanceStateName.Stopping.toString(),
            InstanceStateName.ShuttingDown.toString()
    );

    private static final int BATCH_SIZE = 900;

    private static final String NOT_FOUND_ERROR_CODE = "InvalidInstanceID.NotFound";
    private static final Pattern INSTANCE_ID_PATTERN = Pattern.compile("(i-[0-9a-zA-Z]+)");

    private static List<String> parseInstanceIdsFromNotFoundException(final String errorMessage) {
        final Matcher fullMessageMatcher = INSTANCE_ID_PATTERN.matcher(errorMessage);

        final List<String> instanceIds = new ArrayList<>();
        while (fullMessageMatcher.find()) {
            instanceIds.add(fullMessageMatcher.group(1));
        }

        return instanceIds;
    }

    public Set<String> describeTerminated(final AmazonEC2 ec2, final Set<String> instanceIds) {
        return describeTerminated(ec2, instanceIds, BATCH_SIZE);
    }

    public Set<String> describeTerminated(final AmazonEC2 ec2, final Set<String> instanceIds, final int batchSize) {
        // assume all terminated until we get opposite info
        final Set<String> terminated = new HashSet<>(instanceIds);
        // don't do actual call if no data
        if (instanceIds.isEmpty()) return terminated;

        final List<List<String>> batches = Lists.partition(new ArrayList<>(instanceIds), batchSize);
        for (final List<String> batch : batches) {
            describeTerminatedBatch(ec2, terminated, batch);
        }
        return terminated;
    }

    private static void describeTerminatedBatch(final AmazonEC2 ec2, final Set<String> terminated, final List<String> batch) {
        // we are going to modify list, so copy
        final List<String> copy = new ArrayList<>(batch);

        // just to simplify debug by having consist order
        Collections.sort(copy);

        // because instances could be terminated at any time we do multiple
        // retry to get status and all time remove from request all non found instances if any
        while (copy.size() > 0) {
            try {
                final DescribeInstancesRequest request = new DescribeInstancesRequest().withInstanceIds(copy);

                DescribeInstancesResult result;
                do {
                    result = ec2.describeInstances(request);
                    request.setNextToken(result.getNextToken());

                    for (final Reservation r : result.getReservations()) {
                        for (final Instance instance : r.getInstances()) {
                            // if instance not in terminated state, remove it from terminated
                            if (!TERMINATED_STATES.contains(instance.getState().getName())) {
                                terminated.remove(instance.getInstanceId());
                            }
                        }
                    }
                } while (result.getNextToken() != null);

                // all good, clear request batch to stop
                copy.clear();
            } catch (final AmazonEC2Exception exception) {
                // if we cannot find instance, that's fine assume them as terminated
                // remove from request and try again
                if (exception.getErrorCode().equals(NOT_FOUND_ERROR_CODE)) {
                    final List<String> notFoundInstanceIds = parseInstanceIdsFromNotFoundException(exception.getMessage());
                    if (notFoundInstanceIds.isEmpty()) {
                        // looks like we cannot parse correctly, rethrow
                        throw exception;
                    }
                    copy.removeAll(notFoundInstanceIds);
                }
            }
        }
    }

    public AmazonEC2 connect(final String awsCredentialsId, final String regionName, final String endpoint) {
        final AmazonWebServicesCredentials credentials = AWSCredentialsHelper.getCredentials(awsCredentialsId, Jenkins.getInstance());
        final AmazonEC2Client client =
                credentials != null ?
                        new AmazonEC2Client(credentials) :
                        new AmazonEC2Client();

        final String effectiveEndpoint = getEndpoint(regionName, endpoint);
        if (effectiveEndpoint != null) client.setEndpoint(effectiveEndpoint);
        return client;
    }

    /**
     * Derive EC2 API endpoint. If <code>endpoint</code> parameter not empty will use
     * it as first priority, otherwise will try to find region in {@link RegionUtils} by <code>regionName</code>
     * and use endpoint from it, if not available will generate endpoint as string and check if
     * region name looks like China <code>cn-</code> prefix.
     *
     * Implementation details
     *
     * {@link RegionUtils} is static information, and to get new region required to be updated,
     * as it's not possible too fast as you need to check new version of lib, moreover new version of lib
     * could be pointed to new version of Jenkins which is not a case for our plugin as some of installation
     * still on <code>1.6.x</code>
     *
     * For example latest AWS SDK lib depends on Jackson2 plugin which starting from version <code>2.8.7.0</code>
     * require Jenkins at least <code>2.60</code> https://plugins.jenkins.io/jackson2-api
     *
     * List of all AWS endpoints
     * https://docs.aws.amazon.com/general/latest/gr/rande.html
     *
     * @param regionName like us-east-1 not a airport code, could be <code>null</code>
     * @param endpoint   custom endpoint could be <code>null</code>
     * @return <code>null</code> or actual endpoint
     */
    @Nullable
    public String getEndpoint(@Nullable final String regionName, @Nullable final String endpoint) {
        if (StringUtils.isNotEmpty(endpoint)) {
            return endpoint;
        } else if (StringUtils.isNotEmpty(regionName)) {
            final Region region = RegionUtils.getRegion(regionName);
            if (region != null && region.isServiceSupported(endpoint)) {
                return region.getServiceEndpoint(endpoint);
            } else {
                final String domain = regionName.startsWith("cn-") ? "amazonaws.com.cn" : "amazonaws.com";
                return "https://ec2." + regionName + "." + domain;
            }
        } else {
            return null;
        }
    }

}
