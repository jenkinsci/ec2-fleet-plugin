package com.amazon.jenkins.ec2fleet;

import jenkins.model.Jenkins;
import org.apache.commons.lang.RandomStringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class CloudNames {

  public static final int SUFFIX_LENGTH = 8;

  private static final Set<String> usedSuffixes = new HashSet<String>();

  public static Boolean isUnique(final String name) {
    return !Jenkins.get().clouds.stream().anyMatch(c -> c.name.equals(name));
  }

  public static Boolean isDuplicated(final String name) { return Jenkins.get().clouds.stream().filter(c -> c.name.equals(name)).count() > 1; }

  public static String generateUnique(final String proposedName) {
    final Set<String> usedNames = Jenkins.get().clouds != null
            ? Jenkins.get().clouds.stream().map(c -> c.name).collect(Collectors.toSet())
            : Collections.emptySet();

    if (proposedName.equals(FleetCloud.BASE_DEFAULT_FLEET_CLOUD_ID) || proposedName.equals(FleetLabelCloud.BASE_DEFAULT_FLEET_CLOUD_ID) || usedNames.contains(proposedName)) {
      return proposedName + "-" + generateSuffix();
    }

    return proposedName;
  }
  
  /**
   * We are using a randomly generated string as a suffix here because Jenkins creates its clouds as a batch.
   * This functionality means if a CasC user has two empty strings for the name field prompting two clouds with 
   * default names, they would theoretically have the same default name. By appending a random suffix we can be 
   * sure w.h.p that the suffixes created will be different.
   */
  private static String generateSuffix() {
    String suffix;

    do {
      suffix = RandomStringUtils.randomAlphanumeric(SUFFIX_LENGTH);
    } while (usedSuffixes.contains(suffix));

    usedSuffixes.add(suffix);
    return suffix;
  }
}
