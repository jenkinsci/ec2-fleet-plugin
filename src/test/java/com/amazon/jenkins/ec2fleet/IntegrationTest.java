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
import com.amazonaws.services.ec2.model.ModifySpotFleetRequestRequest;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfig;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfigData;
import hudson.Functions;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.QueueTaskFuture;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Base class for integration tests, which require mocked jenkins and utils
 * method to assert node states etc.
 */
@SuppressWarnings("WeakerAccess")
public abstract class IntegrationTest {

    @ClassRule
    public static BuildWatcher bw = new BuildWatcher();

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @After
    public void after() {
        // restore
        Registry.setEc2Api(new EC2Api());
    }

    protected static void assertQueueAndNodesIdle(final Node node) {
        tryUntil(new Runnable() {
            @Override
            public void run() {
                Assert.assertTrue(Queue.getInstance().isEmpty() && node.toComputer().isIdle());
            }
        });
    }

    protected static void assertQueueIsEmpty() {
        tryUntil(new Runnable() {
            @Override
            public void run() {
                System.out.println("check if queue is empty");
                Assert.assertTrue(Queue.getInstance().isEmpty());
            }
        });
    }

    protected void assertAtLeastOneNode() {
        tryUntil(new Runnable() {
            @Override
            public void run() {
                System.out.println("check if non zero nodes!");
                Assert.assertFalse(j.jenkins.getNodes().isEmpty());
            }
        });
    }

    protected void assertLastBuildResult(final Result... lastBuildResults) {
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

    protected void assertNodeIsOnline(final Node node) {
        tryUntil(new Runnable() {
            @Override
            public void run() {
                System.out.println("wait when back online");
                Assert.assertTrue(node.toComputer().isOnline());
            }
        });
    }

    protected static void tryUntil(Runnable r) {
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

    protected void cancelTasks(List<QueueTaskFuture> rs) {
        for (QueueTaskFuture r : rs) {
            r.cancel(true);
        }
    }

    protected List<QueueTaskFuture> getQueueTaskFutures(int count) throws IOException {
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

    protected static Set<String> labelsToNames(Set<Label> labels) {
        Set<String> r = new HashSet<>();
        for (Label l : labels) r.add(l.getName());
        return r;
    }

    protected void mockEc2ApiToDescribeInstancesWhenModified(final InstanceStateName instanceStateName) {
        EC2Api ec2Api = mock(EC2Api.class);
        Registry.setEc2Api(ec2Api);

        AmazonEC2 amazonEC2 = mock(AmazonEC2.class);
        when(ec2Api.connect(anyString(), anyString(), Mockito.nullable(String.class))).thenReturn(amazonEC2);

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .then(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) {
                        DescribeInstancesRequest request = invocationOnMock.getArgument(0);
                        return new DescribeInstancesResult().withReservations(
                                new Reservation().withInstances(
                                        new Instance()
                                                .withState(new InstanceState().withName(instanceStateName))
                                                .withPublicIpAddress("public-io")
                                                .withInstanceId(request.getInstanceIds().get(0))
                                ));
                    }
                });

        final AtomicInteger targetCapacity = new AtomicInteger(0);

        when(amazonEC2.modifySpotFleetRequest(any(ModifySpotFleetRequestRequest.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                ModifySpotFleetRequestRequest argument = invocationOnMock.getArgument(0);
                targetCapacity.set(argument.getTargetCapacity());
                return null;
            }
        });

        when(amazonEC2.describeSpotFleetInstances(any(DescribeSpotFleetInstancesRequest.class)))
                .then(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) {
                        final List<ActiveInstance> activeInstances = new ArrayList<>();
                        final int size = targetCapacity.get();
                        for (int i = 0; i < size; i++) {
                            activeInstances.add(new ActiveInstance().withInstanceId("i-" + i));
                        }
                        return new DescribeSpotFleetInstancesResult().withActiveInstances(activeInstances);
                    }
                });

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = new DescribeSpotFleetRequestsResult();
        describeSpotFleetRequestsResult.setSpotFleetRequestConfigs(Arrays.asList(
                new SpotFleetRequestConfig()
                        .withSpotFleetRequestState("active")
                        .withSpotFleetRequestConfig(
                                new SpotFleetRequestConfigData().withTargetCapacity(0))));
        when(amazonEC2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);
    }

    protected void mockEc2ApiToDescribeFleetNotInstanceWhenModified() {
        EC2Api ec2Api = mock(EC2Api.class);
        Registry.setEc2Api(ec2Api);

        AmazonEC2 amazonEC2 = mock(AmazonEC2.class);
        when(ec2Api.connect(anyString(), anyString(), Mockito.nullable(String.class))).thenReturn(amazonEC2);

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .then(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) {
                        return new DescribeInstancesResult().withReservations(new Reservation());
                    }
                });

        final AtomicInteger targetCapacity = new AtomicInteger(0);

        when(amazonEC2.modifySpotFleetRequest(any(ModifySpotFleetRequestRequest.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                ModifySpotFleetRequestRequest argument = invocationOnMock.getArgument(0);
                targetCapacity.set(argument.getTargetCapacity());
                return null;
            }
        });

        when(amazonEC2.describeSpotFleetInstances(any(DescribeSpotFleetInstancesRequest.class)))
                .then(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) {
                        final List<ActiveInstance> activeInstances = new ArrayList<>();
                        final int size = targetCapacity.get();
                        for (int i = 0; i < size; i++) {
                            activeInstances.add(new ActiveInstance().withInstanceId("i-" + i));
                        }
                        return new DescribeSpotFleetInstancesResult().withActiveInstances(activeInstances);
                    }
                });

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = new DescribeSpotFleetRequestsResult();
        describeSpotFleetRequestsResult.setSpotFleetRequestConfigs(Arrays.asList(
                new SpotFleetRequestConfig()
                        .withSpotFleetRequestState("active")
                        .withSpotFleetRequestConfig(
                                new SpotFleetRequestConfigData().withTargetCapacity(0))));
        when(amazonEC2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);
    }

}
