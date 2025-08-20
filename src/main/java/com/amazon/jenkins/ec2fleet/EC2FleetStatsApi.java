package com.amazon.jenkins.ec2fleet;

import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.Extension;
import hudson.model.RootAction;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Extension
public class EC2FleetStatsApi implements RootAction {

    @Override
    public String getIconFileName() {
        return null; // Hide from UI
    }

    @Override
    public String getDisplayName() {
        return null; // Hide from UI
    }

    @Override
    public String getUrlName() {
        return "api/ec2-fleets/json";
    }

    public void doIndex(HttpServletRequest req, HttpServletResponse rsp) throws IOException, ServletException {
        List<Map<String, Object>> statsList = new ArrayList<>();
        for (Cloud cloud : Jenkins.get().clouds) {
            if (cloud instanceof EC2FleetCloud) {
                EC2FleetCloud fleetCloud = (EC2FleetCloud) cloud;
                FleetStateStats stats = fleetCloud.getStats();
                if (stats != null) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("fleet", fleetCloud.getFleet());
                    data.put("state", stats.getState().getDetailed());
                    data.put("label", fleetCloud.getLabelString());
                    data.put("numActive", stats.getNumActive());
                    data.put("numDesired", stats.getNumDesired());
                    statsList.add(data);
                }
            }
        }
        rsp.setContentType("application/json");
        new ObjectMapper().writeValue(rsp.getWriter(), statsList);
    }
}
