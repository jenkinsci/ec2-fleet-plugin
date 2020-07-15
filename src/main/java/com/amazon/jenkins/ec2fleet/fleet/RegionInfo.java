package com.amazon.jenkins.ec2fleet.fleet;

import java.util.ArrayList;
import java.util.List;

/**
 *  Copied from SDK <code>com.amazonaws.regions.Regions</code> to avoid upgrading SDK for newer regions
 */
public enum RegionInfo {
    GovCloud("us-gov-west-1", "AWS GovCloud (US)"),
    US_GOV_EAST_1("us-gov-east-1", "AWS GovCloud (US-East)"),
    US_EAST_1("us-east-1", "US East (N. Virginia)"),
    US_EAST_2("us-east-2", "US East (Ohio)"),
    US_WEST_1("us-west-1", "US West (N. California)"),
    US_WEST_2("us-west-2", "US West (Oregon)"),
    EU_WEST_1("eu-west-1", "EU (Ireland)"),
    EU_WEST_2("eu-west-2", "EU (London)"),
    EU_WEST_3("eu-west-3", "EU (Paris)"),
    EU_CENTRAL_1("eu-central-1", "EU (Frankfurt)"),
    EU_NORTH_1("eu-north-1", "EU (Stockholm)"),
    AP_EAST_1("ap-east-1", "Asia Pacific (Hong Kong)"),
    AP_SOUTH_1("ap-south-1", "Asia Pacific (Mumbai)"),
    AP_SOUTHEAST_1("ap-southeast-1", "Asia Pacific (Singapore)"),
    AP_SOUTHEAST_2("ap-southeast-2", "Asia Pacific (Sydney)"),
    AP_NORTHEAST_1("ap-northeast-1", "Asia Pacific (Tokyo)"),
    AP_NORTHEAST_2("ap-northeast-2", "Asia Pacific (Seoul)"),
    SA_EAST_1("sa-east-1", "South America (Sao Paulo)"),
    CN_NORTH_1("cn-north-1", "China (Beijing)"),
    CN_NORTHWEST_1("cn-northwest-1", "China (Ningxia)"),
    CA_CENTRAL_1("ca-central-1", "Canada (Central)"),
    ME_SOUTH_1("me-south-1", "Middle East (Bahrain)"),
    AP_NORTHEAST_3("ap-northeast-3", "Asia Pacific (Osaka-Local)");

    private final String name;
    private final String description;

    private RegionInfo(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public static RegionInfo fromName(String regionName) {
        for (final RegionInfo region : values()) {
            if (region.getName().equalsIgnoreCase(regionName)) {
                return region;
            }
        }
        return null;
    }

    public static List<String> getRegionNames() {
        final List<String> regionNames = new ArrayList<>();
        for(final RegionInfo regionInfo : values()) {
            regionNames.add(regionInfo.getName());
        }
        return regionNames;
    }
}
