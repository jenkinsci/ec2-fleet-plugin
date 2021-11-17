package com.amazon.jenkins.ec2fleet;

import hudson.model.Executor;
import hudson.slaves.NodeProperty;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

import java.util.ArrayList;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class EC2MaxTotalUsesRetentionStrategyTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void shouldNotTerminateWhenUsageIsGreaterThanOne() throws Exception {
        final AbstractEC2FleetCloud cloud = Mockito.mock(AbstractEC2FleetCloud.class);

        EC2RetentionStrategy rs = new EC2RetentionStrategy();
        int usageCount = 5;
        // We test usageCount down to 0
        while (usageCount > -1) {

            r.jenkins.addNode(new EC2FleetNode("name", "id", "fs", 1, null,
                    "label", new ArrayList<NodeProperty<?>>(), cloud, null, usageCount));

            final EC2FleetNode node = (EC2FleetNode) r.jenkins.getNodes().get(0);
            final EC2FleetNodeComputer computer = new EC2FleetNodeComputer(node, "name", cloud);
            final Executor executor = new Executor(computer, 0);

            rs.taskAccepted(executor, null);

            if (!computer.isAcceptingTasks()) {
                rs.taskCompleted(executor, null, 0);
            }
            if (usageCount == 1) {
                verify(cloud, times(1)).scheduleToTerminate("name", true);
            } else if (usageCount == 0) {
                // We would have called terminate twice: 0 & 1
                verify(cloud, times(2)).scheduleToTerminate("name", true);
            } else {
                verify(cloud, times(0)).scheduleToTerminate("name", true);
            }
            usageCount--;
        }
    }

    @Test
    public void shouldNotTerminateWhenUsageIsSetUnlimited() throws Exception {
        final AbstractEC2FleetCloud cloud = Mockito.mock(AbstractEC2FleetCloud.class);

        EC2RetentionStrategy rs = new EC2RetentionStrategy();
        int usageCount = -1;
        r.jenkins.addNode(new EC2FleetNode("name", "id", "fs", 1, null,
                "label", new ArrayList<NodeProperty<?>>(), cloud, null, usageCount));

        final EC2FleetNode node = (EC2FleetNode) r.jenkins.getNodes().get(0);
        final EC2FleetNodeComputer computer = new EC2FleetNodeComputer(node, "name", cloud);
        final Executor executor = new Executor(computer, 0);

        rs.taskAccepted(executor, null);

        if (!computer.isAcceptingTasks()) {
            rs.taskCompleted(executor, null, 0);
        }
        verify(cloud, times(0)).scheduleToTerminate("name", true);
    }
}
