package com.amazon.jenkins.ec2fleet;

import jenkins.model.CauseOfInterruption;

import javax.annotation.Nonnull;

public class EC2TerminationCause extends CauseOfInterruption {

    @Nonnull
    private final String nodeName;

    @SuppressWarnings("WeakerAccess")
    public EC2TerminationCause(@Nonnull String nodeName) {
        this.nodeName = nodeName;
    }

    @Override
    public String getShortDescription() {
        return "EC2 instance for node " + nodeName + " was terminated";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EC2TerminationCause that = (EC2TerminationCause) o;
        return nodeName.equals(that.nodeName);
    }

    @Override
    public int hashCode() {
        return nodeName.hashCode();
    }

    @Override
    public String toString() {
        return getShortDescription();
    }

}
