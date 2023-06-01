package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.fleet.EC2Fleet;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleets;
import com.amazon.jenkins.ec2fleet.utils.AwsPermissionChecker;
import com.amazon.jenkins.ec2fleet.utils.EC2FleetCloudAwareUtils;
import com.amazon.jenkins.ec2fleet.utils.RegionHelper;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsHelper;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * @see CloudNanny
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class EC2FleetCloud extends AbstractEC2FleetCloud {

    public static final String EC2_INSTANCE_TAG_NAMESPACE = "ec2-fleet-plugin";
    public static final String EC2_INSTANCE_CLOUD_NAME_TAG = EC2_INSTANCE_TAG_NAMESPACE + ":cloud-name";

    public static final String FLEET_CLOUD_ID = "FleetCloud";

    public static final int DEFAULT_CLOUD_STATUS_INTERVAL_SEC = 10;

    private static final int DEFAULT_INIT_ONLINE_TIMEOUT_SEC = 3 * 60;
    private static final int DEFAULT_INIT_ONLINE_CHECK_INTERVAL_SEC = 15;

    private static final int DEFAULT_MAX_TOTAL_USES = -1;

    private static final SimpleFormatter sf = new SimpleFormatter();
    private static final Logger LOGGER = Logger.getLogger(EC2FleetCloud.class.getName());
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    // Counter to keep track of planned nodes per EC2FleetCloud, used in node's display name
    private transient AtomicInteger plannedNodeCounter = new AtomicInteger(1);

    /**
     * Provide unique identifier for this instance of {@link EC2FleetCloud}, <code>transient</code>
     * will not be stored. Not available for customer, instead use {@link EC2FleetCloud#name}
     * will be used only during Jenkins configuration update <code>config.jelly</code>,
     * when new instance of same cloud is created and we need to find old instance and
     * repoint resources like {@link Computer} {@link Node} etc.
     * <p>
     * It's lazy to support old versions which don't have this field at all.
     * <p>
     * However it's stable, as soon as it will be created and called first uuid will be same
     * for all future calls to the same instances of lazy uuid.
     *
     * @see EC2FleetCloudAware
     */
    private transient LazyUuid id;

    /**
     * Replaced with {@link EC2FleetCloud#awsCredentialsId}
     * <p>
     * Plugin is using {@link EC2FleetCloud#computerConnector} for node connection credentials
     * on UI it's introduced same field <code>credentialsId</code> as we, so it's undefined
     * which field will be use for operations from UI.
     * <p>
     * As result we need to rename this field.
     * <p>
     * However we keep old one to be able load stored data for already installed plugins.
     * https://wiki.jenkins.io/display/JENKINS/Hint+on+retaining+backward+compatibility
     * <p>
     * Will be deleted in future when usage for old version <= 1.1.9 will be totally dropped.
     */
    @Deprecated
    private final String credentialsId;

    private final String awsCredentialsId;
    private final String region;
    private final String endpoint;

    /**
     * In fact fleet ID, not name or anything else
     */
    private final String fleet;
    private final String fsRoot;
    private final ComputerConnector computerConnector;
    private final boolean privateIpUsed;
    private final boolean alwaysReconnect;
    private final String labelString;
    private final Integer idleMinutes;
    private final int minSize;
    private final int maxSize;
    private final int minSpareSize;
    private final int numExecutors;
    private final boolean addNodeOnlyIfRunning;
    private final boolean restrictUsage;
    private final boolean scaleExecutorsByWeight;
    private final Integer initOnlineTimeoutSec;
    private final Integer initOnlineCheckIntervalSec;
    private final Integer cloudStatusIntervalSec;
    private final Integer maxTotalUses;

    /**
     * @see EC2FleetAutoResubmitComputerLauncher
     */
    private final boolean disableTaskResubmit;

    /**
     * @see NoDelayProvisionStrategy
     */
    private final boolean noDelayProvision;

    /**
     * {@link EC2FleetCloud#update()} updating this field, this is one thread
     * related to {@link CloudNanny}. At the same time {@link EC2RetentionStrategy}
     * call {@link EC2FleetCloud#scheduleToTerminate(String, boolean)} to terminate instance when it is free
     * and uses this field to know the current capacity.
     * <p>
     * It could be situation that <code>stats</code> is outdated and plugin will make wrong decision,
     * however refresh time is low and probability of this event is low. We preferred to reduce amount of calls
     * to API EC2 and increase plugin performance versus be precise. Any way outdated will be fixed after next update.
     */
    private transient FleetStateStats stats;

    private transient int toAdd;

    private transient Set<String> instanceIdsToTerminate;

    private transient Set<NodeProvisioner.PlannedNode> plannedNodesCache;

    private transient ArrayList<ScheduledFuture<?>> plannedNodeScheduledFutures;

    @DataBoundConstructor
    public EC2FleetCloud(final String name,
                         final String oldId,
                         final String awsCredentialsId,
                         final @Deprecated String credentialsId,
                         final String region,
                         final String endpoint,
                         final String fleet,
                         final String labelString,
                         final String fsRoot,
                         final ComputerConnector computerConnector,
                         final boolean privateIpUsed,
                         final boolean alwaysReconnect,
                         final Integer idleMinutes,
                         final int minSize,
                         final int maxSize,
                         final int minSpareSize,
                         final int numExecutors,
                         final boolean addNodeOnlyIfRunning,
                         final boolean restrictUsage,
                         final String maxTotalUses,
                         final boolean disableTaskResubmit,
                         final Integer initOnlineTimeoutSec,
                         final Integer initOnlineCheckIntervalSec,
                         final boolean scaleExecutorsByWeight,
                         final Integer cloudStatusIntervalSec,
                         final boolean noDelayProvision) {
        super(StringUtils.isBlank(name) ? FLEET_CLOUD_ID : name);
        init();
        this.credentialsId = credentialsId;
        this.awsCredentialsId = awsCredentialsId;
        this.region = region;
        this.endpoint = endpoint;
        this.fleet = fleet;
        this.fsRoot = fsRoot;
        this.computerConnector = computerConnector;
        this.labelString = labelString;
        this.idleMinutes = idleMinutes;
        this.privateIpUsed = privateIpUsed;
        this.alwaysReconnect = alwaysReconnect;
        if (minSize < 0) {
            warning("Cloud parameter 'minSize' can't be less than 0, setting to 0");
        }
        this.minSize = Math.max(0, minSize);
        this.maxSize = maxSize;
        this.minSpareSize = Math.max(0, minSpareSize);
        this.maxTotalUses = StringUtils.isBlank(maxTotalUses) ? -1 : Integer.parseInt(maxTotalUses);
        this.numExecutors = Math.max(numExecutors, 1);
        this.addNodeOnlyIfRunning = addNodeOnlyIfRunning;
        this.restrictUsage = restrictUsage;
        this.scaleExecutorsByWeight = scaleExecutorsByWeight;
        this.disableTaskResubmit = disableTaskResubmit;
        this.initOnlineTimeoutSec = initOnlineTimeoutSec;
        this.initOnlineCheckIntervalSec = initOnlineCheckIntervalSec;
        this.cloudStatusIntervalSec = cloudStatusIntervalSec;
        this.noDelayProvision = noDelayProvision;

        if (fleet != null) {
            this.stats = EC2Fleets.get(fleet).getState(
                    getAwsCredentialsId(), region, endpoint, getFleet());
            // Reassign existing nodes/computer with new reference of cloud
            EC2FleetCloudAwareUtils.reassign(fleet, this);
        }
    }

    public boolean isNoDelayProvision() {
        return noDelayProvision;
    }

    /**
     * See {@link EC2FleetCloud#awsCredentialsId} documentation. Don't use fields directly to be able
     * get old version of plugin and for new.
     *
     * @return credentials ID
     */
    public String getAwsCredentialsId() {
        return StringUtils.isNotBlank(awsCredentialsId) ? awsCredentialsId : credentialsId;
    }

    /**
     * Called old as will be used by new instance of cloud, for
     * which this id is old (not current)
     *
     * @return id of current cloud
     */
    public String getOldId() {
        return id.getValue();
    }

    public boolean isDisableTaskResubmit() {
        return disableTaskResubmit;
    }

    public int getInitOnlineTimeoutSec() {
        return initOnlineTimeoutSec == null ? DEFAULT_INIT_ONLINE_TIMEOUT_SEC : initOnlineTimeoutSec;
    }

    public int getScheduledFutureTimeoutSec() {
        // Wait 3 update cycles before timing out. Gives a little cushion in case fleet is under modification
        return getCloudStatusIntervalSec() * 3;
    }

    public int getCloudStatusIntervalSec() {
        return cloudStatusIntervalSec == null ? DEFAULT_CLOUD_STATUS_INTERVAL_SEC : cloudStatusIntervalSec;
    }

    public int getInitOnlineCheckIntervalSec() {
        return initOnlineCheckIntervalSec == null ? DEFAULT_INIT_ONLINE_CHECK_INTERVAL_SEC : initOnlineCheckIntervalSec;
    }

    public String getRegion() {
        return region;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public int getMaxTotalUses() {
        return maxTotalUses == null ? DEFAULT_MAX_TOTAL_USES : maxTotalUses;
    }

    @Override
    public String getFleet() {
        return fleet;
    }

    public String getFsRoot() {
        return fsRoot;
    }

    public boolean isAddNodeOnlyIfRunning() {
        return addNodeOnlyIfRunning;
    }

    public ComputerConnector getComputerConnector() {
        return computerConnector;
    }

    public boolean isPrivateIpUsed() {
        return privateIpUsed;
    }

    public boolean isAlwaysReconnect() {
        return alwaysReconnect;
    }

    public String getLabelString() {
        return labelString;
    }

    public int getIdleMinutes() {
        return (idleMinutes != null) ? idleMinutes : 0;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getMinSize() {
        return minSize;
    }

    public int getMinSpareSize() {
        return minSpareSize;
    }

    public int getNumExecutors() {
        return numExecutors;
    }

    public boolean isScaleExecutorsByWeight() {
        return scaleExecutorsByWeight;
    }

    public String getJvmSettings() {
        return "";
    }

    public boolean isRestrictUsage() {
        return restrictUsage;
    }

    // Visible for testing
    synchronized Set<NodeProvisioner.PlannedNode> getPlannedNodesCache() {
        return plannedNodesCache;
    }

    // Visible for testing
    synchronized ArrayList<ScheduledFuture<?>> getPlannedNodeScheduledFutures() { return plannedNodeScheduledFutures; }

    // Visible for testing
    synchronized void setPlannedNodeScheduledFutures(final ArrayList<ScheduledFuture<?>> futures) {
        this.plannedNodeScheduledFutures = futures;
    }

    // Visible for testing
    synchronized Set<String> getInstanceIdsToTerminate() {
        return instanceIdsToTerminate;
    }

    // Visible for testing
    synchronized int getToAdd() {
        return toAdd;
    }

    // Visible for testing
    synchronized FleetStateStats getStats() {
        return stats;
    }

    // Visible for testing
    synchronized void setStats(final FleetStateStats stats) {
        this.stats = stats;
    }

    @Override
    public synchronized boolean hasExcessCapacity() {
        if(stats == null) {
            // Let plugin sync up with current state of fleet
            return false;
        }
        if(stats.getNumDesired() - instanceIdsToTerminate.size() > maxSize) {
            info("Fleet has excess capacity of %s more than the max allowed: %s", stats.getNumDesired() - instanceIdsToTerminate.size(), maxSize);
            return true;
        }
        return false;
    }

    private synchronized int getNextPlannedNodeCounter() {
        if (plannedNodeCounter == null) {
            plannedNodeCounter = new AtomicInteger(1);
        }
        return plannedNodeCounter.getAndIncrement();
    }

    @Override
    public synchronized Collection<NodeProvisioner.PlannedNode> provision(@Nonnull final Cloud.CloudState cloudState, final int excessWorkload) {
        fine("excessWorkload %s", excessWorkload);

        if (stats == null) {
            info("First update not completed, still setting configuring cloud state. Skipping provision");
            return Collections.emptyList();
        }

        final int cap = stats.getNumDesired() + toAdd;

        if (cap >= getMaxSize()) {
            info("Max instance size '%s' reached. Skipping provision", getMaxSize());
            return Collections.emptyList();
        }

        if (!stats.getState().isActive()) {
            info("Fleet is in a non-active state '%s'. Skipping provision", stats.getState().getDetailed());
            return Collections.emptyList();
        }

        // if the planned node has 0 executors configured force it to 1 so we end up doing an unweighted check
        final int numExecutors1 = this.numExecutors == 0 ? 1 : this.numExecutors;

        // Calculate the ceiling, without having to work with doubles from Math.ceil
        // https://stackoverflow.com/a/21830188/877024
        final int weightedExcessWorkload = (excessWorkload + numExecutors1 - 1) / numExecutors1;
        int targetCapacity = Math.min(cap + weightedExcessWorkload, getMaxSize());

        int toProvision = targetCapacity - cap;
        fine("to provision = %s", toProvision);

        if (toProvision < 1) {
            info("toProvision is less than 1. Skipping provision");
            return Collections.emptyList();
        }

        toAdd += toProvision;

        final List<NodeProvisioner.PlannedNode> resultList = new ArrayList<>();
        for (int f = 0; f < toProvision; ++f) {
            final CompletableFuture<Node> completableFuture = new CompletableFuture<>();
            final NodeProvisioner.PlannedNode plannedNode = new NodeProvisioner.PlannedNode(
                    String.format("FleetNode-%s-%d", getDisplayName(), getNextPlannedNodeCounter()), completableFuture, this.numExecutors);

            resultList.add(plannedNode);
            plannedNodesCache.add(plannedNode);

            // create a ScheduledFuture that will cancel the planned node future after a timeout.
            // This protects us from leaving planned nodes stranded within Jenkins NodeProvisioner when the Fleet
            // is updated or removed before it can scale. After scaling, EC2FleetOnlineChecker will cancel the future
            // if something happens to the Fleet.
            final ScheduledFuture<?> scheduledFuture = EXECUTOR.schedule(() -> {
                if (completableFuture.isDone()) {
                    return;
                }
                info("Scaling timeout reached, removing node from Jenkins's plannedCapacitySnapshot");
                // with complete(null) Jenkins will remove future from plannedCapacity without making a fuss
                completableFuture.complete(null);
                return;
                },
                getScheduledFutureTimeoutSec(), TimeUnit.SECONDS);
            plannedNodeScheduledFutures.add(scheduledFuture);
        }
        return resultList;
    }

    /**
     * Perform sync of plugin data with EC2 Spot Fleet state.
     *
     * @return current state
     */
    public FleetStateStats update() {
        fine("start cloud %s", this);

        // Make a snapshot of current cloud state to work with.
        // We should always work with the snapshot since data could be modified in another thread
        FleetStateStats currentState = EC2Fleets.get(fleet).getState(
                getAwsCredentialsId(), region, endpoint, getFleet());

        // Some Fleet implementations (e.g. EC2SpotFleet) reflect their state only at the end of modification
        if (currentState.getState().isModifying()) {
            info("Fleet '%s' is currently under modification. Skipping update", currentState.getFleetId());
            synchronized (this) {
                return stats;
            }
        }
        final int currentToAdd;
        final Set<String> currentInstanceIdsToTerminate;
        synchronized (this) {
            if(minSpareSize > 0) {
                // Check spare instances by considering FleetStateStats#getNumDesired so we account for newer instances which are in progress
                final int currentSpareInstanceCount = getCurrentSpareInstanceCount(currentState, currentState.getNumDesired());
                final int additionalSpareInstancesRequired = minSpareSize - currentSpareInstanceCount;
                fine("currentSpareInstanceCount: %s additionalSpareInstancesRequired: %s", currentSpareInstanceCount, additionalSpareInstancesRequired);
                if (additionalSpareInstancesRequired > 0) {
                    toAdd = toAdd + additionalSpareInstancesRequired;
                }
            }
            currentToAdd = toAdd;
            currentInstanceIdsToTerminate = new HashSet<>(instanceIdsToTerminate);
        }

        currentState = updateByState(currentToAdd, currentInstanceIdsToTerminate, currentState);

        // lock and update state of plugin, so terminate or provision could work with new state of world
        synchronized (this) {
            instanceIdsToTerminate.removeAll(currentInstanceIdsToTerminate);
            // toAdd only grows outside of this method, so we can subtract
            toAdd = toAdd - currentToAdd;
            fine("setting stats");
            stats = currentState;

            removePlannedNodeScheduledFutures(currentToAdd);

            // since data could be changed between two sync blocks we need to recalculate target capacity
            final int updatedTargetCapacity = Math.max(0,
                    stats.getNumDesired() - instanceIdsToTerminate.size() + toAdd);
            // limit planned pool according to real target capacity
            while (plannedNodesCache.size() > updatedTargetCapacity) {
                info("Planned number of nodes '%s' is greater than the targetCapacity '%s'. Canceling a node", plannedNodesCache.size(), updatedTargetCapacity);
                final Iterator<NodeProvisioner.PlannedNode> iterator = plannedNodesCache.iterator();
                final NodeProvisioner.PlannedNode plannedNodeToCancel = iterator.next();
                iterator.remove();
                // cancel to let jenkins know that the node is not valid anymore
                plannedNodeToCancel.future.cancel(true);
            }
            return stats;
        }
    }

    public boolean removePlannedNodeScheduledFutures(final int numToRemove) {
        if (numToRemove < 1) {
            return false;
        }
        Iterator<ScheduledFuture<?>> iterator = plannedNodeScheduledFutures.iterator();
        for (int i = 0; i < numToRemove; i++) {
            if(!iterator.hasNext()){
                fine("Expected a scheduled future to exist but no more are present");
                return false;
            }
            ScheduledFuture<?> nextFuture = iterator.next();
            nextFuture.cancel(true);
            iterator.remove();
        }
        return true;
    }

    private FleetStateStats updateByState(
            final int currentToAdd, final Set<String> currentInstanceIdsToTerminate, final FleetStateStats currentState) {
        final Jenkins jenkins = Jenkins.get();
        final AmazonEC2 ec2 = Registry.getEc2Api().connect(getAwsCredentialsId(), region, endpoint);

        // Ensure target capacity is not negative (covers capacity updates from outside the plugin)
        final int targetCapacity = Math.max(minSize,
                Math.min(maxSize, currentState.getNumDesired() - currentInstanceIdsToTerminate.size() + currentToAdd));

        // Modify target capacity when an instance is removed or added, even if the value of target capacity doesn't change.
        // For example, if we remove an instance and add an instance the net change is 0, but we still make the API call.
        // This lets us update the fleet settings with NoTermination policy, which lets us terminate instances on our own
        if (currentToAdd > 0 || currentInstanceIdsToTerminate.size() > 0 || targetCapacity != currentState.getNumDesired()) {
            EC2Fleets.get(fleet).modify(
                    getAwsCredentialsId(), region, endpoint, fleet, targetCapacity, minSize, maxSize);
            info("Set target capacity to '%s'", targetCapacity);
        }

        final FleetStateStats updatedState = new FleetStateStats(currentState, targetCapacity);

        if (currentInstanceIdsToTerminate.size() > 0) {
            // internally removeNode lock on queue to correctly update node list
            // we do big block for all removal to avoid delay on lock waiting
            // for each node
            Queue.withLock(new Runnable() {
                @Override
                public void run() {
                    for (final String instanceId : currentInstanceIdsToTerminate) {
                        final Node node = jenkins.getNode(instanceId);
                        if (node != null) {
                            try {
                                jenkins.removeNode(node);
                            } catch (IOException e) {
                                warning("Failed to remove node '%s' from Jenkins before termination", instanceId);
                            }
                        }
                    }
                }
            });
            info("Terminating nodes: %s", currentInstanceIdsToTerminate);
            Registry.getEc2Api().terminateInstances(ec2, currentInstanceIdsToTerminate);
        }

        fine("Fleet instances: %s", updatedState.getInstances());

        // Set up the lists of Jenkins nodes and fleet instances
        final Set<String> fleetInstances = new HashSet<>(updatedState.getInstances());
        final Map<String, Instance> described = Registry.getEc2Api().describeInstances(ec2, fleetInstances);

        // Sometimes described includes just deleted instances
        described.keySet().removeAll(currentInstanceIdsToTerminate);
        fine("Described instances: %s", described.keySet());

        // Fleet takes a while to display terminated instances. Update stats with current view of active instance count
        updatedState.setNumActive(described.size());

        final Set<String> jenkinsInstances = new HashSet<>();
        for (final Node node : jenkins.getNodes()) {
            if (node instanceof EC2FleetNode && ((EC2FleetNode) node).getCloud().getFleet().equals(fleet)) {
                jenkinsInstances.add(node.getNodeName());
            }
        }
        fine("Jenkins nodes: %s", jenkinsInstances);

        // contains Jenkins nodes that were once fleet instances but are no longer in the fleet
        final Set<String> jenkinsNodesWithoutInstance = new HashSet<>(jenkinsInstances);
        jenkinsNodesWithoutInstance.removeAll(fleetInstances);
        if(!jenkinsNodesWithoutInstance.isEmpty()) {
            fine("Jenkins nodes without instance(s): %s", jenkinsNodesWithoutInstance);
        }
        // terminatedFleetInstances contains fleet instances that are terminated, stopped, stopping, or shutting down
        final Set<String> terminatedFleetInstances = new HashSet<>(fleetInstances);

        // terminated are any current which cannot be described
        terminatedFleetInstances.removeAll(described.keySet());
        if(!terminatedFleetInstances.isEmpty()) {
            fine("Terminated Fleet instance(s): %s", terminatedFleetInstances);
        }
        // newFleetInstances contains running fleet instances that are not already Jenkins nodes
        final Map<String, Instance> newFleetInstances = new HashMap<>(described);
        for (final String instanceId : jenkinsInstances) newFleetInstances.remove(instanceId);
        if(!newFleetInstances.isEmpty()) {
            fine("New instance(s) not yet registered as nodes in Jenkins: %s ", newFleetInstances.keySet());
        }
        // update caches
        final List<String> jenkinsNodesToRemove = new ArrayList<>();
        jenkinsNodesToRemove.addAll(terminatedFleetInstances);
        jenkinsNodesToRemove.addAll(jenkinsNodesWithoutInstance);
        // Remove dying fleet instances from Jenkins
        for (final String instance : jenkinsNodesToRemove) {
            removeNode(instance);
        }

        // Update the label for all Jenkins nodes in the fleet instance cache
        for (final String instanceId : jenkinsInstances) {
            final Node node = jenkins.getNode(instanceId);
            if (node == null) {
                info("Skipping label update, the Jenkins node for instance '%s' was null", instanceId);
                continue;
            }

            if (!labelString.equals(node.getLabelString())) {
                try {
                    info("Updating label on node '%s' to \"%s\".", instanceId, labelString);
                    node.setLabelString(labelString);
                } catch (final Exception ex) {
                    warning(ex, "Failed to set label on node '%s': ", instanceId, ex.toString());
                }
            }
        }

        // If we have new instances - create nodes for them!
        if (newFleetInstances.size() > 0) {
            // We tag new instances to help users to identify instances launched from plugin managed fleets.
            // If it fails we are fine to skip this call
            try {
                Registry.getEc2Api().tagInstances(ec2, newFleetInstances.keySet(),
                        EC2_INSTANCE_CLOUD_NAME_TAG, name);
            } catch (final Exception e) {
                warning(e, "Failed to tag new instances: %s", newFleetInstances.keySet());
            }

            // addNewSlave will call addNode which calls queue lock.
            // We speed this up by getting one lock for all nodes to add
            Queue.withLock(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (final Instance instance : newFleetInstances.values()) {
                            addNewSlave(ec2, instance, updatedState);
                        }
                    } catch (final Exception ex) {
                        warning(ex, "Unable to set label on node");
                    }
                }
            });
        }

        return updatedState;
    }

    /**
     * Schedules Jenkins Node and EC2 instance for termination.
     * If <code>force</code> is false and target capacity falls below <code>minSize</code> OR <code>minSpareSize</code> thresholds, then reject termination.
     * Else if <code>force</code> is true, schedule instance for termination even if it breaches <code>minSize</code> OR <code>minSpareSize</code>
     * <p>
     * Real termination will happens in {@link EC2FleetCloud#update()} which is periodically called by
     * {@link CloudNanny}. So there could be some lag between the decision to terminate the node
     * and actual termination, you can find max lag size in {@link CloudNanny#getRecurrencePeriod()}
     * <p>
     * This method doesn't do real termination to reduce load for Jenkins in case when multiple nodes should be
     * terminated in short time, without schedule process and batch termination, multiple calls should be raised
     * to AWS EC2 API which takes some time and block cloud class.
     *
     * @param instanceId node name or instance ID
     * @param force terminate instance even if it breaches min size constraint
     * @return <code>true</code> if node scheduled for termination, otherwise <code>false</code>
     */
    public synchronized boolean scheduleToTerminate(final String instanceId, final boolean force) {
        if (stats == null) {
            info("First update not done, skipping termination scheduling for '%s'", instanceId);
            return false;
        }
        // We can't remove instances beyond minSize or minSpareSize unless force true
        if(!force) {
            if (minSize > 0 && stats.getNumActive() - instanceIdsToTerminate.size() <= minSize) {
                info("Not scheduling instance '%s' for termination because we need a minimum of %s instance(s) running", instanceId, minSize);
                fine("cloud: %s, instanceIdsToTerminate: %s", this, instanceIdsToTerminate);
                return false;
            }
            if (minSpareSize > 0) {
                // Check spare instances by considering FleetStateStats#getNumActive as we want to consider only running instances
                final int currentSpareInstanceCount = getCurrentSpareInstanceCount(stats, stats.getNumActive());
                if (currentSpareInstanceCount - instanceIdsToTerminate.size() <= minSpareSize) {
                    info("Not scheduling instance '%s' for termination because we need a minimum of %s spare instance(s) running", instanceId, minSpareSize);
                    return false;
                }
            }
        }
        info("Scheduling instance '%s' for termination on cloud %s with force: %b", instanceId, this, force);
        instanceIdsToTerminate.add(instanceId);
        fine("InstanceIdsToTerminate: %s", instanceIdsToTerminate);
        return true;
    }

    @Override
    public boolean canProvision(final Cloud.CloudState cloudState) {
        final Label label = cloudState.getLabel();
        fine("CanProvision called on fleet: \"" + this.labelString + "\" wanting: \"" + (label == null ? "(unspecified)" : label.getName()) + "\".");
        if (fleet == null) {
            fine("Fleet/ASG for cloud is null, returning false");
            return false;
        }
        if (this.restrictUsage && labelString != null && label == null) {
            fine("RestrictUsage is enabled while label is null, returning false");
            return false;
        }
        if (label != null && !Label.parse(this.labelString).containsAll(label.listAtoms())) {
            fine("Label '%s' not found within Fleet's labels '%s', returning false", label, this.labelString);
            return false;
        }
        return true;
    }

    private Object readResolve() {
        init();
        return this;
    }

    private void init() {
        id = new LazyUuid();

        plannedNodesCache = new HashSet<>();
        instanceIdsToTerminate = new HashSet<>();
        plannedNodeScheduledFutures = new ArrayList<>();
    }

    private void removeNode(final String instanceId) {
        final Jenkins jenkins = Jenkins.get();
        // If this node is dying, remove it from Jenkins
        final Node n = jenkins.getNode(instanceId);
        if (n != null) {
            try {
                info("Fleet '%s' no longer has the instance '%s'. Removing instance from Jenkins", getLabelString(), instanceId);
                jenkins.removeNode(n);
            } catch (final Exception ex) {
                throw new IllegalStateException(String.format("Error removing instance '%s' from Jenkins", instanceId), ex);
            }
        }
    }

    /**
     * https://github.com/jenkinsci/ec2-plugin/blob/master/src/main/java/hudson/plugins/ec2/EC2Cloud.java#L640
     *
     * @param ec2      ec2 client
     * @param instance instance
     */
    private void addNewSlave(final AmazonEC2 ec2, final Instance instance, FleetStateStats stats) throws Exception {
        final String instanceId = instance.getInstanceId();

        // instance state check enabled and not running, skip adding
        if (addNodeOnlyIfRunning && InstanceStateName.Running != InstanceStateName.fromValue(instance.getState().getName())) {
            return;
        }

        final String address = privateIpUsed ? instance.getPrivateIpAddress() : instance.getPublicIpAddress();
        // Check if we have the address to use. Nodes don't get it immediately.
        if (address == null) {
            if (!privateIpUsed) {
                info("Instance '%s' public IP address not assigned. Either it could take some time or" +
                        " the Spot Request is not configured to assign public IPs", instance.getInstanceId());
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

        final Double instanceTypeWeight = stats.getInstanceTypeWeights().get(instance.getInstanceType());
        final int effectiveNumExecutors;
        if (scaleExecutorsByWeight && instanceTypeWeight != null) {
            effectiveNumExecutors = (int) Math.max(Math.round(numExecutors * instanceTypeWeight), 1);
        } else {
            effectiveNumExecutors = numExecutors;
        }

        final EC2FleetAutoResubmitComputerLauncher computerLauncher = new EC2FleetAutoResubmitComputerLauncher(
                computerConnector.launch(address, TaskListener.NULL));
        final Node.Mode nodeMode = restrictUsage ? Node.Mode.EXCLUSIVE : Node.Mode.NORMAL;
        final EC2FleetNode node = new EC2FleetNode(instanceId, "Fleet slave for " + instanceId,
                effectiveFsRoot, effectiveNumExecutors, nodeMode, labelString, new ArrayList<NodeProperty<?>>(),
                this, computerLauncher, getMaxTotalUses());

        // Initialize our retention strategy
        node.setRetentionStrategy(new EC2RetentionStrategy());

        final Jenkins jenkins = Jenkins.get();
        // jenkins automatically remove old node with same name if any
        jenkins.addNode(node);

        // todo use plannedNodesCache in thread-safe way
        final CompletableFuture<Node> future;
        if (plannedNodesCache.isEmpty()) {
            // handle the case where we have new nodes the plugin didn't request
            future = new CompletableFuture<>();
        } else {
            // handle the standard case where this node came from one of our scale up events
            final NodeProvisioner.PlannedNode plannedNode = plannedNodesCache.iterator().next();
            plannedNodesCache.remove(plannedNode);
            future = ((CompletableFuture<Node>) plannedNode.future);
        }

        // Use getters for timeout and interval as they provide a default value
        // when a user installs a new plugin version and doesn't recreate the cloud
        EC2FleetOnlineChecker.start(node, future,
                TimeUnit.SECONDS.toMillis(getInitOnlineTimeoutSec()),
                TimeUnit.SECONDS.toMillis(getInitOnlineCheckIntervalSec()));
    }

    private int getCurrentSpareInstanceCount(final FleetStateStats currentState, final int countOfInstances) {
        final int currentSpareInstanceCount = 0;
        if(minSpareSize > 0) {
            final Jenkins jenkins = Jenkins.get();
            int currentBusyInstances = 0;
            for (final Computer computer : jenkins.getComputers()) {
                if (computer instanceof EC2FleetNodeComputer && !computer.isIdle()) {
                    final Node compNode = computer.getNode();
                    if (compNode == null) {
                        continue;
                    }

                    // Do not count computer if it is not a part of the given fleet
                    if (!Objects.equals(((EC2FleetNodeComputer) computer).getCloud().getFleet(), currentState.getFleetId())) {
                        continue;
                    }
                    currentBusyInstances++;
                }
            }
            return countOfInstances - currentBusyInstances;
        }
        return 0;
    }

    private String getLogPrefix() {
        return getDisplayName() + " [" + getLabelString() + "] ";
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

    @Extension
    @SuppressWarnings("unused")
    public static class DescriptorImpl extends Descriptor<Cloud> {

        public DescriptorImpl() {
            super();
            load();
        }

        @Override
        public String getDisplayName() {
            return "Amazon EC2 Fleet";
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

        public FormValidation doCheckMaxTotalUses(@QueryParameter String value) {
            try {
                int val = Integer.parseInt(value);
                if (val >= -1)
                    return FormValidation.ok();
            } catch (NumberFormatException nfe) {
            }
            return FormValidation.error("Maximum Total Uses must be greater or equal to -1");
        }

        public ListBoxModel doFillFleetItems(@QueryParameter final boolean showAllFleets,
                                             @QueryParameter final String region,
                                             @QueryParameter final String endpoint,
                                             @QueryParameter final String awsCredentialsId,
                                             @QueryParameter final String fleet) {
            final ListBoxModel model = new ListBoxModel();
            try {
                for (final EC2Fleet EC2Fleet : EC2Fleets.all()) {
                    EC2Fleet.describe(
                            awsCredentialsId, region, endpoint, model, fleet, showAllFleets);
                }
            } catch (final Exception ex) {
                LOGGER.log(Level.WARNING, String.format("Cannot describe fleets in %s or by endpoint %s", region, endpoint), ex);
                return model;
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

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
        }

    }

}
