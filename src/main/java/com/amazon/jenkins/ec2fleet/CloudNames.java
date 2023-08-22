package com.amazon.jenkins.ec2fleet;

import hudson.slaves.Cloud;
import jenkins.model.Jenkins;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class CloudNames {

  public static Boolean isUnique(final String name) {
    return !Jenkins.get().clouds.stream().anyMatch(c -> c.name.equals(name));
  }

  public static Boolean isDuplicated(final String name) { return Jenkins.get().clouds.stream().filter(c -> c.name.equals(name)).count() > 1; }

  public static String generateUnique(final String defaultName) {
    final Set<String> usedNames = Jenkins.get().clouds != null
            ? Jenkins.get().clouds.stream().map(c -> c.name).collect(Collectors.toSet())
            : Collections.emptySet();
    String uniqName = defaultName;

    int suffix = 1;
    while (usedNames.contains(uniqName)) {
      uniqName = defaultName + "-" + suffix++;
    }

    return uniqName;
  }
}
