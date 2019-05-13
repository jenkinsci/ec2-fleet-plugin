package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.cloud.FleetNode;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.BatchState;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeRegionsResult;
import com.amazonaws.services.ec2.model.DescribeSpotFleetInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.ModifySpotFleetRequestRequest;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfig;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsHelper;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
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
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.springframework.util.ObjectUtils;

import javax.annotation.Nonnull;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * @see CloudNanny
 */
@SuppressWarnings("unused")
public class EC2FleetCloud extends Cloud {
    public static final String FLEET_CLOUD_ID = "FleetCloud";
    private static final SimpleFormatter sf = new SimpleFormatter();

    private final String credentialsId;
    private final String region;
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


    private transient @Nonnull
    FleetStateStats statusCache;

    private transient Set<NodeProvisioner.PlannedNode> plannedNodesCache;
    // fleetInstancesCache contains all Jenkins nodes known to be in the fleet, not in dyingFleetInstancesCache
    private transient Set<String> fleetInstancesCache;
    // dyingFleetInstancesCache contains Jenkins nodes known to be in the fleet that are ready to be terminated
    private transient Set<String> dyingFleetInstancesCache;

    private static final Logger LOGGER = Logger.getLogger(EC2FleetCloud.class.getName());

    public static String join(final String separator, final Iterable<String> elements) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (String i : elements) {
            if (!isFirst) sb.append(separator);
            else isFirst = false;
            sb.append(i);
        }
        return sb.toString();
    }

    @DataBoundConstructor
    public EC2FleetCloud(final String credentialsId,
                         final String region,
                         final String fleet,
                         final String labelString,
                         final String fsRoot,
                         final ComputerConnector computerConnector,
                         final boolean privateIpUsed,
                         final boolean alwaysReconnect,
                         final Integer idleMinutes,
                         final Integer minSize,
                         final Integer maxSize,
                         final Integer numExecutors) {
        super(FLEET_CLOUD_ID);
        initCaches();
        this.credentialsId = credentialsId;
        this.region = region;
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
    }

    private Object readResolve() {
        initCaches();
        return this;
    }

    private void initCaches() {
        statusCache = new FleetStateStats(fleet, 0, "Initializing", Collections.<String>emptySet(), labelString);
        plannedNodesCache = new HashSet<NodeProvisioner.PlannedNode>();
        fleetInstancesCache = new HashSet<String>();
        dyingFleetInstancesCache = new HashSet<String>();
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getRegion() {
        return region;
    }

    public String getFleet() {
        return fleet;
    }

    public String getFsRoot() {
        return fsRoot;
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
        return this.labelString;
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

    public String getJvmSettings() {
        return "";
    }

    public @Nonnull
    FleetStateStats getStatusCache() {
        return statusCache;
    }

    public static void log(final Logger logger, final Level level,
                           final TaskListener listener, final String message) {
        log(logger, level, listener, message, null);
    }

    public static void log(final Logger logger, final Level level,
                           final TaskListener listener, String message, final Throwable exception) {
        logger.log(level, message, exception);
        if (listener != null) {
            if (exception != null)
                message += " Exception: " + exception;
            final LogRecord lr = new LogRecord(level, message);
            final PrintStream printStream = listener.getLogger();
            printStream.print(sf.format(lr));
        }
    }

    @Override
    public Collection<NodeProvisioner.PlannedNode> provision(
            final Label label, final int excessWorkload) {
        try {
            return Queue.withLock(new Callable<Collection<NodeProvisioner.PlannedNode>>() {
                @Override
                public Collection<NodeProvisioner.PlannedNode> call()
                        throws Exception {
                    return provisionInternal(label, excessWorkload);
                }
            });
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "provisionInternal failed", exception);
            throw new IllegalStateException(exception);
        }
    }

    public synchronized Collection<NodeProvisioner.PlannedNode> provisionInternal(
            final Label label, final int excessWorkload) {


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

        if (targetCapacity > maxAllowed)
            targetCapacity = maxAllowed;

        int toProvision = targetCapacity - stats.getNumDesired();

        if (toProvision < 1)
            return Collections.emptyList();

        LOGGER.log(Level.INFO, "Provisioning nodes. Excess workload: " + Integer.toString(weightedExcessWorkload) + ", Provisioning: " + Integer.toString(toProvision));

        final ModifySpotFleetRequestRequest request = new ModifySpotFleetRequestRequest();
        request.setSpotFleetRequestId(fleet);
        request.setTargetCapacity(targetCapacity);

        final AmazonEC2 ec2 = connect(credentialsId, region);
        ec2.modifySpotFleetRequest(request);

        final List<NodeProvisioner.PlannedNode> resultList =
                new ArrayList<NodeProvisioner.PlannedNode>();
        for (int f = 0; f < toProvision; ++f) {
            final SettableFuture<Node> futureNode = SettableFuture.create();
            final NodeProvisioner.PlannedNode plannedNode =
                    new NodeProvisioner.PlannedNode("FleetNode-" + f, futureNode, this.numExecutors);
            resultList.add(plannedNode);
            this.plannedNodesCache.add(plannedNode);
        }
        return resultList;
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
                    LOGGER.log(Level.WARNING, "Error removing node " + instanceId);
                    throw new IllegalStateException(ex);
                }
            }
        }
    }

    public synchronized FleetStateStats updateStatus() {
        final AmazonEC2 ec2 = connect(credentialsId, region);
        final FleetStateStats curStatus = FleetStateStats.readClusterState(ec2, getFleet(), this.labelString);
        statusCache = curStatus;
        LOGGER.log(Level.FINE, "Fleet Update Status called");

        // Set up the lists of Jenkins nodes and fleet instances
        // currentFleetInstances contains instances currently in the fleet
        final Set<String> currentFleetInstances = new HashSet<String>(curStatus.getInstances());
        // currentJenkinsNodes contains all Nodes currently registered in Jenkins
        final Set<String> currentJenkinsNodes = new HashSet<String>();
        for (final Node node : Jenkins.getInstance().getNodes()) {
            currentJenkinsNodes.add(node.getNodeName());
        }
        // missingFleetInstances contains Jenkins nodes that were once fleet instances but are no longer in the fleet
        final Set<String> missingFleetInstances = new HashSet<String>();
        missingFleetInstances.addAll(currentJenkinsNodes);
        missingFleetInstances.retainAll(fleetInstancesCache);
        missingFleetInstances.removeAll(currentFleetInstances);
        // terminatedFleetInstances contains fleet instances that are terminated, stopped, stopping, or shutting down
        final Set<String> terminatedFleetInstances = new HashSet<String>();
        for (final String instance : currentFleetInstances) {
            try {
                if (isTerminated(ec2, instance)) terminatedFleetInstances.add(instance);
            } catch (final Exception ex) {
                LOGGER.log(Level.WARNING, "Unable to check the instance state of " + instance);
            }
        }
        // newFleetInstances contains running fleet instances that are not already Jenkins nodes
        final Set<String> newFleetInstances = new HashSet<String>();
        newFleetInstances.addAll(currentFleetInstances);
        newFleetInstances.removeAll(terminatedFleetInstances);
        newFleetInstances.removeAll(currentJenkinsNodes);

        // update caches
        dyingFleetInstancesCache.addAll(missingFleetInstances);
        dyingFleetInstancesCache.addAll(terminatedFleetInstances);
        dyingFleetInstancesCache.retainAll(currentJenkinsNodes);
        fleetInstancesCache.addAll(currentFleetInstances);
        fleetInstancesCache.removeAll(dyingFleetInstancesCache);
        fleetInstancesCache.retainAll(currentJenkinsNodes);

        LOGGER.log(Level.FINE, "# of current Jenkins nodes:" + currentJenkinsNodes.size());
        LOGGER.log(Level.FINE, "Fleet (" + getLabelString() + ") contains instances [" + join(", ", currentFleetInstances) + "]");
        LOGGER.log(Level.FINE, "Jenkins contains dying instances [" + join(", ", dyingFleetInstancesCache) + "]");
        LOGGER.log(Level.FINER, "Jenkins contains fleet instances [" + join(", ", fleetInstancesCache) + "]");
        LOGGER.log(Level.FINER, "Current Jenkins nodes [" + join(", ", currentJenkinsNodes) + "]");
        LOGGER.log(Level.FINER, "New fleet instances [" + join(", ", newFleetInstances) + "]");
        LOGGER.log(Level.FINER, "Missing fleet instances [" + join(", ", missingFleetInstances) + "]");
        LOGGER.log(Level.FINER, "Terminated fleet instances [" + join(", ", terminatedFleetInstances) + "]");

        // Remove dying fleet instances from Jenkins
        for (final String instance : dyingFleetInstancesCache) {
            LOGGER.log(Level.INFO, "Fleet (" + getLabelString() + ") no longer has the instance " + instance + ", removing from Jenkins.");
            removeNode(instance);
            dyingFleetInstancesCache.remove(instance);
        }

        // Update the label for all Jenkins nodes in the fleet instance cache
        for (final String instance : fleetInstancesCache) {
            Node node = Jenkins.getInstance().getNode(instance);
            if (node == null)
                continue;

            if (!this.labelString.equals(node.getLabelString())) {
                try {
                    LOGGER.log(Level.INFO, "Updating label on node " + instance + " to \"" + this.labelString + "\".");
                    node.setLabelString(this.labelString);
                } catch (final Exception ex) {
                    LOGGER.log(Level.WARNING, "Unable to set label on node " + instance);
                }
            }
        }

        // If we have new instances - create nodes for them!
        try {
            if (newFleetInstances.size() > 0) {
                LOGGER.log(Level.INFO, "Found new instances from fleet (" + getLabelString() + "): [" + join(", ", newFleetInstances) + "]");
            }
            for (final String instanceId : newFleetInstances) {
                addNewSlave(ec2, instanceId);
            }
        } catch (final Exception ex) {
            LOGGER.log(Level.WARNING, "Unable to add a new instance. Exception: " + ex.toString());
        }

        return curStatus;
    }


    private boolean isTerminated(final AmazonEC2 ec2, final String instanceId) throws Exception {
        final DescribeInstancesResult result = ec2.describeInstances(
                new DescribeInstancesRequest().withInstanceIds(instanceId));
        if (result.getReservations().isEmpty()) //Can't find this instance, assume it is terminated
            return true;
        final Instance instance = result.getReservations().get(0).getInstances().get(0);
        final String instanceState = instance.getState().getName();
        final Set<String> terminatedStates = new HashSet<String>();
        terminatedStates.add(InstanceStateName.Terminated.toString());
        terminatedStates.add(InstanceStateName.Stopped.toString());
        terminatedStates.add(InstanceStateName.Stopping.toString());
        terminatedStates.add(InstanceStateName.ShuttingDown.toString());

        return terminatedStates.contains(instanceState);
    }

    private void addNewSlave(final AmazonEC2 ec2, final String instanceId) throws Exception {
        // Generate a random FS root if one isn't specified
        String fsRoot = this.fsRoot;
        if (fsRoot == null || fsRoot.equals("")) {
            fsRoot = "/tmp/jenkins-" + UUID.randomUUID().toString().substring(0, 8);
        }

        final DescribeInstancesResult result = ec2.describeInstances(
                new DescribeInstancesRequest().withInstanceIds(instanceId));
        if (result.getReservations().isEmpty()) //Can't find this instance, skip it
            return;
        final Instance instance = result.getReservations().get(0).getInstances().get(0);
        final String address = isPrivateIpUsed() ?
                instance.getPrivateIpAddress() : instance.getPublicIpAddress();

        // Check if we have the address to use. Nodes don't get it immediately.
        if (address == null)
            return; // Wait some more...

        final FleetNode slave = new FleetNode(instanceId, "Fleet slave for" + instanceId,
                fsRoot, this.numExecutors.toString(), Node.Mode.NORMAL, this.labelString, new ArrayList<NodeProperty<?>>(),
                FLEET_CLOUD_ID, computerConnector.launch(address, TaskListener.NULL));

        // Initialize our retention strategy
        slave.setRetentionStrategy(new IdleRetentionStrategy(this));

        final Jenkins jenkins = Jenkins.getInstance();
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (jenkins) {
            // Try to avoid duplicate nodes
            final Node n = jenkins.getNode(instanceId);
            if (n != null)
                jenkins.removeNode(n);
            jenkins.addNode(slave);
        }

        //A new node, wheee!
        if (!plannedNodesCache.isEmpty()) {
            //If we're waiting for a new node - mark it as ready
            final NodeProvisioner.PlannedNode curNode = plannedNodesCache.iterator().next();
            plannedNodesCache.remove(curNode);
            ((SettableFuture<Node>) curNode.future).set(slave);
        }
    }

    public synchronized boolean terminateInstance(final String instanceId) {
        LOGGER.log(Level.INFO, "Attempting to terminate instance: " + instanceId);

        final FleetStateStats stats = updateStatus();

        if (!fleetInstancesCache.contains(instanceId)) {
            LOGGER.log(Level.INFO, "Unknown instance terminated: " + instanceId);
            return false;
        }

        final AmazonEC2 ec2 = connect(credentialsId, region);

        if (!dyingFleetInstancesCache.contains(instanceId)) {
            // We can't remove instances beyond minSize
            if (stats.getNumDesired() == this.getMinSize() || !"active".equals(stats.getState())) {
                LOGGER.log(Level.INFO, "Not terminating " + instanceId + " because we need a minimum of " + Integer.toString(this.getMinSize()) + " instances running.");
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
                        Messages._SlaveComputer_DisconnectedBy(this.FLEET_CLOUD_ID, this.fleet)));
            }
        }
        final Computer c = jenkins.getNode(instanceId).toComputer();
        try {
            c.waitUntilOffline();
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "Interrupted while diconnecting " + c.getDisplayName());
        }
        // terminateInstances is idempotent so it can be called until it's successful
        final TerminateInstancesResult result = ec2.terminateInstances(new TerminateInstancesRequest(Collections.singletonList(instanceId)));
        LOGGER.log(Level.INFO, "Instance " + instanceId + " termination result: " + result.toString());

        return true;
    }

    @Override
    public boolean canProvision(final Label label) {
        boolean result = fleet != null && (label == null || Label.parse(this.labelString).containsAll(label.listAtoms()));
        LOGGER.log(Level.FINE, "CanProvision called on fleet: \"" + this.labelString + "\" wanting: \"" + (label == null ? "(unspecified)" : label.getName()) + "\". Returning " + Boolean.toString(result) + ".");
        return result;
    }

    private static AmazonEC2 connect(final String credentialsId, final String region) {
        final AmazonWebServicesCredentials credentials = AWSCredentialsHelper.getCredentials(credentialsId, Jenkins.getInstance());
        final AmazonEC2Client client =
                credentials != null ?
                        new AmazonEC2Client(credentials) :
                        new AmazonEC2Client();
        if (region != null)
            client.setEndpoint("https://ec2." + region + ".amazonaws.com/");
        return client;
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
        public String privateKey;
        public boolean showNonActiveSpotFleets;

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

        public ListBoxModel doFillCredentialsIdItems() {
            return AWSCredentialsHelper.doFillCredentialsIdItems(Jenkins.getInstance());
        }

        public ListBoxModel doFillRegionItems(@QueryParameter final String credentialsId,
                                              @QueryParameter final String region) {
            final List<Region> regionList;

            try {
                final AmazonEC2 client = connect(credentialsId, null);
                final DescribeRegionsResult regions = client.describeRegions();
                regionList = regions.getRegions();
            } catch (final Exception ex) {
                //Ignore bad exceptions
                return new ListBoxModel();
            }

            final ListBoxModel model = new ListBoxModel();
            for (final Region reg : regionList) {
                model.add(new ListBoxModel.Option(reg.getRegionName(), reg.getRegionName()));
            }
            return model;
        }

        public ListBoxModel doFillFleetItems(@QueryParameter final boolean showNonActiveSpotFleets,
                                             @QueryParameter final String region,
                                             @QueryParameter final String credentialsId,
                                             @QueryParameter final String fleet) {
            final ListBoxModel model = new ListBoxModel();
            try {
                final AmazonEC2 client = connect(credentialsId, region);
                String token = null;
                do {
                    final DescribeSpotFleetRequestsRequest req = new DescribeSpotFleetRequestsRequest();
                    req.withNextToken(token);
                    final DescribeSpotFleetRequestsResult result = client.describeSpotFleetRequests(req);
                    for (final SpotFleetRequestConfig config : result.getSpotFleetRequestConfigs()) {
                        final String curFleetId = config.getSpotFleetRequestId();
                        final boolean selected = ObjectUtils.nullSafeEquals(fleet, curFleetId);
                        if (selected || showNonActiveSpotFleets || isSpotFleetActive(config)) {
                            final String displayStr = curFleetId + " (" + config.getSpotFleetRequestState() + ")";
                            model.add(new ListBoxModel.Option(displayStr, curFleetId, selected));
                        }
                    }
                    token = result.getNextToken();
                } while (token != null);

            } catch (final Exception ex) {
                //Ignore bad exceptions
                return model;
            }

            return model;
        }

        /**
         * @param config - config
         * @return return <code>true</code> not only for {@link BatchState#Active} but for any other
         * in which fleet in theory could accept load.
         */
        private boolean isSpotFleetActive(final SpotFleetRequestConfig config) {
            return BatchState.Active.toString().equals(config.getSpotFleetRequestState())
                    || BatchState.Modifying.toString().equals(config.getSpotFleetRequestState())
                    || BatchState.Submitted.toString().equals(config.getSpotFleetRequestState());
        }

        public FormValidation doTestConnection(
                @QueryParameter final String credentialsId,
                @QueryParameter final String region,
                @QueryParameter final String fleet) {
            try {
                final AmazonEC2 client = connect(credentialsId, region);
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

        public boolean isShowNonActiveSpotFleets() {
            return showNonActiveSpotFleets;
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
