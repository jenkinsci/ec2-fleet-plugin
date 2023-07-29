package com.amazon.jenkins.ec2fleet;

import jenkins.model.Jenkins;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
public class EC2FleetCloudUtilTest {

  @ClassRule
  public static BuildWatcher bw = new BuildWatcher();
  @Rule
  public JenkinsRule j = new JenkinsRule();

  @Test
  public void getValidName_validName() {
    Assert.assertEquals("test-cloud", EC2FleetCloudUtil.getValidName("FleetCloud", "test-cloud"));
  }
  @Test
  public void getValidName_blankName() {
    Assert.assertEquals("FleetCloud", EC2FleetCloudUtil.getValidName("FleetCloud", ""));
  }

  @Test
  public void getValidName_duplicateName() {
    Jenkins.get().clouds.add(new EC2FleetCloud("test-cloud", null, null, null, null, null,
        "test-label", null, null, false, false,
        0, 0, 0, 0, 0, true, false,
        "-1", false, 0, 0, false,
        10, false));
    Assert.assertEquals("test-cloud-1", EC2FleetCloudUtil.getValidName("FleetCloud", "test-cloud"));
  }

  @Test
  public void getValidName_blankName_duplicate() {
    Jenkins.get().clouds.add(new EC2FleetCloud("FleetCloud", null, null, null, null, null,
        "test-label", null, null, false, false,
        0, 0, 0, 0, 0, true, false,
        "-1", false, 0, 0, false,
        10, false));
    Assert.assertEquals("FleetCloud-1", EC2FleetCloudUtil.getValidName("FleetCloud", ""));
  }

  @Test
  public void getValidName_multipleDuplicateNames() {
    Jenkins.get().clouds.add(new EC2FleetCloud("test-cloud", null, null, null, null, null,
        "test-label", null, null, false, false,
        0, 0, 0, 0, 0, true, false,
        "-1", false, 0, 0, false,
        10, false));
    Jenkins.get().clouds.add(new EC2FleetCloud("test-cloud-1", null, null, null, null, null,
        "test-label", null, null, false, false,
        0, 0, 0, 0, 0, true, false,
        "-1", false, 0, 0, false,
        10, false));
    Assert.assertEquals("test-cloud-2", EC2FleetCloudUtil.getValidName("FleetCloud", "test-cloud"));
  }
}
