package com.amazon.jenkins.ec2fleet.aws;

import com.amazon.jenkins.ec2fleet.aws.RegionInfo;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionInfoTest {

    @Test
    void verifyRegionInfoDescriptionIsSameAsSDK() {
        final Map<String, String> sdkRegionDescriptions = Region.regions().stream()
                .collect(Collectors.toMap(Region::id, r -> r.metadata().description()));

        int checked = 0;
        for (final RegionInfo regionInfo : RegionInfo.values()) {
            final String sdkDescription = sdkRegionDescriptions.get(regionInfo.getName());
            if (sdkDescription != null) {
                assertEquals(regionInfo.getDescription(), sdkDescription);
                checked++;
            }
        }
        assertTrue(checked > 0);
    }
}
