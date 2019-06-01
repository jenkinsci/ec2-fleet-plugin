package com.amazon.jenkins.ec2fleet;

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

    public AmazonEC2 connect(final String awsCredentialsId, final String region) {
        final AmazonWebServicesCredentials credentials = AWSCredentialsHelper.getCredentials(awsCredentialsId, Jenkins.getInstance());
        final AmazonEC2Client client =
                credentials != null ?
                        new AmazonEC2Client(credentials) :
                        new AmazonEC2Client();
        if (region != null)
            client.setEndpoint("https://ec2." + region + ".amazonaws.com/");
        return client;
    }

}
