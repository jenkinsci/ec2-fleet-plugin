package com.amazon.jenkins.ec2fleet;

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

    public static void reassign(final @Nonnull String oldName, @Nonnull final EC2FleetCloud cloud) {
        for (final Computer computer : Jenkins.getActiveInstance().getComputers()) {
            checkAndReassign(oldName, cloud, computer);
        }

        for (final Node node : Jenkins.getActiveInstance().getNodes()) {
            checkAndReassign(oldName, cloud, node);
        }

        LOGGER.info("Finish to reassign resources from cloud " + oldName + " to " + cloud.getDisplayName());
    }

    private static void checkAndReassign(final String oldName, final EC2FleetCloud cloud, final Object object) {
        if (object instanceof EC2FleetCloudAware) {
            final EC2FleetCloudAware cloudAware = (EC2FleetCloudAware) object;
            final EC2FleetCloud oldCloud = cloudAware.getCloud();
            if (oldCloud != null && oldName.equals(oldCloud.getDisplayName())) {
                ((EC2FleetCloudAware) object).setCloud(cloud);
                LOGGER.info("Reassign " + object + " from " + oldName + " to " + cloud.getDisplayName());
            }
        }
    }
}
