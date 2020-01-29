package com.amazon.jenkins.ec2fleet;

import com.google.common.annotations.VisibleForTesting;
import hudson.Extension;
import hudson.model.Label;
import hudson.model.LoadStatistics;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import jenkins.model.Jenkins;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of {@link NodeProvisioner.Strategy} which will provision a new node immediately as
 * a task enter the queue.
 * Now that EC2 is billed by the minute, we don't really need to wait before provisioning a new node.
 * <p>
 * As based we are used
 * <a href="https://github.com/jenkinsci/ec2-plugin/blob/master/src/main/java/hudson/plugins/ec2/NoDelayProvisionerStrategy.java">EC2 Jenkins Plugin</a>
 */
@Extension(ordinal = 100)
public class NoDelayProvisionStrategy extends NodeProvisioner.Strategy {

    private static final Logger LOGGER = Logger.getLogger(NoDelayProvisionStrategy.class.getName());

    @Override
    public NodeProvisioner.StrategyDecision apply(final NodeProvisioner.StrategyState strategyState) {
        final Label label = strategyState.getLabel();

        final LoadStatistics.LoadStatisticsSnapshot snapshot = strategyState.getSnapshot();
        final int availableCapacity =
                snapshot.getAvailableExecutors()   // live executors
                        + snapshot.getConnectingExecutors()  // executors present but not yet connected
                        + strategyState.getPlannedCapacitySnapshot()     // capacity added by previous strategies from previous rounds
                        + strategyState.getAdditionalPlannedCapacity();  // capacity added by previous strategies _this round_

        int currentDemand = snapshot.getQueueLength() - availableCapacity;
        LOGGER.log(Level.INFO,
                "currentDemand {0} availableCapacity {1} (availableExecutors {2} connectingExecutors {3} plannedCapacitySnapshot {4} additionalPlannedCapacity {5})",
                new Object[]{currentDemand, availableCapacity, snapshot.getAvailableExecutors(),
                        snapshot.getConnectingExecutors(), strategyState.getPlannedCapacitySnapshot(),
                        strategyState.getAdditionalPlannedCapacity()});

        for (final Cloud cloud : getClouds()) {
            if (currentDemand < 1) break;

            if (!(cloud instanceof EC2FleetCloud)) continue;
            if (!cloud.canProvision(label)) continue;

            final EC2FleetCloud ec2 = (EC2FleetCloud) cloud;
            if (!ec2.isNoDelayProvision()) continue;

            final Collection<NodeProvisioner.PlannedNode> plannedNodes = cloud.provision(label, currentDemand);
            for (NodeProvisioner.PlannedNode plannedNode : plannedNodes) {
                currentDemand -= plannedNode.numExecutors;
            }
            LOGGER.log(Level.FINE, "Planned {0} new nodes", plannedNodes.size());
            strategyState.recordPendingLaunches(plannedNodes);
            LOGGER.log(Level.FINE, "After provisioning currentDemand={0}", new Object[]{currentDemand});
        }

        if (currentDemand < 1) {
            LOGGER.log(Level.FINE, "Provisioning completed");
            return NodeProvisioner.StrategyDecision.PROVISIONING_COMPLETED;
        } else {
            LOGGER.log(Level.FINE, "Provisioning not complete, consulting remaining strategies");
            return NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES;
        }
    }

    @VisibleForTesting
    protected List<Cloud> getClouds() {
        return Jenkins.getInstance().clouds;
    }

}