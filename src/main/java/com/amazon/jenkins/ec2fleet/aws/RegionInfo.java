package com.amazon.jenkins.ec2fleet.aws;

import java.util.ArrayList;
import java.util.List;

/**
 *  Copied from SDK to avoid upgrading SDK for newer regions
 */
public enum RegionInfo {
    GovCloud("us-gov-west-1", "AWS GovCloud (US-West)"),
    US_GOV_EAST_1("us-gov-east-1", "AWS GovCloud (US-East)"),
    US_EAST_1("us-east-1", "US East (N. Virginia)"),
    US_EAST_2("us-east-2", "US East (Ohio)"),
    US_WEST_1("us-west-1", "US West (N. California)"),
    US_WEST_2("us-west-2", "US West (Oregon)"),
    EU_WEST_1("eu-west-1", "Europe (Ireland)"),
    EU_WEST_2("eu-west-2", "Europe (London)"),
    EU_WEST_3("eu-west-3", "Europe (Paris)"),
    EU_CENTRAL_1("eu-central-1", "Europe (Frankfurt)"),
    EU_CENTRAL_2("eu-central-2", "Europe (Zurich)"),
    EU_NORTH_1("eu-north-1", "Europe (Stockholm)"),
    EU_SOUTH_1("eu-south-1", "Europe (Milan)"),
    EU_SOUTH_2("eu-south-2", "Europe (Spain)"),
    AP_EAST_1("ap-east-1", "Asia Pacific (Hong Kong)"),
    AP_SOUTH_1("ap-south-1", "Asia Pacific (Mumbai)"),
    AP_SOUTH_2("ap-south-2", "Asia Pacific (Hyderabad)"),
    AP_SOUTHEAST_1("ap-southeast-1", "Asia Pacific (Singapore)"),
    AP_SOUTHEAST_2("ap-southeast-2", "Asia Pacific (Sydney)"),
    AP_SOUTHEAST_3("ap-southeast-3", "Asia Pacific (Jakarta)"),
    AP_SOUTHEAST_4("ap-southeast-4", "Asia Pacific (Melbourne)"),
    AP_NORTHEAST_1("ap-northeast-1", "Asia Pacific (Tokyo)"),
    AP_NORTHEAST_2("ap-northeast-2", "Asia Pacific (Seoul)"),
    AP_NORTHEAST_3("ap-northeast-3", "Asia Pacific (Osaka)"),

    SA_EAST_1("sa-east-1", "South America (Sao Paulo)"),
    CN_NORTH_1("cn-north-1", "China (Beijing)"),
    CN_NORTHWEST_1("cn-northwest-1", "China (Ningxia)"),
    CA_CENTRAL_1("ca-central-1", "Canada (Central)"),
    CA_WEST_1("ca-west-1", "Canada West (Calgary)"),
    ME_CENTRAL_1("me-central-1", "Middle East (UAE)"),
    ME_SOUTH_1("me-south-1", "Middle East (Bahrain)"),
    AF_SOUTH_1("af-south-1", "Africa (Cape Town)"),
    US_ISO_EAST_1("us-iso-east-1", "US ISO East"),
    US_ISOB_EAST_1("us-isob-east-1", "US ISOB East (Ohio)"),
    US_ISO_WEST_1("us-iso-west-1", "US ISO WEST"),
    IL_CENTRAL_1("il-central-1", "Israel (Tel Aviv)"),
    AWS_CN_GLOBAL("aws-cn-global", "aws-cn global region"),
    US_ISOF_SOUTH_1("us-isof-south-1", "US ISOF SOUTH"),
    AP_EAST_2("ap-east-2", "Asia Pacific (Taipei)"),
    AP_SOUTHEAST_5("ap-southeast-5", "Asia Pacific (Malaysia)"),
    AP_SOUTHEAST_7("ap-southeast-7", "Asia Pacific (Thailand)"),
    AWS_ISO_E_GLOBAL("aws-iso-e-global", "aws-iso-e global region"),
    MX_CENTRAL_1("mx-central-1", "Mexico (Central)"),
    EUSC_DE_EAST_1("eusc-de-east-1", "EU (Germany)"),
    EU_ISOE_WEST_1("eu-isoe-west-1", "EU ISOE West"),
    AWS_GLOBAL("aws-global", "aws global region"),
    AWS_ISO_GLOBAL("aws-iso-global", "aws-iso global region"),
    AWS_ISO_B_GLOBAL("aws-iso-b-global", "aws-iso-b global region"),
    AWS_ISO_F_GLOBAL("aws-iso-f-global", "aws-iso-f global region"),
    AWS_US_GOV_GLOBAL("aws-us-gov-global", "aws-us-gov global region"),
    US_ISOF_EAST_1("us-isof-east-1", "US ISOF EAST"),
    AP_SOUTHEAST_6("ap-southeast-6", "Asia Pacific (New Zealand)");

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
