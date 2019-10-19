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
            return new State(true, detailed);
        }

        public static State active() {
            return active("active");
        }

        public static State notActive(final String detailed) {
            return new State(false, detailed);
        }

        private final String detailed;
        private final boolean active;

        public State(final boolean active, final String detailed) {
            this.detailed = detailed;
            this.active = active;
        }

        /**
         * Consumed by {@link EC2FleetCloud#update()} to check if fleet is ok to
         * continue provision otherwise provision will be ignored
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
    private final int numActive;
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

    @Nonnull
    public String getFleetId() {
        return fleetId;
    }

    public int getNumActive() {
        return numActive;
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
