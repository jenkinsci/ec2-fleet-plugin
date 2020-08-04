package com.amazon.jenkins.ec2fleet.fleet;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Arrays;
import java.util.List;

@ThreadSafe
public class EC2Fleets {

    private static final String EC2_SPOT_FLEET_PREFIX = "sfr-";
    private static final EC2SpotFleet EC2_SPOT_FLEET = new EC2SpotFleet();

    private static EC2Fleet GET = null;

    private EC2Fleets() {
        throw new UnsupportedOperationException("util class");
    }

    public static List<EC2Fleet> all() {
        return Arrays.asList(
                new EC2SpotFleet(),
                new AutoScalingGroupFleet()
        );
    }

    public static EC2Fleet get(final String id) {
        if (GET != null) return GET;

        if (isEC2Fleet(id)) {
            return EC2_SPOT_FLEET;
        } else {
            return new AutoScalingGroupFleet();
        }
    }

    public static boolean isEC2Fleet(final String fleet) {
        return StringUtils.startsWith(fleet, EC2_SPOT_FLEET_PREFIX);
    }

    @VisibleForTesting
    public static void setGet(EC2Fleet ec2Fleet) {
        GET = ec2Fleet;
    }

}
