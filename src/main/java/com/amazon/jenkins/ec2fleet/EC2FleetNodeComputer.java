package com.amazon.jenkins.ec2fleet;

import hudson.EnvVars;
import hudson.model.Node;
import hudson.slaves.Cloud;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@link EC2FleetNodeComputer} represents the running state of {@link EC2FleetNode} that holds executors.
 * @see hudson.model.Computer
 */
@ThreadSafe
public class EC2FleetNodeComputer extends SlaveComputer {
    private static final Logger LOGGER = Logger.getLogger(EC2FleetNodeComputer.class.getName());
    private boolean isMarkedForDeletion;
    /**
     * Set once the agent is scheduled for termination. Survives {@link #getNode()} returning null during
     * {@code removeNode}, which is when {@link EC2FleetAutoResubmitComputerLauncher} still needs the cloud.
     */
    private volatile String cloudName;
    private volatile String cachedDisplayName;
    private volatile boolean scheduledForTermination;
    private volatile EC2AgentTerminationReason terminationReason;
    private volatile boolean ignoreMinOnTermination;

    public EC2FleetNodeComputer(final EC2FleetNode agent) {
        super(agent);
        this.isMarkedForDeletion = false;
        this.cloudName = agent.getCloudName();
        this.cachedDisplayName = agent.getDisplayName();
    }

    @Override
    protected void setNode(final Node node) {
        super.setNode(node);
        if (node instanceof EC2FleetNode) {
            final EC2FleetNode fleetNode = (EC2FleetNode) node;
            if (fleetNode.getCloudName() != null) {
                this.cloudName = fleetNode.getCloudName();
            }
        }
    }

    public String getCloudName() {
        final EC2FleetNode node = getNode();
        if (node != null && node.getCloudName() != null) {
            cloudName = node.getCloudName();
        }
        return cloudName;
    }

    public boolean isScheduledForTermination() {
        return scheduledForTermination;
    }

    public EC2AgentTerminationReason getTerminationReason() {
        return terminationReason == null ? EC2AgentTerminationReason.IDLE_FOR_TOO_LONG : terminationReason;
    }

    public boolean isIgnoreMinOnTermination() {
        return ignoreMinOnTermination;
    }

    /**
     * Fence the agent as soon as termination is decided. The queue lock is held by
     * {@link hudson.slaves.ComputerRetentionWork} around {@link EC2RetentionStrategy#check}, so this takes effect
     * before another task can be assigned. Temporary offline keeps the channel up for a build that is already
     * running, and is checked by both heavyweight and flyweight assignment.
     */
    public void suspendForTermination(final EC2AgentTerminationReason reason, final boolean ignoreMinConstraints) {
        this.terminationReason = reason == null ? EC2AgentTerminationReason.IDLE_FOR_TOO_LONG : reason;
        this.ignoreMinOnTermination = ignoreMinConstraints;
        this.scheduledForTermination = true;
        setAcceptingTasks(false);
        if (getNode() != null && !isTemporarilyOffline()) {
            try {
                setTemporaryOfflineCause(new ScheduledForTerminationOfflineCause(this.terminationReason.getDescription()));
            } catch (RuntimeException ex) {
                LOGGER.log(Level.WARNING, "Failed to mark node offline for termination: " + getName(), ex);
            }
        }
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
        if (node != null) {
            if (node.getCloudName() != null) {
                cloudName = node.getCloudName();
            }
            return node.getCloud();
        }
        // removeNode drops the node before afterDisconnect runs, so resolve the cloud that launched this agent.
        return lookupCloud(cloudName);
    }

    @CheckForNull
    private static AbstractEC2FleetCloud lookupCloud(final String name) {
        if (name == null) {
            return null;
        }
        final Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            return null;
        }
        final Cloud cloud = jenkins.getCloud(name);
        return cloud instanceof AbstractEC2FleetCloud ? (AbstractEC2FleetCloud) cloud : null;
    }

    @Nonnull
    public Map<String, String> getConfiguredEnvironmentVariables() {
        if (!hasPermission(CONFIGURE)) {
            return Collections.emptyMap();
        }

        final EC2FleetNode node = getNode();
        if (node == null) {
            return Collections.emptyMap();
        }

        final EnvironmentVariablesNodeProperty environmentVariablesNodeProperty =
                node.getNodeProperties().get(EnvironmentVariablesNodeProperty.class);
        if (environmentVariablesNodeProperty == null) {
            return Collections.emptyMap();
        }

        final EnvVars envVars = environmentVariablesNodeProperty.getEnvVars();
        if (envVars == null || envVars.isEmpty()) {
            return Collections.emptyMap();
        }
        return new LinkedHashMap<>(envVars);
    }

    public boolean isConfiguredEnvironmentVariablesVisible() {
        return hasPermission(CONFIGURE) && !getConfiguredEnvironmentVariables().isEmpty();
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
        if (node != null) {
            final int usesRemaining = node.getUsesRemaining();
            if (usesRemaining >= 0) {
                cachedDisplayName = String.format("%s Builds left: %d ", node.getDisplayName(), usesRemaining);
            } else {
                cachedDisplayName = node.getDisplayName();
            }
            return cachedDisplayName;
        }
        if (cachedDisplayName != null) {
            return cachedDisplayName;
        }
        return "unknown fleet" + " " + getName();
    }

    /**
     * When the agent is deleted, schedule EC2 instance for termination
     *
     * @return HttpResponse
     */
    @RequirePOST
    @Override
    public HttpResponse doDoDelete() throws IOException {
        checkPermission(DELETE);
        final EC2FleetNode node = getNode();
        if (node != null) {
            final String instanceId = node.getInstanceId();
            final AbstractEC2FleetCloud cloud = node.getCloud();
            if (cloud != null && StringUtils.isNotBlank(instanceId)) {
                // Suspend the computer before scheduling so the queue cannot dispatch new work to it
                // between now and when the cloud's next update cycle terminates the instance on EC2.
                setAcceptingTasks(false);
                if (cloud.scheduleToTerminate(instanceId, false, EC2AgentTerminationReason.AGENT_DELETED)) {
                    suspendForTermination(EC2AgentTerminationReason.AGENT_DELETED, false);
                }
                // Persist a flag here as the cloud objects can be re-created on user-initiated changes, hence, losing track of instance ids scheduled to terminate.
                this.isMarkedForDeletion = true;
            }
        }
        return super.doDoDelete();
    }
}
