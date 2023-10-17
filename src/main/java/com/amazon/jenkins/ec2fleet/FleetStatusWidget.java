package com.amazon.jenkins.ec2fleet;

import hudson.Extension;
import hudson.widgets.Widget;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.List;

/**
 * This class should be thread safe, consumed by Jenkins and updated
 * by {@link FleetStatusWidgetUpdater}
 */
@Extension
@ThreadSafe
public class FleetStatusWidget extends Widget {

    private volatile List<FleetStatusInfo> statusList = Collections.emptyList();

    public void setStatusList(final List<FleetStatusInfo> statusList) {
        this.statusList = statusList;
    }

    @SuppressWarnings("unused")
    public List<FleetStatusInfo> getStatusList() {
        return statusList;
    }
}
