package com.amazon.jenkins.ec2fleet;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class CloudEnvironmentVariable extends AbstractDescribableImpl<CloudEnvironmentVariable> {

    private final String name;
    private final String value;

    @DataBoundConstructor
    public CloudEnvironmentVariable(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<CloudEnvironmentVariable> {
        @Override
        public String getDisplayName() {
            return "Environment variable";
        }
    }
}
