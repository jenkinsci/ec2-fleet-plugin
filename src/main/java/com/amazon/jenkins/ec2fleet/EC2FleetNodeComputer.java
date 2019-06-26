package com.amazon.jenkins.ec2fleet;

import hudson.model.Slave;
import hudson.slaves.SlaveComputer;

import javax.annotation.Nonnull;

/**
 * @see EC2FleetNode
 * @see EC2FleetAutoResubmitComputerLauncher
 */
public class EC2FleetNodeComputer extends SlaveComputer {

    public EC2FleetNodeComputer(final Slave slave) {
        super(slave);
    }

    @Override
    public EC2FleetNode getNode() {
        return (EC2FleetNode) super.getNode();
    }

    /**
     * Return label which will represent executor in "Build Executor Status"
     * section of Jenkins UI. After reconfiguration actual {@link EC2FleetNode} could
     * be removed before this, so name will be just predefined static.
     *
     * @return node display name or if node is <code>null</code> predefined text about that
     */
    @Nonnull
    @Override
    public String getDisplayName() {
        // getNode() hit map to find node by name
        final EC2FleetNode node = getNode();
        return node == null ? "removing fleet node" : node.getDisplayName();
    }

}
