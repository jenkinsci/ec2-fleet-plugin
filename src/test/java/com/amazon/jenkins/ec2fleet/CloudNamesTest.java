package com.amazon.jenkins.ec2fleet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.*;

@WithJenkins
class CloudNamesTest {

  private final EC2FleetCloud.ExecutorScaler noScaling = new EC2FleetCloud.NoScaler();

    private JenkinsRule j;

    @BeforeEach
    void before(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void isUnique_true() {
    j.jenkins.clouds.add(new EC2FleetCloud("SomeDefaultName", null, null, null, null, null,
            "test-label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0,
            10, false, false, noScaling));

    assertTrue(CloudNames.isUnique("TestCloud"));
  }

    @Test
    void isUnique_false() {
    j.jenkins.clouds.add(new EC2FleetCloud("SomeDefaultName", null, null, null, null, null,
            "test-label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0,
            10, false, false, noScaling));

    assertFalse(CloudNames.isUnique("SomeDefaultName"));
  }

    @Test
    void isDuplicated_false() {
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

    assertFalse(CloudNames.isDuplicated("TestCloud"));
  }

    @Test
    void isDuplicated_true() {
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

    assertTrue(CloudNames.isDuplicated("TestCloud"));
  }

    @Test
    void generateUnique_noSuffix() {
    assertEquals("UniqueCloud", CloudNames.generateUnique("UniqueCloud"));
  }

    @Test
    void generateUnique_addsSuffixOnlyWhenNeeded() {
    j.jenkins.clouds.add(new EC2FleetCloud("UniqueCloud-1", null, null, null, null, null,
            "test-label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0,
            10, false, false, noScaling));

    assertEquals("UniqueCloud", CloudNames.generateUnique("UniqueCloud"));
  }

    @Test
    void generateUnique_addsSuffixCorrectly() {
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
      assertEquals(actual.length(), ("UniqueCloud".length() + CloudNames.SUFFIX_LENGTH + 1));
    assertTrue(actual.startsWith("UniqueCloud-"));
  }

    @Test
    void generateUnique_emptyStringInConstructor() {
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

    assertEquals(("FleetCloud".length() + CloudNames.SUFFIX_LENGTH + 1), fleetCloud.name.length());
    assertTrue(fleetCloud.name.startsWith(EC2FleetCloud.BASE_DEFAULT_FLEET_CLOUD_ID));
    assertEquals(("FleetLabelCloud".length() + CloudNames.SUFFIX_LENGTH + 1), fleetLabelCloud.name.length());
    assertTrue(fleetLabelCloud.name.startsWith(EC2FleetLabelCloud.BASE_DEFAULT_FLEET_CLOUD_ID));
  }

    @Test
    void generateUnique_nonEmptyStringInConstructor() {
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
            
    assertEquals("UniqueCloud", fleetCloud.name);
    assertEquals("UniqueLabelCloud", fleetLabelCloud.name);
  }
}
