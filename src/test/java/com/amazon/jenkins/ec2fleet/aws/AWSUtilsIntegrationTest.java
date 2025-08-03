package com.amazon.jenkins.ec2fleet.aws;

import hudson.ProxyConfiguration;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import software.amazon.awssdk.http.apache.ApacheHttpClient;

import java.lang.reflect.Field;

public class AWSUtilsIntegrationTest {

    private static final int PROXY_PORT = 8888;
    private static final String PROXY_HOST = "localhost";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void getHttpClient_when_no_proxy_returns_configuration_without_proxy() throws NoSuchFieldException, IllegalAccessException {
        j.jenkins.proxy = null;
        ApacheHttpClient client = AWSUtils.getApacheHttpClient("somehost");
        Field proxyConfigField = client.getClass().getDeclaredField("proxyConfiguration");
        proxyConfigField.setAccessible(true);
        ProxyConfiguration proxyConfig = (ProxyConfiguration) proxyConfigField.get(client);
        Assert.assertNull(proxyConfig);
    }

    @Test
    public void getHttpClient_when_proxy_returns_configuration_with_proxy() throws NoSuchFieldException, IllegalAccessException {
        j.jenkins.proxy = new ProxyConfiguration(PROXY_HOST, PROXY_PORT);
        ApacheHttpClient client = AWSUtils.getApacheHttpClient("somehost");
        Field proxyConfigField = client.getClass().getDeclaredField("proxyConfiguration");
        proxyConfigField.setAccessible(true);
        ProxyConfiguration proxyConfig = (ProxyConfiguration) proxyConfigField.get(client);
        Assert.assertEquals(PROXY_HOST, proxyConfig.getName());
        Assert.assertEquals(PROXY_PORT, proxyConfig.getPort());
        Assert.assertNull(proxyConfig.getUserName());
        Assert.assertNull(proxyConfig.getSecretPassword());
    }

    @Test
    public void getHttpClient_when_proxy_with_credentials_returns_configuration_with_proxy() throws NoSuchFieldException, IllegalAccessException {
        j.jenkins.proxy = new ProxyConfiguration(PROXY_HOST, PROXY_PORT, "a", "b");
        ApacheHttpClient client = AWSUtils.getApacheHttpClient("somehost");
        Field proxyConfigField = client.getClass().getDeclaredField("proxyConfiguration");
        proxyConfigField.setAccessible(true);
        ProxyConfiguration proxyConfig = (ProxyConfiguration) proxyConfigField.get(client);
        Assert.assertEquals(PROXY_HOST, proxyConfig.getName());
        Assert.assertEquals(PROXY_PORT, proxyConfig.getPort());
        Assert.assertEquals("a", proxyConfig.getUserName());
        Assert.assertEquals("b", proxyConfig.getPassword());
    }

    @Test
    public void getHttpClient_when_endpoint_is_invalid_url_use_it_as_is() throws NoSuchFieldException, IllegalAccessException {
        j.jenkins.proxy = new ProxyConfiguration(PROXY_HOST, PROXY_PORT);
        ApacheHttpClient client = AWSUtils.getApacheHttpClient("rumba");
        Field proxyConfigField = client.getClass().getDeclaredField("proxyConfiguration");
        proxyConfigField.setAccessible(true);
        ProxyConfiguration proxyConfig = (ProxyConfiguration) proxyConfigField.get(client);
        Assert.assertEquals(PROXY_HOST, proxyConfig.getName());
        Assert.assertEquals(PROXY_PORT, proxyConfig.getPort());
    }

}
