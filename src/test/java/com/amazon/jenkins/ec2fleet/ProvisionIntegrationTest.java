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
import com.amazonaws.services.ec2.model.ModifySpotFleetRequestRequest;
import com.amazonaws.services.ec2.model.ModifySpotFleetRequestResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfig;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfigData;
import com.google.common.collect.ImmutableSet;
import hudson.Functions;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.TaskListener;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.ComputerConnector;
import hudson.slaves.ComputerLauncher;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
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
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProvisionIntegrationTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @ClassRule
    public static BuildWatcher bw = new BuildWatcher();

    private static class MockAmazonEC2 extends EmptyAmazonEC2 {

        private static final Logger LOGGER = Logger.getLogger(MockAmazonEC2.class.getName());

        private volatile int targetCapacity = 0;

        @Override
        public DescribeInstancesResult describeInstances(DescribeInstancesRequest request) {
            Reservation reservation = new Reservation();
            if (request.getInstanceIds().size() > 0) {
                reservation.setInstances(Arrays.asList(new Instance()
                        .withInstanceId(request.getInstanceIds().get(0))
                        .withPublicIpAddress("public-ip")
                ));
            }
            List<Reservation> reservations = new ArrayList<>();
            reservations.add(reservation);
            return new DescribeInstancesResult().withReservations(reservations);
        }

        @Override
        public DescribeSpotFleetInstancesResult describeSpotFleetInstances(DescribeSpotFleetInstancesRequest request) {
            LOGGER.info("Describe spot fleet instances with target capacity " + targetCapacity);
            DescribeSpotFleetInstancesResult result = new DescribeSpotFleetInstancesResult();
            List<ActiveInstance> activeInstances = new ArrayList<>();
            for (int i = 0; i < targetCapacity; i++) {
                activeInstances.add(new ActiveInstance().withInstanceId("i-" + i));
            }
            result.setActiveInstances(activeInstances);
            return result;
        }

        @Override
        public DescribeSpotFleetRequestsResult describeSpotFleetRequests(DescribeSpotFleetRequestsRequest request) {
            LOGGER.info("Describe spot fleet requests");
            return new DescribeSpotFleetRequestsResult().withSpotFleetRequestConfigs(Arrays.asList(
                    new SpotFleetRequestConfig()
                            .withSpotFleetRequestState("active")
                            .withSpotFleetRequestConfig(
                                    new SpotFleetRequestConfigData().withTargetCapacity(0))));
        }

        @Override
        public ModifySpotFleetRequestResult modifySpotFleetRequest(ModifySpotFleetRequestRequest request) {
            LOGGER.info("Set target capacity to " + request.getTargetCapacity());
            targetCapacity = request.getTargetCapacity();
            return null;
        }

    }

    @After
    public void after() {
        // restore
        Registry.setEc2Api(new EC2Api());
    }

    @Test
    public void should_not_do_anything_if_fleet_is_empty_and_max_size_isreached() throws Exception {
        ComputerLauncher computerLauncher = mock(ComputerLauncher.class);
        ComputerConnector computerConnector = mock(ComputerConnector.class);
        when(computerConnector.launch(anyString(), any(TaskListener.class))).thenReturn(computerLauncher);

        EC2FleetCloud cloud = new EC2FleetCloud(null, "credId", null, "region", "fId",
                "momo", null, computerConnector, false, false,
                0, 0, 0, 1, false, false);
        j.jenkins.clouds.add(cloud);

        EC2Api ec2Api = mock(EC2Api.class);
        Registry.setEc2Api(ec2Api);

        AmazonEC2 amazonEC2 = mock(AmazonEC2.class);
        when(ec2Api.connect(anyString(), anyString())).thenReturn(amazonEC2);

        when(amazonEC2.describeSpotFleetInstances(any(DescribeSpotFleetInstancesRequest.class)))
                .thenReturn(new DescribeSpotFleetInstancesResult());

        DescribeSpotFleetRequestsResult describeSpotFleetRequestsResult = new DescribeSpotFleetRequestsResult();
        describeSpotFleetRequestsResult.setSpotFleetRequestConfigs(Arrays.asList(
                new SpotFleetRequestConfig()
                        .withSpotFleetRequestState("active")
                        .withSpotFleetRequestConfig(
                                new SpotFleetRequestConfigData().withTargetCapacity(0))));
        when(amazonEC2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenReturn(describeSpotFleetRequestsResult);

        List<QueueTaskFuture> rs = getQueueTaskFutures(5);

        Assert.assertEquals(0, j.jenkins.getNodes().size());

        Thread.sleep(TimeUnit.SECONDS.toMillis(30));

        Assert.assertEquals(0, j.jenkins.getNodes().size());

        cancelTasks(rs);
    }

    @Test
    public void should_add_planned_if_capacity_required_but_not_described_yet() throws Exception {
        ComputerLauncher computerLauncher = mock(ComputerLauncher.class);
        ComputerConnector computerConnector = mock(ComputerConnector.class);
        when(computerConnector.launch(anyString(), any(TaskListener.class))).thenReturn(computerLauncher);

        EC2FleetCloud cloud = new EC2FleetCloud(null, "credId", null, "region", "fId",
                "momo", null, computerConnector, false, false,
                0, 0, 10, 1, false, false);
        j.jenkins.clouds.add(cloud);

        EC2Api ec2Api = mock(EC2Api.class);
        Registry.setEc2Api(ec2Api);

        AmazonEC2 amazonEC2 = mock(AmazonEC2.class);
        when(ec2Api.connect(anyString(), anyString())).thenReturn(amazonEC2);

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(new DescribeInstancesResult());

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

        List<QueueTaskFuture> rs = getQueueTaskFutures(1);

        Assert.assertEquals(0, j.jenkins.getNodes().size());

        tryUntil(new Runnable() {
            @Override
            public void run() {
                Assert.assertEquals(0, j.jenkins.getNodes().size());
                Assert.assertEquals(2, j.jenkins.getLabels().size());
                Assert.assertEquals(1, j.jenkins.getLabelAtom("momo").nodeProvisioner.getPendingLaunches().size());
            }
        });

        cancelTasks(rs);
    }

    @Test
    public void should_convert_planed_to_node_if_describe_instance() throws Exception {
        ComputerLauncher computerLauncher = mock(ComputerLauncher.class);
        ComputerConnector computerConnector = mock(ComputerConnector.class);
        when(computerConnector.launch(anyString(), any(TaskListener.class))).thenReturn(computerLauncher);

        EC2FleetCloud cloud = new EC2FleetCloud(null, "credId", null, "region", "fId",
                "momo", null, computerConnector, false, false,
                0, 0, 10, 1, false, false);
        j.jenkins.clouds.add(cloud);

        EC2Api ec2Api = mock(EC2Api.class);
        Registry.setEc2Api(ec2Api);

        AmazonEC2 amazonEC2 = mock(AmazonEC2.class);
        when(ec2Api.connect(anyString(), anyString())).thenReturn(amazonEC2);

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .then(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) {
                        DescribeInstancesRequest request = invocationOnMock.getArgument(0);
                        return new DescribeInstancesResult().withReservations(
                                new Reservation().withInstances(
                                        new Instance()
                                                .withPublicIpAddress("public-io")
                                                .withInstanceId(request.getInstanceIds().get(0))
                                ));
                    }
                });

        final AtomicInteger targetCapacity = new AtomicInteger(0);

        when(amazonEC2.modifySpotFleetRequest(any(ModifySpotFleetRequestRequest.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                ModifySpotFleetRequestRequest request = invocationOnMock.getArgument(0);
                targetCapacity.set(request.getTargetCapacity());
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

        List<QueueTaskFuture> rs = getQueueTaskFutures(1);

        Assert.assertEquals(0, j.jenkins.getNodes().size());

        tryUntil(new Runnable() {
            @Override
            public void run() {
                // todo check is that correct that node added to label it's name?
                Assert.assertEquals(ImmutableSet.of("master", "momo", "i-0"), labelsToNames(j.jenkins.getLabels()));
                // it's mock we cannot really connect so jenkins will request +1 which is fine
                Assert.assertEquals(1, j.jenkins.getLabelAtom("momo").nodeProvisioner.getPendingLaunches().size());
                Assert.assertEquals(1, j.jenkins.getNodes().size());
            }
        });

        cancelTasks(rs);
    }

    @Test
    public void should_not_convert_planned_to_node_if_state_is_not_running_and_check_state_enabled() throws Exception {
        ComputerLauncher computerLauncher = mock(ComputerLauncher.class);
        ComputerConnector computerConnector = mock(ComputerConnector.class);
        when(computerConnector.launch(anyString(), any(TaskListener.class))).thenReturn(computerLauncher);

        EC2FleetCloud cloud = new EC2FleetCloud(null, "credId", null, "region", "fId",
                "momo", null, computerConnector, false, false,
                0, 0, 10, 1, true, false);
        j.jenkins.clouds.add(cloud);

        EC2Api ec2Api = mock(EC2Api.class);
        Registry.setEc2Api(ec2Api);

        AmazonEC2 amazonEC2 = mock(AmazonEC2.class);
        when(ec2Api.connect(anyString(), anyString())).thenReturn(amazonEC2);

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .then(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) {
                        DescribeInstancesRequest request = invocationOnMock.getArgument(0);
                        return new DescribeInstancesResult().withReservations(
                                new Reservation().withInstances(
                                        new Instance()
                                                .withPublicIpAddress("public-io")
                                                .withInstanceId(request.getInstanceIds().get(0))
                                ));
                    }
                });

        final AtomicInteger targetCapacity = new AtomicInteger(0);

        when(amazonEC2.modifySpotFleetRequest(any(ModifySpotFleetRequestRequest.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                ModifySpotFleetRequestRequest request = invocationOnMock.getArgument(0);
                targetCapacity.set(request.getTargetCapacity());
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

        List<QueueTaskFuture> rs = getQueueTaskFutures(1);

        Assert.assertEquals(0, j.jenkins.getNodes().size());

        tryUntil(new Runnable() {
            @Override
            public void run() {
                Assert.assertEquals(ImmutableSet.of("master", "momo"), labelsToNames(j.jenkins.getLabels()));
                Assert.assertEquals(1, j.jenkins.getLabelAtom("momo").nodeProvisioner.getPendingLaunches().size());
                Assert.assertEquals(0, j.jenkins.getNodes().size());
            }
        });

        cancelTasks(rs);
    }

    private static Set<String> labelsToNames(Set<Label> labels) {
        Set<String> r = new HashSet<>();
        for (Label l : labels) r.add(l.getName());
        return r;
    }

    private static void tryUntil(Runnable r) {
        long d = TimeUnit.SECONDS.toMillis(10);
        long time = TimeUnit.SECONDS.toMillis(110);
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
        final String command = "echo hello";

        final List<QueueTaskFuture> rs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final FreeStyleProject project = j.createFreeStyleProject();
            project.setAssignedLabel(label);
            project.getBuildersList().add(Functions.isWindows() ? new BatchFile(command) : new Shell(command));
            rs.add(project.scheduleBuild2(0));
        }
        return rs;
    }

}
