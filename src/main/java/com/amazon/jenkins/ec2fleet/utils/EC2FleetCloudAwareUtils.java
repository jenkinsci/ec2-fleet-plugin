package com.amazon.jenkins.ec2fleet.utils;

import com.amazon.jenkins.ec2fleet.AbstractEC2FleetCloud;
import com.amazon.jenkins.ec2fleet.EC2FleetCloudAware;
import hudson.model.Computer;
import hudson.model.Node;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * @see EC2FleetCloudAware
 */
@SuppressWarnings("WeakerAccess")
public class EC2FleetCloudAwareUtils {

    private static final Logger LOGGER = Logger.getLogger(EC2FleetCloudAwareUtils.class.getName());

    public static void reassign(final @Nonnull String id, @Nonnull final AbstractEC2FleetCloud cloud) {
        if (Jenkins.getActiveInstance() != null ) {
            if (Jenkins.getActiveInstance().getComputers() != null) {
                for (final Computer computer : Jenkins.getActiveInstance().getComputers()) {
                    LOGGER.info("Trying to reassign Jenkins computer:" + computer.getDisplayName());
                    checkAndReassign(id, cloud, computer);
                }
            }

            if (Jenkins.getActiveInstance().getNodes() != null) {
                for (final Node node : Jenkins.getActiveInstance().getNodes()) {
                    LOGGER.info("Trying to reassign Jenkins node:" + node.getDisplayName());
                    checkAndReassign(id, cloud, node);
                }
            }
        }
    }

    private static void checkAndReassign(final String id, final AbstractEC2FleetCloud cloud, final Object object) {
        if (object instanceof EC2FleetCloudAware) {
            final EC2FleetCloudAware cloudAware = (EC2FleetCloudAware) object;
            final AbstractEC2FleetCloud oldCloud = cloudAware.getCloud();
            // EC2FleetLabelCloud uses `oldId` and EC2FleetCloud uses `fleet` as id to map the cloud reference
            if (oldCloud != null && (StringUtils.equals(id, oldCloud.getOldId()) || StringUtils.equals(id, oldCloud.getFleet()))) {
                ((EC2FleetCloudAware) object).setCloud(cloud);
                LOGGER.info("Reassigned " + object + " from " + oldCloud.getDisplayName() + " to " + cloud.getDisplayName());
            }
        }
    }
}
