package com.amazon.jenkins.ec2fleet.aws;

import java.util.Locale;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Validates AWS region names used to derive service endpoints.
 */
public final class AwsRegionValidator {

    // AWS regions are lowercase labels separated by hyphens (e.g. us-east-1, us-gov-west-1, us-isob-east-1).
    private static final Pattern REGION_NAME_PATTERN = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{0,62}[a-z0-9])?$");

    private AwsRegionValidator() {}

    @Nullable
    public static String normalizeRegionName(@Nullable final String regionName) {
        if (regionName == null) {
            return null;
        }
        final String normalized = regionName.trim().toLowerCase(Locale.ENGLISH);
        return normalized.isEmpty() ? null : normalized;
    }

    public static boolean isValidRegionName(@Nullable final String regionName) {
        final String normalized = normalizeRegionName(regionName);
        return normalized != null
                && normalized.indexOf('.') < 0
                && REGION_NAME_PATTERN.matcher(normalized).matches();
    }
}
