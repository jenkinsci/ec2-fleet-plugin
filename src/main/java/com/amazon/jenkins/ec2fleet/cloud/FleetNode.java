package com.amazon.jenkins.ec2fleet.cloud;

import com.amazon.jenkins.ec2fleet.EC2FleetCloud;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.EphemeralNode;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.List;

public class FleetNode extends Slave implements EphemeralNode {

    private static final long LAUNCH_TIMEOUT_MS = 1000 * 1000L;
    private final String cloudName;

    public FleetNode(final String name, final String nodeDescription, final String remoteFS, final String numExecutors, final Mode mode, final String label,
                     final List<? extends NodeProperty<?>> nodeProperties, final String cloudName, ComputerLauncher launcher) throws IOException, Descriptor.FormException {
        super(name, nodeDescription, remoteFS, numExecutors, mode, label,
                launcher, RetentionStrategy.NOOP, nodeProperties);
        this.cloudName = cloudName;
    }

    @Override
    public Node asNode() {
        return this;
    }

    @Override
    public Computer createComputer() {
        return new FleetNodeComputer(this);
    }

    public long getLaunchTimeoutInMillis() {
        return LAUNCH_TIMEOUT_MS;
    }

    public EC2FleetCloud getCloud() {
        return (EC2FleetCloud) Jenkins.getInstance().getCloud(cloudName);
    }

    @Extension
    public static final class DescriptorImpl extends SlaveDescriptor {
        public DescriptorImpl() {
            super();
        }

        public String getDisplayName() {
            return "Fleet Slave";
        }

        /**
         * We only create this kind of nodes programatically.
         */
        @Override
        public boolean isInstantiable() {
            return false;
        }
    }
}
