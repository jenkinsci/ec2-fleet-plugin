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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @see EC2FleetCloud
 */
@Extension
@SuppressWarnings("unused")
public class CloudNanny extends PeriodicWork {

    private static final Logger LOGGER = Logger.getLogger(CloudNanny.class.getName());

    @Override
    public long getRecurrencePeriod() {
        return 10000L;
    }

    @Override
    protected void doRun() throws Exception {

        // Trigger reprovisioning as well
        Jenkins.getActiveInstance().unlabeledNodeProvisioner.suggestReviewNow();

        final List<FleetStateStats> stats = new ArrayList<FleetStateStats>();
        for (final Cloud cloud : Jenkins.getActiveInstance().clouds) {
            if (!(cloud instanceof EC2FleetCloud))
                continue;

            // Update the cluster states
            final EC2FleetCloud fleetCloud = (EC2FleetCloud) cloud;
            LOGGER.log(Level.FINE, "Checking cloud: " + fleetCloud.getLabelString());
            stats.add(Queue.withLock(new Callable<FleetStateStats>() {
                @Override
                public FleetStateStats call()
                        throws Exception {
                    return fleetCloud.updateStatus();
                }
            }));
        }

        for (final Widget w : Jenkins.getInstance().getWidgets()) {
            if (!(w instanceof FleetStatusWidget))
                continue;

            ((FleetStatusWidget) w).setStatusList(stats);
        }
    }
}
