package com.amazon.jenkins.ec2fleet;

import com.amazonaws.ClientConfiguration;
import hudson.ProxyConfiguration;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class AWSUtilsIntegrationTest {

    private static final int PROXY_PORT = 8888;
    private static final String PROXY_HOST = "localhost";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void getClientConfiguration_when_no_proxy_returns_configuration_without_proxy() {
        j.jenkins.proxy = null;
        ClientConfiguration clientConfiguration = AWSUtils.getClientConfiguration("somehost");
        Assert.assertNull(clientConfiguration.getProxyHost());
    }

    @Test
    public void getClientConfiguration_when_proxy_returns_configuration_with_proxy() {
        j.jenkins.proxy = new ProxyConfiguration(PROXY_HOST, PROXY_PORT);
        ClientConfiguration clientConfiguration = AWSUtils.getClientConfiguration("somehost");
        Assert.assertEquals(PROXY_HOST, clientConfiguration.getProxyHost());
        Assert.assertEquals(PROXY_PORT, clientConfiguration.getProxyPort());
        Assert.assertNull(clientConfiguration.getProxyUsername());
        Assert.assertNull(clientConfiguration.getProxyPassword());
    }

    @Test
    public void getClientConfiguration_when_proxy_with_credentials_returns_configuration_with_proxy() {
        j.jenkins.proxy = new ProxyConfiguration(PROXY_HOST, PROXY_PORT, "a", "b");
        ClientConfiguration clientConfiguration = AWSUtils.getClientConfiguration("somehost");
        Assert.assertEquals(PROXY_HOST, clientConfiguration.getProxyHost());
        Assert.assertEquals(PROXY_PORT, clientConfiguration.getProxyPort());
        Assert.assertEquals("a", clientConfiguration.getProxyUsername());
        Assert.assertEquals("b", clientConfiguration.getProxyPassword());
    }

    @Test
    public void getClientConfiguration_when_endpoint_is_invalid_url_use_it_as_is() {
        j.jenkins.proxy = new ProxyConfiguration(PROXY_HOST, PROXY_PORT);
        ClientConfiguration clientConfiguration = AWSUtils.getClientConfiguration("rumba");
        Assert.assertEquals(PROXY_HOST, clientConfiguration.getProxyHost());
        Assert.assertEquals(PROXY_PORT, clientConfiguration.getProxyPort());
    }

}
