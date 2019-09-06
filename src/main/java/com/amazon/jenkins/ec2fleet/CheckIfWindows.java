package com.amazon.jenkins.ec2fleet;

import hudson.remoting.Callable;
import org.jenkinsci.remoting.RoleChecker;

public class CheckIfWindows implements Callable<Boolean, RuntimeException> {

    private static final long serialVersionUID = 1;

    @Override
    public Boolean call() throws RuntimeException {
        return System.getenv("CMDCMDLINE") != null;
    }

    @Override
    public void checkRoles(final RoleChecker roleChecker) throws SecurityException {
    }

}
