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
 * The {@link FleetNodeComputer} represents the running state of {@link FleetNode} that holds executors.
 * @see hudson.model.Computer
 */
@ThreadSafe
public class FleetNodeComputer extends SlaveComputer {
    private static final Logger LOGGER = Logger.getLogger(FleetNodeComputer.class.getName());
    private boolean isMarkedForDeletion;

    public FleetNodeComputer(final FleetNode agent) {
        super(agent);
        this.isMarkedForDeletion = false;
    }

    public boolean isMarkedForDeletion() {
        return isMarkedForDeletion;
    }

    @Override
    public FleetNode getNode() {
        return (FleetNode) super.getNode();
    }

    @CheckForNull
    public String getInstanceId() {
        FleetNode node = getNode();
        return node == null ? null : node.getInstanceId();
    }

    public AbstractFleetCloud getCloud() {
        final FleetNode node = getNode();
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
        final FleetNode node = getNode();
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
        final FleetNode node = getNode();
        if (node != null) {
            final String instanceId = node.getInstanceId();
            final AbstractFleetCloud cloud = node.getCloud();
            if (cloud != null && StringUtils.isNotBlank(instanceId)) {
                cloud.scheduleToTerminate(instanceId, false, EC2AgentTerminationReason.AGENT_DELETED);
                // Persist a flag here as the cloud objects can be re-created on user-initiated changes, hence, losing track of instance ids scheduled to terminate.
                this.isMarkedForDeletion = true;
            }
        }
        return super.doDoDelete();
    }
}
