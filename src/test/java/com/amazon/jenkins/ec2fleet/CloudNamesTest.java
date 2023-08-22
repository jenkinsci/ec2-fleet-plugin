package com.amazon.jenkins.ec2fleet;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class CloudNamesTest {
  @Rule
  public JenkinsRule j = new JenkinsRule();

  @Test
  public void isUnique_true() {
    j.jenkins.clouds.add(new EC2FleetCloud("SomeDefaultName", null, null, null, null, null,
            "test-label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0, false,
            10, false));

    Assert.assertTrue(CloudNames.isUnique("TestCloud"));
  }

  @Test
  public void isUnique_false() {
    j.jenkins.clouds.add(new EC2FleetCloud("SomeDefaultName", null, null, null, null, null,
            "test-label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0, false,
            10, false));

    Assert.assertFalse(CloudNames.isUnique("SomeDefaultName"));
  }

  @Test
  public void isDuplicated_false() {
    j.jenkins.clouds.add(new EC2FleetCloud("TestCloud", null, null, null, null, null,
        "test-label", null, null, false, false,
        0, 0, 0, 0, 0, true, false,
        "-1", false, 0, 0, false,
        10, false));

    j.jenkins.clouds.add(new EC2FleetCloud("TestCloud2", null, null, null, null, null,
        "test-label", null, null, false, false,
        0, 0, 0, 0, 0, true, false,
        "-1", false, 0, 0, false,
        10, false));

    Assert.assertFalse(CloudNames.isDuplicated("TestCloud"));
  }

  @Test
  public void isDuplicated_true() {
    j.jenkins.clouds.add(new EC2FleetCloud("TestCloud", null, null, null, null, null,
        "test-label", null, null, false, false,
        0, 0, 0, 0, 0, true, false,
        "-1", false, 0, 0, false,
        10, false));

    j.jenkins.clouds.add(new EC2FleetCloud("TestCloud", null, null, null, null, null,
        "test-label", null, null, false, false,
        0, 0, 0, 0, 0, true, false,
        "-1", false, 0, 0, false,
        10, false));

    Assert.assertTrue(CloudNames.isDuplicated("TestCloud"));
  }

  @Test
  public void generateUnique_noSuffix() {
    Assert.assertEquals("FleetCloud", CloudNames.generateUnique("FleetCloud"));
  }

  @Test
  public void generateUnique_addsSuffixOnlyWhenNeeded() {
    j.jenkins.clouds.add(new EC2FleetCloud("FleetCloud-1", null, null, null, null, null,
            "test-label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0, false,
            10, false));

    Assert.assertEquals("FleetCloud", CloudNames.generateUnique("FleetCloud"));
  }

  @Test
  public void generateUnique_addsSuffixCorrectly() {
    j.jenkins.clouds.add(new EC2FleetCloud("FleetCloud", null, null, null, null, null,
            "test-label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0, false,
            10, false));
    j.jenkins.clouds.add(new EC2FleetCloud("FleetCloud-1", null, null, null, null, null,
            "test-label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0, false,
            10, false));

    Assert.assertEquals("FleetCloud-2", CloudNames.generateUnique("FleetCloud"));
  }
}
