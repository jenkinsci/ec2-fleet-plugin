package com.amazon.jenkins.ec2fleet.aws;

import com.amazon.jenkins.ec2fleet.Registry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.util.ListBoxModel;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsResponse;
import software.amazon.awssdk.services.ec2.model.Region;

import java.util.TreeMap;
import java.util.stream.Collectors;

public class RegionHelper {

    /**
     * Fill Regions
     *
     * Get region codes (e.g. us-east-1) from EC2 API and AWS SDK.
     * DescribeRegions API does not have region descriptions (such as us-east-1 - US East (N. Virginia))
     * We fetch descriptions from our <code>RegionInfo</code> enum to avoid unnecessarily upgrading
     * AWS Java SDK for newer regions and fallback to AWS Java SDK enum.
     *
     * @param awsCredentialsId aws credentials id
     * @return <code>ListBoxModel</code> with label and values
     */
    @SuppressFBWarnings(
            value = {"DE_MIGHT_IGNORE", "WMI_WRONG_MAP_ITERATOR"},
            justification = "Ignore API exceptions and key iterator is really intended")
    public static ListBoxModel getRegionsListBoxModel(final String awsCredentialsId) {
        // to keep user consistent order tree map, default value to regionCode (eg. us-east-1)
        final TreeMap<String, String> regionDisplayNames = new TreeMap<>();
        try {
            final Ec2Client client = Registry.getEc2Api().connect(awsCredentialsId, null, null);
            final DescribeRegionsResponse regions = client.describeRegions();
            regionDisplayNames.putAll(regions.regions().stream()
                    .collect(Collectors.toMap(Region::regionName, Region::regionName)));
        } catch (final Exception ex) {
            // ignore exception it could be case that credentials are not belong to default region
            // which we are using to describe regions
        }
        // Add SDK regions as user can have latest SDK
        regionDisplayNames.putAll(software.amazon.awssdk.regions.Region.regions().stream()
                .collect(Collectors.toMap(software.amazon.awssdk.regions.Region::id, software.amazon.awssdk.regions.Region::id)));
        // Add regions from enum as user may have older SDK
        regionDisplayNames.putAll(RegionInfo.getRegionNames().stream()
                .collect(Collectors.toMap(r -> r, r -> r)));

        final ListBoxModel model = new ListBoxModel();
        for (final String regionName : regionDisplayNames.keySet()) {
            String regionDescription;
            try {
                final RegionInfo region = RegionInfo.fromName(regionName);
                if (region != null) {
                    regionDescription = region.getDescription();
                } else {
                    // Fallback to SDK when region description not found in RegionInfo
                    software.amazon.awssdk.regions.Region sdkRegion = software.amazon.awssdk.regions.Region.of(regionName);
                    if (sdkRegion != null && sdkRegion.metadata() != null && sdkRegion.metadata().description() != null) {
                        regionDescription = sdkRegion.metadata().description();
                    } else {
                        // If metadata or description is missing, use region code
                        regionDescription = null;
                    }
                }
                final String regionDisplayName = regionDescription != null ? String.format("%s %s", regionName, regionDescription) : regionName;

                // Update map only when description exists else leave default to region code eg. us-east-1
                regionDisplayNames.put(regionName, regionDisplayName);
            } catch (final IllegalArgumentException ex) {
                // Description missing in both enum and SDK, ignore and leave default
            }
            model.add(new ListBoxModel.Option(regionDisplayNames.get(regionName), regionName));
        }
        return model;
    }
}
