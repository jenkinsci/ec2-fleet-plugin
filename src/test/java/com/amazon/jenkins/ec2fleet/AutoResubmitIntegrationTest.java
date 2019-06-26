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
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfig;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfigData;
import hudson.Functions;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.ComputerConnector;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.OfflineCause;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AutoResubmitIntegrationTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @ClassRule
    public static BuildWatcher bw = new BuildWatcher();

    @Before
    public void before() {
        EC2Api ec2Api = mock(EC2Api.class);
        Registry.setEc2Api(ec2Api);

        AmazonEC2 amazonEC2 = mock(AmazonEC2.class);
        when(ec2Api.connect(anyString(), anyString(), Mockito.nullable(String.class))).thenReturn(amazonEC2);

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(
                new DescribeInstancesResult().withReservations(
                        new Reservation().withInstances(
                                new Instance()
                                        .withPublicIpAddress("public-io")
                                        .withInstanceId("i-1")
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

    @After
    public void after() {
        // restore
        Registry.setEc2Api(new EC2Api());
    }

    @Test
    public void should_successfully_resubmit_freestyle_task() throws Exception {
        EC2FleetCloud cloud = new EC2FleetCloud(null, "credId", null, "region",
                null, "fId", "momo", null, new SingleComputerConnector(j), false, false,
                0, 0, 10, 1, false, false, false);
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
        EC2FleetCloud cloud = new EC2FleetCloud(null, "credId", null, "region",
                null, "fId", "momo", null, new SingleComputerConnector(j), false, false,
                0, 0, 10, 1, false, false, false);
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
        EC2FleetCloud cloud = new EC2FleetCloud(null, "credId", null, "region",
                null, "fId", "momo", null, new SingleComputerConnector(j), false, false,
                0, 0, 10, 1, false, false, true);
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

    private static void assertQueueAndNodesIdle(final Node node) {
        tryUntil(new Runnable() {
            @Override
            public void run() {
                Assert.assertTrue(Queue.getInstance().isEmpty() && node.toComputer().isIdle());
            }
        });
    }

    private static void assertQueueIsEmpty() {
        tryUntil(new Runnable() {
            @Override
            public void run() {
                System.out.println("check if queue is empty");
                Assert.assertTrue(Queue.getInstance().isEmpty());
            }
        });
    }

    private void assertAtLeastOneNode() {
        tryUntil(new Runnable() {
            @Override
            public void run() {
                System.out.println("check if non zero nodes!");
                Assert.assertFalse(j.jenkins.getNodes().isEmpty());
            }
        });
    }

    private void assertLastBuildResult(final Result... lastBuildResults) {
        tryUntil(new Runnable() {
            @Override
            public void run() {
                final AbstractBuild lastBuild = j.jenkins.getProjects().get(0).getLastBuild();
                System.out.println("wait until " + Arrays.toString(lastBuildResults) + " current state "
                        + (lastBuild == null ? "<none>" : lastBuild.getResult()));
                Assert.assertNotNull(lastBuild);
                Assert.assertTrue(
                        lastBuild.getResult() + " should be in " + Arrays.toString(lastBuildResults),
                        Arrays.asList(lastBuildResults).contains(lastBuild.getResult()));
            }
        });
    }

    private void assertNodeIsOnline(final Node node) {
        tryUntil(new Runnable() {
            @Override
            public void run() {
                System.out.println("wait when back online");
                Assert.assertTrue(node.toComputer().isOnline());
            }
        });
    }

    private static void tryUntil(Runnable r) {
        long d = TimeUnit.SECONDS.toMillis(5);
        long time = TimeUnit.SECONDS.toMillis(60);
        final long start = System.currentTimeMillis();

        while (true) {
            try {
                r.run();
                return;
            } catch (AssertionError e) {
                if (System.currentTimeMillis() - start > time) {
                    throw e;
                } else {
                    try {
                        Thread.sleep(d);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private void cancelTasks(List<QueueTaskFuture> rs) {
        for (QueueTaskFuture r : rs) {
            r.cancel(true);
        }
    }

    private List<QueueTaskFuture> getQueueTaskFutures(int count) throws IOException {
        final LabelAtom label = new LabelAtom("momo");

        final List<QueueTaskFuture> rs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final FreeStyleProject project = j.createFreeStyleProject();
            project.setAssignedLabel(label);
            project.getBuildersList().add(Functions.isWindows() ? new BatchFile("Ping -n 30 127.0.0.1 > nul") : new Shell("sleep 30"));
            rs.add(project.scheduleBuild2(0));
        }
        return rs;
    }

    private static class SingleComputerConnector extends ComputerConnector implements Serializable {

        @Nonnull
        private transient final JenkinsRule j;

        private SingleComputerConnector(final JenkinsRule j) {
            this.j = j;
        }

        @Override
        public ComputerLauncher launch(@Nonnull String host, TaskListener listener) throws IOException {
            try {
                return j.createComputerLauncher(null);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
