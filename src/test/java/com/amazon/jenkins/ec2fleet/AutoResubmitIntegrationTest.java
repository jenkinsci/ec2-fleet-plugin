package com.amazon.jenkins.ec2fleet;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.ActiveInstance;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSpotFleetInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfig;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfigData;
import hudson.Functions;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.OfflineCause;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@SuppressWarnings({"ArraysAsListWithZeroOrOneArgument", "deprecation"})
public class AutoResubmitIntegrationTest extends IntegrationTest {

    @Before
    public void before() {
        EC2Api ec2Api = spy(EC2Api.class);
        Registry.setEc2Api(ec2Api);

        AmazonEC2 amazonEC2 = mock(AmazonEC2.class);
        when(ec2Api.connect(anyString(), anyString(), Mockito.nullable(String.class))).thenReturn(amazonEC2);

        final Instance instance = new Instance()
                .withState(new InstanceState().withName(InstanceStateName.Running))
                .withPublicIpAddress("public-io")
                .withInstanceId("i-1");

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(
                new DescribeInstancesResult().withReservations(
                        new Reservation().withInstances(
                                instance
                        )));

        when(amazonEC2.describeSpotFleetInstances(any(DescribeSpotFleetInstancesRequest.class)))
                .thenReturn(new DescribeSpotFleetInstancesResult()
                        .withActiveInstances(new ActiveInstance().withInstanceId("i-1")));

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = new DescribeSpotFleetRequestsResult();
        describeSpotFleetRequestsResult.setSpotFleetRequestConfigs(Arrays.asList(
                new SpotFleetRequestConfig()
                        .withSpotFleetRequestState("active")
                        .withSpotFleetRequestConfig(
                                new SpotFleetRequestConfigData().withTargetCapacity(1))));
        when(amazonEC2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);
    }

    @Test
    public void should_successfully_resubmit_freestyle_task() throws Exception {
        EC2FleetCloud cloud = new EC2FleetCloud(null, null, "credId", null, "region",
                null, "fId", "momo", null, new SingleLocalComputerConnector(j), false, false,
                0, 0, 10, 1, false, false,
                false, 0, 0, false);
        j.jenkins.clouds.add(cloud);

        List<QueueTaskFuture> rs = getQueueTaskFutures(1);

        System.out.println("check if zero nodes!");
        Assert.assertEquals(0, j.jenkins.getNodes().size());

        assertAtLeastOneNode();

        final Node node = j.jenkins.getNodes().get(0);
        assertQueueIsEmpty();

        System.out.println("disconnect node");
        node.toComputer().disconnect(new OfflineCause.ChannelTermination(new UnsupportedOperationException("Test")));

        // due to test nature job could be failed if started or aborted as we call disconnect
        // in prod code it's not matter
        assertLastBuildResult(Result.FAILURE, Result.ABORTED);

        node.toComputer().connect(true);
        assertNodeIsOnline(node);
        assertQueueAndNodesIdle(node);

        Assert.assertEquals(1, j.jenkins.getProjects().size());
        Assert.assertEquals(Result.SUCCESS, j.jenkins.getProjects().get(0).getLastBuild().getResult());
        Assert.assertEquals(2, j.jenkins.getProjects().get(0).getBuilds().size());

        cancelTasks(rs);
    }

    @Test
    public void should_successfully_resubmit_parametrized_task() throws Exception {
        EC2FleetCloud cloud = new EC2FleetCloud(null, null, "credId", null, "region",
                null, "fId", "momo", null, new SingleLocalComputerConnector(j), false, false,
                0, 0, 10, 1, false, false,
                false, 0, 0, false);
        j.jenkins.clouds.add(cloud);

        List<QueueTaskFuture> rs = new ArrayList<>();
        final FreeStyleProject project = j.createFreeStyleProject();
        project.setAssignedLabel(new LabelAtom("momo"));
        project.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition("number", "opa")));
        /*
        example of actions for project

        actions = {CopyOnWriteArrayList@14845}  size = 2
            0 = {ParametersAction@14853}
            safeParameters = {TreeSet@14855}  size = 0
            parameters = {ArrayList@14856}  size = 1
            0 = {StringParameterValue@14862} "(StringParameterValue) number='1'"
            value = "1"
            name = "number"
            description = ""
            parameterDefinitionNames = {ArrayList@14857}  size = 1
            0 = "number"
            build = null
            run = {FreeStyleBuild@14834} "parameter #14"
         */
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("Ping -n %number% 127.0.0.1 > nul") : new Shell("sleep ${number}"));

        rs.add(project.scheduleBuild2(0, new ParametersAction(new StringParameterValue("number", "30"))));

        System.out.println("check if zero nodes!");
        Assert.assertEquals(0, j.jenkins.getNodes().size());

        assertAtLeastOneNode();

        final Node node = j.jenkins.getNodes().get(0);
        assertQueueIsEmpty();

        System.out.println("disconnect node");
        node.toComputer().disconnect(new OfflineCause.ChannelTermination(new UnsupportedOperationException("Test")));

        assertLastBuildResult(Result.FAILURE, Result.ABORTED);

        node.toComputer().connect(true);
        assertNodeIsOnline(node);
        assertQueueAndNodesIdle(node);

        Assert.assertEquals(1, j.jenkins.getProjects().size());
        Assert.assertEquals(Result.SUCCESS, j.jenkins.getProjects().get(0).getLastBuild().getResult());
        Assert.assertEquals(2, j.jenkins.getProjects().get(0).getBuilds().size());

        cancelTasks(rs);
    }

    @Test
    public void should_not_resubmit_if_disabled() throws Exception {
        EC2FleetCloud cloud = new EC2FleetCloud(null, null, "credId", null, "region",
                null, "fId", "momo", null, new SingleLocalComputerConnector(j), false, false,
                0, 0, 10, 1, false, false,
                true, 0, 0, false);
        j.jenkins.clouds.add(cloud);

        List<QueueTaskFuture> rs = getQueueTaskFutures(1);

        System.out.println("check if zero nodes!");
        Assert.assertEquals(0, j.jenkins.getNodes().size());

        assertAtLeastOneNode();

        final Node node = j.jenkins.getNodes().get(0);
        assertQueueIsEmpty();

        System.out.println("disconnect node");
        node.toComputer().disconnect(new OfflineCause.ChannelTermination(new UnsupportedOperationException("Test")));

        assertLastBuildResult(Result.FAILURE, Result.ABORTED);

        node.toComputer().connect(true);
        assertNodeIsOnline(node);
        assertQueueAndNodesIdle(node);

        Assert.assertEquals(1, j.jenkins.getProjects().size());
        Assert.assertEquals(Result.FAILURE, j.jenkins.getProjects().get(0).getLastBuild().getResult());
        Assert.assertEquals(1, j.jenkins.getProjects().get(0).getBuilds().size());

        cancelTasks(rs);
    }

}
