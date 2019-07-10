package com.amazon.jenkins.ec2fleet;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CancelSpotFleetRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsRequest;
import com.amazonaws.services.ec2.model.RequestSpotFleetRequest;
import com.amazonaws.services.ec2.model.RequestSpotFleetResult;
import com.amazonaws.services.ec2.model.SpotFleetLaunchSpecification;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfig;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfigData;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsImpl;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import org.apache.commons.lang.StringUtils;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Ignore("for manual run as you need to provide real AWS EC2 API credentials")
public class RealEc2ApiIntegrationTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @ClassRule
    public static BuildWatcher bw = new BuildWatcher();

    @Test
    public void shouldSuccessfullyUpdatePluginWithFleetStatus() throws Exception {
        int targetCapacity = 0;

        final AWSCredentials awsCredentials = getAwsCredentials();

        SystemCredentialsProvider.getInstance().getCredentials().add(
                new AWSCredentialsImpl(CredentialsScope.SYSTEM, "credId",
                        awsCredentials.getAWSAccessKeyId(), awsCredentials.getAWSSecretKey(), "d"));

        withFleet(awsCredentials, targetCapacity, new WithFleetBody() {
            @Override
            public void run(AmazonEC2 amazonEC2, String fleetId) throws Exception {
                EC2FleetCloud cloud = new EC2FleetCloud("", null, "credId", null, null, null, fleetId,
                        null, null, null, false, false,
                        0, 0, 0, 0, false, false,
                        false, 0, 0, false);
                j.jenkins.clouds.add(cloud);

                // 10 sec refresh time so wait
                Thread.sleep(TimeUnit.SECONDS.toMillis(60));

                // assertEquals(0, cloud.getStatusCache().getNumActive());
                // assertEquals(fleetId, cloud.getStatusCache().getFleetId());
            }
        });
    }

    /**
     * Related to https://github.com/jenkinsci/ec2-fleet-plugin/issues/60
     *
     * @throws Exception e
     */
    @Test
    public void shouldSuccessfullyUpdateBigFleetPluginWithFleetStatus() throws Exception {
        final int targetCapacity = 30;

        final AWSCredentials awsCredentials = getAwsCredentials();

        SystemCredentialsProvider.getInstance().getCredentials().add(
                new AWSCredentialsImpl(CredentialsScope.SYSTEM, "credId",
                        awsCredentials.getAWSAccessKeyId(), awsCredentials.getAWSSecretKey(), "d"));

        withFleet(awsCredentials, targetCapacity, new WithFleetBody() {
            @Override
            public void run(AmazonEC2 amazonEC2, String fleetId) throws Exception {
                EC2FleetCloud cloud = new EC2FleetCloud(null, null, "credId", null, null, null, fleetId,
                        null, null, null, false, false,
                        0, 0, 0, 0, false, false,
                        false, 0, 0, false);
                j.jenkins.clouds.add(cloud);

                final long start = System.currentTimeMillis();
                final long max = TimeUnit.MINUTES.toMillis(2);
                while (System.currentTimeMillis() - start < max) {
                    // if (cloud.getStatusCache().getNumActive() >= targetCapacity) break;
                    Thread.sleep(TimeUnit.SECONDS.toMillis(10));
                }

                // todo replace with proper accessor assertEquals(targetCapacity, cloud.getStatusCache().getNumActive());
                // assertEquals(fleetId, cloud.getStatusCache().getFleetId());
            }
        });
    }

    private interface WithFleetBody {
        void run(AmazonEC2 amazonEC2, String fleetId) throws Exception;
    }

    private void withFleet(AWSCredentials awsCredentials, int targetCapacity, WithFleetBody body) throws Exception {
        final AmazonEC2 amazonEC2 = new AmazonEC2Client(awsCredentials);

        final SpotFleetRequestConfigData data = new SpotFleetRequestConfigData();
        data.setLaunchSpecifications(Arrays.asList(
                new SpotFleetLaunchSpecification()
                        .withInstanceType("t2.micro")
                        .withImageId("ami-009d6802948d06e52")
        ));
        data.setIamFleetRole("arn:aws:iam::...:role/aws-service-role/spotfleet.amazonaws.com/AWSServiceRoleForEC2SpotFleet");
        data.setTargetCapacity(targetCapacity);

        final RequestSpotFleetResult result = amazonEC2.requestSpotFleet(
                new RequestSpotFleetRequest().withSpotFleetRequestConfig(data));

        try {
            final List<SpotFleetRequestConfig> configs = amazonEC2.describeSpotFleetRequests(
                    new DescribeSpotFleetRequestsRequest().withSpotFleetRequestIds(
                            result.getSpotFleetRequestId())).getSpotFleetRequestConfigs();

            if (configs.isEmpty()) throw new IllegalArgumentException();

            final int f = configs.get(0).getSpotFleetRequestConfig().getFulfilledCapacity().intValue();
            System.out.println("Fulfilment " + f);

            body.run(amazonEC2, result.getSpotFleetRequestId());
        } finally {
            amazonEC2.cancelSpotFleetRequests(new CancelSpotFleetRequestsRequest()
                    .withSpotFleetRequestIds(result.getSpotFleetRequestId()).withTerminateInstances(true));
        }
    }

    private AWSCredentials getAwsCredentials() {
        final String accessKey = System.getProperty("AWS_ACCESS_KEY");
        final String secretKey = System.getProperty("AWS_SECRET_KEY");

        if (StringUtils.isBlank(accessKey) || StringUtils.isBlank(secretKey)) {
            throw new IllegalArgumentException("AWS_ACCESS_KEY or AWS_SECRET_KEY is not specified in system properties, -D");
        }

        return new BasicAWSCredentials(accessKey, secretKey);
    }

}
