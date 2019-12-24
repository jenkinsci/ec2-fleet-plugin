package com.amazon.jenkins.ec2fleet;

import javax.annotation.Nonnull;

/**
 * Interface to mark object that it's require cloud change update. Jenkins always creates
 * new instance of {@link hudson.slaves.Cloud} each time when you save Jenkins configuration page
 * regardless of was it actually changed or not.
 * <p>
 * Jenkins never mutate existent {@link hudson.slaves.Cloud} instance
 * <p>
 * As result all objects which depends on info from cloud
 * should be start to consume new instance of object to be able get new configuration if any.
 * <p>
 * {@link EC2FleetCloud} is responsible to update all dependencies with new reference
 */
public interface EC2FleetCloudAware {

    AbstractEC2FleetCloud getCloud();

    void setCloud(@Nonnull AbstractEC2FleetCloud cloud);

}
