package com.amazon.jenkins.ec2fleet.cloud;

import com.amazon.jenkins.ec2fleet.EC2FleetCloud;
import hudson.model.Slave;
import hudson.slaves.SlaveComputer;

public class FleetNodeComputer extends SlaveComputer {

    @SuppressWarnings("WeakerAccess")
    public FleetNodeComputer(final Slave slave) {
        super(slave);
    }

    @Override
    public FleetNode getNode() {
        return (FleetNode) super.getNode();
    }

    public EC2FleetCloud getCloud() {
        return getNode().getCloud();
    }
}
