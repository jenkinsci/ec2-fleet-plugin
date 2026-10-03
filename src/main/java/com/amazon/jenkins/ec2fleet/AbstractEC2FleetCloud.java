package com.amazon.jenkins.ec2fleet;

import hudson.slaves.Cloud;

public abstract class AbstractEC2FleetCloud extends Cloud {

    protected AbstractEC2FleetCloud(String name) {
        super(name);
    }

    public abstract boolean isDisableTaskResubmit();

    public abstract int getIdleMinutes();

    public abstract boolean isAlwaysReconnect();

    public abstract boolean hasExcessCapacity();

    public abstract boolean scheduleToTerminate(String instanceId, boolean ignoreMinConstraints, EC2AgentTerminationReason reason);

    /**
     * Whether {@code instanceId} is already recorded for termination on this cloud.
     * A replaced cloud object starts empty, so callers can reschedule a condemned agent onto the new object.
     */
    public abstract boolean isTerminationScheduled(String instanceId);
}
