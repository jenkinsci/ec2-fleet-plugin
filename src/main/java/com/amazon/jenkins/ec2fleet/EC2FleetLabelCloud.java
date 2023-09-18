package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.aws.AwsPermissionChecker;
import com.amazon.jenkins.ec2fleet.aws.CloudFormationApi;
import com.amazon.jenkins.ec2fleet.aws.EC2Api;
import com.amazon.jenkins.ec2fleet.aws.RegionHelper;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleets;
import com.amazon.jenkins.ec2fleet.fleet.EC2SpotFleet;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsHelper;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Failure;
import hudson.model.Item;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.slaves.Cloud;
import hudson.slaves.ComputerConnector;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodeProvisioner;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

import static com.amazon.jenkins.ec2fleet.CloudConstants.*;

/**
 * @see CloudNanny
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class EC2FleetLabelCloud extends AbstractEC2FleetCloud {

    public static final String BASE_DEFAULT_FLEET_CLOUD_ID = "FleetCloudLabel";

//    private static final String NEW_EC2_KEY_PAIR_VALUE = "- New Key Pair -";

    private static final SimpleFormatter sf = new SimpleFormatter();
    private static final Logger LOGGER = Logger.getLogger(EC2FleetLabelCloud.class.getName());

    private String awsCredentialsId;
    private final String region;
    private String endpoint;

    private String fsRoot;
    private final ComputerConnector computerConnector;
    private boolean privateIpUsed = DEFAULT_PRIVATE_IP_USED;
    private boolean alwaysReconnect = DEFAULT_ALWAYS_RECONNECT;
    private int idleMinutes = DEFAULT_IDLE_MINUTES;
    private int minSize = DEFAULT_MIN_SIZE;
    private int maxSize = DEFAULT_MAX_SIZE;
    private int numExecutors = DEFAULT_NUM_EXECUTORS;
    private boolean restrictUsage = DEFAULT_RESTRICT_USAGE;
    private Integer initOnlineTimeoutSec = DEFAULT_INIT_ONLINE_TIMEOUT_SEC;
    private Integer initOnlineCheckIntervalSec = DEFAULT_INIT_ONLINE_CHECK_INTERVAL_SEC;
    private Integer cloudStatusIntervalSec = DEFAULT_CLOUD_STATUS_INTERVAL_SEC;
    private final String ec2KeyPairName;

    /**
     * @see EC2FleetAutoResubmitComputerLauncher
     */
    private boolean disableTaskResubmit;

    /**
     * @see NoDelayProvisionStrategy
     */
    private boolean noDelayProvision;

    private transient Map<String, State> states;

    @DataBoundConstructor
    public EC2FleetLabelCloud(@Nonnull final String name,
                              final String region,
                              final ComputerConnector computerConnector,
                              final String ec2KeyPairName) {
        super(StringUtils.isNotBlank(name) ? name : CloudNames.generateUnique(BASE_DEFAULT_FLEET_CLOUD_ID));
        init();
        this.region = region;
        this.computerConnector = computerConnector;
        this.ec2KeyPairName = ec2KeyPairName;
    }

    @Deprecated
    public EC2FleetLabelCloud(final String name,
                              final String awsCredentialsId,
                              final String region,
                              final String endpoint,
                              final String fsRoot,
                              final ComputerConnector computerConnector,
                              final boolean privateIpUsed,
                              final boolean alwaysReconnect,
                              final int idleMinutes,
                              final Integer minSize,
                              final Integer maxSize,
                              final Integer numExecutors,
                              final boolean restrictUsage,
                              final boolean disableTaskResubmit,
                              final Integer initOnlineTimeoutSec,
                              final Integer initOnlineCheckIntervalSec,
                              final Integer cloudStatusIntervalSec,
                              final boolean noDelayProvision,
                              final String ec2KeyPairName) {
        this(name, region, computerConnector, ec2KeyPairName);
        setAwsCredentialsId(awsCredentialsId);
        setEndpoint(endpoint);
        setFsRoot(fsRoot);
        setPrivateIpUsed(privateIpUsed);
        setAlwaysReconnect(alwaysReconnect);
        setIdleMinutes(idleMinutes);
        setMinSize(minSize);
        setMaxSize(maxSize);
        setNumExecutors(numExecutors);
        setRestrictUsage(restrictUsage);
        setDisableTaskResubmit(disableTaskResubmit);
        setInitOnlineTimeoutSec(initOnlineTimeoutSec);
        setInitOnlineCheckIntervalSec(initOnlineCheckIntervalSec);
        setCloudStatusIntervalSec(cloudStatusIntervalSec);
        setNoDelayProvision(noDelayProvision);
    }

    public String getEc2KeyPairName() {
        return ec2KeyPairName;
    }

    public boolean isNoDelayProvision() {
        return noDelayProvision;
    }

    @DataBoundSetter
    public void setNoDelayProvision(boolean noDelayProvision) {
        this.noDelayProvision = noDelayProvision;
    }

    public String getAwsCredentialsId() {
        return awsCredentialsId;
    }

    @DataBoundSetter
    public void setAwsCredentialsId(String awsCredentialsId) {
        this.awsCredentialsId = awsCredentialsId;
    }

    public boolean isDisableTaskResubmit() {
        return disableTaskResubmit;
    }

    @DataBoundSetter
    public void setDisableTaskResubmit(boolean disableTaskResubmit) {
        this.disableTaskResubmit = disableTaskResubmit;
    }

    public int getInitOnlineTimeoutSec() {
        return initOnlineTimeoutSec;
    }

    @DataBoundSetter
    public void setInitOnlineTimeoutSec(Integer initOnlineTimeoutSec) {
        this.initOnlineTimeoutSec = initOnlineTimeoutSec;
    }

    public int getCloudStatusIntervalSec() {
        return cloudStatusIntervalSec;
    }

    @DataBoundSetter
    public void setCloudStatusIntervalSec(Integer cloudStatusIntervalSec) {
        this.cloudStatusIntervalSec = cloudStatusIntervalSec;
    }

    public int getInitOnlineCheckIntervalSec() {
        return initOnlineCheckIntervalSec;
    }

    @DataBoundSetter
    public void setInitOnlineCheckIntervalSec(Integer initOnlineCheckIntervalSec) {
        this.initOnlineCheckIntervalSec = initOnlineCheckIntervalSec;
    }

    public String getRegion() {
        return region;
    }

    public String getEndpoint() {
        return endpoint;
    }

    @DataBoundSetter
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getFsRoot() {
        return fsRoot;
    }

    @DataBoundSetter
    public void setFsRoot(String fsRoot) {
        this.fsRoot = fsRoot;
    }

    public ComputerConnector getComputerConnector() {
        return computerConnector;
    }

    public boolean isPrivateIpUsed() {
        return privateIpUsed;
    }

    @DataBoundSetter
    public void setPrivateIpUsed(boolean privateIpUsed) {
        this.privateIpUsed = privateIpUsed;
    }

    public boolean isAlwaysReconnect() {
        return alwaysReconnect;
    }

    @DataBoundSetter
    public void setAlwaysReconnect(boolean alwaysReconnect) {
        this.alwaysReconnect = alwaysReconnect;
    }

    public int getIdleMinutes() {
        return idleMinutes;
    }

    @DataBoundSetter
    public void setIdleMinutes(int idleMinutes) {
        this.idleMinutes = Math.max(0, idleMinutes);
    }

    public int getMaxSize() {
        return maxSize;
    }

    @DataBoundSetter
    public void setMaxSize(int maxSize) {
        if (maxSize < minSize) {
            int newMaxSize = Math.max(1, minSize);
            warning("Cloud parameter 'maxSize' can't be less than 'minSize' or 1, setting to %d", newMaxSize);
            maxSize = newMaxSize;
        }
        this.maxSize = Math.max(1, maxSize);
    }

    public int getMinSize() {
        return minSize;
    }

    @DataBoundSetter
    public void setMinSize(int minSize) {
        if (minSize < 0) {
            warning("Cloud parameter 'minSize' can't be less than 0, setting to 0");
        }
        minSize = Math.max(0, minSize);
        //TODO: This validation is only in place for unit tests since constructor is run twice on CasC load but not
        // for unit tests
        if (minSize > maxSize) {
            warning("Cloud parameter 'minSize' cannot be greater than 'maxSize', setting 'maxSize' to %d. " +
                    "Ignore this if caused after a CasC load.", minSize);
            this.maxSize = minSize;
        }
        this.minSize = minSize;
    }

    public int getNumExecutors() {
        return numExecutors;
    }

    @DataBoundSetter
    public void setNumExecutors(int numExecutors) {
        this.numExecutors = Math.max(numExecutors, 1);
    }

    public String getJvmSettings() {
        return "";
    }

    public boolean isRestrictUsage() {
        return restrictUsage;
    }

    @DataBoundSetter
    public void setRestrictUsage(boolean restrictUsage) {
        this.restrictUsage = restrictUsage;
    }

    @Override
    public synchronized boolean hasExcessCapacity() {
        //  TODO: Check if the current count of instances are greater than allowed
        return Boolean.FALSE;
    }

//    @VisibleForTesting
//    synchronized Set<NodeProvisioner.PlannedNode> getPlannedNodesCache() {
//        return plannedNodesCache;
//    }

//    @VisibleForTesting
//    synchronized Set<String> getInstanceIdsToTerminate() {
//        return instanceIdsToTerminate;
//    }

//    @VisibleForTesting
//    synchronized int getToAdd() {
//        return toAdd;
//    }

//    @VisibleForTesting
//    synchronized FleetStateStats getStats() {
//        return stats;
//    }

//    @VisibleForTesting
//    synchronized void setStats(final FleetStateStats stats) {
//        this.stats = stats;
//    }

    @Override
    public synchronized Collection<NodeProvisioner.PlannedNode> provision(@Nonnull final Cloud.CloudState cloudState, int excessWorkload) {
        Jenkins jenkinsInstance = Jenkins.get();
        if (jenkinsInstance.isQuietingDown()) {
            LOGGER.log(Level.FINE, "Not provisioning nodes, Jenkins instance is quieting down");
            return Collections.emptyList();
        } else if (jenkinsInstance.isTerminating()) {
            LOGGER.log(Level.FINE, "Not provisioning nodes, Jenkins instance is terminating");
            return Collections.emptyList();
        }

        info("excessWorkload %s", excessWorkload);

        final Label label = cloudState.getLabel();
        List<NodeProvisioner.PlannedNode> r = new ArrayList<>();

        for (Map.Entry<String, State> state : states.entrySet()) {
            if (Label.parse(state.getKey()).containsAll(label.listAtoms())) {
                LOGGER.info("provision " + label + " excessWorkload " + excessWorkload);

                final FleetStateStats stats = state.getValue().stats;
                final int cap = stats.getNumDesired() + state.getValue().toAdd;

                if (!stats.getState().isActive()) {
                    info("fleet in %s not active state", stats.getState().getDetailed());
                    continue;
                }

                final EC2FleetLabelParameters parameters = new EC2FleetLabelParameters(state.getKey());

                final int maxSize = parameters.getIntOrDefault("maxSize", this.maxSize);
                if (cap >= maxSize) {
                    info("max %s reached, no more provision", maxSize);
                    continue;
                }

                // if the planned node has 0 executors configured force it to 1 so we end up doing an unweighted check
                final int numExecutors1 = Math.max(parameters.getIntOrDefault("numExecutors", numExecutors), 1);

                // Calculate the ceiling, without having to work with doubles from Math.ceil
                // https://stackoverflow.com/a/21830188/877024
                final int weightedExcessWorkload = (excessWorkload + numExecutors1 - 1) / numExecutors1;
                int targetCapacity = Math.min(cap + weightedExcessWorkload, maxSize);

                int toProvision = targetCapacity - cap;
                info("to provision = %s", toProvision);

                if (toProvision < 1) return Collections.emptyList();

                state.getValue().toAdd += toProvision;

                for (int f = 0; f < toProvision; ++f) {
                    // todo make name unique per fleet
                    final NodeProvisioner.PlannedNode plannedNode = new NodeProvisioner.PlannedNode(
                            "FleetNode-" + r.size(), new CompletableFuture<>(), this.numExecutors);
                    r.add(plannedNode);
                    state.getValue().plannedNodes.add(plannedNode);
                }
            }
        }

        return r;
    }

    private static class State {
        final String fleetId;
        FleetStateStats stats;
        int targetCapacity;
        int toAdd;
        final Set<NodeProvisioner.PlannedNode> plannedNodes;
        final Set<NodeProvisioner.PlannedNode> plannedNodesToRemove;
        final Map<String, EC2AgentTerminationReason> instanceIdsToTerminate;

        public State(String fleetId) {
            this.fleetId = fleetId;
            this.plannedNodes = new HashSet<>();
            this.plannedNodesToRemove = new HashSet<>();
            this.instanceIdsToTerminate = new HashMap<>();
        }

        public State(State state) {
            this.plannedNodes = new HashSet<>(state.plannedNodes);
            this.fleetId = state.fleetId;
            this.stats = state.stats;
            this.targetCapacity = state.targetCapacity;
            this.toAdd = state.toAdd;
            this.plannedNodesToRemove = new HashSet<>(state.plannedNodesToRemove);
            this.instanceIdsToTerminate = new HashMap<>(state.instanceIdsToTerminate);
        }
    }

    public void update() {
        info("start");

        final Map<String, State> currentStates;
        synchronized (this) {
            currentStates = new HashMap<>();
            for (Map.Entry<String, State> state : states.entrySet()) {
                currentStates.put(state.getKey(), new State(state.getValue()));
            }
        }

        final Set<String> fleetIds = new HashSet<>();
        for (State state : states.values()) fleetIds.add(state.fleetId);
        final Map<String, FleetStateStats> currentStats = new EC2SpotFleet().getStateBatch(
                getAwsCredentialsId(), region, endpoint, fleetIds);
        for (State state : currentStates.values()) {
            // todo what if we don't find this fleet in map
            state.stats = currentStats.get(state.fleetId);

            state.targetCapacity = Math.max(0,
                    state.stats.getNumDesired() - state.instanceIdsToTerminate.size() + state.toAdd);
            state.stats = new FleetStateStats(state.stats, state.targetCapacity);
        }

        updateByState(currentStates);

        synchronized (this) {
            for (Map.Entry<String, State> entry : currentStates.entrySet()) {
                final State state = states.get(entry.getKey());

                state.stats = entry.getValue().stats;
                state.instanceIdsToTerminate.keySet().removeAll(entry.getValue().instanceIdsToTerminate.keySet());
                // toAdd only grow outside of this method, so we can subtract
                state.toAdd = state.toAdd - entry.getValue().toAdd;
                // remove released planned nodes
                state.plannedNodes.removeAll(entry.getValue().plannedNodesToRemove);
                // limit planned pool according to real target capacity
                while (state.plannedNodes.size() > entry.getValue().targetCapacity) {
                    final Iterator<NodeProvisioner.PlannedNode> iterator = state.plannedNodes.iterator();
                    final NodeProvisioner.PlannedNode plannedNodeToCancel = iterator.next();
                    iterator.remove();
                    // cancel to let jenkins no that node is not valid any more
                    plannedNodeToCancel.future.cancel(true);
                }
            }
        }
    }

    private void updateByState(final Map<String, State> states) {
        final Jenkins jenkins = Jenkins.get();

        final AmazonEC2 ec2 = Registry.getEc2Api().connect(getAwsCredentialsId(), region, endpoint);

        for (State state : states.values()) {
            if (state.toAdd > 0 || state.instanceIdsToTerminate.size() > 0) {
                // todo fix negative value
                // we do update any time even real capacity was not update like remove one add one to
                // update fleet settings with NoTermination so we can terminate instances on our own
                EC2Fleets.get(state.fleetId).modify(
                        getAwsCredentialsId(), region, endpoint, state.fleetId, state.targetCapacity, minSize, maxSize);
                info("Update fleet target capacity to %s", state.targetCapacity);
            }
        }

        final Map<String, EC2AgentTerminationReason> instanceIdsToRemove = new HashMap<>();
        for (State state : states.values()) {
            instanceIdsToRemove.putAll(state.instanceIdsToTerminate);
        }

        if (instanceIdsToRemove.size() > 0) {
            // internally removeNode lock on queue to correctly update node list
            // we do big block for all removal to avoid delay on lock waiting
            // for each node
            Queue.withLock(new Runnable() {
                @Override
                public void run() {
                    info("Removing Jenkins nodes before terminating corresponding EC2 instances");
                    for (final String instanceId : instanceIdsToRemove.keySet()) {
                        final Node node = jenkins.getNode(instanceId);
                        if (node != null) {
                            try {
                                jenkins.removeNode(node);
                            } catch (IOException e) {
                                warning("unable to remove node %s from Jenkins, skip, just terminate EC2 instance", instanceId);
                            }
                        }
                    }
                }
            });
            info("Delete terminating nodes from Jenkins %s", instanceIdsToRemove);

            Registry.getEc2Api().terminateInstances(ec2, instanceIdsToRemove.keySet());
            info("Instances %s were terminated with result", instanceIdsToRemove);
        }

        for (final Map.Entry<String, State> entry : states.entrySet()) {
            final State state = entry.getValue();
            info("fleet instances %s", state.stats.getInstances());

            // Set up the lists of Jenkins nodes and fleet instances
            // currentFleetInstances contains instances currently in the fleet
            final Set<String> fleetInstances = new HashSet<>(state.stats.getInstances());

            final Map<String, Instance> described = Registry.getEc2Api().describeInstances(ec2, fleetInstances);
            info("described instances %s", described.keySet());

            // currentJenkinsNodes contains all registered Jenkins nodes related to this cloud
            final Set<String> jenkinsInstances = new HashSet<>();
            for (final Node node : jenkins.getNodes()) {
                if (node instanceof EC2FleetNode) {
                    final EC2FleetNode node1 = (EC2FleetNode) node;
                    // cloud and label are same
                    if (node1.getCloud() == this && node1.getLabelString().equals(entry.getKey())) {
                        jenkinsInstances.add(node.getNodeName());
                    }
                }
            }
            info("jenkins nodes %s", jenkinsInstances);

            // contains Jenkins nodes that were once fleet instances but are no longer in the fleet
            final Set<String> jenkinsNodesWithInstance = new HashSet<>(jenkinsInstances);
            jenkinsNodesWithInstance.removeAll(fleetInstances);
            info("jenkins nodes without instance %s", jenkinsNodesWithInstance);

            // terminatedFleetInstances contains fleet instances that are terminated, stopped, stopping, or shutting down
            final Set<String> terminatedFleetInstances = new HashSet<>(fleetInstances);
            // terminated are any current which cannot be described
            terminatedFleetInstances.removeAll(described.keySet());
            info("terminated instances " + terminatedFleetInstances);

            // newFleetInstances contains running fleet instances that are not already Jenkins nodes
            final Map<String, Instance> newFleetInstances = new HashMap<>(described);
            for (final String instanceId : jenkinsInstances) newFleetInstances.remove(instanceId);
            info("new instances " + newFleetInstances.keySet());

            // update caches
            final List<String> jenkinsNodesToRemove = new ArrayList<>();
            jenkinsNodesToRemove.addAll(terminatedFleetInstances);
            jenkinsNodesToRemove.addAll(jenkinsNodesWithInstance);
            // Remove dying fleet instances from Jenkins
            for (final String instance : jenkinsNodesToRemove) {
//                info("Fleet (" + getLabelString() + ") no longer has the instance " + instance + ", removing from Jenkins.");
                JenkinsUtils.removeNode(instance);
            }

            // Update the label for all Jenkins nodes in the fleet instance cache
//            for (final String instanceId : jenkinsInstances) {
//                final Node node = jenkins.getNode(instanceId);
//                if (node == null) continue;
//
//                if (!labelString.equals(node.getLabelString())) {
//                    try {
//                        info("Updating label on node %s to \"%s\".", instanceId, labelString);
//                        node.setLabelString(labelString);
//                    } catch (final Exception ex) {
//                        warning(ex, "Unable to set label on node %s", instanceId);
//                    }
//                }
//            }

            // If we have new instances - create nodes for them!
            if (newFleetInstances.size() > 0) {
                // we tag new instances to help users to identify instances launched from plugin managed fleets
                // if failed we are fine to skip this call
                try {
                    Registry.getEc2Api().tagInstances(ec2, newFleetInstances.keySet(),
                            EC2_INSTANCE_CLOUD_NAME_TAG, name);
                } catch (final Exception e) {
                    warning(e, "failed to tag new instances %s, skip", newFleetInstances.keySet());
                }

                // addNewAgent will call addNode which call queue lock
                // we speed up this by getting one lock for all nodes to all
                Queue.withLock(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            for (final Instance instance : newFleetInstances.values()) {
                                addNewAgent(ec2, instance, entry.getKey(), state);
                            }
                        } catch (final Exception ex) {
                            warning(ex, "Unable to set label on node");
                        }
                    }
                });
            }
        }
    }

    @Override
    public synchronized boolean scheduleToTerminate(final String instanceId, final boolean ignoreMinConstraints,
                                                    final EC2AgentTerminationReason terminationReason) {
        info("Attempting to terminate instance: %s", instanceId);

        final Node node = Jenkins.get().getNode(instanceId);
        if (node == null) return false;

        final State state = states.get(node.getLabelString());
        if (state == null) {
            info("Skip termination, unknown label " + node.getLabelString() + " for node " + instanceId);
            return false;
        }

        // We can't remove instances beyond minSize unless ignoreMinConstraints is true
        final EC2FleetLabelParameters parameters = new EC2FleetLabelParameters(node.getLabelString());
        final int minSize = parameters.getIntOrDefault("minSize", this.minSize);
        if (!ignoreMinConstraints && (minSize > 0 && state.stats.getNumDesired() - state.instanceIdsToTerminate.size() <= minSize)) {
            info("Not terminating %s because we need a minimum of %s instances running.", instanceId, minSize);
            return false;
        }
        info("Scheduling instance '%s' for termination on cloud %s because of reason: %s", instanceId, this, terminationReason);
        state.instanceIdsToTerminate.put(instanceId, terminationReason);
        return true;
    }

    // sync as we are using modifiable state
    @Override
    public synchronized boolean canProvision(final Cloud.CloudState cloudState) {
        final Label label = cloudState.getLabel();
        for (String labelString : states.keySet()) {
            final boolean r = label == null || Label.parse(labelString).containsAll(label.listAtoms());
            fine("CanProvision called on fleet: \"" + labelString + "\" wanting: \"" + (label == null ? "(unspecified)" : label.getName()) + "\". Returning " + r + ".");
            if (r) return true;
        }
        return false;
    }

    private Object readResolve() {
        init();
        return this;
    }

    private void init() {
        states = new HashMap<>();
    }

    private void addNewAgent(
            final AmazonEC2 ec2, final Instance instance, final String labelString, final State state) throws Exception {
        final String instanceId = instance.getInstanceId();

        // instance state check enabled and not running, skip adding
        if (InstanceStateName.Running != InstanceStateName.fromValue(instance.getState().getName()))
            return;

        final String address = privateIpUsed ? instance.getPrivateIpAddress() : instance.getPublicIpAddress();
        // Check if we have the address to use. Nodes don't get it immediately.
        if (address == null) {
            if (!privateIpUsed) {
                info("%s instance public IP address not assigned, it could take some time or" +
                        " Spot Request is not configured to assign public IPs", instance.getInstanceId());
            }
            return; // wait more time, probably IP address not yet assigned
        }

        // Generate a random FS root if one isn't specified
        final String effectiveFsRoot;
        if (StringUtils.isBlank(fsRoot)) {
            effectiveFsRoot = "/tmp/jenkins-" + UUID.randomUUID().toString().substring(0, 8);
        } else {
            effectiveFsRoot = fsRoot;
        }

        final Double instanceTypeWeight = state.stats.getInstanceTypeWeights().get(instance.getInstanceType());
        final int effectiveNumExecutors;
        // todo add scaleExecutorsByWeight
        if (instanceTypeWeight != null) {
            effectiveNumExecutors = (int) Math.max(Math.round(numExecutors * instanceTypeWeight), 1);
        } else {
            effectiveNumExecutors = numExecutors;
        }

        final EC2FleetAutoResubmitComputerLauncher computerLauncher = new EC2FleetAutoResubmitComputerLauncher(
                computerConnector.launch(address, TaskListener.NULL));
        final Node.Mode nodeMode = restrictUsage ? Node.Mode.EXCLUSIVE : Node.Mode.NORMAL;
        //TODO: Add maxTotalUses to EC2FleetLabelCloud similar to EC2FleetCloud
        final EC2FleetNode node = new EC2FleetNode(instanceId, "Fleet agent for " + instanceId,
                effectiveFsRoot, effectiveNumExecutors, nodeMode, labelString, new ArrayList<NodeProperty<?>>(),
                this.name, computerLauncher, -1);

        // Initialize our retention strategy
        node.setRetentionStrategy(new EC2RetentionStrategy());

        final Jenkins jenkins = Jenkins.get();
        // jenkins automatically remove old node with same name if any
        jenkins.addNode(node);

        final CompletableFuture<Node> future;
        if (state.plannedNodes.isEmpty()) {
            future = new CompletableFuture<>();
        } else {
            final Iterator<NodeProvisioner.PlannedNode> iterator = state.plannedNodes.iterator();
            final NodeProvisioner.PlannedNode plannedNode = iterator.next();
            // we remove for list as it could be multiple nodes added in one update
            iterator.remove();
            state.plannedNodesToRemove.add(plannedNode);
            future = ((CompletableFuture<Node>) plannedNode.future);
        }

        // use getters for timeout and interval as they provide default value
        // when user just install new version and did't recreate fleet
        EC2FleetOnlineChecker.start(node, future,
                TimeUnit.SECONDS.toMillis(getInitOnlineTimeoutSec()),
                TimeUnit.SECONDS.toMillis(getInitOnlineCheckIntervalSec()));
    }

    private String getLogPrefix() {
        return getDisplayName() + " ";
    }

    private void info(final String msg, final Object... args) {
        LOGGER.info(getLogPrefix() + String.format(msg, args));
    }

    private void fine(final String msg, final Object... args) {
        LOGGER.fine(getLogPrefix() + String.format(msg, args));
    }

    private void warning(final String msg, final Object... args) {
        LOGGER.warning(getLogPrefix() + String.format(msg, args));
    }

    private void warning(final Throwable t, final String msg, final Object... args) {
        LOGGER.log(Level.WARNING, getLogPrefix() + String.format(msg, args), t);
    }

    // todo to collection utils
    private static <T> Set<T> missed(Set<T> what, Set<T> where) {
        final Set<T> m = new HashSet<>();
        for (T item : what) {
            if (!where.contains(item)) m.add(item);
        }
        return m;
    }

    public void updateStacks() {
//        if (NEW_EC2_KEY_PAIR_VALUE.equals(ec2KeyPairName)) {
//            // need to create key first
//            final AmazonEC2 amazonEC2 = Registry.getEc2Api().connect(awsCredentialsId, region, endpoint);
//            final CreateKeyPairResult result = amazonEC2.createKeyPair(new CreateKeyPairRequest().withKeyName(
//                    "ec2-fleet-plugin-" + new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss").format(new Date())));
//            Jenkins.getActiveInstance().cre
//            ec2KeyPairName = result.getKeyPair().getKeyName();
//        }

        final Jenkins jenkins = Jenkins.get();
        final CloudFormationApi cloudFormationApi = Registry.getCloudFormationApi();
        final AmazonCloudFormation client = cloudFormationApi.connect(awsCredentialsId, region, endpoint);
        final Map<String, CloudFormationApi.StackInfo> allStacks = cloudFormationApi.describe(client, name);

        // labels
        final Set<String> labels = new HashSet<>();
        for (final Item item : Jenkins.get().getAllItems()) {
            if (!(item instanceof AbstractProject)) continue;
            final AbstractProject abstractProject = (AbstractProject) item;
            // assinged label could be null
            final String labelString = StringUtils.defaultString(abstractProject.getAssignedLabelString());

            if (labelString.startsWith(name)) {
                labels.add(labelString);
            }
        }

        Set<String> labelsWithoutStacks = missed(labels, allStacks.keySet());
        Set<String> stacksWithoutLabels = missed(allStacks.keySet(), labels);

        final Set<String> runningStacksWithLabels = new HashSet<>();
        for (Map.Entry<String, CloudFormationApi.StackInfo> stack : allStacks.entrySet()) {
            if (labels.contains(stack.getKey()) && stack.getValue().stackStatus == StackStatus.CREATE_COMPLETE) {
                runningStacksWithLabels.add(stack.getKey());
            }
        }
        LOGGER.info("running stacks " + runningStacksWithLabels);

        // sync with stacks

        // new stacks
        for (final String label : labelsWithoutStacks) {
            LOGGER.info("creating stack for label " + label);
            cloudFormationApi.create(client, name, ec2KeyPairName, label);
        }

        // delete unused stacks
        for (final String label : stacksWithoutLabels) {
            final CloudFormationApi.StackInfo stack = allStacks.get(label);
            if (stack.stackStatus == StackStatus.CREATE_COMPLETE) {
                LOGGER.info("deleting unused stack " + stack.stackId + " for label " + label);
                cloudFormationApi.delete(client, stack.stackId);

                // delete all nodes which belongs to this stack
                final List<String> instanceIdsToRemove = new ArrayList<>();
                Queue.withLock(new Runnable() {
                    @Override
                    public void run() {
                        for (final Node node : jenkins.getNodes()) {
                            if (label.equals(node.getLabelString())) {
                                final String instanceId = node.getNodeName();
                                instanceIdsToRemove.add(instanceId);
                                try {
                                    jenkins.removeNode(node);
                                } catch (IOException e) {
                                    warning("unable delete node %s from Jenkins, skip, " +
                                            "actual instance will be terminated by stack", instanceId);
                                }
                            }
                        }
                    }
                });
                info("Delete nodes from deleted stack from Jenkins %s", instanceIdsToRemove);
            } else {
                LOGGER.info("unused stack " + stack.stackId + " for label " + label
                        + " is status " + stack.stackStatus + ", skip to delete");
            }
        }

        synchronized (this) {
            // sync states

            // add states with new stack
            for (final String label : runningStacksWithLabels) {
                if (!states.containsKey(label)) {
                    final CloudFormationApi.StackInfo stack = allStacks.get(label);
                    states.put(label, new State(stack.fleetId));
                }
            }

            // remove states without stack
            Iterator<Map.Entry<String, State>> iterator = states.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, State> state = iterator.next();
                if (!runningStacksWithLabels.contains(state.getKey())) {
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public EC2FleetLabelCloud.DescriptorImpl getDescriptor() {
        return (EC2FleetLabelCloud.DescriptorImpl) super.getDescriptor();
    }

    @Extension
    @SuppressWarnings("unused")
    public static class DescriptorImpl extends Descriptor<Cloud> {

        public DescriptorImpl() {
            super();
            load();
        }

        @Override
        public String getDisplayName() {
            return "Amazon EC2 Fleet label based";
        }

        public List getComputerConnectorDescriptors() {
            return Jenkins.get().getDescriptorList(ComputerConnector.class);
        }

        public ListBoxModel doFillAwsCredentialsIdItems() {
            return AWSCredentialsHelper.doFillCredentialsIdItems(Jenkins.get());
        }

        public ListBoxModel doFillRegionItems(@QueryParameter final String awsCredentialsId) {
            return RegionHelper.getRegionsListBoxModel(awsCredentialsId);
        }

        public ListBoxModel doFillEc2KeyPairNameItems(
                @QueryParameter final String awsCredentialsId,
                @QueryParameter final String region,
                @QueryParameter final String endpoint) {
            final ListBoxModel model = new ListBoxModel();

            try {
                final AmazonEC2 amazonEC2 = new EC2Api().connect(awsCredentialsId, region, endpoint);
                final List<KeyPairInfo> keyPairs = amazonEC2.describeKeyPairs().getKeyPairs();
                for (final KeyPairInfo keyPair : keyPairs) {
                    model.add(new ListBoxModel.Option(keyPair.getKeyName(), keyPair.getKeyName()));
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, String.format(
                        "Cannot describe key pairs credentials %s region %s endpoint %s",
                        awsCredentialsId, region, endpoint), e);
            }
            return model;
        }

        public FormValidation doTestConnection(
                @QueryParameter final String awsCredentialsId,
                @QueryParameter final String region,
                @QueryParameter final String endpoint,
                @QueryParameter final String fleet) {
            // Check if any missing AWS Permissions
            final AwsPermissionChecker awsPermissionChecker = new AwsPermissionChecker(awsCredentialsId, region, endpoint);
            final List<String> missingPermissions = awsPermissionChecker.getMissingPermissions(fleet);
            // TODO: DryRun does not work as expected for TerminateInstances and does not exists for UpdateAutoScalingGroup
            final String disclaimer = String.format("Skipping validation for following permissions: %s, %s",
                    AwsPermissionChecker.FleetAPI.TerminateInstances,
                    AwsPermissionChecker.FleetAPI.UpdateAutoScalingGroup);
            if(missingPermissions.isEmpty()) {
                return FormValidation.ok(String.format("Success! %s", disclaimer));
            }
            final String errorMessage = String.format("Following AWS permissions are missing: %s ", String.join(", ", missingPermissions));
            LOGGER.log(Level.WARNING, String.format("[TestConnection] %s", errorMessage));
            return FormValidation.error(String.format("%s %n %s", errorMessage, disclaimer));
        }

        public FormValidation doCheckName(@QueryParameter final String name, @QueryParameter final String isNewCloud) {
            try {
                Jenkins.checkGoodName(name);
            } catch (Failure e) {
                return FormValidation.error(e.getMessage());
            }

            // check if name is unique
            if (Boolean.valueOf(isNewCloud) && !CloudNames.isUnique(name)) {
                return FormValidation.error("Please choose a unique name. Existing clouds: " + Jenkins.get().clouds.stream().map(c -> c.name).collect(Collectors.joining(",")));
            }
            else if (!Boolean.valueOf(isNewCloud) && CloudNames.isDuplicated(name)) {
                Set<String> uniqueNames = new HashSet<>();
                Jenkins.get().clouds.forEach(cloud -> {uniqueNames.add(cloud.name);});
                return FormValidation.error("This cloud name is not unique. Please choose a unique name and click save. Existing clouds: " + uniqueNames);
            }

            return FormValidation.ok();
        }

        public String getDefaultCloudName() {
            return CloudNames.generateUnique(BASE_DEFAULT_FLEET_CLOUD_ID);
        }

        public Boolean isExistingCloudNameDuplicated(@QueryParameter final String name) { return CloudNames.isDuplicated(name); }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
        }
    }
}
