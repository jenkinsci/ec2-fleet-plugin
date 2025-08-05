package com.amazon.jenkins.ec2fleet.aws;

import hudson.ProxyConfiguration;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import software.amazon.awssdk.http.apache.ApacheHttpClient;

import java.net.URI;

public class AWSUtilsIntegrationTest {

    private static final int PROXY_PORT = 8888;
    private static final String PROXY_HOST = "localhost";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void getHttpClient_when_no_proxy_returns_configuration_without_proxy() {
        j.jenkins.proxy = null;
        software.amazon.awssdk.http.apache.ProxyConfiguration.Builder builderSpy =
            Mockito.spy(software.amazon.awssdk.http.apache.ProxyConfiguration.builder());
        try (MockedStatic<AWSUtils> utilities = Mockito.mockStatic(AWSUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utilities.when(AWSUtils::createSdkProxyBuilder).thenReturn(builderSpy);
            ApacheHttpClient client = AWSUtils.getApacheHttpClient("somehost");
            Mockito.verify(builderSpy, Mockito.never()).endpoint(Mockito.any());
            Mockito.verify(builderSpy, Mockito.never()).username(Mockito.any());
            Mockito.verify(builderSpy, Mockito.never()).password(Mockito.any());
        }
    }

    @Test
    public void getHttpClient_when_proxy_returns_configuration_with_proxy() {
        j.jenkins.proxy = new ProxyConfiguration(PROXY_HOST, PROXY_PORT);
        URI expectedUri = URI.create("http://" + PROXY_HOST + ":" + PROXY_PORT);
        software.amazon.awssdk.http.apache.ProxyConfiguration.Builder builderSpy =
            Mockito.spy(software.amazon.awssdk.http.apache.ProxyConfiguration.builder());
        try (MockedStatic<AWSUtils> utilities = Mockito.mockStatic(AWSUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utilities.when(AWSUtils::createSdkProxyBuilder).thenReturn(builderSpy);
            ApacheHttpClient client = AWSUtils.getApacheHttpClient("somehost");
            Mockito.verify(builderSpy).endpoint(expectedUri);
            Mockito.verify(builderSpy, Mockito.never()).username(Mockito.any());
            Mockito.verify(builderSpy, Mockito.never()).password(Mockito.any());
        }
    }

    @Test
    public void getHttpClient_when_proxy_with_credentials_returns_configuration_with_proxy() {
        j.jenkins.proxy = new ProxyConfiguration(PROXY_HOST, PROXY_PORT, "a", "b");
        URI expectedUri = URI.create("http://" + PROXY_HOST + ":" + PROXY_PORT);
        software.amazon.awssdk.http.apache.ProxyConfiguration.Builder builderSpy =
            Mockito.spy(software.amazon.awssdk.http.apache.ProxyConfiguration.builder());
        try (MockedStatic<AWSUtils> utilities = Mockito.mockStatic(AWSUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utilities.when(AWSUtils::createSdkProxyBuilder).thenReturn(builderSpy);
            ApacheHttpClient client = AWSUtils.getApacheHttpClient("somehost");
            Mockito.verify(builderSpy).endpoint(expectedUri);
            Mockito.verify(builderSpy).username("a");
            Mockito.verify(builderSpy).password("b");
        }
    }

    @Test
    public void getHttpClient_when_endpoint_is_invalid_url_use_it_as_is() {
        j.jenkins.proxy = new ProxyConfiguration(PROXY_HOST, PROXY_PORT);
        URI expectedUri = URI.create("http://" + PROXY_HOST + ":" + PROXY_PORT);
        software.amazon.awssdk.http.apache.ProxyConfiguration.Builder builderSpy =
            Mockito.spy(software.amazon.awssdk.http.apache.ProxyConfiguration.builder());
        try (MockedStatic<AWSUtils> utilities = Mockito.mockStatic(AWSUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utilities.when(AWSUtils::createSdkProxyBuilder).thenReturn(builderSpy);
            ApacheHttpClient client = AWSUtils.getApacheHttpClient("rumba");
            Mockito.verify(builderSpy).endpoint(expectedUri);
            Mockito.verify(builderSpy, Mockito.never()).username(Mockito.any());
            Mockito.verify(builderSpy, Mockito.never()).password(Mockito.any());
        }
    }

    @Test
    public void getHttpClient_when_no_proxy_does_not_call_builder_methods() {
        j.jenkins.proxy = null;
        software.amazon.awssdk.http.apache.ProxyConfiguration.Builder builderSpy =
            Mockito.spy(software.amazon.awssdk.http.apache.ProxyConfiguration.builder());
        try (MockedStatic<AWSUtils> utilities = Mockito.mockStatic(AWSUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utilities.when(AWSUtils::createSdkProxyBuilder).thenReturn(builderSpy);
            AWSUtils.getApacheHttpClient("somehost");
            Mockito.verify(builderSpy, Mockito.never()).endpoint(Mockito.any());
            Mockito.verify(builderSpy, Mockito.never()).username(Mockito.any());
            Mockito.verify(builderSpy, Mockito.never()).password(Mockito.any());
        }
    }

    @Test
    public void getHttpClient_when_proxy_calls_builder_methods_without_credentials() {
        j.jenkins.proxy = new ProxyConfiguration(PROXY_HOST, PROXY_PORT);
        URI expectedUri = URI.create("http://" + PROXY_HOST + ":" + PROXY_PORT);
        software.amazon.awssdk.http.apache.ProxyConfiguration.Builder builderSpy =
            Mockito.spy(software.amazon.awssdk.http.apache.ProxyConfiguration.builder());
        try (MockedStatic<AWSUtils> utilities = Mockito.mockStatic(AWSUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utilities.when(AWSUtils::createSdkProxyBuilder).thenReturn(builderSpy);
            AWSUtils.getApacheHttpClient("somehost");
            Mockito.verify(builderSpy).endpoint(expectedUri);
            Mockito.verify(builderSpy, Mockito.never()).username(Mockito.any());
            Mockito.verify(builderSpy, Mockito.never()).password(Mockito.any());
        }
    }

    @Test
    public void getHttpClient_when_proxy_with_credentials_calls_builder_methods() {
        j.jenkins.proxy = new ProxyConfiguration(PROXY_HOST, PROXY_PORT, "a", "b");
        URI expectedUri = URI.create("http://" + PROXY_HOST + ":" + PROXY_PORT);
        software.amazon.awssdk.http.apache.ProxyConfiguration.Builder builderSpy =
            Mockito.spy(software.amazon.awssdk.http.apache.ProxyConfiguration.builder());
        try (MockedStatic<AWSUtils> utilities = Mockito.mockStatic(AWSUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utilities.when(AWSUtils::createSdkProxyBuilder).thenReturn(builderSpy);
            AWSUtils.getApacheHttpClient("somehost");
            Mockito.verify(builderSpy).endpoint(expectedUri);
            Mockito.verify(builderSpy).username("a");
            Mockito.verify(builderSpy).password("b");
        }
    }

    @Test
    public void getHttpClient_when_endpoint_is_invalid_url_calls_builder_methods() {
        j.jenkins.proxy = new ProxyConfiguration(PROXY_HOST, PROXY_PORT);
        URI expectedUri = URI.create("http://" + PROXY_HOST + ":" + PROXY_PORT);
        software.amazon.awssdk.http.apache.ProxyConfiguration.Builder builderSpy =
            Mockito.spy(software.amazon.awssdk.http.apache.ProxyConfiguration.builder());
        try (MockedStatic<AWSUtils> utilities = Mockito.mockStatic(AWSUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utilities.when(AWSUtils::createSdkProxyBuilder).thenReturn(builderSpy);
            AWSUtils.getApacheHttpClient("rumba");
            Mockito.verify(builderSpy).endpoint(expectedUri);
            Mockito.verify(builderSpy, Mockito.never()).username(Mockito.any());
            Mockito.verify(builderSpy, Mockito.never()).password(Mockito.any());
        }
    }

}
