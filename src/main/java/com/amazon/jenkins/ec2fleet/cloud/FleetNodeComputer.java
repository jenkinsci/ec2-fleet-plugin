package com.amazon.jenkins.ec2fleet.cloud;

import com.amazon.jenkins.ec2fleet.EC2FleetCloud;
import hudson.model.Slave;
import hudson.slaves.SlaveComputer;

/**
 * User: cyberax
 * Date: 4/15/16
 * Time: 01:13
 */
public class FleetNodeComputer extends SlaveComputer
{
    public FleetNodeComputer(final Slave slave) {
        super(slave);
    }

    @Override public FleetNode getNode() {
        return (FleetNode) super.getNode();
    }

    @Override public String getDisplayName() { return getNode().getDisplayName(); }

    public EC2FleetCloud getCloud() {
        return getNode().getCloud();
    }
}
