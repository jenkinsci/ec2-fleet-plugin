package com.amazon.jenkins.ec2fleet;

import hudson.model.Slave;
import hudson.slaves.SlaveComputer;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @see EC2FleetNode
 * @see EC2FleetAutoResubmitComputerLauncher
 */
@ThreadSafe
public class EC2FleetNodeComputer extends SlaveComputer implements EC2FleetCloudAware {

    private final String name;

    private volatile AbstractEC2FleetCloud cloud;

    public EC2FleetNodeComputer(final Slave slave, @Nonnull final String name, @Nonnull final AbstractEC2FleetCloud cloud) {
        super(slave);
        this.name = name;
        this.cloud = cloud;
    }

    @Override
    public EC2FleetNode getNode() {
        return (EC2FleetNode) super.getNode();
    }

    /**
     * Return label which will represent executor in "Build Executor Status"
     * section of Jenkins UI.
     *
     * @return node display name
     */
    @Nonnull
    @Override
    public String getDisplayName() {
        // in some multi-thread edge cases cloud could be null for some time, just be ok with that
        return (cloud == null ? "unknown fleet" : cloud.getDisplayName()) + " " + name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCloud(@Nonnull final AbstractEC2FleetCloud cloud) {
        this.cloud = cloud;
    }

    @Override
    public AbstractEC2FleetCloud getCloud() {
        return cloud;
    }

}
