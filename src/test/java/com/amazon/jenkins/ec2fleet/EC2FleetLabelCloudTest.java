package com.amazon.jenkins.ec2fleet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazon.jenkins.ec2fleet.aws.AwsPermissionChecker;
import com.amazon.jenkins.ec2fleet.aws.EC2Api;
import hudson.model.Item;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

@WithJenkins
class EC2FleetLabelCloudTest {

    private JenkinsRule j;

    @BeforeEach
    void before(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void descriptorImpl_doTestConnection_allowsAwsEndpoint() {
        final Item item = mock(Item.class);
        try (MockedConstruction<AwsPermissionChecker> mockedAwsPermissionChecker = Mockito.mockConstruction(
                AwsPermissionChecker.class,
                (awsPermissionChecker, context) ->
                        when(awsPermissionChecker.getMissingPermissions(null)).thenReturn(new ArrayList<>()))) {
            final FormValidation formValidation = new EC2FleetLabelCloud.DescriptorImpl()
                    .doTestConnection(item, "credentials", null, "https://ec2.us-east-1.amazonaws.com", null);

            assertEquals(FormValidation.Kind.OK, formValidation.kind);
            assertTrue(formValidation.getMessage().contains("Success"));
        }
    }

    @Test
    void descriptorImpl_doTestConnection_allowsChinaAwsEndpoint() {
        final Item item = mock(Item.class);
        try (MockedConstruction<AwsPermissionChecker> mockedAwsPermissionChecker = Mockito.mockConstruction(
                AwsPermissionChecker.class,
                (awsPermissionChecker, context) ->
                        when(awsPermissionChecker.getMissingPermissions(null)).thenReturn(new ArrayList<>()))) {
            final FormValidation formValidation = new EC2FleetLabelCloud.DescriptorImpl()
                    .doTestConnection(item, "credentials", null, "https://ec2.cn-north-1.amazonaws.com.cn", null);

            assertEquals(FormValidation.Kind.OK, formValidation.kind);
            assertTrue(formValidation.getMessage().contains("Success"));
        }
    }

    @Test
    void descriptorImpl_doTestConnection_allowsMixedCaseAwsEndpoint() {
        final Item item = mock(Item.class);
        try (MockedConstruction<AwsPermissionChecker> mockedAwsPermissionChecker = Mockito.mockConstruction(
                AwsPermissionChecker.class,
                (awsPermissionChecker, context) ->
                        when(awsPermissionChecker.getMissingPermissions(null)).thenReturn(new ArrayList<>()))) {
            final FormValidation formValidation = new EC2FleetLabelCloud.DescriptorImpl()
                    .doTestConnection(item, "credentials", null, " HTTPS://EC2.US-EAST-1.AMAZONAWS.COM ", null);

            assertEquals(FormValidation.Kind.OK, formValidation.kind);
            assertTrue(formValidation.getMessage().contains("Success"));
        }
    }

    @Test
    void descriptorImpl_doTestConnection_rejectsLookalikeEndpoint() {
        final Item item = mock(Item.class);
        try (MockedConstruction<AwsPermissionChecker> mockedAwsPermissionChecker =
                Mockito.mockConstruction(AwsPermissionChecker.class)) {
            final FormValidation formValidation = new EC2FleetLabelCloud.DescriptorImpl()
                    .doTestConnection(item, "credentials", null, "https://evilamazonaws.com", null);

            assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
            assertTrue(formValidation.getMessage().contains("valid AWS endpoint URL"));
            assertEquals(0, mockedAwsPermissionChecker.constructed().size());
        }
    }

    @Test
    void descriptorImpl_doFillEc2KeyPairNameItems_invalidEndpoint_doesNotConnect() {
        try (MockedConstruction<EC2Api> mockedEc2Api = Mockito.mockConstruction(EC2Api.class)) {
            final ListBoxModel model = new EC2FleetLabelCloud.DescriptorImpl()
                    .doFillEc2KeyPairNameItems("credentials", "us-east-1", "https://evilamazonaws.com");

            assertEquals(0, mockedEc2Api.constructed().size());
            assertEquals(0, model.size());
        }
    }
}
