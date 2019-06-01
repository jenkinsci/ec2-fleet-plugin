package com.amazon.jenkins.ec2fleet;

import hudson.Extension;
import hudson.model.PeriodicWork;
import hudson.model.Queue;
import hudson.slaves.Cloud;
import hudson.widgets.Widget;
import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @see EC2FleetCloud
 */
@Extension
@SuppressWarnings("unused")
public class CloudNanny extends PeriodicWork {

    @Override
    public long getRecurrencePeriod() {
        return 10000L;
    }

    @Override
    protected void doRun() throws Exception {
        // Trigger reprovisioning as well
        Jenkins.getActiveInstance().unlabeledNodeProvisioner.suggestReviewNow();

        final List<FleetStateStats> stats = new ArrayList<>();
        for (final Cloud cloud : Jenkins.getActiveInstance().clouds) {
            if (!(cloud instanceof EC2FleetCloud))
                continue;

            // Update the cluster states
            final EC2FleetCloud fleetCloud = (EC2FleetCloud) cloud;
            stats.add(Queue.withLock(new Callable<FleetStateStats>() {
                @Override
                public FleetStateStats call() {
                    return fleetCloud.updateStatus();
                }
            }));
        }

        for (final Widget w : Jenkins.getInstance().getWidgets()) {
            if (w instanceof FleetStatusWidget) ((FleetStatusWidget) w).setStatusList(stats);
        }
    }
}
