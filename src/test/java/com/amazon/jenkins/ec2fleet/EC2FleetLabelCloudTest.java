package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.aws.AwsPermissionChecker;
import hudson.model.Item;
import hudson.util.FormValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
                (awsPermissionChecker, context) -> when(awsPermissionChecker.getMissingPermissions(null)).thenReturn(new ArrayList<>()))) {
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
                (awsPermissionChecker, context) -> when(awsPermissionChecker.getMissingPermissions(null)).thenReturn(new ArrayList<>()))) {
            final FormValidation formValidation = new EC2FleetLabelCloud.DescriptorImpl()
                    .doTestConnection(item, "credentials", null, "https://ec2.cn-north-1.amazonaws.com.cn", null);

            assertEquals(FormValidation.Kind.OK, formValidation.kind);
            assertTrue(formValidation.getMessage().contains("Success"));
        }
    }

    @Test
    void descriptorImpl_doTestConnection_rejectsNonAwsEndpoint() {
        final Item item = mock(Item.class);

        final FormValidation formValidation = new EC2FleetLabelCloud.DescriptorImpl()
                .doTestConnection(item, "credentials", null, "https://example.com", null);

        assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
        assertTrue(formValidation.getMessage().contains("valid AWS endpoint URL"));
    }
}


