package com.amazon.jenkins.ec2fleet.utils;

import hudson.model.Node;
import jenkins.model.Jenkins;

public class JenkinsUtils {

    public static void removeNode(final String instanceId) {
        final Jenkins jenkins = Jenkins.getActiveInstance();
        // If this node is dying, remove it from Jenkins
        final Node n = jenkins.getNode(instanceId);
        if (n != null) {
            try {
                jenkins.removeNode(n);
            } catch (final Exception ex) {
                throw new IllegalStateException(String.format("Error removing node %s", instanceId), ex);
            }
        }
    }

}
