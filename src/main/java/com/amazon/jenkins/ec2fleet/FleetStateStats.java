package com.amazon.jenkins.ec2fleet;

import com.amazonaws.services.ec2.model.BatchState;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @see EC2FleetCloud
 */
@SuppressWarnings({"unused"})
@ThreadSafe
public final class FleetStateStats {

    /**
     * Abstract state of different implementation of
     * {@link com.amazon.jenkins.ec2fleet.fleet.EC2Fleet}
     */
    public static class State {

        public static State active(final String detailed) {
            return new State(true, false, detailed);
        }

        public static State modifying(final String detailed) {
            return new State(true, true, detailed);
        }

        public static State active() {
            return active("active");
        }

        public static State notActive(final String detailed) {
            return new State(false, false, detailed);
        }

        private final String detailed;
        private final boolean active;
        private final boolean modifying;

        public State(final boolean active, final boolean modifying, final String detailed) {
            this.detailed = detailed;
            this.active = active;
            this.modifying = modifying;
        }

        /**
         * Is underline fleet is updating so we need to suppress update
         * until modification will be completed and fleet state will be stabilized.
         *
         * This is important only for {@link com.amazon.jenkins.ec2fleet.fleet.EC2SpotFleet}
         * as it has delay between update request and actual update of target capacity, while
         * {@link com.amazon.jenkins.ec2fleet.fleet.AutoScalingGroupFleet} does it in sync with
         * update call.
         *
         * Consumed by {@link EC2FleetCloud#update()}
         *
         * @return true or false
         */
        public boolean isModifying() {
            return modifying;
        }

        /**
         * Fleet is good to be used for plugin, it will be shown on UI as option to use
         * and plugin will use it for provision {@link EC2FleetCloud#provision(hudson.slaves.Cloud.CloudState, int)} ()} and de-provision
         * otherwise activity will be ignored until state will not be updated.
         *
         * @return true or false
         */
        public boolean isActive() {
            return active;
        }

        /**
         * Detailed information about EC2 Fleet for example
         * EC2 Spot Fleet states are {@link BatchState}
         *
         * @return string
         */
        public String getDetailed() {
            return detailed;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            State state = (State) o;
            return active == state.active &&
                    Objects.equals(detailed, state.detailed);
        }

        @Override
        public int hashCode() {
            return Objects.hash(detailed, active);
        }

    }

    @Nonnull
    private final String fleetId;
    @Nonnegative
    private int numActive;
    @Nonnegative
    private final int numDesired;
    @Nonnull
    private final State state;
    @Nonnull
    private final Set<String> instances;
    @Nonnull
    private final Map<String, Double> instanceTypeWeights;

    public FleetStateStats(final @Nonnull String fleetId,
                           final int numDesired, final @Nonnull State state,
                           final @Nonnull Set<String> instances,
                           final @Nonnull Map<String, Double> instanceTypeWeights) {
        this.fleetId = fleetId;
        this.numActive = instances.size();
        this.numDesired = numDesired;
        this.state = state;
        this.instances = instances;
        this.instanceTypeWeights = instanceTypeWeights;
    }

    public FleetStateStats(final @Nonnull FleetStateStats stats,
                           final int numDesired) {
        this.fleetId = stats.fleetId;
        this.numActive = stats.instances.size();
        this.numDesired = numDesired;
        this.state = stats.state;
        this.instances = stats.instances;
        this.instanceTypeWeights = stats.instanceTypeWeights;
    }

    @Nonnull
    public String getFleetId() {
        return fleetId;
    }

    public int getNumActive() {
        return numActive;
    }

    // Fleet does not immediately display the active instances and syncs up eventually
    public void setNumActive(final int activeCount) {
        numActive = activeCount;
    }

    public int getNumDesired() {
        return numDesired;
    }

    @Nonnull
    public State getState() {
        return state;
    }

    @Nonnull
    public Set<String> getInstances() {
        return instances;
    }

    @Nonnull
    public Map<String, Double> getInstanceTypeWeights() {
        return instanceTypeWeights;
    }

}
