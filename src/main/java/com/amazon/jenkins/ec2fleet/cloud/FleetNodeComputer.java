package com.amazon.jenkins.ec2fleet.cloud;

import hudson.model.Slave;
import hudson.slaves.SlaveComputer;

import javax.annotation.Nonnull;

/**
 * @see FleetNode
 */
public class FleetNodeComputer extends SlaveComputer {

    public FleetNodeComputer(final Slave slave) {
        super(slave);
    }

    @Override
    public FleetNode getNode() {
        return (FleetNode) super.getNode();
    }

    /**
     * Return label which will represent executor in "Build Executor Status"
     * section of Jenkins UI. After reconfiguration actual {@link FleetNode} could
     * be removed before this, so name will be just predefined static.
     *
     * @return node display name or if node is <code>null</code> predefined text about that
     */
    @Nonnull
    @Override
    public String getDisplayName() {
        // getNode() hit map to find node by name
        final FleetNode node = getNode();
        return node == null ? "removing fleet node" : node.getDisplayName();
    }

}
