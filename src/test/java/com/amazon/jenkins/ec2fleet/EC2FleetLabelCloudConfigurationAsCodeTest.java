package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.fleet.EC2Fleet;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleets;
import hudson.plugins.sshslaves.SSHConnector;
import hudson.plugins.sshslaves.verifiers.NonVerifyingKeyVerificationStrategy;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WithJenkinsConfiguredWithCode
class EC2FleetLabelCloudConfigurationAsCodeTest {

    @BeforeEach
    void before() {
        final EC2Fleet fleet = mock(EC2Fleet.class);
        EC2Fleets.setGet(fleet);
        // Mock both 4-parameter and 5-parameter getState methods
        when(fleet.getState(anyString(), anyString(), nullable(String.class), anyString()))
                .thenReturn(new FleetStateStats("", 2, FleetStateStats.State.active(), new HashSet<>(Arrays.asList("i-1", "i-2")), Collections.emptyMap()));
        when(fleet.getState(anyString(), anyString(), nullable(String.class), anyString(), Mockito.anyBoolean()))
                .thenReturn(new FleetStateStats("", 2, FleetStateStats.State.active(), new HashSet<>(Arrays.asList("i-1", "i-2")), Collections.emptyMap()));
    }

    @Test
    @ConfiguredWithCode(
            value = "EC2FleetLabelCloud/name-required-configuration-as-code.yml",
            expected = ConfiguratorException.class,
            message = "error configuring 'jenkins' with class io.jenkins.plugins.casc.core.JenkinsConfigurator configurator")
    void configurationWithNullName_shouldFail(JenkinsConfiguredWithCodeRule jenkinsRule) {
        // NOP
    }

    @Test
    @ConfiguredWithCode("EC2FleetLabelCloud/min-configuration-as-code.yml")
    void shouldCreateCloudFromMinConfiguration(JenkinsConfiguredWithCodeRule jenkinsRule) {
        assertEquals(1, jenkinsRule.jenkins.clouds.size());
        EC2FleetLabelCloud cloud = (EC2FleetLabelCloud) jenkinsRule.jenkins.clouds.getByName("ec2-fleet-label");

        assertEquals("ec2-fleet-label", cloud.name);
        assertNull(cloud.getRegion());
        assertNull(cloud.getEndpoint());
        assertNull(cloud.getFsRoot());
        assertFalse(cloud.isPrivateIpUsed());
        assertFalse(cloud.isAlwaysReconnect());
        assertEquals(0, cloud.getIdleMinutes());
        assertEquals(0, cloud.getMinSize());
        assertEquals(0, cloud.getMaxSize());
        assertEquals(1, cloud.getNumExecutors());
        assertFalse(cloud.isRestrictUsage());
        assertEquals(180, cloud.getInitOnlineTimeoutSec());
        assertEquals(15, cloud.getInitOnlineCheckIntervalSec());
        assertEquals(10, cloud.getCloudStatusIntervalSec());
        assertFalse(cloud.isDisableTaskResubmit());
        assertFalse(cloud.isNoDelayProvision());
        assertNull(cloud.getEc2KeyPairName());
    }

    @Test
    @ConfiguredWithCode("EC2FleetLabelCloud/max-configuration-as-code.yml")
    void shouldCreateCloudFromMaxConfiguration(JenkinsConfiguredWithCodeRule jenkinsRule) {
        assertEquals(1, jenkinsRule.jenkins.clouds.size());
        EC2FleetLabelCloud cloud = (EC2FleetLabelCloud) jenkinsRule.jenkins.clouds.getByName("ec2-fleet-label");

        assertEquals("ec2-fleet-label", cloud.name);
        assertEquals("us-east-2", cloud.getRegion());
        assertEquals("http://a.com", cloud.getEndpoint());
        assertEquals("my-root", cloud.getFsRoot());
        assertTrue(cloud.isPrivateIpUsed());
        assertTrue(cloud.isAlwaysReconnect());
        assertEquals(22, cloud.getIdleMinutes());
        assertEquals(11, cloud.getMinSize());
        assertEquals(75, cloud.getMaxSize());
        assertEquals(24, cloud.getNumExecutors());
        assertFalse(cloud.isRestrictUsage());
        assertEquals(267, cloud.getInitOnlineTimeoutSec());
        assertEquals(13, cloud.getInitOnlineCheckIntervalSec());
        assertEquals(11, cloud.getCloudStatusIntervalSec());
        assertTrue(cloud.isDisableTaskResubmit());
        assertFalse(cloud.isNoDelayProvision());
        assertEquals("xx", cloud.getAwsCredentialsId());
        assertEquals("keyPairName", cloud.getEc2KeyPairName());

        SSHConnector sshConnector = (SSHConnector) cloud.getComputerConnector();
        assertEquals(NonVerifyingKeyVerificationStrategy.class, sshConnector.getSshHostKeyVerificationStrategy().getClass());
    }

    @Test
    @ConfiguredWithCode("EC2FleetLabelCloud/empty-name-configuration-as-code.yml")
    void configurationWithEmptyName_shouldUseDefault(JenkinsConfiguredWithCodeRule jenkinsRule) {
        assertEquals(3, jenkinsRule.jenkins.clouds.size());

        for (EC2FleetLabelCloud cloud : jenkinsRule.jenkins.clouds.getAll(EC2FleetLabelCloud.class)){

            assertTrue(cloud.name.startsWith(EC2FleetLabelCloud.BASE_DEFAULT_FLEET_CLOUD_ID));
            assertEquals(("FleetLabelCloud".length() + CloudNames.SUFFIX_LENGTH + 1), cloud.name.length());
        }
    }
}
