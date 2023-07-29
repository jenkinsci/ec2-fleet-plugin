package com.amazon.jenkins.ec2fleet;

import java.util.stream.Collectors;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

public class EC2FleetCloudUtil {

  /**
   This method is used to validate a user specified cloud name. If no name is provided, then a default name is used. Afterwards, the name
   is validated against existing clouds to ensure there are no duplicates. If there are duplicates, then a suffix is appended to make the
   name unique. For this plugin, once cloud name is specified it's not modifiable, and it must be called as part of a constructor's super call.
   In order to use this method as part of a super call in all implementations of this class, we must make it static.
   */
  protected static String getValidName(final String defaultName, final String nameParam) {
    String name = StringUtils.isBlank(nameParam) ? defaultName : nameParam;
    final String nonSuffixedName = name;
    boolean duplicateName;
    int iteration = 0;
    do {
      name = iteration > 0 ? nonSuffixedName + "-" + iteration : name;
      iteration++;
      duplicateName = Jenkins.get().clouds.stream().map(c -> c.name).collect(Collectors.toSet()).contains(name);
    } while (duplicateName);

    return name;
  }
}
