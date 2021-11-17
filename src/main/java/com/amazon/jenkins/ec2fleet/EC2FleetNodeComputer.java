package com.amazon.jenkins.ec2fleet;

import hudson.model.Slave;
import hudson.slaves.SlaveComputer;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.HttpResponse;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;

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
        if(cloud != null) {
            final String displayName = String.format("%s %s", cloud.getDisplayName(), name);
            final EC2FleetNode node = getNode();
            if(node != null) {
                final int totalUses = node.getMaxTotalUses();
                if(totalUses != -1) {
                    return String.format("%s Builds left: %d ", displayName, totalUses);
                }
            }
            return displayName;
        }
        // in some multi-thread edge cases cloud could be null for some time, just be ok with that
        return "unknown fleet" + " " + name;
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

    /**
     * When the agent is deleted, schedule EC2 instance for termination
     *
     * @return HttpResponse
     */
    @Override
    public HttpResponse doDoDelete() throws IOException {
        checkPermission(DELETE);
        final EC2FleetNode node = getNode();
        if (node != null) {
            final String instanceId = node.getNodeName();
            final AbstractEC2FleetCloud cloud = node.getCloud();
            if (cloud != null && StringUtils.isNotBlank(instanceId)) {
                cloud.scheduleToTerminate(instanceId, false);
            }
        }
        return super.doDoDelete();
    }
}
