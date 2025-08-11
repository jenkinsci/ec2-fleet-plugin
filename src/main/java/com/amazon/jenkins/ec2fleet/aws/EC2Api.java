package com.amazon.jenkins.ec2fleet.aws;

import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsHelper;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.Ec2ClientBuilder;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public class EC2Api {

    private static final Logger LOGGER = Logger.getLogger(EC2Api.class.getName());

    private static final Set<String> TERMINATED_STATES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            InstanceStateName.TERMINATED.toString(),
            InstanceStateName.STOPPED.toString(),
            InstanceStateName.STOPPING.toString(),
            InstanceStateName.SHUTTING_DOWN.toString()
    )));

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

    public Map<String, Instance> describeInstances(final Ec2Client ec2, final Set<String> instanceIds) {
        return describeInstances(ec2, instanceIds, BATCH_SIZE);
    }

    public Map<String, Instance> describeInstances(final Ec2Client ec2, final Set<String> instanceIds, final int batchSize) {
        final Map<String, Instance> described = new HashMap<>();
        // don't do actual call if no data
        if (instanceIds.isEmpty()) return described;

        final List<String> instanceIdsList = new ArrayList<>(instanceIds);
        final List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < instanceIdsList.size(); i += batchSize) {
            batches.add(instanceIdsList.subList(i, Math.min(i + batchSize, instanceIdsList.size())));
        }
        for (final List<String> batch : batches) {
            describeInstancesBatch(ec2, described, batch);
        }
        return described;
    }

    private static void describeInstancesBatch(
            final Ec2Client ec2, final Map<String, Instance> described, final List<String> batch) {
        // we are going to modify list, so copy
        final List<String> copy = new ArrayList<>(batch);

        // just to simplify debug by having consist order
        Collections.sort(copy);

        // because instances could be terminated at any time we do multiple
        // retry to get status and all time remove from request all non found instances if any
        while (!copy.isEmpty()) {
            try {
                DescribeInstancesRequest request = DescribeInstancesRequest.builder().instanceIds(copy)
                        .build();

                DescribeInstancesResponse result;
                do {
                    result = ec2.describeInstances(request);
                    request = request.toBuilder().nextToken(result.nextToken()).build();

                    for (final Reservation r : result.reservations()) {
                        for (final Instance instance : r.instances()) {
                            // if instance not in terminated state, add it to described
                            if (!TERMINATED_STATES.contains(instance.state().name().toString())) {
                                described.put(instance.instanceId(), instance);
                            }
                        }
                    }
                } while (result.nextToken() != null);

                // all good, clear request batch to stop
                copy.clear();
            } catch (final Ec2Exception exception) {
                // if we cannot find instance, that's fine assume them as terminated
                // remove from request and try again
                if (exception.awsErrorDetails().errorCode().equals(NOT_FOUND_ERROR_CODE)) {
                    final List<String> notFoundInstanceIds = parseInstanceIdsFromNotFoundException(exception.getMessage());
                    if (notFoundInstanceIds.isEmpty()) {
                        // looks like we cannot parse correctly, rethrow
                        throw exception;
                    }
                    copy.removeAll(notFoundInstanceIds);
                } else {
                    throw exception;
                }
            }
        }
    }

    /**
     * Auto handle instance not found exception if any and assume those instances as already terminated
     *
     * @param ec2 ec2 client
     * @param instanceIds set of instance ids
     */
    public void terminateInstances(final Ec2Client ec2, final Collection<String> instanceIds) {
        final List<String> temp = new ArrayList<>(instanceIds);
        // Retry if termination failed due to NOT_FOUND_ERROR_CODE
        while (!temp.isEmpty()) {
            try {
                TerminateInstancesRequest request = TerminateInstancesRequest.builder()
                        .instanceIds(temp)
                        .build();
                ec2.terminateInstances(request);
                // clear after successful termination
                temp.clear();
            } catch (Ec2Exception exception) {
                // if we cannot find instance, that's fine assume them as terminated
                // remove from request and try again
                if (exception.awsErrorDetails().errorCode().equals(NOT_FOUND_ERROR_CODE)) {
                    final List<String> notFoundInstanceIds = parseInstanceIdsFromNotFoundException(exception.getMessage());
                    if (notFoundInstanceIds.isEmpty()) {
                        // looks like we cannot parse correctly, rethrow
                        throw exception;
                    }
                    temp.removeAll(notFoundInstanceIds);
                } else {
                    LOGGER.warning(String.format("Failed terminating EC2 instanceId(s): %s with following exception: %s",
                            StringUtils.join(instanceIds, ","), exception.getMessage()));
                    throw exception;
                }
            }
        }
    }

    public void tagInstances(final Ec2Client ec2, final Set<String> instanceIds, final String key, final String value) {
        if (instanceIds.isEmpty()) return;

        final CreateTagsRequest request = CreateTagsRequest.builder()
                .resources(instanceIds)
                // if you don't need value EC2 API requires empty string
                .tags(Collections.singletonList(Tag.builder().key(key).value(value == null ? "" : value)
                        .build()))
                .build();
        ec2.createTags(request);
    }

    public Ec2Client connect(final String awsCredentialsId, final String regionName, final String endpoint) {
        final ClientOverrideConfiguration clientConfiguration = AWSUtils.getClientConfiguration();
        final AmazonWebServicesCredentials credentials = AWSCredentialsHelper.getCredentials(awsCredentialsId, Jenkins.get());
        Ec2ClientBuilder clientBuilder =
                credentials != null ?
                        Ec2Client.builder()
                                .credentialsProvider(AWSUtils.toSdkV2CredentialsProvider(credentials))
                                .overrideConfiguration(clientConfiguration) :
                        Ec2Client.builder()
                                .overrideConfiguration(clientConfiguration);

        if (StringUtils.isNotBlank(regionName)) clientBuilder.region(Region.of(regionName));
        final String effectiveEndpoint = getEndpoint(regionName, endpoint);
        if (effectiveEndpoint != null) clientBuilder.endpointOverride(URI.create(effectiveEndpoint));
        clientBuilder.httpClient(AWSUtils.getApacheHttpClient(endpoint));
        return clientBuilder.build();
    }

    /**
     * Derive EC2 API endpoint. If <code>endpoint</code> parameter not empty will use
     * it as first priority, otherwise will generate endpoint as string and check if
     * region name looks like China <code>cn-</code> prefix.
     * <p>
     * Implementation details
     * <p>
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
            final String domain = regionName.startsWith("cn-") ? "amazonaws.com.cn" : "amazonaws.com";
            return "https://ec2." + regionName + "." + domain;
        } else {
            return null;
        }
    }
}
