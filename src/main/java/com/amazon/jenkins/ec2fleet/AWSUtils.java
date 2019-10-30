package com.amazon.jenkins.ec2fleet;

import com.amazonaws.ClientConfiguration;

public final class AWSUtils {

    private static final String USER_AGENT_PREFIX = "ec2-fleet-plugin";

    public static ClientConfiguration getClientConfiguration() {
        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setUserAgentPrefix(USER_AGENT_PREFIX);
        return clientConfiguration;
    }

    private AWSUtils() {
        throw new UnsupportedOperationException("util class");
    }

}
