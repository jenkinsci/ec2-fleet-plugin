package com.amazon.jenkins.ec2fleet;

import hudson.slaves.OfflineCause;

import javax.annotation.Nonnull;

/**
 * @see EC2FleetOnlineChecker
 */
public class EC2OnlineCheckScriptCause extends OfflineCause {

    @Nonnull
    private final String nodeName;

    @SuppressWarnings("WeakerAccess")
    public EC2OnlineCheckScriptCause(@Nonnull String nodeName) {
        this.nodeName = nodeName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EC2OnlineCheckScriptCause that = (EC2OnlineCheckScriptCause) o;
        return nodeName.equals(that.nodeName);
    }

    @Override
    public int hashCode() {
        return nodeName.hashCode();
    }

    @Override
    public String toString() {
        return "Connection script failed for " + nodeName;
    }

}
