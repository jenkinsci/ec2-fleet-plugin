package com.amazon.jenkins.ec2fleet;

import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.EnvVars;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

final class NodeEnvironmentVariables {

    private NodeEnvironmentVariables() {}

    static List<CloudEnvironmentVariable> normalize(final List<CloudEnvironmentVariable> environmentVariables) {
        if (environmentVariables == null || environmentVariables.isEmpty()) {
            return Collections.emptyList();
        }

        final LinkedHashMap<String, String> valuesByName = new LinkedHashMap<>();
        for (final CloudEnvironmentVariable environmentVariable : environmentVariables) {
            if (environmentVariable == null || StringUtils.isBlank(environmentVariable.getName())) {
                continue;
            }
            valuesByName.put(environmentVariable.getName(), environmentVariable.getValue());
        }

        if (valuesByName.isEmpty()) {
            return Collections.emptyList();
        }

        final List<CloudEnvironmentVariable> normalized = new ArrayList<>(valuesByName.size());
        for (final Map.Entry<String, String> entry : valuesByName.entrySet()) {
            normalized.add(new CloudEnvironmentVariable(entry.getKey(), entry.getValue()));
        }
        return normalized;
    }

    static List<NodeProperty<?>> toNodeProperties(final List<CloudEnvironmentVariable> environmentVariables) {
        final List<EnvironmentVariablesNodeProperty.Entry> entries = toEntries(environmentVariables);
        if (entries.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Collections.singletonList(new EnvironmentVariablesNodeProperty(entries)));
    }

    static void reconcile(final Node node, final List<CloudEnvironmentVariable> environmentVariables)
            throws IOException, Descriptor.FormException {
        final EnvironmentVariablesNodeProperty existingProperty =
                node.getNodeProperties().get(EnvironmentVariablesNodeProperty.class);
        final List<EnvironmentVariablesNodeProperty.Entry> desiredEntries = toEntries(environmentVariables);
        final Map<String, String> existingMap = toMap(existingProperty);
        final Map<String, String> desiredMap = toMap(desiredEntries);
        if (existingMap.equals(desiredMap)) {
            return;
        }

        if (existingProperty != null) {
            node.getNodeProperties().remove(existingProperty);
        }
        if (!desiredEntries.isEmpty()) {
            node.getNodeProperties().add(new EnvironmentVariablesNodeProperty(desiredEntries));
        }
    }

    private static List<EnvironmentVariablesNodeProperty.Entry> toEntries(
            final List<CloudEnvironmentVariable> environmentVariables) {
        final List<CloudEnvironmentVariable> normalized = normalize(environmentVariables);
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }

        final List<EnvironmentVariablesNodeProperty.Entry> entries = new ArrayList<>(normalized.size());
        for (final CloudEnvironmentVariable normalizedVariable : normalized) {
            entries.add(new EnvironmentVariablesNodeProperty.Entry(
                    normalizedVariable.getName(), normalizedVariable.getValue()));
        }
        return entries;
    }

    private static Map<String, String> toMap(final EnvironmentVariablesNodeProperty property) {
        if (property == null) {
            return Collections.emptyMap();
        }
        return toMap(property.getEnvVars());
    }

    private static Map<String, String> toMap(final EnvVars envVars) {
        if (envVars == null || envVars.isEmpty()) {
            return Collections.emptyMap();
        }
        return new LinkedHashMap<>(envVars);
    }

    private static Map<String, String> toMap(final List<EnvironmentVariablesNodeProperty.Entry> entries) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyMap();
        }
        final LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (final EnvironmentVariablesNodeProperty.Entry entry : entries) {
            map.put(entry.key, entry.value);
        }
        return map;
    }
}
