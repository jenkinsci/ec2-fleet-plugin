package com.amazon.jenkins.ec2fleet;

import hudson.plugins.sshslaves.SSHConnector;
import hudson.plugins.sshslaves.verifiers.NonVerifyingKeyVerificationStrategy;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;

public class EC2FleetCloudConfigurationAsCodeTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("EC2FleetCloud/min-configuration-as-code.yml")
    public void shouldCreateCloudFromMinConfiguration() {
        assertEquals(jenkinsRule.jenkins.clouds.size(), 1);
        EC2FleetCloud cloud = (EC2FleetCloud) jenkinsRule.jenkins.clouds.getByName("ec2-fleet");

        assertEquals("ec2-fleet", cloud.name);
        assertEquals(cloud.getRegion(), null);
        assertEquals(cloud.getEndpoint(), null);
        assertEquals(cloud.getFleet(), null);
        assertEquals(cloud.getFsRoot(), null);
        assertEquals(cloud.isPrivateIpUsed(), false);
        assertEquals(cloud.isAlwaysReconnect(), false);
        assertEquals(cloud.getLabelString(), null);
        assertEquals(cloud.getIdleMinutes(), 0);
        assertEquals(cloud.getMinSize(), 0);
        assertEquals(cloud.getMaxSize(), 0);
        assertEquals(cloud.getNumExecutors(), 1);
        assertEquals(cloud.isAddNodeOnlyIfRunning(), false);
        assertEquals(cloud.isRestrictUsage(), false);
        assertEquals(cloud.isScaleExecutorsByWeight(), false);
        assertEquals(cloud.getInitOnlineTimeoutSec(), 180);
        assertEquals(cloud.getInitOnlineCheckIntervalSec(), 15);
        assertEquals(cloud.getCloudStatusIntervalSec(), 10);
        assertEquals(cloud.isDisableTaskResubmit(), false);
        assertEquals(cloud.isNoDelayProvision(), false);
    }

    @Test
    @ConfiguredWithCode("EC2FleetCloud/max-configuration-as-code.yml")
    public void shouldCreateCloudFromMaxConfiguration() {
        assertEquals(jenkinsRule.jenkins.clouds.size(), 1);
        EC2FleetCloud cloud = (EC2FleetCloud) jenkinsRule.jenkins.clouds.getByName("ec2-fleet");

        assertEquals("ec2-fleet", cloud.name);
        assertEquals(cloud.getRegion(), "us-east-2");
        assertEquals(cloud.getEndpoint(), "http://a.com");
        assertEquals(cloud.getFleet(), "my-fleet");
        assertEquals(cloud.getFsRoot(), "my-root");
        assertEquals(cloud.isPrivateIpUsed(), true);
        assertEquals(cloud.isAlwaysReconnect(), true);
        assertEquals(cloud.getLabelString(), "myLabel");
        assertEquals(cloud.getIdleMinutes(), 33);
        assertEquals(cloud.getMinSize(), 15);
        assertEquals(cloud.getMaxSize(), 90);
        assertEquals(cloud.getNumExecutors(), 12);
        assertEquals(cloud.isAddNodeOnlyIfRunning(), true);
        assertEquals(cloud.isRestrictUsage(), true);
        assertEquals(cloud.isScaleExecutorsByWeight(), true);
        assertEquals(cloud.getInitOnlineTimeoutSec(), 181);
        assertEquals(cloud.getInitOnlineCheckIntervalSec(), 13);
        assertEquals(cloud.getCloudStatusIntervalSec(), 11);
        assertEquals(cloud.isDisableTaskResubmit(), true);
        assertEquals(cloud.isNoDelayProvision(), true);
        assertEquals(cloud.getAwsCredentialsId(), "xx");

        SSHConnector sshConnector = (SSHConnector) cloud.getComputerConnector();
        assertEquals(sshConnector.getSshHostKeyVerificationStrategy().getClass(), NonVerifyingKeyVerificationStrategy.class);
    }
}