package com.amazon.jenkins.ec2fleet;

import com.amazonaws.services.ec2.model.InstanceStateName;
import hudson.model.FreeStyleBuild;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.ComputerConnector;
import hudson.slaves.NodeProvisioner;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * https://support.cloudbees.com/hc/en-us/articles/115000060512-New-Shared-Agents-Clouds-are-not-being-provisioned-for-my-jobs-in-the-queue-when-I-have-agents-that-are-suspended
 * <p>
 * Run example:
 * https://docs.google.com/spreadsheets/d/e/2PACX-1vSuPWeDJD8xAbvHpyJPigAIMYJyL0YvljjAatutNqaFqUQofTx2PxY-sfqZgfsWqRxMGl2elJErbH5n/pubchart?oid=983520837&format=interactive
 */
@Ignore
public class NoDelayProvisionStrategyPerformanceTest extends IntegrationTest {

    @BeforeClass
    public static void beforeClass() {
        turnOffJenkinsTestTimout();
        // set default MARGIN for Jenkins
        System.setProperty(NodeProvisioner.class.getName() + ".MARGIN", Integer.toString(10));
    }

    @Test
    public void noDelayProvisionStrategy() throws Exception {
        test(true);
    }

    @Test
    public void defaultProvisionStrategy() throws Exception {
        test(false);
    }

    private void test(final boolean noDelay) throws IOException, InterruptedException {
        final int maxWorkers = 100;
        final int scheduleInterval = 15;
        final int batchSize = 9;

        mockEc2FleetApiToEc2SpotFleetWithDelay(InstanceStateName.Running, 500);

        final ComputerConnector computerConnector = new LocalComputerConnector(j);
        final String label = "momo";
        final EC2FleetCloudWithHistory cloud = new EC2FleetCloudWithHistory(null, null, "credId", null, "region",
                null, "fId", label, null, computerConnector, false, false,
                1, 0, maxWorkers, 1, true, false,
                false, 0, 0, false,
                15, noDelay);
        j.jenkins.clouds.add(cloud);

        System.out.println("waiting cloud start");
        // updated plugin requires some init time to get first update
        // so wait this event to be really correct with perf comparison as old version is not require init time
        tryUntil(new Runnable() {
            @Override
            public void run() {
                Assert.assertNotNull(cloud.getStats());
            }
        });

        // warm up jenkins queue, as it takes some time when jenkins run first task and start scale in/out
        // so let's run one task and wait it finish
        System.out.println("waiting warm up task execution");
        final List<QueueTaskFuture> warmUpTasks = enqueTask(1);
        waitTasksFinish(warmUpTasks);

        final List<ImmutableTriple<Long, Integer, Integer>> metrics = new ArrayList<>();
        final Thread monitor = new Thread(new Runnable() {

            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    final int queueSize = j.jenkins.getQueue().countBuildableItems()  // tasks to build
                            + j.jenkins.getQueue().getPendingItems().size() // tasks to start
                            + j.jenkins.getLabelAtom(label).getBusyExecutors(); // tasks in progress
                    final int executors = j.jenkins.getLabelAtom(label).getTotalExecutors();
                    final ImmutableTriple<Long, Integer, Integer> data = new ImmutableTriple<>(
                            System.currentTimeMillis(), queueSize, executors);
                    metrics.add(data);
                    System.out.println(new Date(data.left) + " " + data.middle + " " + data.right);

                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(5));
                    } catch (InterruptedException e) {
                        throw new RuntimeException("stopped");
                    }
                }
            }
        });
        monitor.start();

        System.out.println("start test");
        int taskCount = 0;
        final List<QueueTaskFuture> tasks = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            tasks.addAll(enqueTask(batchSize));
            taskCount += batchSize;
            System.out.println("schedule " + taskCount + " tasks, waiting " + scheduleInterval + " sec");
            Thread.sleep(TimeUnit.SECONDS.toMillis(scheduleInterval));
        }

        waitTasksFinish(tasks);

        monitor.interrupt();
        monitor.join();

        for (ImmutableTriple<Long, Integer, Integer> data : metrics) {
            System.out.println(data.middle + "  " + data.right);
        }
    }

    private static void waitTasksFinish(List<QueueTaskFuture> tasks) {
        for (final QueueTaskFuture task : tasks) {
            try {
                task.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
