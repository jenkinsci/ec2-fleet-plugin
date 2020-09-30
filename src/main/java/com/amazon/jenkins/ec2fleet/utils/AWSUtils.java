package com.amazon.jenkins.ec2fleet.utils;

import com.amazonaws.ClientConfiguration;
import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

public final class AWSUtils {

    private static final String USER_AGENT_PREFIX = "ec2-fleet-plugin";

    /**
     * Create {@link ClientConfiguration} for AWS-SDK with proper inited
     * {@link ClientConfiguration#getUserAgentPrefix()} and proxy if
     * Jenkins configured to use proxy
     *
     * @param endpoint real endpoint which need to be called,
     *                 required to find if proxy configured to bypass some of hosts
     *                 and real host in that whitelist
     * @return client configuration
     */
    public static ClientConfiguration getClientConfiguration(final String endpoint) {
        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setUserAgentPrefix(USER_AGENT_PREFIX);

        final ProxyConfiguration proxyConfig = Jenkins.getActiveInstance().proxy;
        if (proxyConfig != null) {
            Proxy proxy;
            try {
                proxy = proxyConfig.createProxy(new URL(endpoint).getHost());
            } catch (MalformedURLException e) {
                // no to fix it here, so just skip
                proxy = proxyConfig.createProxy(endpoint);
            }

            if (!proxy.equals(Proxy.NO_PROXY) && proxy.address() instanceof InetSocketAddress) {
                InetSocketAddress address = (InetSocketAddress) proxy.address();
                clientConfiguration.setProxyHost(address.getHostName());
                clientConfiguration.setProxyPort(address.getPort());
                if (null != proxyConfig.getUserName()) {
                    clientConfiguration.setProxyUsername(proxyConfig.getUserName());
                    clientConfiguration.setProxyPassword(proxyConfig.getPassword());
                }
            }
        }

        return clientConfiguration;
    }

    private AWSUtils() {
        throw new UnsupportedOperationException("util class");
    }

}
