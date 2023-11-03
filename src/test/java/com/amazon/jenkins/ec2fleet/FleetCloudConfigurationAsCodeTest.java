package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.fleet.Fleet;
import com.amazon.jenkins.ec2fleet.fleet.Fleets;
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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FleetCloudConfigurationAsCodeTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsConfiguredWithCodeRule();

    @Before
    public void before() {
        final Fleet fleet = mock(Fleet.class);
        Fleets.setGet(fleet);
        when(fleet.getState(anyString(), anyString(), nullable(String.class), anyString()))
                .thenReturn(new FleetStateStats("", 2, FleetStateStats.State.active(), new HashSet<>(Arrays.asList("i-1", "i-2")), Collections.emptyMap()));
    }

    @Test
    @ConfiguredWithCode(
            value = "FleetCloud/name-required-configuration-as-code.yml",
            expected = ConfiguratorException.class,
            message = "error configuring 'jenkins' with class io.jenkins.plugins.casc.core.JenkinsConfigurator configurator")
    public void configurationWithNullName_shouldFail() {
    }

    @Test
    @ConfiguredWithCode("FleetCloud/min-configuration-as-code.yml")
    public void shouldCreateCloudFromMinConfiguration() {
        assertEquals(jenkinsRule.jenkins.clouds.size(), 1);
        FleetCloud cloud = (FleetCloud) jenkinsRule.jenkins.clouds.getByName("ec2-fleet");

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
        assertEquals(cloud.getExecutorScaler().getClass(), FleetCloud.NoScaler.class);
        assertEquals(cloud.getInitOnlineTimeoutSec(), 180);
        assertEquals(cloud.getInitOnlineCheckIntervalSec(), 15);
        assertEquals(cloud.getCloudStatusIntervalSec(), 10);
        assertEquals(cloud.isDisableTaskResubmit(), false);
        assertEquals(cloud.isNoDelayProvision(), false);
    }

    @Test
    @ConfiguredWithCode("FleetCloud/max-configuration-as-code.yml")
    public void shouldCreateCloudFromMaxConfiguration() {
        assertEquals(jenkinsRule.jenkins.clouds.size(), 1);
        FleetCloud cloud = (FleetCloud) jenkinsRule.jenkins.clouds.getByName("ec2-fleet");

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
        assertEquals(cloud.getExecutorScaler().getClass(), FleetCloud.WeightedScaler.class);
        assertEquals(cloud.getInitOnlineTimeoutSec(), 181);
        assertEquals(cloud.getInitOnlineCheckIntervalSec(), 13);
        assertEquals(cloud.getCloudStatusIntervalSec(), 11);
        assertEquals(cloud.isDisableTaskResubmit(), true);
        assertEquals(cloud.isNoDelayProvision(), true);
        assertEquals(cloud.getAwsCredentialsId(), "xx");

        SSHConnector sshConnector = (SSHConnector) cloud.getComputerConnector();
        assertEquals(sshConnector.getSshHostKeyVerificationStrategy().getClass(), NonVerifyingKeyVerificationStrategy.class);
    }

    @Test
    @ConfiguredWithCode("FleetCloud/empty-name-configuration-as-code.yml")
    public void configurationWithEmptyName_shouldUseDefault() {
        assertEquals(jenkinsRule.jenkins.clouds.size(), 3);

        for (FleetCloud cloud : jenkinsRule.jenkins.clouds.getAll(FleetCloud.class)){

            assertTrue(cloud.name.startsWith(FleetCloud.BASE_DEFAULT_FLEET_CLOUD_ID));
            assertEquals(("FleetCloud".length() + CloudNames.SUFFIX_LENGTH + 1), cloud.name.length());
        }
    }
}