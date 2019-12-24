package com.amazon.jenkins.ec2fleet;

import hudson.Extension;
import hudson.model.PeriodicWork;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

// todo make configurable
@Extension
@SuppressWarnings("unused")
public class EC2FleetLabelUpdater extends PeriodicWork {

    private static final Logger LOGGER = Logger.getLogger(EC2FleetLabelUpdater.class.getName());

    @Override
    public long getRecurrencePeriod() {
        return TimeUnit.SECONDS.toMillis(30);
    }

    @Override
    protected void doRun() {
        for (Cloud cloud : Jenkins.getActiveInstance().clouds) {
            if (!(cloud instanceof EC2FleetLabelCloud)) continue;
            final EC2FleetLabelCloud ec2FleetLabelCloud = (EC2FleetLabelCloud) cloud;
            try {
                ec2FleetLabelCloud.updateStacks();
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "Cloud stacks update error", t);
            }

            try {
                ec2FleetLabelCloud.update();
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "Cloud update error", t);
            }
        }
    }

}
