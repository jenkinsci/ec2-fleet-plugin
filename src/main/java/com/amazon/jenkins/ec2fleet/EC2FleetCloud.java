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
import com.amazonaws.services.ec2.model.SpotFleetRequestConfig;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsHelper;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.google.common.util.concurrent.SettableFuture;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.security.ACL;
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
    private final ComputerConnector computerConnector;
    private final boolean privateIpUsed;
    private final Integer idleMinutes;
    private final Integer maxSize;
    private final Integer numExecutors;
    private @Nonnull FleetStateStats status;

    private final Set<NodeProvisioner.PlannedNode> plannedNodes =
            new HashSet<NodeProvisioner.PlannedNode>();
    private final Set<String> instancesSeen = new HashSet<String>();
    private final Set<String> instancesDying = new HashSet<String>();

    @DataBoundConstructor
    public EC2FleetCloud(final String credentialsId,
                         final String region, final String fleet,
                         final ComputerConnector computerConnector,
                         final boolean privateIpUsed,
                         final Integer idleMinutes, final Integer maxSize,
                         final Integer numExecutors) {
        super(FLEET_CLOUD_ID);
        this.credentialsId = credentialsId;
        this.region = region;
        this.fleet = fleet;
        this.computerConnector = computerConnector;
        this.idleMinutes = idleMinutes;
        this.privateIpUsed = privateIpUsed;
        this.maxSize = maxSize;
        this.numExecutors = numExecutors;

        this.status = new FleetStateStats(fleet, 0, "Initializing", Collections.<String>emptySet());
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

    public ComputerConnector getComputerConnector() {
        return computerConnector;
    }

    public boolean isPrivateIpUsed() {
        return privateIpUsed;
    }

    public Integer getIdleMinutes() {
        return idleMinutes;
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public String getNumExecutors() {
        return numExecutors.toString();
    }

    public String getJvmSettings() {
        return "";
    }

    public @Nonnull FleetStateStats getStatus() {
        return status;
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

    @Override public synchronized Collection<NodeProvisioner.PlannedNode> provision(
            final Label label, final int excessWorkload) {

        final FleetStateStats stats=updateStatus();
        final int maxAllowed = this.getMaxSize();

        if (stats.getNumDesired() >= maxAllowed || !"active".equals(stats.getState()))
            return Collections.emptyList();

        final ModifySpotFleetRequestRequest request=new ModifySpotFleetRequestRequest();
        request.setSpotFleetRequestId(fleet);
        request.setTargetCapacity(stats.getNumDesired() + excessWorkload);

        final AmazonEC2 ec2=connect(credentialsId, region);
        ec2.modifySpotFleetRequest(request);

        final List<NodeProvisioner.PlannedNode> resultList =
                new ArrayList<NodeProvisioner.PlannedNode>();
        for(int f=0;f<excessWorkload; ++f)
        {
            final SettableFuture<Node> futureNode=SettableFuture.create();
            final NodeProvisioner.PlannedNode plannedNode=
                    new NodeProvisioner.PlannedNode("FleetNode-"+f, futureNode, 1);
            resultList.add(plannedNode);
            this.plannedNodes.add(plannedNode);
        }
        return resultList;
    }

    public synchronized FleetStateStats updateStatus() {
        final AmazonEC2 ec2=connect(credentialsId, region);
        final FleetStateStats curStatus=FleetStateStats.readClusterState(ec2, getFleet());
        status = curStatus;

        // Check the nodes to see if we have some new ones
        final Set<String> newInstances = new HashSet<String>(curStatus.getInstances());
        final Set<String> jenkinsInstances = new HashSet<String>();
        for(final Node node : Jenkins.getActiveInstance().getNodes()) {
            newInstances.remove(node.getDisplayName());
            jenkinsInstances.add(node.getDisplayName());
        }
        newInstances.removeAll(instancesSeen);
        newInstances.removeAll(instancesDying);

        // Instance unknown to Jenkins but known to Fleet. Terminate it.
        for(final String instId : instancesSeen) {
            if (!instancesDying.contains(instId) &&
                    !jenkinsInstances.contains(instId)) {
                // Use a nuclear option to terminate an unknown instance
                ec2.terminateInstances(new TerminateInstancesRequest(Collections.singletonList(instId)));
            }
        }

        // If we have new instances - create nodes for them!
        for(final String instanceId : newInstances) {
            try {
                addNewSlave(ec2, instanceId);
            } catch(final Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        return curStatus;
    }

    private void addNewSlave(final AmazonEC2 ec2, final String instanceId) throws Exception {
        // Generate a random FS root
        final String fsRoot = "/tmp/jenkins-"+UUID.randomUUID().toString().substring(0, 8);

        final DescribeInstancesResult result=ec2.describeInstances(
                new DescribeInstancesRequest().withInstanceIds(instanceId));
        if (result.getReservations().isEmpty()) //Can't find this instance, skip it
            return;
        final Instance instance=result.getReservations().get(0).getInstances().get(0);
        final String address = isPrivateIpUsed() ?
                instance.getPrivateIpAddress() : instance.getPublicIpAddress();

        // Check if we have the address to use. Nodes don't get it immediately.
        if (address == null)
            return; // Wait some more...

        final FleetNode slave = new FleetNode(instanceId, "Fleet slave for" + instanceId,
                fsRoot, this.getNumExecutors(), Node.Mode.NORMAL, "ec2-fleet", new ArrayList<NodeProperty<?>>(),
                FLEET_CLOUD_ID, computerConnector.launch(address, TaskListener.NULL));

        // Initialize our retention strategy
        if (getIdleMinutes() != null)
            slave.setRetentionStrategy(new IdleRetentionStrategy(getIdleMinutes(), this));

        final Jenkins jenkins=Jenkins.getActiveInstance();
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (jenkins) {
            // Try to avoid duplicate nodes
            final Node n = jenkins.getNode(name);
            if (n != null)
                jenkins.removeNode(n);
            jenkins.addNode(slave);
        }

        //A new node, wheee!
        instancesSeen.add(instanceId);
        if (!plannedNodes.isEmpty())
        {
            //If we're waiting for a new node - mark it as ready
            final NodeProvisioner.PlannedNode curNode=plannedNodes.iterator().next();
            plannedNodes.remove(curNode);
            ((SettableFuture<Node>)curNode.future).set(slave);
        }
    }

    public synchronized void terminateInstance(final String instanceId) {
        if (!instancesSeen.contains(instanceId) || instancesDying.contains(instanceId))
            throw new IllegalStateException("Unknown instance terminated: " + instanceId);

        final FleetStateStats stats=updateStatus();
        //We can't remove the last instance
        if (stats.getNumDesired() == 1 || !"active".equals(stats.getState()))
            return;

        final AmazonEC2 ec2 = connect(credentialsId, region);

        final ModifySpotFleetRequestRequest request=new ModifySpotFleetRequestRequest();
        request.setSpotFleetRequestId(fleet);
        request.setTargetCapacity(stats.getNumDesired() - 1);
        request.setExcessCapacityTerminationPolicy("NoTermination");
        ec2.modifySpotFleetRequest(request);

        ec2.terminateInstances(new TerminateInstancesRequest(Collections.singletonList(instanceId)));

        //And remove the instance
        instancesSeen.remove(instanceId);
        instancesDying.add(instanceId);
    }

    @Override public boolean canProvision(final Label label) {
        return fleet != null;
    }

    private static AmazonEC2 connect(final String credentialsId, final String region) {

        final AmazonWebServicesCredentials credentials = AWSCredentialsHelper.getCredentials(credentialsId, Jenkins.getActiveInstance());
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
            return Jenkins.getActiveInstance().getDescriptorList(ComputerConnector.class);
        }

        public ListBoxModel doFillCredentialsIdItems() {
            return AWSCredentialsHelper.doFillCredentialsIdItems(Jenkins.getActiveInstance());
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
