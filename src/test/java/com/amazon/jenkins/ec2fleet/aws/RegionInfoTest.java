package com.amazon.jenkins.ec2fleet.aws;

import com.amazon.jenkins.ec2fleet.aws.RegionInfo;

import org.junit.Test;
import software.amazon.awssdk.regions.Region;

import static org.junit.Assert.assertEquals;

public class RegionInfoTest {

    @Test
    public void verifyRegionInfoDescriptionIsSameAsSDK() {
        // Get regions from SDK
        final Region[] regions = Region.values();

        for(final Region region : regions) {
            assertEquals(RegionInfo.fromName(region.id()).getDescription(), region.getDescription());
        }
    }
}
