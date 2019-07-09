package com.amazon.jenkins.ec2fleet;

import hudson.widgets.Widget;

import java.util.Objects;

/**
 * This consumed by jelly file <code>EC2FleetStatusWidget/index.jelly</code>
 * to render fleet information about all fleets, don't forget to update it
 * if you change fields name
 *
 * @see EC2FleetStatusWidget
 * @see CloudNanny
 */
public class EC2FleetStatusInfo extends Widget {

    private final String id;
    private final String state;
    private final String label;
    private final int numActive;
    private final int numDesired;

    public EC2FleetStatusInfo(String id, String state, String label, int numActive, int numDesired) {
        this.id = id;
        this.state = state;
        this.label = label;
        this.numActive = numActive;
        this.numDesired = numDesired;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getState() {
        return state;
    }

    public int getNumActive() {
        return numActive;
    }

    public int getNumDesired() {
        return numDesired;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EC2FleetStatusInfo that = (EC2FleetStatusInfo) o;
        return numActive == that.numActive &&
                numDesired == that.numDesired &&
                Objects.equals(state, that.state) &&
                Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, label, numActive, numDesired);
    }

}
