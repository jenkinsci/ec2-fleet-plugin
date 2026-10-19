package com.amazon.jenkins.ec2fleet;

import hudson.slaves.OfflineCause;

import javax.annotation.Nonnull;

/**
 * Marks an agent offline once it has been scheduled for EC2 termination.
 * The channel stays up so a build that is already running can finish; the queue will not assign new work.
 */
public class ScheduledForTerminationOfflineCause extends OfflineCause {

    private final String reason;

    public ScheduledForTerminationOfflineCause(@Nonnull final String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return reason;
    }
}
