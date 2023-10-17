package com.amazon.jenkins.ec2fleet;

import hudson.slaves.Cloud;

public abstract class AbstractFleetCloud extends Cloud {

    protected AbstractFleetCloud(String name) {
        super(name);
    }

    public abstract boolean isDisableTaskResubmit();

    public abstract int getIdleMinutes();

    public abstract boolean isAlwaysReconnect();

    public abstract boolean hasExcessCapacity();

    public abstract boolean scheduleToTerminate(String instanceId, boolean ignoreMinConstraints, EC2AgentTerminationReason reason);
}
