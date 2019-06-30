package com.amazon.jenkins.ec2fleet;

import hudson.model.TaskListener;
import hudson.slaves.ComputerConnector;
import hudson.slaves.ComputerLauncher;
import org.jvnet.hudson.test.JenkinsRule;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;

/**
 * For testing only.
 *
 * @see AutoResubmitIntegrationTest
 */
class SingleLocalComputerConnector extends ComputerConnector implements Serializable {

    @Nonnull
    private transient final JenkinsRule j;

    SingleLocalComputerConnector(final JenkinsRule j) {
        this.j = j;
    }

    @Override
    public ComputerLauncher launch(@Nonnull String host, TaskListener listener) throws IOException {
        try {
            return j.createComputerLauncher(null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
