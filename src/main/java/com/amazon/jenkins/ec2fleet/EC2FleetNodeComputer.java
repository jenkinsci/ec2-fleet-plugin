package com.amazon.jenkins.ec2fleet;

import hudson.slaves.SlaveComputer;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.HttpResponse;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * The {@link EC2FleetNodeComputer} represents the running state of {@link EC2FleetNode} that holds executors.
 * @see hudson.model.Computer
 */
@ThreadSafe
public class EC2FleetNodeComputer extends SlaveComputer {
    private static final Logger LOGGER = Logger.getLogger(EC2FleetNodeComputer.class.getName());
    private boolean isMarkedForDeletion;

    public EC2FleetNodeComputer(final EC2FleetNode agent) {
        super(agent);
        this.isMarkedForDeletion = false;
    }

    public boolean isMarkedForDeletion() {
        return isMarkedForDeletion;
    }

    @Override
    public EC2FleetNode getNode() {
        return (EC2FleetNode) super.getNode();
    }

    @CheckForNull
    public String getInstanceId() {
        EC2FleetNode node = getNode();
        return node == null ? null : node.getInstanceId();
    }

    public AbstractEC2FleetCloud getCloud() {
        final EC2FleetNode node = getNode();
        return node == null ? null : node.getCloud();
    }

    /**
     * Return label which will represent executor in "Build Executor Status"
     * section of Jenkins UI.
     *
     * @return Node's display name
     */
    @Nonnull
    @Override
    public String getDisplayName() {
        final EC2FleetNode node = getNode();
        if(node != null) {
            final int totalUses = node.getMaxTotalUses();
            if(totalUses != -1) {
                return String.format("%s Builds left: %d ", node.getDisplayName(), totalUses);
            }
            return node.getDisplayName();
        }
        return "unknown fleet" + " " + getName();
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
            final String instanceId = node.getInstanceId();
            final AbstractEC2FleetCloud cloud = node.getCloud();
            if (cloud != null && StringUtils.isNotBlank(instanceId)) {
                cloud.scheduleToTerminate(instanceId, false, EC2AgentTerminationReason.AGENT_DELETED);
                // Persist a flag here as the cloud objects can be re-created on user-initiated changes, hence, losing track of instance ids scheduled to terminate.
                this.isMarkedForDeletion = true;
            }
        }
        return super.doDoDelete();
    }
}
