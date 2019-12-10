package com.amazon.jenkins.ec2fleet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.MapMaker;
import hudson.Extension;
import hudson.model.PeriodicWork;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @see EC2FleetCloud
 */
@Extension
@SuppressWarnings("unused")
public class CloudNanny extends PeriodicWork {

    private static final Logger LOGGER = Logger.getLogger(CloudNanny.class.getName());

    private final ConcurrentMap<EC2FleetCloud, AtomicInteger> recurrenceCounters = new MapMaker()
            .weakKeys() // the map should not hold onto fleet instances to allow deletion of fleets.
            .concurrencyLevel(1)
            .makeMap();

    @Override
    public long getRecurrencePeriod() {
        return 1000L;
    }

    /**
     * <h2>Exceptions</h2>
     * This method will be executed by {@link PeriodicWork} inside {@link java.util.concurrent.ScheduledExecutorService}
     * by default it stops execution if task throws exception, however {@link PeriodicWork} fix that
     * by catch any exception and just log it, so we safe to throw exception here.
     */
    @Override
    protected void doRun() {
        for (final Cloud cloud : getClouds()) {
            if (!(cloud instanceof EC2FleetCloud)) continue;
            final EC2FleetCloud fleetCloud = (EC2FleetCloud) cloud;

            final AtomicInteger recurrenceCounter = getRecurrenceCounter(fleetCloud);

            if (recurrenceCounter.decrementAndGet() > 0) {
                continue;
            }

            recurrenceCounter.set(fleetCloud.getCloudStatusIntervalSec());

            try {
                // Update the cluster states
                fleetCloud.update();
            } catch (Exception e) {
                // could bad configuration or real exception, we can't do too much here
                LOGGER.log(Level.INFO, String.format("Error during fleet %s stats update", fleetCloud.name), e);
            }
        }
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

    @VisibleForTesting
    private AtomicInteger getRecurrenceCounter(EC2FleetCloud fleetCloud) {
        AtomicInteger counter = new AtomicInteger(fleetCloud.getCloudStatusIntervalSec());
        // If a counter already exists, return the value, otherwise set the new counter value and return it.
        return Objects.firstNonNull(recurrenceCounters.putIfAbsent(fleetCloud, counter), counter);
    }
}
