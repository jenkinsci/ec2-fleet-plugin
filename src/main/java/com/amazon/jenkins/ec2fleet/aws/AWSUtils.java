package com.amazon.jenkins.ec2fleet.aws;

import com.amazonaws.retry.PredefinedRetryPolicies;
import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

public final class AWSUtils {

    private static final String USER_AGENT_PREFIX = "ec2-fleet-plugin";
    private static final int MAX_ERROR_RETRY = 5;

    /**
     * Create {@link ClientOverrideConfiguration} for AWS-SDK with proper inited
     * {@link ClientOverrideConfiguration#getUserAgentPrefix()} and proxy if
     * Jenkins configured to use proxy
     *
     * @param endpoint real endpoint which need to be called,
     *                 required to find if proxy configured to bypass some of hosts
     *                 and real host in that whitelist
     * @return client configuration
     */
    public static ClientOverrideConfiguration getClientConfiguration(final String endpoint) {
        final ClientOverrideConfiguration clientConfiguration = ClientOverrideConfiguration.builder()
                .retryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(MAX_ERROR_RETRY))
                .build();
        clientConfiguration/*AWS SDK for Java v2 migration: userAgentPrefix override is a request-level config in v2. See https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/core/RequestOverrideConfiguration.Builder.html#addApiName(software.amazon.awssdk.core.ApiName).*/.setUserAgentPrefix(USER_AGENT_PREFIX);

        final ProxyConfiguration proxyConfig = Jenkins.get().proxy;
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
                    clientConfiguration.setProxyPassword(proxyConfig.getSecretPassword().getPlainText());
                }
            }
        }

        return clientConfiguration;
    }

    private AWSUtils() {
        throw new UnsupportedOperationException("util class");
    }

}
