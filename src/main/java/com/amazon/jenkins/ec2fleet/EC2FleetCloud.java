package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.cloud.FleetNode;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeRegionsResult;
import com.amazonaws.services.ec2.model.DescribeSpotFleetInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.ModifySpotFleetRequestRequest;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfig;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsHelper;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import com.google.common.util.concurrent.SettableFuture;
import hudson.Extension;
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
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.springframework.util.ObjectUtils;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
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
 * User: cyberax
 * Date: 12/14/15
 * Time: 22:22
 */
@SuppressWarnings("unused")
public class EC2FleetCloud extends Cloud
{
    public static final String FLEET_CLOUD_ID="FleetCloud";
    private static final SimpleFormatter sf = new SimpleFormatter();

    private final String credentialsId;
    private final String region;
    private final String fleet;
    private final String fsRoot;
    private final ComputerConnector computerConnector;
    private final boolean privateIpUsed;
    private final String labelString;
    private final Integer idleMinutes;
    private final Integer minSize;
    private final Integer maxSize;
    private final Integer numExecutors;


    private transient @Nonnull FleetStateStats statusCache;

    private transient Set<NodeProvisioner.PlannedNode> plannedNodesCache;
    // instancesSeenCache are all nodes known to both Jenkins and the fleet
    private transient Set<String> instancesSeenCache;
    // instancesDyingCache are terminated nodes known to both Jenkins and the fleet,
    // that are waiting for termination
    private transient Set<String> instancesDyingCache;

    private static final Logger LOGGER = Logger.getLogger(EC2FleetCloud.class.getName());

    public static String join(final String separator, final Iterable<String> elements) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (String i: elements) {
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
        instancesSeenCache = new HashSet<String>();
        instancesDyingCache = new HashSet<String>();
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

    public String getLabelString(){
        return this.labelString;
    }

    public Integer getIdleMinutes() {
        return idleMinutes;
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

    public @Nonnull FleetStateStats getStatusCache() {
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

    @Override public Collection<NodeProvisioner.PlannedNode> provision(
            final Label label, final int excessWorkload) {
        try {
            return Queue.withLock(new Callable<Collection<NodeProvisioner.PlannedNode>>()
            {
                @Override
                public Collection<NodeProvisioner.PlannedNode> call()
                        throws Exception
                {
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


        final FleetStateStats stats=updateStatus();
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

        final ModifySpotFleetRequestRequest request=new ModifySpotFleetRequestRequest();
        request.setSpotFleetRequestId(fleet);
        request.setTargetCapacity(targetCapacity);

        final AmazonEC2 ec2=connect(credentialsId, region);
        ec2.modifySpotFleetRequest(request);

        final List<NodeProvisioner.PlannedNode> resultList =
                new ArrayList<NodeProvisioner.PlannedNode>();
        for(int f=0;f<toProvision; ++f)
        {
            final SettableFuture<Node> futureNode=SettableFuture.create();
            final NodeProvisioner.PlannedNode plannedNode=
                    new NodeProvisioner.PlannedNode("FleetNode-"+f, futureNode, this.numExecutors);
            resultList.add(plannedNode);
            this.plannedNodesCache.add(plannedNode);
        }
        return resultList;
    }

    private synchronized void removeNode(String instanceId) {
        final Jenkins jenkins=Jenkins.getInstance();
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (jenkins) {
            // If this node is dying, remove it from Jenkins
            final Node n = jenkins.getNode(instanceId);
            if (n != null) {
                try {
                    jenkins.removeNode(n);
                } catch(final Exception ex) {
                    LOGGER.log(Level.WARNING, "Error removing node " + instanceId);
                    throw new IllegalStateException(ex);
                }
            }
        }
    }

    public synchronized FleetStateStats updateStatus() {
        final AmazonEC2 ec2=connect(credentialsId, region);
        final FleetStateStats curStatus=FleetStateStats.readClusterState(ec2, getFleet(), this.labelString);
        statusCache = curStatus;
        LOGGER.log(Level.FINE, "Fleet Update Status called");
        LOGGER.log(Level.FINE, "# of nodes:" + Jenkins.getInstance().getNodes().size());

        // Check the nodes to see if we have some new ones
        final Set<String> newInstances = new HashSet<String>(curStatus.getInstances());
        instancesSeenCache.clear();
        LOGGER.log(Level.FINE, "Fleet (" + getLabelString() + ") contains instances [" + join(", ", newInstances) + "]");
        LOGGER.log(Level.FINE, "Jenkins contains dying instances [" + join(", ", instancesDyingCache) + "]");
        for(final Node node : Jenkins.getInstance().getNodes()) {
            if (newInstances.contains(node.getNodeName())) {
                // instancesSeenCache should only have the intersection of nodes
                // known by Jenkins and by the fleet.
                instancesSeenCache.add(node.getNodeName());
            } else if (instancesDyingCache.contains(node.getNodeName())) {
                LOGGER.log(Level.INFO, "Fleet (" + getLabelString() + ") no longer has the instance " + node.getNodeName() + ", removing from Jenkins.");
                removeNode(node.getNodeName());
                instancesDyingCache.remove(node.getNodeName());
                instancesSeenCache.remove(node.getNodeName());
            }
        }

        // We should only keep dying instances that are still visible to both
        // Jenkins and the fleet.
        instancesDyingCache.retainAll(instancesSeenCache);

        // New instances are only ones that Jenkins hasn't seen
        newInstances.removeAll(instancesSeenCache);

        // Update the label for all seen instances, unless they're dying
        for(final String instId : instancesSeenCache) {
            if (instancesDyingCache.contains(instId))
                continue;

            Node node = Jenkins.getInstance().getNode(instId);
            if (node == null)
                continue;

            if (!this.labelString.equals(node.getLabelString())) {
                try {
                    LOGGER.log(Level.INFO, "Updating label on node " + instId + " to \"" + this.labelString + "\".");
                    node.setLabelString(this.labelString);
                } catch (final Exception ex) {
                    LOGGER.log(Level.WARNING, "Unable to set label on node " + instId);
                }
            }
        }

        // If we have new instances - create nodes for them!
        try {
            if (newInstances.size() > 0) {
                LOGGER.log(Level.INFO, "Found new instances from fleet (" + getLabelString() + "): [" + join(", ", newInstances) + "]");
            }
            for(final String instanceId : newInstances) {
                addNewSlave(ec2, instanceId);
            }
        } catch(final Exception ex) {
            LOGGER.log(Level.WARNING, "Unable to add a new instance. Exception: " + ex.toString());
        }

        return curStatus;
    }

    private Instance instanceFromResult(final DescribeInstancesResult result, final String instanceId) {
        // Obtain an instance with a specific instanceId from a DescribeInstancesResult
        Instance instance = null;
        final List<Reservation> reservations = result.getReservations();
        outerloop:
        for(final Reservation reservation : reservations) {
            final List<Instance> instances=reservation.getInstances();
            final String resId = reservation.getReservationId();
            for(final Instance instanceCandidate : instances) {
                final String id = instanceCandidate.getInstanceId();
                final String address = instanceCandidate.getPrivateIpAddress();
                LOGGER.log(Level.FINE, "instance in reservation: " + resId + " "  + id + " " + address);
                if(id.equals(instanceId)) {
                    LOGGER.log(Level.FINE, "it's a match: " + id + " " + instanceId);
                    instance = instanceCandidate;
                    break outerloop;
                } else {
                    LOGGER.log(Level.FINE, "it's NOT a match: " + id + " " + instanceId);
                }
            }
        }
        return instance;
    }

    private void addNewSlave(final AmazonEC2 ec2, final String instanceId) throws Exception {
        // Generate a random FS root if one isn't specified
        String fsRoot = this.fsRoot;
        if (fsRoot == null || fsRoot.equals("")) {
            fsRoot = "/tmp/jenkins-"+UUID.randomUUID().toString().substring(0, 8);
        }

        final DescribeInstancesResult result=ec2.describeInstances(
                new DescribeInstancesRequest().withInstanceIds(instanceId));
        if (result.getReservations().isEmpty()) //Can't find this instance, skip it
            return;
        final Instance instance = instanceFromResult(result, instanceId);
        final String address = isPrivateIpUsed() ?
                instance.getPrivateIpAddress() : instance.getPublicIpAddress();

        // Check if we have the address to use. Nodes don't get it immediately.
        if (address == null)
            return; // Wait some more...

        final FleetNode slave = new FleetNode(instanceId, "Fleet slave for" + instanceId,
                fsRoot, this.numExecutors.toString(), Node.Mode.NORMAL, this.labelString, new ArrayList<NodeProperty<?>>(),
                FLEET_CLOUD_ID, computerConnector.launch(address, TaskListener.NULL));

        // Initialize our retention strategy
        if (getIdleMinutes() != null)
            slave.setRetentionStrategy(new IdleRetentionStrategy(getIdleMinutes(), this));

        final Jenkins jenkins=Jenkins.getInstance();
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (jenkins) {
            // Try to avoid duplicate nodes
            final Node n = jenkins.getNode(instanceId);
            if (n != null)
                jenkins.removeNode(n);
            jenkins.addNode(slave);
        }

        //A new node, wheee!
        instancesSeenCache.add(instanceId);
        if (!plannedNodesCache.isEmpty())
        {
            //If we're waiting for a new node - mark it as ready
            final NodeProvisioner.PlannedNode curNode= plannedNodesCache.iterator().next();
            plannedNodesCache.remove(curNode);
            ((SettableFuture<Node>)curNode.future).set(slave);
        }
    }

    public synchronized boolean terminateInstance(final String instanceId) {
        LOGGER.log(Level.INFO, "Attempting to terminate instance: " + instanceId);

        final FleetStateStats stats=updateStatus();

        if (!instancesSeenCache.contains(instanceId)) {
            LOGGER.log(Level.INFO, "Unknown instance terminated: " + instanceId);
            return false;
        }

        final AmazonEC2 ec2 = connect(credentialsId, region);

        if (!instancesDyingCache.contains(instanceId)) {
            // We can't remove instances beyond minSize
            if (stats.getNumDesired() == this.getMinSize() || !"active".equals(stats.getState())) {
                LOGGER.log(Level.INFO, "Not terminating " + instanceId + " because we need a minimum of " + Integer.toString(this.getMinSize()) + " instances running.");
                return false;
            }

            // These operations aren't idempotent so only do them once
            final ModifySpotFleetRequestRequest request=new ModifySpotFleetRequestRequest();
            request.setSpotFleetRequestId(fleet);
            request.setTargetCapacity(stats.getNumDesired() - 1);
            request.setExcessCapacityTerminationPolicy("NoTermination");
            ec2.modifySpotFleetRequest(request);

            //And remove the instance
            instancesDyingCache.add(instanceId);
        }

        // terminateInstances is idempotent so it can be called until it's successful
        final TerminateInstancesResult result = ec2.terminateInstances(new TerminateInstancesRequest(Collections.singletonList(instanceId)));
        LOGGER.log(Level.INFO, "Instance " + instanceId + " termination result: " + result.toString());

        return true;
    }

    @Override public boolean canProvision(final Label label) {
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
        public String userName="root";
        public boolean privateIpUsed;
        public String privateKey;

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
                                              @QueryParameter final String region)
                throws IOException, ServletException {
            final List<Region> regionList;

            try {
                final AmazonEC2 client = connect(credentialsId, null);
                final DescribeRegionsResult regions=client.describeRegions();
                regionList=regions.getRegions();
            } catch(final Exception ex) {
                //Ignore bad exceptions
                return new ListBoxModel();
            }

            final ListBoxModel model = new ListBoxModel();
            for(final Region reg : regionList) {
                model.add(new ListBoxModel.Option(reg.getRegionName(), reg.getRegionName()));
            }
            return model;
        }

        public ListBoxModel doFillFleetItems(@QueryParameter final String region,
                                             @QueryParameter final String credentialsId,
                                             @QueryParameter final String fleet)
                throws IOException, ServletException {

            final ListBoxModel model = new ListBoxModel();
            try {
                final AmazonEC2 client=connect(credentialsId, region);
                String token = null;
                do {
                    final DescribeSpotFleetRequestsRequest req=new DescribeSpotFleetRequestsRequest();
                    req.withNextToken(token);
                    final DescribeSpotFleetRequestsResult result=client.describeSpotFleetRequests(req);
                    for(final SpotFleetRequestConfig config : result.getSpotFleetRequestConfigs()) {
                        final String curFleetId=config.getSpotFleetRequestId();
                        final String displayStr=curFleetId+
                                " ("+config.getSpotFleetRequestState()+")";
                        model.add(new ListBoxModel.Option(displayStr, curFleetId,
                                ObjectUtils.nullSafeEquals(fleet, curFleetId)));
                    }
                    token = result.getNextToken();
                } while(token != null);

            } catch(final Exception ex) {
                //Ignore bad exceptions
                return model;
            }

            return model;
        }

        public FormValidation doTestConnection(
                @QueryParameter final String credentialsId,
                @QueryParameter final String region,
                @QueryParameter final String fleet)
        {
            try {
                final AmazonEC2 client=connect(credentialsId, region);
                client.describeSpotFleetInstances(
                        new DescribeSpotFleetInstancesRequest().withSpotFleetRequestId(fleet));
            } catch(final Exception ex)
            {
                return FormValidation.error(ex.getMessage());
            }
            return FormValidation.ok("Success");
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req,formData);
        }

        public boolean isUseInstanceProfileForCredentials() {
            return useInstanceProfileForCredentials;
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
