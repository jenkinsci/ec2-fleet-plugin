package com.amazon.jenkins.ec2fleet.fleet;

import org.apache.commons.lang.StringUtils;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Arrays;
import java.util.List;

@ThreadSafe
public class Fleets {

    private static final String EC2_SPOT_FLEET_PREFIX = "sfr-";
    private static final SpotFleet EC2_SPOT_FLEET = new SpotFleet();

    private static final String EC2_FLEET_PREFIX = "fleet-";
    private static final EC2Fleet EC2_FLEET = new EC2Fleet();

    private static Fleet GET = null;

    private Fleets() {
        throw new UnsupportedOperationException("util class");
    }

    public static List<Fleet> all() {
        return Arrays.asList(
                new SpotFleet(),
                new EC2Fleet(),
                new AutoScalingGroupFleet()
        );
    }

    public static Fleet get(final String id) {
        if (GET != null) return GET;

        if (isSpotFleet(id)) {
            return EC2_SPOT_FLEET;
        } else if(isEC2Fleet(id)) {
            return EC2_FLEET;
        } else {
            return new AutoScalingGroupFleet();
        }
    }

    public static boolean isSpotFleet(final String fleet) {
        return StringUtils.startsWith(fleet, EC2_SPOT_FLEET_PREFIX);
    }

    public static boolean isEC2Fleet(final String fleet) {
        return StringUtils.startsWith(fleet, EC2_FLEET_PREFIX);
    }

    // Visible for testing
    public static void setGet(Fleet fleet) {
        GET = fleet;
    }

}
