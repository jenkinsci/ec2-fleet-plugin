package com.amazon.jenkins.ec2fleet;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.ec2.model.InstanceStateName;
import hudson.model.FreeStyleProject;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.QueueTaskFuture;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;

public class EC2FleetLabelCloudIntegrationTest extends IntegrationTest {

    @BeforeClass
    public static void beforeClass() {
        setJenkinsTestTimoutTo720();
    }

    @Test
    public void should_create_stack_and_provision_node_for_task_execution() throws Exception {
        mockEc2FleetApiToEc2SpotFleet(InstanceStateName.Running);
        mockCloudFormationApi();

        EC2FleetLabelCloud cloud = new EC2FleetLabelCloud("FleetLabel", "credId", "region",
                null, null, new LocalComputerConnector(j), false, false,
                0, 0, 0, 1, false,
                false, 0, 0,
                2, false, "test1");
        j.jenkins.clouds.add(cloud);

        // set max size to > 0 otherwise nothing to provision
        final String labelString = "FleetLabel_maxSize=1";
        final List<QueueTaskFuture> rs = enqueTask(1, labelString, JOB_SLEEP_TIME);

        Assert.assertEquals(0, j.jenkins.getNodes().size());

        tryUntil(new Runnable() {
            @Override
            public void run() {
                triggerSuggestReviewNow(labelString);
                assertTasksDone(rs);
            }
        }, TimeUnit.MINUTES.toMillis(4));

        cancelTasks(rs);
    }

    @Test
    public void should_delete_resources_if_label_unused() throws Exception {
        mockEc2FleetApiToEc2SpotFleet(InstanceStateName.Running);
        final AmazonCloudFormation amazonCloudFormation = mockCloudFormationApi();

        EC2FleetLabelCloud cloud = new EC2FleetLabelCloud("FleetLabel", "credId", "region",
                null, null, new LocalComputerConnector(j), false, false,
                0, 0, 0, 1, false,
                false, 0, 0,
                2, false, "test1");
        j.jenkins.clouds.add(cloud);

        // set max size to > 0 otherwise nothing to provision
        final String labelString = "FleetLabel_maxSize=1";
        final List<QueueTaskFuture> rs = enqueTask(1, labelString, JOB_SLEEP_TIME);

        // wait until tasks will be completed
        tryUntil(new Runnable() {
            @Override
            public void run() {
                triggerSuggestReviewNow(labelString);
                assertTasksDone(rs);
            }
        }, TimeUnit.MINUTES.toMillis(4));

        // remove label from task (unused)
        FreeStyleProject freeStyleProject = (FreeStyleProject) j.jenkins.getAllItems().get(0);
        freeStyleProject.setAssignedLabel(new LabelAtom("nothing"));

        // wait until stack will be deleted and nodes will be removed as well
        tryUntil(new Runnable() {
            @Override
            public void run() {
                Assert.assertEquals(Collections.emptyList(), j.jenkins.getNodes());
                Mockito.verify(amazonCloudFormation).deleteStack(any(DeleteStackRequest.class));
            }
        }, TimeUnit.MINUTES.toMillis(2));

        cancelTasks(rs);
    }

}
