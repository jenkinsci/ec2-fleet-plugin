package com.amazon.jenkins.ec2fleet.monitor;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.AdministrativeMonitor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.verb.POST;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Extension
public class EC2FleetExecutionErrorMonitor extends AdministrativeMonitor {

    private final List<String> lastErrorMessage = Collections.synchronizedList(new ArrayList<>());

    public static EC2FleetExecutionErrorMonitor getInstance() {
        return ExtensionList.lookupSingleton(EC2FleetExecutionErrorMonitor.class);
    }

    @Override
    public boolean isActivated() {
        return !lastErrorMessage.isEmpty();
    }

    @Override
    public String getDisplayName() {
        return "EC2 Fleet Plugin";
    }

    public void reportError(String message) {
        this.lastErrorMessage.add(message);
    }

    public void clear() {
        this.lastErrorMessage.clear();
    }

    public List<String> getLastErrorMessage() {
        return lastErrorMessage;
    }

    @POST
    public HttpResponse doAct(StaplerRequest req, StaplerResponse rsp) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        clear();
        return HttpResponses.redirectToContextRoot();
    }

}
