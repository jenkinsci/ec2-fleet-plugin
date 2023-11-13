package com.amazon.jenkins.ec2fleet;

import hudson.Extension;
import hudson.model.PeriodicWork;
import hudson.slaves.Cloud;
import hudson.widgets.Widget;
import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.List;

/**
 * @see EC2FleetCloud
 * @see EC2FleetStatusWidget
 */
@Extension
@SuppressWarnings("unused")
public class EC2FleetStatusWidgetUpdater extends PeriodicWork {

    @Override
    public long getRecurrencePeriod() {
        return 10000L;
    }

    /**
     * <h2>Exceptions</h2>
     * This method will be executed by {@link PeriodicWork} inside {@link java.util.concurrent.ScheduledExecutorService}
     * by default it stops execution if task throws exception, however {@link PeriodicWork} fix that
     * by catch any exception and just log it, so we safe to throw exception here.
     */
    @Override
    protected void doRun() {
        final List<EC2FleetStatusInfo> info = new ArrayList<>();
        for (final Cloud cloud : getClouds()) {
            if (!(cloud instanceof EC2FleetCloud)) continue;
            final EC2FleetCloud fleetCloud = (EC2FleetCloud) cloud;
            final FleetStateStats stats = fleetCloud.getStats();
            // could be when plugin just started and not yet updated, ok to skip
            if (stats == null) continue;

            info.add(new EC2FleetStatusInfo(
                    fleetCloud.getFleet(), stats.getState().getDetailed(), fleetCloud.getLabelString(),
                    stats.getNumActive(), stats.getNumDesired()));
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
    private static List<Widget> getWidgets() {
        return Jenkins.get().getWidgets();
    }

    /**
     * We return {@link List} instead of original {@link Jenkins.CloudList}
     * to simplify testing as jenkins list requires actual {@link Jenkins} instance.
     *
     * @return basic java list
     */
    private static List<Cloud> getClouds() {
        return Jenkins.get().clouds;
    }

}
