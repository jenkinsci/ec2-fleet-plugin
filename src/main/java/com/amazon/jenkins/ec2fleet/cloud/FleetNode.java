package com.amazon.jenkins.ec2fleet.cloud;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.EphemeralNode;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;

import java.io.IOException;
import java.util.List;

public class FleetNode extends Slave implements EphemeralNode {

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
    public String getDisplayName() {
        return cloudName + " " + name;
    }

    @Override
    public Computer createComputer() {
        return new FleetNodeComputer(this);
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
         * We only create this kind of nodes programmatically.
         */
        @Override
        public boolean isInstantiable() {
            return false;
        }
    }
}
