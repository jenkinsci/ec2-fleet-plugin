package com.amazon.jenkins.ec2fleet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.slaves.NodeProperty;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class NodeEnvironmentVariablesTest {

    @Test
    void shouldNormalizeDroppingBlankAndUsingLastValuePerName() {
        List<CloudEnvironmentVariable> normalized = NodeEnvironmentVariables.normalize(Arrays.asList(
                new CloudEnvironmentVariable("", "a"),
                new CloudEnvironmentVariable("JAVA_HOME", "/a"),
                new CloudEnvironmentVariable("JAVA_HOME", "/b"),
                new CloudEnvironmentVariable("PATH", "/bin")));

        assertEquals(2, normalized.size());
        assertEquals("JAVA_HOME", normalized.get(0).getName());
        assertEquals("/b", normalized.get(0).getValue());
        assertEquals("PATH", normalized.get(1).getName());
        assertEquals("/bin", normalized.get(1).getValue());
    }

    @Test
    void shouldBuildEmptyNodePropertiesWhenNoVariables() {
        List<NodeProperty<?>> properties = NodeEnvironmentVariables.toNodeProperties(Collections.emptyList());
        assertTrue(properties.isEmpty());
    }
}
