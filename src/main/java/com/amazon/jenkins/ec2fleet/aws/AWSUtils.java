package com.amazon.jenkins.ec2fleet.aws;

import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.apache.ApacheHttpClient;

import java.net.*;

public final class AWSUtils {

    private static final String USER_AGENT_PREFIX = "ec2-fleet-plugin";
    private static final int MAX_ERROR_RETRY = 5;

    /**
     * Create {@link ClientOverrideConfiguration} for AWS-SDK with proper inited
     * {@link SdkAdvancedClientOption#USER_AGENT_PREFIX} and proxy if
     * Jenkins configured to use proxy
     *
     * @return client configuration
     */
    public static ClientOverrideConfiguration getClientConfiguration() {
        ClientOverrideConfiguration.Builder overrideConfig = ClientOverrideConfiguration.builder()
                .retryPolicy(RetryPolicy.forRetryMode(RetryMode.STANDARD).builder().numRetries(MAX_ERROR_RETRY).build())
                .putAdvancedOption(SdkAdvancedClientOption.USER_AGENT_PREFIX, USER_AGENT_PREFIX);
        return overrideConfig.build();
    }

    /**
     * For testability: create a ProxyConfiguration builder. Can be spied/mocked in tests.
     */
    static software.amazon.awssdk.http.apache.ProxyConfiguration.Builder createSdkProxyBuilder() {
        return software.amazon.awssdk.http.apache.ProxyConfiguration.builder();
    }

    /**
     * Creates an {@link ApacheHttpClient} with proxy configuration if Jenkins is configured to use a proxy.
     * If no proxy is configured, it returns a default ApacheHttpClient.
     * @param endpoint real endpoint which need to be called,
     *      *                 required to find if proxy configured to bypass some of hosts
     *      *                 and real host in that whitelist
     * @return http client
     */
    public static ApacheHttpClient getApacheHttpClient(final String endpoint) {
        final ProxyConfiguration proxyConfig = Jenkins.get().proxy;
        if (proxyConfig != null) {
            String host;
            try {
                host = new URL(endpoint).getHost();
            } catch (MalformedURLException e) {
                host = endpoint;
            }
            Proxy proxy = proxyConfig.createProxy(host);
            if (!proxy.equals(Proxy.NO_PROXY) && proxy.address() instanceof InetSocketAddress) {
                InetSocketAddress address = (InetSocketAddress) proxy.address();
                String proxyHost = address.getHostName();
                int proxyPort = address.getPort();
                String proxyScheme = "http"; // Jenkins ProxyConfiguration does not expose scheme, default to http
                URI proxyUri = URI.create(proxyScheme + "://" + proxyHost + ":" + proxyPort);
                software.amazon.awssdk.http.apache.ProxyConfiguration.Builder sdkProxyBuilder = createSdkProxyBuilder();
                sdkProxyBuilder.endpoint(proxyUri);
                if (proxyConfig.getUserName() != null) {
                    sdkProxyBuilder.username(proxyConfig.getUserName());
                    sdkProxyBuilder.password(proxyConfig.getSecretPassword().getPlainText());
                }
                return (ApacheHttpClient) ApacheHttpClient.builder().proxyConfiguration(sdkProxyBuilder.build()).build();
            }
        }
        return (ApacheHttpClient) ApacheHttpClient.builder().build();
    }

    /**
     * Converts Jenkins AmazonWebServicesCredentials to AWS SDK v2 AwsCredentialsProvider.
     */
    public static AwsCredentialsProvider toSdkV2CredentialsProvider(AmazonWebServicesCredentials credentials) {
        if (credentials == null) return null;
        String accessKey = credentials.resolveCredentials().accessKeyId();
        String secretKey = credentials.resolveCredentials().secretAccessKey();
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
        return StaticCredentialsProvider.create(awsCreds);
    }

    private AWSUtils() {
        throw new UnsupportedOperationException("util class");
    }

}
