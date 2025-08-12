package com.amazon.jenkins.ec2fleet.aws;

import com.amazon.jenkins.ec2fleet.aws.RegionInfo;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegionInfoTest {

    @Test
    void verifyRegionInfoDescriptionIsSameAsSDK() {
        // Get regions from SDK
        final List<Region> regions = Region.regions();

        for(final Region region : regions) {
            assertEquals(RegionInfo.fromName(region.id()).getDescription(), region.metadata().description());
        }
    }
}
