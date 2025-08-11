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
class EC2FleetCloudConfigurationAsCodeTest {

    @BeforeEach
    void before() {
        final EC2Fleet ec2Fleet = mock(EC2Fleet.class);
        EC2Fleets.setGet(ec2Fleet);
        when(ec2Fleet.getState(anyString(), anyString(), nullable(String.class), anyString()))
                .thenReturn(new FleetStateStats("", 2, FleetStateStats.State.active(), new HashSet<>(Arrays.asList("i-1", "i-2")), Collections.emptyMap()));
    }

    @Test
    @ConfiguredWithCode(
            value = "EC2FleetCloud/name-required-configuration-as-code.yml",
            expected = ConfiguratorException.class,
            message = "name is required to configure class com.amazon.jenkins.ec2fleet.EC2FleetCloud")
    void configurationWithNullName_shouldFail(JenkinsConfiguredWithCodeRule jenkinsRule) {
        // NOP
    }

    @Test
    @ConfiguredWithCode("EC2FleetCloud/min-configuration-as-code.yml")
    void shouldCreateCloudFromMinConfiguration(JenkinsConfiguredWithCodeRule jenkinsRule) {
        assertEquals(1, jenkinsRule.jenkins.clouds.size());
        EC2FleetCloud cloud = (EC2FleetCloud) jenkinsRule.jenkins.clouds.getByName("ec2-fleet");

        assertEquals("ec2-fleet", cloud.name);
        assertNull(cloud.getRegion());
        assertNull(cloud.getEndpoint());
        assertNull(cloud.getFleet());
        assertNull(cloud.getFsRoot());
        assertFalse(cloud.isPrivateIpUsed());
        assertFalse(cloud.isAlwaysReconnect());
        assertNull(cloud.getLabelString());
        assertEquals(0, cloud.getIdleMinutes());
        assertEquals(0, cloud.getMinSize());
        assertEquals(0, cloud.getMaxSize());
        assertEquals(1, cloud.getNumExecutors());
        assertFalse(cloud.isAddNodeOnlyIfRunning());
        assertFalse(cloud.isRestrictUsage());
        assertEquals(EC2FleetCloud.NoScaler.class, cloud.getExecutorScaler().getClass());
        assertEquals(180, cloud.getInitOnlineTimeoutSec());
        assertEquals(15, cloud.getInitOnlineCheckIntervalSec());
        assertEquals(10, cloud.getCloudStatusIntervalSec());
        assertFalse(cloud.isDisableTaskResubmit());
        assertFalse(cloud.isNoDelayProvision());
    }

    @Test
    @ConfiguredWithCode("EC2FleetCloud/max-configuration-as-code.yml")
    void shouldCreateCloudFromMaxConfiguration(JenkinsConfiguredWithCodeRule jenkinsRule) {
        assertEquals(1, jenkinsRule.jenkins.clouds.size());
        EC2FleetCloud cloud = (EC2FleetCloud) jenkinsRule.jenkins.clouds.getByName("ec2-fleet");

        assertEquals("ec2-fleet", cloud.name);
        assertEquals("us-east-2", cloud.getRegion());
        assertEquals("http://a.com", cloud.getEndpoint());
        assertEquals("my-fleet", cloud.getFleet());
        assertEquals("my-root", cloud.getFsRoot());
        assertTrue(cloud.isPrivateIpUsed());
        assertTrue(cloud.isAlwaysReconnect());
        assertEquals("myLabel", cloud.getLabelString());
        assertEquals(33, cloud.getIdleMinutes());
        assertEquals(15, cloud.getMinSize());
        assertEquals(90, cloud.getMaxSize());
        assertEquals(12, cloud.getNumExecutors());
        assertTrue(cloud.isAddNodeOnlyIfRunning());
        assertTrue(cloud.isRestrictUsage());
        assertEquals(EC2FleetCloud.WeightedScaler.class, cloud.getExecutorScaler().getClass());
        assertEquals(181, cloud.getInitOnlineTimeoutSec());
        assertEquals(13, cloud.getInitOnlineCheckIntervalSec());
        assertEquals(11, cloud.getCloudStatusIntervalSec());
        assertTrue(cloud.isDisableTaskResubmit());
        assertTrue(cloud.isNoDelayProvision());
        assertEquals("xx", cloud.getAwsCredentialsId());

        SSHConnector sshConnector = (SSHConnector) cloud.getComputerConnector();
        assertEquals(NonVerifyingKeyVerificationStrategy.class, sshConnector.getSshHostKeyVerificationStrategy().getClass());
    }

    @Test
    @ConfiguredWithCode("EC2FleetCloud/empty-name-configuration-as-code.yml")
    void configurationWithEmptyName_shouldUseDefault(JenkinsConfiguredWithCodeRule jenkinsRule) {
        assertEquals(3, jenkinsRule.jenkins.clouds.size());

        for (EC2FleetCloud cloud : jenkinsRule.jenkins.clouds.getAll(EC2FleetCloud.class)){

            assertTrue(cloud.name.startsWith(EC2FleetCloud.BASE_DEFAULT_FLEET_CLOUD_ID));
            assertEquals(("FleetCloud".length() + CloudNames.SUFFIX_LENGTH + 1), cloud.name.length());
        }
    }
}