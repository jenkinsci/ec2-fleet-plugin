package com.amazon.jenkins.ec2fleet;

import hudson.slaves.Cloud;
import java.util.ArrayList;
import java.util.HashSet;
import jenkins.model.Jenkins;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class CloudNames {

  public static Boolean isUnique(final String name) {
    return !Jenkins.get().clouds.stream().anyMatch(c -> c.name.equals(name));
  }

  public static Boolean isExistingUnique(final String name) {
    int found = 0;
    for (Cloud c : Jenkins.get().clouds) {
      if (c.name.equals(name)) {
        found++;
      }
    }

    return !(found > 1);
  }

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
