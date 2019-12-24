package com.amazon.jenkins.ec2fleet;

import hudson.slaves.Cloud;

public abstract class AbstractEC2FleetCloud extends Cloud {

    protected AbstractEC2FleetCloud(String name) {
        super(name);
    }

    public abstract boolean isDisableTaskResubmit();

    public abstract int getIdleMinutes();

    public abstract boolean isAlwaysReconnect();

    public abstract boolean scheduleToTerminate(String instanceId);

    public abstract String getOldId();

}
