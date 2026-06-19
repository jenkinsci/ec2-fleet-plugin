package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.FleetStateStats;
import com.amazon.jenkins.ec2fleet.EC2FleetCloud;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.Page;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

@WithJenkins
class EC2FleetStatsApiTest {

    private JenkinsRule j;

    @BeforeEach
    void before(JenkinsRule rule) {
        j = rule;
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        final FullControlOnceLoggedInAuthorizationStrategy authorizationStrategy =
                new FullControlOnceLoggedInAuthorizationStrategy();
        authorizationStrategy.setAllowAnonymousRead(false);
        j.jenkins.setAuthorizationStrategy(authorizationStrategy);
    }

    @Test
    void testFleetStatsApiEndpointReturnsExpectedJson() throws Exception {
        FleetStateStats.State state = new FleetStateStats.State(true, false, "active");
        FleetStateStats stats = new FleetStateStats("fleet-1", 2, state,
                new HashSet<>(Arrays.asList("i-1", "i-2")), Collections.emptyMap());

        EC2FleetCloud cloud = Mockito.mock(EC2FleetCloud.class);
        Mockito.when(cloud.getStats()).thenReturn(stats);
        Mockito.when(cloud.getFleet()).thenReturn("fleet-1");
        Mockito.when(cloud.getLabelString()).thenReturn("label-1");

        j.jenkins.clouds.add(cloud);

        final JenkinsRule.WebClient webClient = j.createWebClient();
        webClient.withBasicCredentials("admin");
        final Page page = webClient.goTo("ec2-fleet/stats", "application/json");
        final String json = page.getWebResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(json.contains("\"fleet\":\"fleet-1\""));
        assertTrue(json.contains("\"state\":\"active\""));
        assertTrue(json.contains("\"label\":\"label-1\""));
        assertTrue(json.contains("\"numActive\":2"));
    }

    @Test
    void testFleetStatsApiEndpointRejectsAnonymous() {
        final JenkinsRule.WebClient webClient = j.createWebClient();

        final FailingHttpStatusCodeException exception =
                assertThrows(FailingHttpStatusCodeException.class, () -> webClient.goTo("ec2-fleet/stats"));
        assertEquals(403, exception.getStatusCode());
    }
}