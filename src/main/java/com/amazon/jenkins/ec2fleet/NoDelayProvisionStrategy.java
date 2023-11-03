package com.amazon.jenkins.ec2fleet;

import hudson.Extension;
import hudson.model.Label;
import hudson.model.LoadStatistics;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import jenkins.model.Jenkins;

import java.util.Collection;
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
        final int availableCapacity = snapshot.getAvailableExecutors() // available executors
                + strategyState.getPlannedCapacitySnapshot()     // capacity added by previous strategies from previous rounds
                + strategyState.getAdditionalPlannedCapacity();  // capacity added by previous strategies _this round_

        int qLen = snapshot.getQueueLength();
        int excessWorkload = qLen - availableCapacity;
        LOGGER.log(Level.FINE, "label [{0}]: queueLength {1} availableCapacity {2} (availableExecutors {3} plannedCapacitySnapshot {4} additionalPlannedCapacity {5})",
                new Object[]{label, qLen, availableCapacity, snapshot.getAvailableExecutors(),
                        strategyState.getPlannedCapacitySnapshot(), strategyState.getAdditionalPlannedCapacity()});

        if (excessWorkload <= 0) {
            LOGGER.log(Level.INFO, "label [{0}]: No excess workload, provisioning not needed.", label);
            return NodeProvisioner.StrategyDecision.PROVISIONING_COMPLETED;
        }

        for (final Cloud c : getClouds()) {
            if (excessWorkload < 1) {
                break;
            }

            if (!(c instanceof FleetCloud)) {
                LOGGER.log(Level.FINE, "label [{0}]: cloud {1} is not an FleetCloud, continuing...",
                        new Object[]{label, c.getDisplayName()});
                continue;
            }

            Cloud.CloudState cloudState = new Cloud.CloudState(label, strategyState.getAdditionalPlannedCapacity());
            if (!c.canProvision(cloudState)) {
                LOGGER.log(Level.INFO, "label [{0}]: cloud {1} can not provision for this label, continuing...",
                        new Object[]{label, c.getDisplayName()});
                continue;
            }

            if (!((FleetCloud) c).isNoDelayProvision()) {
                LOGGER.log(Level.FINE, "label [{0}]: cloud {1} does not use No Delay Provision Strategy, continuing...",
                        new Object[]{label, c.getDisplayName()});
                continue;
            }

            LOGGER.log(Level.FINE, "label [{0}]: cloud {1} can provision for this label",
                    new Object[]{label, c.getDisplayName()});
            final Collection<NodeProvisioner.PlannedNode> plannedNodes = c.provision(cloudState, excessWorkload);
            for (NodeProvisioner.PlannedNode pn : plannedNodes) {
                excessWorkload -= pn.numExecutors;
                LOGGER.log(Level.INFO, "Started provisioning {0} from {1} with {2,number,integer} "
                                + "executors. Remaining excess workload: {3,number,#.###}",
                        new Object[]{pn.displayName, c.name, pn.numExecutors, excessWorkload});
            }
            strategyState.recordPendingLaunches(plannedNodes);
        }

        if (excessWorkload > 0) {
            LOGGER.log(Level.FINE, "Provisioning not complete, consulting remaining strategies");
            return NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES;
        }

        LOGGER.log(Level.FINE, "Provisioning completed");
        return NodeProvisioner.StrategyDecision.PROVISIONING_COMPLETED;
    }

    // Visible for testing
    protected List<Cloud> getClouds() {
        return Jenkins.get().clouds;
    }

}