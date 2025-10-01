package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.aws.EC2Api;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleet;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleets;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@SuppressWarnings({"deprecation"})
class AutoResubmitIntegrationTest extends IntegrationTest {

    private EC2FleetCloud.ExecutorScaler noScaling;

    @BeforeEach
    void before() {
        EC2Fleet ec2Fleet = mock(EC2Fleet.class);

        EC2Fleets.setGet(ec2Fleet);

        EC2Api ec2Api = spy(EC2Api.class);
        Registry.setEc2Api(ec2Api);

        when(ec2Fleet.getState(anyString(), anyString(), nullable(String.class), anyString())).thenReturn(
                new FleetStateStats("", 1, FleetStateStats.State.active(), Collections.singleton("i-1"),
                        Collections.emptyMap()));

        Ec2Client amazonEC2 = mock(Ec2Client.class);
        when(ec2Api.connect(anyString(), anyString(), Mockito.nullable(String.class))).thenReturn(amazonEC2);

        final Instance instance = Instance.builder()
                .state(InstanceState.builder().name(InstanceStateName.RUNNING)
                        .build())
                .publicIpAddress("public-io")
                .instanceId("i-1")
                .build();

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(
                DescribeInstancesResponse.builder().reservations(
                        Reservation.builder().instances(
                                instance
                        )
                        .build())
                .build());

        noScaling = new EC2FleetCloud.NoScaler();
    }

    @Test
    void should_successfully_resubmit_freestyle_task() throws Exception {
        EC2FleetCloud cloud = new EC2FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, new LocalComputerConnector(j), false, false,
                0, 0, 10, 0, 1, false, true,
                "-1", false, 0, 0,
                10, false, false, noScaling, false, false);
        j.jenkins.clouds.add(cloud);

        List<QueueTaskFuture> rs = enqueTask(1);
        triggerSuggestReviewNow();

        assertAtLeastOneNode();

        final Node node = j.jenkins.getNodes().get(0);
        assertQueueIsEmpty();

        System.out.println("disconnect node");
        // Regardless of cause for disconnect, we should resubmit the job
        node.toComputer().disconnect(new OfflineCause.ByCLI("disconnect"));

        // due to test nature job could be failed if started or aborted as we call disconnect
        // in prod code it's not matter
        assertLastBuildResult(Result.FAILURE, Result.ABORTED);

        node.toComputer().connect(true);
        assertNodeIsOnline(node);
        assertQueueAndNodesIdle(node);

        assertEquals(1, j.jenkins.getProjects().size());
        assertEquals(Result.SUCCESS, j.jenkins.getProjects().get(0).getLastBuild().getResult());
        assertEquals(2, j.jenkins.getProjects().get(0).getBuilds().size());

        cancelTasks(rs);
    }

    @Test
    void should_successfully_resubmit_parametrized_task() throws Exception {
        EC2FleetCloud cloud = new EC2FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, new LocalComputerConnector(j), false, false,
                0, 0, 10, 0, 1, false, true,
                "-1", false, 0, 0,
                10, false, false, noScaling, false, false);
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

        triggerSuggestReviewNow();
        assertAtLeastOneNode();

        final Node node = j.jenkins.getNodes().get(0);
        assertQueueIsEmpty();

        System.out.println("disconnect node");
        // Regardless of cause for disconnect, we should resubmit the job
        node.toComputer().disconnect(new OfflineCause.ChannelTermination(new UnsupportedOperationException("Test")));

        assertLastBuildResult(Result.FAILURE, Result.ABORTED);

        node.toComputer().connect(true);
        assertNodeIsOnline(node);
        assertQueueAndNodesIdle(node);

        assertEquals(1, j.jenkins.getProjects().size());
        assertEquals(Result.SUCCESS, j.jenkins.getProjects().get(0).getLastBuild().getResult());
        assertEquals(2, j.jenkins.getProjects().get(0).getBuilds().size());

        cancelTasks(rs);
    }

    @Test
    void should_not_resubmit_if_disabled() throws Exception {
        EC2FleetCloud cloud = new EC2FleetCloud("TestCloud", "credId", null, "region",
                null, "fId", "momo", null, new LocalComputerConnector(j), false, false,
                0, 0, 10, 0, 1, false, true,
                "-1", true, 0, 0, 10, false, false, noScaling, false, false);
        j.jenkins.clouds.add(cloud);

        List<QueueTaskFuture> rs = enqueTask(1);
        triggerSuggestReviewNow();

        assertAtLeastOneNode();

        final Node node = j.jenkins.getNodes().get(0);
        assertQueueIsEmpty();

        System.out.println("disconnect node");
        // Regardless of cause for disconnect, we should resubmit the job
        node.toComputer().disconnect(new OfflineCause.IdleOfflineCause());

        assertLastBuildResult(Result.FAILURE, Result.ABORTED);

        node.toComputer().connect(true);
        assertNodeIsOnline(node);
        assertQueueAndNodesIdle(node);

        assertEquals(1, j.jenkins.getProjects().size());
        assertEquals(Result.FAILURE, j.jenkins.getProjects().get(0).getLastBuild().getResult());
        assertEquals(1, j.jenkins.getProjects().get(0).getBuilds().size());

        cancelTasks(rs);
    }

}
