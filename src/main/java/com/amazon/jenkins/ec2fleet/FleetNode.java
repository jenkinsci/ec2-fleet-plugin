package com.amazon.jenkins.ec2fleet;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Failure;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.EphemeralNode;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * The {@link FleetNode} represents an agent running on an EC2 instance, responsible for creating {@link FleetNodeComputer}.
 */
public class FleetNode extends Slave implements EphemeralNode {
    private static final Logger LOGGER = Logger.getLogger(FleetNode.class.getName());

    private String cloudName;
    private String instanceId;
    private final int maxTotalUses;
    private int usesRemaining;

    public FleetNode(final String instanceId, final String nodeDescription, final String remoteFS, final int numExecutors, final Mode mode, final String label,
                     final List<? extends NodeProperty<?>> nodeProperties, final String cloudName, ComputerLauncher launcher, final int maxTotalUses) throws IOException, Descriptor.FormException {
        //noinspection deprecation
        super(instanceId, nodeDescription, remoteFS, numExecutors, mode, label,
                launcher, RetentionStrategy.NOOP, nodeProperties);

        this.cloudName = cloudName;
        this.instanceId = instanceId;
        this.maxTotalUses = maxTotalUses;
        this.usesRemaining = maxTotalUses;
    }

    public String getCloudName() {
        return cloudName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public int getMaxTotalUses() {
        return this.maxTotalUses;
    }

    public int getUsesRemaining() {
        return usesRemaining;
    }

    public void decrementUsesRemaining() {
        this.usesRemaining--;
    }

    @Override
    public Node asNode() {
        return this;
    }

    @Override
    public String getDisplayName() {
        final String name = String.format("%s %s", cloudName, instanceId);
        try {
            Jenkins.checkGoodName(name);
            return name;
        } catch (Failure e) {
            return instanceId;
        }
    }

    @Override
    public Computer createComputer() {
        return new FleetNodeComputer(this);
    }

    public AbstractFleetCloud getCloud() {
        return (AbstractFleetCloud) Jenkins.get().getCloud(cloudName);
    }

    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
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
