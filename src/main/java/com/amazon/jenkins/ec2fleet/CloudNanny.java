package com.amazon.jenkins.ec2fleet;

import com.google.common.annotations.VisibleForTesting;
import hudson.Extension;
import hudson.model.PeriodicWork;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link CloudNanny} is responsible for periodically running update (i.e. sync-state-with-AWS) cycles for {@link EC2FleetCloud}s.
 */
@Extension
@SuppressWarnings("unused")
public class CloudNanny extends PeriodicWork {

    private static final Logger LOGGER = Logger.getLogger(CloudNanny.class.getName());

    // the map should not hold onto fleet instances to allow deletion of fleets.
    private final Map<EC2FleetCloud, AtomicInteger> recurrenceCounters = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public long getRecurrencePeriod() {
        return 1000L;
    }

    /**
     * <p><strong>Exceptions</strong>
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
                updateCloudWithScaler(getClouds(), fleetCloud);
                // Update the cluster states
                fleetCloud.update();
            } catch (Exception e) {
                // could be a bad configuration or a real exception, we can't do too much here
                LOGGER.log(Level.INFO, String.format("Error during fleet '%s' stats update", fleetCloud.name), e);
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
    static Jenkins.CloudList getClouds() {
        return Jenkins.get().clouds;
    }

    private void updateCloudWithScaler(Jenkins.CloudList clouds, EC2FleetCloud oldCloud) throws IOException {
        if(oldCloud.getExecutorScaler() != null) return;

        EC2FleetCloud.ExecutorScaler scaler = oldCloud.isScaleExecutorsByWeight() ? new EC2FleetCloud.WeightedScaler() :
                                                                                    new EC2FleetCloud.NoScaler();
        scaler.withNumExecutors(oldCloud.getNumExecutors());
        EC2FleetCloud fleetCloudWithScaler = createCloudWithScaler(oldCloud, scaler);
        clouds.replace(oldCloud, fleetCloudWithScaler);
        Jenkins.get().save();
    }

    private EC2FleetCloud createCloudWithScaler(EC2FleetCloud oldCloud, EC2FleetCloud.ExecutorScaler scaler) {
        return  new EC2FleetCloud(oldCloud.getDisplayName(), oldCloud.getAwsCredentialsId(),
                oldCloud.getAwsCredentialsId(), oldCloud.getRegion(), oldCloud.getEndpoint(), oldCloud.getFleet(),
                oldCloud.getLabelString(), oldCloud.getFsRoot(), oldCloud.getComputerConnector(),
                oldCloud.isPrivateIpUsed(), oldCloud.isAlwaysReconnect(), oldCloud.getIdleMinutes(),
                oldCloud.getMinSize(), oldCloud.getMaxSize(), oldCloud.getMinSpareSize(), oldCloud.getNumExecutors(),
                oldCloud.isAddNodeOnlyIfRunning(), oldCloud.isRestrictUsage(),
                String.valueOf(oldCloud.getMaxTotalUses()), oldCloud.isDisableTaskResubmit(),
                oldCloud.getInitOnlineTimeoutSec(), oldCloud.getInitOnlineCheckIntervalSec(),
                oldCloud.getCloudStatusIntervalSec(), oldCloud.isNoDelayProvision(),
                oldCloud.isScaleExecutorsByWeight(), scaler);
    }

    private AtomicInteger getRecurrenceCounter(EC2FleetCloud fleetCloud) {
        AtomicInteger counter = new AtomicInteger(fleetCloud.getCloudStatusIntervalSec());
        // If a counter already exists, return the value, otherwise set the new counter value and return it.
        AtomicInteger existing = recurrenceCounters.putIfAbsent(fleetCloud, counter);
        return existing != null ? existing : counter;
    }
}
