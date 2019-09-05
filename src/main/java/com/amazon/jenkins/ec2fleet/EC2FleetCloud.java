package com.amazon.jenkins.ec2fleet;

import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.BatchState;
import com.amazonaws.services.ec2.model.DescribeRegionsResult;
import com.amazonaws.services.ec2.model.DescribeSpotFleetInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsResult;
import com.amazonaws.services.ec2.model.FleetType;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.ModifySpotFleetRequestRequest;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfig;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsHelper;
import com.google.common.util.concurrent.SettableFuture;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.slaves.Cloud;
import hudson.slaves.ComputerConnector;
import hudson.slaves.Messages;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodeProvisioner;
import hudson.slaves.OfflineCause.SimpleOfflineCause;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * @see CloudNanny
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class EC2FleetCloud extends Cloud {

    public static final String FLEET_CLOUD_ID = "FleetCloud";

    private static final int DEFAULT_INIT_ONLINE_TIMEOUT_SEC = 3 * 60;
    private static final int DEFAULT_INIT_ONLINE_CHECK_INTERVAL_SEC = 15;

    private static final SimpleFormatter sf = new SimpleFormatter();
    private static final Logger LOGGER = Logger.getLogger(EC2FleetCloud.class.getName());

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
    private final Integer minSize;
    private final Integer maxSize;
    private final Integer numExecutors;
    private final boolean addNodeOnlyIfRunning;
    private final boolean restrictUsage;
    private final boolean scaleExecutorsByWeight;
    private final Integer initOnlineTimeoutSec;
    private final Integer initOnlineCheckIntervalSec;
    private final String onlineCheckScript;

    /**
     * @see EC2FleetAutoResubmitComputerLauncher
     */
    private final boolean disableTaskResubmit;

    private transient Set<NodeProvisioner.PlannedNode> plannedNodesCache;
    // fleetInstancesCache contains all Jenkins nodes known to be in the fleet, not in dyingFleetInstancesCache
    private transient Set<String> fleetInstancesCache;
    // dyingFleetInstancesCache contains Jenkins nodes known to be in the fleet that are ready to be terminated
    private transient Set<String> dyingFleetInstancesCache;

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
                         final Integer minSize,
                         final Integer maxSize,
                         final Integer numExecutors,
                         final boolean addNodeOnlyIfRunning,
                         final boolean restrictUsage,
                         final boolean disableTaskResubmit,
                         final Integer initOnlineTimeoutSec,
                         final Integer initOnlineCheckIntervalSec,
                         final boolean scaleExecutorsByWeight,
                         final String onlineCheckScript) {
        super(StringUtils.isBlank(name) ? FLEET_CLOUD_ID : name);
        initCaches();
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
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.numExecutors = numExecutors;
        this.addNodeOnlyIfRunning = addNodeOnlyIfRunning;
        this.restrictUsage = restrictUsage;
        this.scaleExecutorsByWeight = scaleExecutorsByWeight;
        this.disableTaskResubmit = disableTaskResubmit;
        this.initOnlineTimeoutSec = initOnlineTimeoutSec;
        this.initOnlineCheckIntervalSec = initOnlineCheckIntervalSec;
        this.onlineCheckScript = onlineCheckScript;

        if (StringUtils.isNotEmpty(oldId)) {
            // existent cloud was modified, let's re-assign all dependencies of old cloud instance
            // to new one
            EC2FleetCloudAwareUtils.reassign(oldId, this);
        }
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

    public int getInitOnlineCheckIntervalSec() {
        return initOnlineCheckIntervalSec == null ? DEFAULT_INIT_ONLINE_CHECK_INTERVAL_SEC : initOnlineCheckIntervalSec;
    }

    public String getOnlineCheckScript() {
        return onlineCheckScript;
    }

    public String getRegion() {
        return region;
    }

    public String getEndpoint() {
        return endpoint;
    }

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

    public Integer getMaxSize() {
        return maxSize;
    }

    public Integer getMinSize() {
        return minSize;
    }

    public Integer getNumExecutors() {
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

    @Override
    public Collection<NodeProvisioner.PlannedNode> provision(
            final Label label, final int excessWorkload) {
        try {
            return Queue.withLock(new Callable<Collection<NodeProvisioner.PlannedNode>>() {
                @Override
                public Collection<NodeProvisioner.PlannedNode> call() {
                    return provisionInternal(label, excessWorkload);
                }
            });
        } catch (Exception exception) {
            warning(exception, "provisionInternal failed");
            throw new IllegalStateException(exception);
        }
    }

    public synchronized Collection<NodeProvisioner.PlannedNode> provisionInternal(
            final Label label, int excessWorkload) {
        info("excessWorkload = %s", excessWorkload);

        final FleetStateStats stats = updateStatus();
        final int maxAllowed = this.getMaxSize();

        if (stats.getNumDesired() >= maxAllowed || !"active".equals(stats.getState()))
            return Collections.emptyList();

        // if the planned node has 0 executors configured force it to 1 so we end up doing an unweighted check
        final int numExecutors = this.numExecutors == 0 ? 1 : this.numExecutors;

        // Calculate the ceiling, without having to work with doubles from Math.ceil
        // https://stackoverflow.com/a/21830188/877024
        final int weightedExcessWorkload = (excessWorkload + numExecutors - 1) / numExecutors;
        int targetCapacity = stats.getNumDesired() + weightedExcessWorkload;

        if (targetCapacity > maxAllowed) targetCapacity = maxAllowed;

        int toProvision = targetCapacity - stats.getNumDesired();
        info("to provision = %s", toProvision);

        if (toProvision < 1)
            return Collections.emptyList();

        final ModifySpotFleetRequestRequest request = new ModifySpotFleetRequestRequest();
        request.setSpotFleetRequestId(fleet);
        request.setTargetCapacity(targetCapacity);

        final AmazonEC2 ec2 = Registry.getEc2Api().connect(getAwsCredentialsId(), region, endpoint);
        ec2.modifySpotFleetRequest(request);

        final List<NodeProvisioner.PlannedNode> resultList = new ArrayList<>();
        for (int f = 0; f < toProvision; ++f) {
            // todo make name unique per fleet
            final NodeProvisioner.PlannedNode plannedNode = new NodeProvisioner.PlannedNode(
                    "FleetNode-" + f, SettableFuture.<Node>create(), this.numExecutors);
            resultList.add(plannedNode);
            this.plannedNodesCache.add(plannedNode);
        }
        return resultList;
    }

    public synchronized FleetStateStats updateStatus() {
        info("start");

        final AmazonEC2 ec2 = Registry.getEc2Api().connect(getAwsCredentialsId(), region, endpoint);
        final FleetStateStats stats = FleetStateStats.readClusterState(ec2, getFleet(), labelString);
        info("fleet instances: %s", stats.getInstances());

        // Set up the lists of Jenkins nodes and fleet instances
        // currentFleetInstances contains instances currently in the fleet
        final Set<String> currentInstanceIds = new HashSet<>(stats.getInstances());

        // currentJenkinsNodes contains all Nodes currently registered in Jenkins
        final Set<String> currentJenkinsNodes = new HashSet<>();
        for (final Node node : Jenkins.getInstance().getNodes()) {
            currentJenkinsNodes.add(node.getNodeName());
        }
        info("jenkins nodes %s", currentJenkinsNodes);

        // missingFleetInstances contains Jenkins nodes that were once fleet instances but are no longer in the fleet
        final Set<String> missingFleetInstances = new HashSet<>(currentJenkinsNodes);
        missingFleetInstances.retainAll(fleetInstancesCache);
        missingFleetInstances.removeAll(currentInstanceIds);
        info("jenkins nodes without instance %s", missingFleetInstances);

        final Map<String, Instance> described = Registry.getEc2Api().describeInstances(ec2, currentInstanceIds);
        info("described instances: %s", described.keySet());

        // terminatedFleetInstances contains fleet instances that are terminated, stopped, stopping, or shutting down
        final Set<String> terminatedInstanceIds = new HashSet<>(currentInstanceIds);
        // terminated are any current which cannot be described
        terminatedInstanceIds.removeAll(described.keySet());
        info("terminated instances " + terminatedInstanceIds);

        // newFleetInstances contains running fleet instances that are not already Jenkins nodes
        final Map<String, Instance> newFleetInstances = new HashMap<>(described);
        for (final String instanceId : currentJenkinsNodes) newFleetInstances.remove(instanceId);
        info("new instances " + newFleetInstances.keySet());

        // update caches
        dyingFleetInstancesCache.addAll(missingFleetInstances);
        dyingFleetInstancesCache.addAll(terminatedInstanceIds);
        dyingFleetInstancesCache.retainAll(currentJenkinsNodes);
        fleetInstancesCache.addAll(currentInstanceIds);
        fleetInstancesCache.removeAll(dyingFleetInstancesCache);
        fleetInstancesCache.retainAll(currentJenkinsNodes);

        // Remove dying fleet instances from Jenkins
        for (final String instance : dyingFleetInstancesCache) {
            info("Fleet (" + getLabelString() + ") no longer has the instance " + instance + ", removing from Jenkins.");
            removeNode(instance);
            dyingFleetInstancesCache.remove(instance);
        }

        // Update the label for all Jenkins nodes in the fleet instance cache
        for (final String instance : fleetInstancesCache) {
            Node node = Jenkins.getInstance().getNode(instance);
            if (node == null)
                continue;

            if (!labelString.equals(node.getLabelString())) {
                try {
                    info("Updating label on node %s to \"%s\".", instance, labelString);
                    node.setLabelString(labelString);
                } catch (final Exception ex) {
                    warning(ex, "Unable to set label on node %s", instance);
                }
            }
        }

        // If we have new instances - create nodes for them!
        try {
            for (final Instance instance : newFleetInstances.values()) {
                addNewSlave(ec2, instance, stats);
            }
        } catch (final Exception ex) {
            warning(ex, "Unable to set label on node");
        }

        return stats;
    }

    public synchronized boolean terminateInstance(final String instanceId) {
        info("Attempting to terminate instance: %s", instanceId);

        final FleetStateStats stats = updateStatus();

        if (!fleetInstancesCache.contains(instanceId)) {
            info("Unknown instance terminated: %s", instanceId);
            return false;
        }

        final AmazonEC2 ec2 = Registry.getEc2Api().connect(getAwsCredentialsId(), region, endpoint);

        if (!dyingFleetInstancesCache.contains(instanceId)) {
            // We can't remove instances beyond minSize
            if (stats.getNumDesired() == minSize || !"active".equals(stats.getState())) {
                info("Not terminating %s because we need a minimum of %s instances running.", instanceId, minSize);
                return false;
            }

            // These operations aren't idempotent so only do them once
            final ModifySpotFleetRequestRequest request = new ModifySpotFleetRequestRequest();
            request.setSpotFleetRequestId(fleet);
            request.setTargetCapacity(stats.getNumDesired() - 1);
            request.setExcessCapacityTerminationPolicy("NoTermination");
            ec2.modifySpotFleetRequest(request);

            //And remove the instance
            dyingFleetInstancesCache.add(instanceId);
        }

        // disconnect the node before terminating the instance
        final Jenkins jenkins = Jenkins.getInstance();
        synchronized (jenkins) {
            final Computer c = jenkins.getNode(instanceId).toComputer();
            if (c.isOnline()) {
                c.disconnect(SimpleOfflineCause.create(
                        Messages._SlaveComputer_DisconnectedBy(this.name, this.fleet)));
            }
        }
        final Computer c = jenkins.getNode(instanceId).toComputer();
        try {
            c.waitUntilOffline();
        } catch (InterruptedException e) {
            warning("Interrupted while disconnecting %s", c.getDisplayName());
        }
        // terminateInstances is idempotent so it can be called until it's successful
        final TerminateInstancesResult result = ec2.terminateInstances(new TerminateInstancesRequest(Collections.singletonList(instanceId)));
        info("Instance %s termination result: %s", instanceId, result.toString());

        return true;
    }

    @Override
    public boolean canProvision(final Label label) {
        boolean result = fleet != null && (label == null || Label.parse(this.labelString).containsAll(label.listAtoms()));
        fine("CanProvision called on fleet: \"" + this.labelString + "\" wanting: \"" + (label == null ? "(unspecified)" : label.getName()) + "\". Returning " + result + ".");
        return result;
    }

    private Object readResolve() {
        initCaches();
        return this;
    }

    private void initCaches() {
        id = new LazyUuid();

        plannedNodesCache = new HashSet<>();
        fleetInstancesCache = new HashSet<>();
        dyingFleetInstancesCache = new HashSet<>();
    }

    private synchronized void removeNode(String instanceId) {
        final Jenkins jenkins = Jenkins.getInstance();
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (jenkins) {
            // If this node is dying, remove it from Jenkins
            final Node n = jenkins.getNode(instanceId);
            if (n != null) {
                try {
                    jenkins.removeNode(n);
                } catch (final Exception ex) {
                    throw new IllegalStateException(String.format("Error removing node %s", instanceId), ex);
                }
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
        if (addNodeOnlyIfRunning && InstanceStateName.Running != InstanceStateName.fromValue(instance.getState().getName()))
            return;

        final String address = privateIpUsed ? instance.getPrivateIpAddress() : instance.getPublicIpAddress();
        // Check if we have the address to use. Nodes don't get it immediately.
        if (address == null) {
            if (!privateIpUsed) {
                info("%s instance %s public IP address not assigned, it could take some time or" +
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
                this, computerLauncher);

        // Initialize our retention strategy
        node.setRetentionStrategy(new IdleRetentionStrategy());

        final Jenkins jenkins = Jenkins.getInstance();
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (jenkins) {
            // jenkins automatically remove old node with same name if any
            jenkins.addNode(node);
        }

        final SettableFuture<Node> future;
        if (plannedNodesCache.isEmpty()) {
            future = SettableFuture.create();
        } else {
            final NodeProvisioner.PlannedNode plannedNode = plannedNodesCache.iterator().next();
            plannedNodesCache.remove(plannedNode);
            future = ((SettableFuture<Node>) plannedNode.future);
        }

        // use getters for timeout and interval as they provide default value
        // when user just install new version and did't recreate fleet
        EC2FleetOnlineChecker.start(node, future,
                TimeUnit.SECONDS.toMillis(getInitOnlineTimeoutSec()),
                TimeUnit.SECONDS.toMillis(getInitOnlineCheckIntervalSec()),
                onlineCheckScript);
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

        public boolean useInstanceProfileForCredentials;
        public String accessId;
        public String secretKey;
        public String region;
        public String fleet;
        public String userName = "root";
        public boolean privateIpUsed;
        public boolean alwaysReconnect;
        public boolean restrictUsage;
        public String privateKey;
        public boolean showAllFleets;

        public DescriptorImpl() {
            super();
            load();
        }

        @Override
        public String getDisplayName() {
            return "Amazon SpotFleet";
        }

        public List getComputerConnectorDescriptors() {
            return Jenkins.getInstance().getDescriptorList(ComputerConnector.class);
        }

        public ListBoxModel doFillAwsCredentialsIdItems() {
            return AWSCredentialsHelper.doFillCredentialsIdItems(Jenkins.getInstance());
        }

        public ListBoxModel doFillRegionItems(@QueryParameter final String awsCredentialsId) {
            // to keep user consistent order tree set
            final Set<String> regionNames = new TreeSet<>();

            try {
                final AmazonEC2 client = Registry.getEc2Api().connect(awsCredentialsId, null, null);
                final DescribeRegionsResult regions = client.describeRegions();
                for (final Region region : regions.getRegions()) {
                    regionNames.add(region.getRegionName());
                }
            } catch (final Exception ex) {
                // ignore exception it could be case that credentials are not belong to default region
                // which we are using to describe regions
            }

            for (final com.amazonaws.regions.Region region : RegionUtils.getRegions()) {
                regionNames.add(region.getName());
            }

            final ListBoxModel model = new ListBoxModel();
            for (final String regionName : regionNames) {
                model.add(new ListBoxModel.Option(regionName, regionName));
            }
            return model;
        }

        public ListBoxModel doFillFleetItems(@QueryParameter final boolean showAllFleets,
                                             @QueryParameter final String region,
                                             @QueryParameter final String endpoint,
                                             @QueryParameter final String awsCredentialsId,
                                             @QueryParameter final String fleet) {
            final ListBoxModel model = new ListBoxModel();
            try {
                final AmazonEC2 client = Registry.getEc2Api().connect(awsCredentialsId, region, endpoint);
                String token = null;
                do {
                    final DescribeSpotFleetRequestsRequest req = new DescribeSpotFleetRequestsRequest();
                    req.withNextToken(token);
                    final DescribeSpotFleetRequestsResult result = client.describeSpotFleetRequests(req);
                    for (final SpotFleetRequestConfig config : result.getSpotFleetRequestConfigs()) {
                        final String curFleetId = config.getSpotFleetRequestId();
                        final boolean selected = ObjectUtils.nullSafeEquals(fleet, curFleetId);
                        if (selected || showAllFleets || isSpotFleetActiveAndMaintain(config)) {
                            final String displayStr = curFleetId +
                                    " (" + config.getSpotFleetRequestState() + ")" +
                                    " (" + config.getSpotFleetRequestConfig().getType() + ")";
                            model.add(new ListBoxModel.Option(displayStr, curFleetId, selected));
                        }
                    }
                    token = result.getNextToken();
                } while (token != null);

            } catch (final Exception ex) {
                LOGGER.log(Level.WARNING, String.format("Cannot describe fleets in %s or by endpoint %s", region, endpoint), ex);
                return model;
            }

            return model;
        }

        /**
         * @param config - config
         * @return return <code>true</code> not only for {@link BatchState#Active} but for any other
         * in which fleet in theory could accept load.
         */
        private boolean isSpotFleetActiveAndMaintain(final SpotFleetRequestConfig config) {
            return FleetType.Maintain.toString().equals(config.getSpotFleetRequestConfig().getType()) && (
                    BatchState.Active.toString().equals(config.getSpotFleetRequestState())
                            || BatchState.Modifying.toString().equals(config.getSpotFleetRequestState())
                            || BatchState.Submitted.toString().equals(config.getSpotFleetRequestState()));
        }

        public FormValidation doTestConnection(
                @QueryParameter final String awsCredentialsId,
                @QueryParameter final String region,
                @QueryParameter final String endpoint,
                @QueryParameter final String fleet) {
            try {
                final AmazonEC2 client = Registry.getEc2Api().connect(awsCredentialsId, region, endpoint);
                client.describeSpotFleetInstances(
                        new DescribeSpotFleetInstancesRequest().withSpotFleetRequestId(fleet));
            } catch (final Exception ex) {
                return FormValidation.error(ex.getMessage());
            }
            return FormValidation.ok("Success");
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
        }

        public boolean isShowAllFleets() {
            return showAllFleets;
        }

        public String getAccessId() {
            return accessId;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public String getRegion() {
            return region;
        }

        public String getFleet() {
            return fleet;
        }
    }

}
