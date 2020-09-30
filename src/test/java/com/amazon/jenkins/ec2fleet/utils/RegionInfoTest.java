package com.amazon.jenkins.ec2fleet.utils;

import com.amazon.jenkins.ec2fleet.utils.RegionInfo;
import com.amazonaws.regions.Regions;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RegionInfoTest {

    @Test
    public void verifyRegionInfoDescriptionIsSameAsSDK() {
        // Get regions from SDK
        final Regions[] regions = Regions.values();

        for(final Regions region : regions) {
            assertEquals(RegionInfo.fromName(region.getName()).getDescription(), region.getDescription());
        }
    }
}
