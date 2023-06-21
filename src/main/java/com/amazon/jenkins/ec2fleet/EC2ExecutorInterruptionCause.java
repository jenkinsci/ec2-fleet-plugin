package com.amazon.jenkins.ec2fleet;

import jenkins.model.CauseOfInterruption;

import javax.annotation.Nonnull;

public class EC2ExecutorInterruptionCause extends CauseOfInterruption {

    @Nonnull
    private final String nodeName;

    @SuppressWarnings("WeakerAccess")
    public EC2ExecutorInterruptionCause(@Nonnull String nodeName) {
        this.nodeName = nodeName;
    }

    @Override
    public String getShortDescription() {
        return "EC2 instance for node " + nodeName + " was terminated";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EC2ExecutorInterruptionCause that = (EC2ExecutorInterruptionCause) o;
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
