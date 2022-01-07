package com.amazon.jenkins.ec2fleet;

import hudson.model.TaskListener;
import hudson.slaves.ComputerConnector;
import hudson.slaves.ComputerLauncher;
import org.jvnet.hudson.test.JenkinsRule;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;

/**
 * For testing only.
 *
 * @see AutoResubmitIntegrationTest
 */
class LocalComputerConnector extends ComputerConnector implements Serializable {

    @NonNull
    private transient final JenkinsRule j;

    LocalComputerConnector(final JenkinsRule j) {
        this.j = j;
    }

    @Override
    public ComputerLauncher launch(@NonNull String host, TaskListener listener) throws IOException {
        System.out.println("Creating computer launcher");
        try {
            return j.createComputerLauncher(null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
