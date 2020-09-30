package com.amazon.jenkins.ec2fleet.utils;

import com.amazon.jenkins.ec2fleet.AbstractEC2FleetCloud;
import com.amazon.jenkins.ec2fleet.EC2FleetCloudAware;
import hudson.model.Computer;
import hudson.model.Node;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * @see EC2FleetCloudAware
 */
@SuppressWarnings("WeakerAccess")
public class EC2FleetCloudAwareUtils {

    private static final Logger LOGGER = Logger.getLogger(EC2FleetCloudAwareUtils.class.getName());

    public static void reassign(final @Nonnull String oldId, @Nonnull final AbstractEC2FleetCloud cloud) {
        for (final Computer computer : Jenkins.getActiveInstance().getComputers()) {
            checkAndReassign(oldId, cloud, computer);
        }

        for (final Node node : Jenkins.getActiveInstance().getNodes()) {
            checkAndReassign(oldId, cloud, node);
        }

        LOGGER.info("Finish to reassign resources from old cloud with id " + oldId + " to " + cloud.getDisplayName());
    }

    private static void checkAndReassign(final String oldId, final AbstractEC2FleetCloud cloud, final Object object) {
        if (object instanceof EC2FleetCloudAware) {
            final EC2FleetCloudAware cloudAware = (EC2FleetCloudAware) object;
            final AbstractEC2FleetCloud oldCloud = cloudAware.getCloud();
            if (oldCloud != null && oldId.equals(oldCloud.getOldId())) {
                ((EC2FleetCloudAware) object).setCloud(cloud);
                LOGGER.info("Reassign " + object + " from " + oldCloud.getDisplayName() + " to " + cloud.getDisplayName());
            }
        }
    }
}
