package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.fleet.EC2Fleet;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleets;
import hudson.plugins.sshslaves.SSHConnector;
import hudson.plugins.sshslaves.verifiers.NonVerifyingKeyVerificationStrategy;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EC2FleetLabelCloudConfigurationAsCodeTest {
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsConfiguredWithCodeRule();

    @Before
    public void before() {
        final EC2Fleet fleet = mock(EC2Fleet.class);
        EC2Fleets.setGet(fleet);
        when(fleet.getState(anyString(), anyString(), nullable(String.class), anyString()))
                .thenReturn(new FleetStateStats("", 2, FleetStateStats.State.active(), new HashSet<>(Arrays.asList("i-1", "i-2")), Collections.emptyMap()));
    }

    @Test
    @ConfiguredWithCode(
            value = "EC2FleetLabelCloud/name-required-configuration-as-code.yml",
            expected = ConfiguratorException.class,
            message = "error configuring 'jenkins' with class io.jenkins.plugins.casc.core.JenkinsConfigurator configurator")
    public void configurationWithNullName_shouldFail() {
    }

    @Test
    @ConfiguredWithCode("EC2FleetLabelCloud/min-configuration-as-code.yml")
    public void shouldCreateCloudFromMinConfiguration() {
        assertEquals(jenkinsRule.jenkins.clouds.size(), 1);
        EC2FleetLabelCloud cloud = (EC2FleetLabelCloud) jenkinsRule.jenkins.clouds.getByName("ec2-fleet-label");

        assertEquals("ec2-fleet-label", cloud.name);
        assertNull(cloud.getRegion());
        assertNull(cloud.getEndpoint());
        assertNull(cloud.getFsRoot());
        assertFalse(cloud.isPrivateIpUsed());
        assertFalse(cloud.isAlwaysReconnect());
        assertEquals(cloud.getIdleMinutes(), 0);
        assertEquals(cloud.getMinSize(), 0);
        assertEquals(cloud.getMaxSize(), 0);
        assertEquals(cloud.getNumExecutors(), 1);
        assertFalse(cloud.isRestrictUsage());
        assertEquals(cloud.getInitOnlineTimeoutSec(), 180);
        assertEquals(cloud.getInitOnlineCheckIntervalSec(), 15);
        assertEquals(cloud.getCloudStatusIntervalSec(), 10);
        assertFalse(cloud.isDisableTaskResubmit());
        assertFalse(cloud.isNoDelayProvision());
        assertNull(cloud.getEc2KeyPairName());
    }

    @Test
    @ConfiguredWithCode("EC2FleetLabelCloud/max-configuration-as-code.yml")
    public void shouldCreateCloudFromMaxConfiguration() {
        assertEquals(jenkinsRule.jenkins.clouds.size(), 1);
        EC2FleetLabelCloud cloud = (EC2FleetLabelCloud) jenkinsRule.jenkins.clouds.getByName("ec2-fleet-label");

        assertEquals("ec2-fleet-label", cloud.name);
        assertEquals(cloud.getRegion(), "us-east-2");
        assertEquals(cloud.getEndpoint(), "http://a.com");
        assertEquals(cloud.getFsRoot(), "my-root");
        assertTrue(cloud.isPrivateIpUsed());
        assertTrue(cloud.isAlwaysReconnect());
        assertEquals(cloud.getIdleMinutes(), 22);
        assertEquals(cloud.getMinSize(), 11);
        assertEquals(cloud.getMaxSize(), 75);
        assertEquals(cloud.getNumExecutors(), 24);
        assertFalse(cloud.isRestrictUsage());
        assertEquals(cloud.getInitOnlineTimeoutSec(), 267);
        assertEquals(cloud.getInitOnlineCheckIntervalSec(), 13);
        assertEquals(cloud.getCloudStatusIntervalSec(), 11);
        assertTrue(cloud.isDisableTaskResubmit());
        assertFalse(cloud.isNoDelayProvision());
        assertEquals(cloud.getAwsCredentialsId(), "xx");
        assertEquals(cloud.getEc2KeyPairName(), "keyPairName");

        SSHConnector sshConnector = (SSHConnector) cloud.getComputerConnector();
        assertEquals(sshConnector.getSshHostKeyVerificationStrategy().getClass(), NonVerifyingKeyVerificationStrategy.class);
    }

    @Test
    @ConfiguredWithCode("EC2FleetLabelCloud/empty-name-configuration-as-code.yml")
    public void configurationWithEmptyName_shouldUseDefault() {
        assertEquals(jenkinsRule.jenkins.clouds.size(), 3);

        for (EC2FleetLabelCloud cloud : jenkinsRule.jenkins.clouds.getAll(EC2FleetLabelCloud.class)){

            assertTrue(cloud.name.startsWith(EC2FleetLabelCloud.BASE_DEFAULT_FLEET_CLOUD_ID));
            assertEquals(("FleetLabelCloud".length() + CloudNames.SUFFIX_LENGTH + 1), cloud.name.length());
        }
    }
}
