package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.aws.EC2Api;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleet;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleets;
import hudson.Functions;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.labels.LabelAtom;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.Reservation;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Covers the idle-agent window in jenkinsci/ec2-fleet-plugin#586: a condemned agent must not take new work,
 * and a build killed by node removal must still be resubmitted after the node is already gone.
 */
class Issue586IntegrationTest extends IntegrationTest {

    private EC2FleetCloud.ExecutorScaler noScaling;

    @BeforeEach
    void before() {
        final EC2Fleet ec2Fleet = mock(EC2Fleet.class);
        EC2Fleets.setGet(ec2Fleet);
        final EC2Api ec2Api = spy(EC2Api.class);
        Registry.setEc2Api(ec2Api);
        when(ec2Fleet.getState(anyString(), anyString(), nullable(String.class), anyString()))
                .thenReturn(new FleetStateStats(
                        "", 1, FleetStateStats.State.active(), Collections.singleton("i-1"), Collections.emptyMap()));
        final Ec2Client amazonEC2 = mock(Ec2Client.class);
        when(ec2Api.connect(anyString(), anyString(), Mockito.nullable(String.class))).thenReturn(amazonEC2);
        final Instance instance = Instance.builder()
                .state(InstanceState.builder().name(InstanceStateName.RUNNING).build())
                .publicIpAddress("public-ip")
                .instanceId("i-1")
                .build();
        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(DescribeInstancesResponse.builder()
                        .reservations(Reservation.builder().instances(instance).build())
                        .build());
        noScaling = new EC2FleetCloud.NoScaler();
    }

    @Test
    void condemned_idle_agent_does_not_accept_new_work() throws Exception {
        final EC2FleetCloud cloud = newCloud();
        cloud.update();
        j.jenkins.clouds.add(cloud);
        assertAtLeastOneNode();

        final Node node = j.jenkins.getNode("i-1");
        assertNotNull(node);
        final EC2FleetNodeComputer computer = (EC2FleetNodeComputer) node.toComputer();
        assertNotNull(computer);
        assertNodeIsOnline(node);
        assertFalse(computer.getExecutors().isEmpty());
        ageIdleTime(computer, TimeUnit.MINUTES.toMillis(2));

        ((EC2RetentionStrategy) computer.getRetentionStrategy()).check(computer);

        assertTrue(computer.isScheduledForTermination());
        assertTrue(computer.isOffline());
        assertFalse(computer.isAcceptingTasks());
        assertFalse(node.isAcceptingTasks());
        assertTrue(cloud.getInstanceIdsToTerminate().containsKey("i-1"));

        final FreeStyleProject project = j.createFreeStyleProject();
        project.setAssignedLabel(new LabelAtom("momo"));
        project.getBuildersList()
                .add(Functions.isWindows() ? new BatchFile("echo should-not-run") : new Shell("echo should-not-run"));
        project.scheduleBuild2(0);
        Queue.getInstance().scheduleMaintenance();
        Thread.sleep(8000);

        assertNull(project.getLastBuild(), "condemned agent accepted a build");
        assertFalse(Queue.getInstance().isEmpty());
    }

    @Test
    void removeNode_resubmits_running_build_after_cloud_reference_is_cleared() throws Exception {
        final EC2FleetCloud cloud = newCloud();
        j.jenkins.clouds.add(cloud);

        final FreeStyleProject project = j.createFreeStyleProject();
        project.setAssignedLabel(new LabelAtom("momo"));
        project.getBuildersList()
                .add(Functions.isWindows() ? new BatchFile("ping -n 20 127.0.0.1 > nul") : new Shell("sleep 20"));
        project.scheduleBuild2(0);
        triggerSuggestReviewNow();
        assertAtLeastOneNode();

        final Node node = j.jenkins.getNode("i-1");
        assertNotNull(node);
        assertQueueIsEmpty();

        j.jenkins.removeNode(node);
        // afterDisconnect runs after the node is unlinked; give it a moment, then let the fleet reattach the instance.
        Thread.sleep(2000);
        cloud.update();

        tryUntil(
                () -> {
                    assertEquals(2, project.getBuilds().size());
                    assertEquals(Result.SUCCESS, project.getLastBuild().getResult());
                },
                TimeUnit.SECONDS.toMillis(120));
    }

    private EC2FleetCloud newCloud() {
        return new EC2FleetCloud(
                "TestCloud",
                "credId",
                null,
                "region",
                null,
                "fId",
                "momo",
                null,
                new LocalComputerConnector(j),
                false,
                false,
                1,
                0,
                2,
                0,
                1,
                false,
                true,
                "-1",
                false,
                0,
                0,
                999,
                false,
                false,
                noScaling);
    }

    private static void ageIdleTime(final Computer computer, final long millisAgo) throws Exception {
        final long aged = System.currentTimeMillis() - millisAgo;
        setLongField(computer, Computer.class, "connectTime", aged);
        for (final Executor executor : computer.getAllExecutors()) {
            setLongField(executor, Executor.class, "creationTime", aged);
        }
        if (System.currentTimeMillis() - computer.getIdleStartMilliseconds() < millisAgo / 2) {
            throw new IllegalStateException("failed to age idle time, idle start is " + computer.getIdleStartMilliseconds());
        }
    }

    /**
     * {@link Executor#creationTime} is final. Java 12+ rejects {@link Field#setLong} on final fields even after
     * {@code setAccessible}, so fall back to {@code Unsafe} which the Jenkins test JVM opens.
     */
    private static void setLongField(final Object target, final Class<?> type, final String name, final long value)
            throws Exception {
        final Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        try {
            field.setLong(target, value);
            return;
        } catch (IllegalAccessException ignored) {
            // final field; use Unsafe below
        }
        final Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        final Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        final Object unsafe = theUnsafe.get(null);
        final long offset = (long) unsafeClass.getMethod("objectFieldOffset", Field.class).invoke(unsafe, field);
        unsafeClass.getMethod("putLong", Object.class, long.class, long.class).invoke(unsafe, target, offset, value);
    }
}
