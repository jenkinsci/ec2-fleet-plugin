package com.amazon.jenkins.ec2fleet;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

final class AwsEndpointValidator {

    private AwsEndpointValidator() {}

    static boolean isValidAwsEndpoint(final String endpoint) {
        if (endpoint == null) {
            return false;
        }

        final String trimmed = endpoint.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        final String host = extractHost(trimmed);
        if (host == null || host.isEmpty()) {
            return false;
        }

        final String normalizedHost;
        try {
            normalizedHost = normalizeHost(host);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return isAwsHost(normalizedHost, "amazonaws.com") || isAwsHost(normalizedHost, "amazonaws.com.cn");
    }

    private static String extractHost(final String endpoint) {
        final URI endpointUri = toUri(endpoint);
        if (endpointUri == null) {
            return null;
        }

        return endpointUri.getHost();
    }

    private static URI toUri(final String endpoint) {
        final URI direct = parseUri(endpoint);
        if (direct != null && direct.getHost() != null) {
            return direct;
        }

        if (endpoint.contains("://")) {
            return direct;
        }

        return parseUri("https://" + endpoint);
    }

    private static URI parseUri(final String value) {
        try {
            return new URI(value);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static String normalizeHost(final String host) {
        String asciiHost = IDN.toASCII(host, IDN.ALLOW_UNASSIGNED).toLowerCase(Locale.ENGLISH);
        while (asciiHost.endsWith(".")) {
            asciiHost = asciiHost.substring(0, asciiHost.length() - 1);
        }
        return asciiHost;
    }

    private static boolean isAwsHost(final String host, final String awsDomain) {
        return host.equals(awsDomain) || host.endsWith('.' + awsDomain);
    }
}
