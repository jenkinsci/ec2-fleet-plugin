package com.amazon.jenkins.ec2fleet;

import hudson.EnvVars;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.DelegatingComputerLauncher;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.SlaveComputer;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * The {@link EC2FleetNodeComputer} represents the running state of {@link EC2FleetNode} that holds executors.
 * @see hudson.model.Computer
 */
@ThreadSafe
public class EC2FleetNodeComputer extends SlaveComputer {
    private static final Logger LOGGER = Logger.getLogger(EC2FleetNodeComputer.class.getName());
    private boolean isMarkedForDeletion;
    private volatile boolean hasConnectedSuccessfully;
    private transient ScheduledFuture<?> connectionFailureCheck;
    private transient boolean connectionFailureCheckScheduled;

    public EC2FleetNodeComputer(final EC2FleetNode agent) {
        super(agent);
        this.isMarkedForDeletion = false;
    }

    public boolean isMarkedForDeletion() {
        return isMarkedForDeletion;
    }

    boolean hasConnectedSuccessfully() {
        return hasConnectedSuccessfully;
    }

    synchronized void markConnectedSuccessfully() {
        hasConnectedSuccessfully = true;
        if (connectionFailureCheck != null) {
            connectionFailureCheck.cancel(false);
            connectionFailureCheck = null;
        }
    }

    synchronized void scheduleConnectionFailureCheck(
            final ScheduledExecutorService executor, final Runnable check, final long delayMillis) {
        if (hasConnectedSuccessfully || connectionFailureCheckScheduled) {
            return;
        }
        connectionFailureCheckScheduled = true;
        connectionFailureCheck = executor.schedule(() -> {
            synchronized (EC2FleetNodeComputer.this) {
                connectionFailureCheck = null;
                if (hasConnectedSuccessfully || isOnline()) {
                    return;
                }
            }
            check.run();
        }, delayMillis, TimeUnit.MILLISECONDS);
    }

    ComputerLauncher getBaseLauncher() {
        ComputerLauncher launcher = getLauncher();
        while (launcher instanceof DelegatingComputerLauncher) {
            launcher = ((DelegatingComputerLauncher) launcher).getLauncher();
        }
        return launcher;
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
                return String.format("%s Builds left: %d ", node.getDisplayName(), usesRemaining);
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
                cloud.scheduleToTerminate(instanceId, false, EC2AgentTerminationReason.AGENT_DELETED);
                // Persist a flag here as the cloud objects can be re-created on user-initiated changes, hence, losing
                // track of instance ids scheduled to terminate.
                this.isMarkedForDeletion = true;
            }
        }
        return super.doDoDelete();
    }
}
