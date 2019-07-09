package com.amazon.jenkins.ec2fleet;

import com.google.common.annotations.VisibleForTesting;
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
 * @see EC2FleetStatusWidget
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
        final List<EC2FleetStatusInfo> info = new ArrayList<>();
        for (final Cloud cloud : getClouds()) {
            if (!(cloud instanceof EC2FleetCloud)) continue;
            final EC2FleetCloud fleetCloud = (EC2FleetCloud) cloud;

            try {
                // Update the cluster states
                info.add(Queue.withLock(new Callable<EC2FleetStatusInfo>() {
                    @Override
                    public EC2FleetStatusInfo call() {
                        final FleetStateStats stats = fleetCloud.updateStatus();
                        return new EC2FleetStatusInfo(
                                fleetCloud.getFleet(), stats.getState(), fleetCloud.getLabelString(),
                                stats.getNumActive(), stats.getNumDesired());
                    }
                }));
            } catch (Exception e) {
                // could bad configuration or real exception, we can't do too much here
                LOGGER.log(Level.INFO, String.format("Error during fleet %s stats update", fleetCloud.name), e);
            }
        }

        for (final Widget w : getWidgets()) {
            if (w instanceof EC2FleetStatusWidget) ((EC2FleetStatusWidget) w).setStatusList(info);
        }
    }

    /**
     * Will be mocked by tests to avoid deal with jenkins
     *
     * @return widgets
     */
    @VisibleForTesting
    private static List<Widget> getWidgets() {
        return Jenkins.getActiveInstance().getWidgets();
    }

    /**
     * We return {@link List} instead of original {@link jenkins.model.Jenkins.CloudList}
     * to simplify testing as jenkins list requires actual {@link Jenkins} instance.
     *
     * @return basic java list
     */
    @VisibleForTesting
    private static List<Cloud> getClouds() {
        return Jenkins.getActiveInstance().clouds;
    }
}
