package com.amazon.jenkins.ec2fleet;

import hudson.Extension;
import hudson.widgets.Widget;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.List;

/**
 * This class should be thread safe, consumed by Jenkins and updated
 * by {@link EC2FleetStatusWidgetUpdater}
 */
@Extension
@ThreadSafe
public class EC2FleetStatusWidget extends Widget {

    private volatile List<EC2FleetStatusInfo> statusList = Collections.emptyList();

    public void setStatusList(final List<EC2FleetStatusInfo> statusList) {
        this.statusList = statusList;
    }

    @SuppressWarnings("unused")
    public List<EC2FleetStatusInfo> getStatusList() {
        return statusList;
    }
}
