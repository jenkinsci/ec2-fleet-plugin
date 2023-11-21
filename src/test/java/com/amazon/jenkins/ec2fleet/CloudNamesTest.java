package com.amazon.jenkins.ec2fleet;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class CloudNamesTest {
  @Rule
  public JenkinsRule j = new JenkinsRule();

  private final EC2FleetCloud.ExecutorScaler noScaling = new EC2FleetCloud.NoScaler();

  @Test
  public void isUnique_true() {
    j.jenkins.clouds.add(new EC2FleetCloud("SomeDefaultName", null, null, null, null, null,
            "test-label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0,
            10, false, false, noScaling));

    Assert.assertTrue(CloudNames.isUnique("TestCloud"));
  }

  @Test
  public void isUnique_false() {
    j.jenkins.clouds.add(new EC2FleetCloud("SomeDefaultName", null, null, null, null, null,
            "test-label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0,
            10, false, false, noScaling));

    Assert.assertFalse(CloudNames.isUnique("SomeDefaultName"));
  }

  @Test
  public void isDuplicated_false() {
    j.jenkins.clouds.add(new EC2FleetCloud("TestCloud", null, null, null, null, null,
        "test-label", null, null, false, false,
        0, 0, 0, 0, 0, true, false,
        "-1", false, 0, 0,
        10, false, false, noScaling));

    j.jenkins.clouds.add(new EC2FleetCloud("TestCloud2", null, null, null, null, null,
        "test-label", null, null, false, false,
        0, 0, 0, 0, 0, true, false,
        "-1", false, 0, 0,
        10, false, false, noScaling));

    Assert.assertFalse(CloudNames.isDuplicated("TestCloud"));
  }

  @Test
  public void isDuplicated_true() {
    j.jenkins.clouds.add(new EC2FleetCloud("TestCloud", null, null, null, null, null,
        "test-label", null, null, false, false,
        0, 0, 0, 0, 0, true, false,
        "-1", false, 0, 0,
        10, false, false, noScaling));

    j.jenkins.clouds.add(new EC2FleetCloud("TestCloud", null, null, null, null, null,
        "test-label", null, null, false, false,
        0, 0, 0, 0, 0, true, false,
        "-1", false, 0, 0,
        10, false, false, noScaling));

    Assert.assertTrue(CloudNames.isDuplicated("TestCloud"));
  }

  @Test
  public void generateUnique_noSuffix() {
    Assert.assertEquals("UniqueCloud", CloudNames.generateUnique("UniqueCloud"));
  }

  @Test
  public void generateUnique_addsSuffixOnlyWhenNeeded() {
    j.jenkins.clouds.add(new EC2FleetCloud("UniqueCloud-1", null, null, null, null, null,
            "test-label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0,
            10, false, false, noScaling));

    Assert.assertEquals("UniqueCloud", CloudNames.generateUnique("UniqueCloud"));
  }

  @Test
  public void generateUnique_addsSuffixCorrectly() {
    j.jenkins.clouds.add(new EC2FleetCloud("UniqueCloud", null, null, null, null, null,
            "test-label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0,
            10, false, false, noScaling));

    j.jenkins.clouds.add(new EC2FleetCloud("UniqueCloud-1", null, null, null, null, null,
            "test-label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0,
            10, false, false, noScaling));

    String actual = CloudNames.generateUnique("UniqueCloud");
    Assert.assertTrue(actual.length() == ("UniqueCloud".length() + CloudNames.SUFFIX_LENGTH + 1));
    Assert.assertTrue(actual.startsWith("UniqueCloud-"));
  }

  @Test
  public void generateUnique_emptyStringInConstructor() {
    EC2FleetCloud fleetCloud = new EC2FleetCloud("", null, null, null, null, null,
            "test-label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0,
            10, false, false, noScaling);

    EC2FleetLabelCloud fleetLabelCloud = new EC2FleetLabelCloud("", null, null,
            null, null, new LocalComputerConnector(j), false, false,
            0, 0, 0, 1, false,
            false, 0, 0,
            2, false, null);

    Assert.assertEquals(("FleetCloud".length() + CloudNames.SUFFIX_LENGTH + 1), fleetCloud.name.length());
    Assert.assertTrue(fleetCloud.name.startsWith(EC2FleetCloud.BASE_DEFAULT_FLEET_CLOUD_ID));
    Assert.assertEquals(("FleetLabelCloud".length() + CloudNames.SUFFIX_LENGTH + 1), fleetLabelCloud.name.length());
    Assert.assertTrue(fleetLabelCloud.name.startsWith(EC2FleetLabelCloud.BASE_DEFAULT_FLEET_CLOUD_ID));
  }

  @Test
  public void generateUnique_nonEmptyStringInConstructor() {
    EC2FleetCloud fleetCloud = new EC2FleetCloud("UniqueCloud", null, null, null, null, null,
            "test-label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0,
            10, false, false, noScaling);

    EC2FleetLabelCloud fleetLabelCloud = new EC2FleetLabelCloud("UniqueLabelCloud", null, null,
            null, null, new LocalComputerConnector(j), false, false,
            0, 0, 0, 1, false,
            false, 0, 0,
            2, false, null);
            
    Assert.assertEquals("UniqueCloud", fleetCloud.name);
    Assert.assertEquals("UniqueLabelCloud", fleetLabelCloud.name);
  }
}
