package com.amazon.jenkins.ec2fleet;

import com.amazonaws.services.ec2.model.InstanceStateName;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.ComputerConnector;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Ignore
public class ProvisionPerformanceTest extends IntegrationTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("jenkins.test.timeout", "720");
    }

    @Test
    public void spikeLoadWorkers10Tasks30() throws Exception {
        test(10, 30);
    }

    @Test
    public void spikeLoadWorkers20Tasks60() throws Exception {
        test(20, 60);
    }

    private void test(int workers, int maxTasks) throws IOException, InterruptedException {
        mockEc2ApiToDescribeInstancesWhenModifiedWithDelay(InstanceStateName.Running, 500);

        final ComputerConnector computerConnector = new LocalComputerConnector(j);
        final EC2FleetCloudWithMeter cloud = new EC2FleetCloudWithMeter(null, null, "credId", null, "region",
                null, "fId", "momo", null, computerConnector, false, false,
                1, 0, workers, 1, true, false,
                false, 0, 0, false, 2);
        j.jenkins.clouds.add(cloud);

        // updated plugin requires some init time to get first update
        // so wait this event to be really correct with perf comparison as old version is not require init time
        tryUntil(new Runnable() {
            @Override
            public void run() {
                Assert.assertNotNull(cloud.getStats());
            }
        });

        System.out.println("start test");
        final long start = System.currentTimeMillis();

        final List<QueueTaskFuture<FreeStyleBuild>> tasks = new ArrayList<>();

        final int taskBatch = 5;

        while (tasks.size() < maxTasks) {
            tasks.addAll((List) getQueueTaskFutures(taskBatch));
            triggerSuggestReviewNow("momo");
            System.out.println(taskBatch + " added into queue, " + (maxTasks - tasks.size()) + " remain");
        }

        for (final QueueTaskFuture<FreeStyleBuild> task : tasks) {
            try {
                Assert.assertEquals(task.get().getResult(), Result.SUCCESS);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("downscale");
        final long finish = System.currentTimeMillis();

        // wait until downscale happens
        tryUntil(new Runnable() {
            @Override
            public void run() {
                // defect in termination logic, that why 1
                Assert.assertThat(j.jenkins.getLabel("momo").getNodes().size(), Matchers.lessThanOrEqualTo(1));
            }
        }, TimeUnit.MINUTES.toMillis(3));

        final long upTime = TimeUnit.MILLISECONDS.toSeconds(finish - start);
        final long downTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - finish);
        final long totalTime = upTime + downTime;
        final long ideaUpTime = (maxTasks / workers) * JOB_SLEEP_TIME;
        final int idealDownTime = 60;
        final long ideaTime = ideaUpTime + idealDownTime;

        System.out.println(maxTasks + " up in " + upTime + " sec, ideal time is " + ideaUpTime + " sec, overhead is " + (upTime - ideaUpTime) + " sec");
        System.out.println(maxTasks + " down in " + downTime + " sec, ideal time is " + idealDownTime + " sec, overhead is " + (downTime - idealDownTime) + " sec");
        System.out.println(maxTasks + " completed in " + totalTime + " sec, ideal time is " + ideaTime + " sec, overhead is " + (totalTime - ideaTime) + " sec");
        System.out.println(cloud.provisionMeter);
        System.out.println(cloud.removeMeter);
        System.out.println(cloud.updateMeter);
    }

}
