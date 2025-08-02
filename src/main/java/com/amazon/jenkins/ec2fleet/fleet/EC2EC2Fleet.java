package com.amazon.jenkins.ec2fleet.fleet;

import com.amazon.jenkins.ec2fleet.FleetStateStats;
import com.amazon.jenkins.ec2fleet.Registry;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import hudson.util.ListBoxModel;
import org.springframework.util.ObjectUtils;

import java.util.*;

public class EC2EC2Fleet implements EC2Fleet {
    @Override
    public void describe(String awsCredentialsId, String regionName, String endpoint, ListBoxModel model, String selectedId, boolean showAll) {
        final Ec2Client client = Registry.getEc2Api().connect(awsCredentialsId, regionName, endpoint);
        for (DescribeFleetsResponse page : client.describeFleetsPaginator(DescribeFleetsRequest.builder().build())) {
            for (final FleetData fleetData : page.fleets()) {
                final String curFleetId = fleetData.fleetId();
                final boolean selected = ObjectUtils.nullSafeEquals(selectedId, curFleetId);
                if (selected || showAll || isActiveAndMaintain(fleetData)) {
                    final String displayStr = "EC2 Fleet - " + curFleetId +
                            " (" + fleetData.fleetState() + ")" +
                            " (" + fleetData.type() + ")";
                    model.add(new ListBoxModel.Option(displayStr, curFleetId, selected));
                }
            }
        }
    }

    private static boolean isActiveAndMaintain(final FleetData fleetData) {
        return FleetType.MAINTAIN.toString().equals(fleetData.type()) && isActive(fleetData);
    }

    private static boolean isActive(final FleetData fleetData) {
        return BatchState.ACTIVE.toString().equals(fleetData.fleetState())
                || BatchState.MODIFYING.toString().equals(fleetData.fleetState())
                || BatchState.SUBMITTED.toString().equals(fleetData.fleetState());
    }

    private static boolean isModifying(final FleetData fleetData) {
        return BatchState.SUBMITTED.toString().equals(fleetData.fleetState())
                || BatchState.MODIFYING.toString().equals(fleetData.fleetState());
    }

    @Override
    public void modify(String awsCredentialsId, String regionName, String endpoint, String id, int targetCapacity, int min, int max) {
        final ModifyFleetRequest request = ModifyFleetRequest.builder()
                .fleetId(id)
                .targetCapacitySpecification(TargetCapacitySpecificationRequest.builder()
                        .totalTargetCapacity(targetCapacity)
                        .build())
                .excessCapacityTerminationPolicy("no-termination")
                .build();

        final Ec2Client ec2 = Registry.getEc2Api().connect(awsCredentialsId, regionName, endpoint);
        ec2.modifyFleet(request);
    }

    @Override
    public FleetStateStats getState(String awsCredentialsId, String regionName, String endpoint, String id) {
        final Ec2Client ec2 = Registry.getEc2Api().connect(awsCredentialsId, regionName, endpoint);

        final DescribeFleetsRequest request = DescribeFleetsRequest.builder()
                .fleetIds(Collections.singleton(id))
                .build();
        final DescribeFleetsResponse result = ec2.describeFleets(request);
        if (result.fleets().isEmpty())
            throw new IllegalStateException("Fleet " + id + " doesn't exist");

        final FleetData fleetData = result.fleets().get(0);
        final List<FleetLaunchTemplateConfig> templateConfigs = fleetData.launchTemplateConfigs();

        // Index configured instance types by weight:
        final Map<String, Double> instanceTypeWeights = new HashMap<>();
        for (FleetLaunchTemplateConfig templateConfig : templateConfigs) {
            for (FleetLaunchTemplateOverrides launchOverrides : templateConfig.overrides()) {
                final String instanceType = String.valueOf(launchOverrides.instanceType());
                if (instanceType == null) continue;

                final Double instanceWeight = launchOverrides.weightedCapacity();
                final Double existingWeight = instanceTypeWeights.get(instanceType);
                if (instanceWeight == null || (existingWeight != null && existingWeight >= instanceWeight)) {
                    continue;
                }
                instanceTypeWeights.put(instanceType, instanceWeight);
            }
        }

        return new FleetStateStats(id,
                fleetData.targetCapacitySpecification().totalTargetCapacity(),
                new FleetStateStats.State(
                        isActive(fleetData),
                        isModifying(fleetData),
                        fleetData.fleetState().toString()),
                getActiveFleetInstances(ec2, id),
                instanceTypeWeights);
    }

    private Set<String> getActiveFleetInstances(Ec2Client ec2, String fleetId) {
        String token = null;
        final Set<String> instances = new HashSet<>();
        do {
            final DescribeFleetInstancesRequest request = DescribeFleetInstancesRequest.builder()
                    .fleetId(fleetId)
                    .nextToken(token)
                    .build();
            final DescribeFleetInstancesResponse result = ec2.describeFleetInstances(request);
            for (final ActiveInstance instance : result.activeInstances()) {
                instances.add(instance.instanceId());
            }

            token = result.nextToken();
        } while (token != null);
        return instances;
    }

    private static class State {
        String id;
        Set<String> instances;
        FleetData fleetData;
    }

    @Override
    public Map<String, FleetStateStats> getStateBatch(String awsCredentialsId, String regionName, String endpoint, Collection<String> ids) {
        final Ec2Client ec2 = Registry.getEc2Api().connect(awsCredentialsId, regionName, endpoint);

        List<State> states = new ArrayList<>();
        for (String id : ids) {
            final State s = new State();
            s.id = id;
            states.add(s);
        }

        for (State state : states) {
            state.instances = getActiveFleetInstances(ec2, state.id);
        }

        final DescribeFleetsRequest request = DescribeFleetsRequest.builder()
                .fleetIds(ids)
                .build();
        final DescribeFleetsResponse result = ec2.describeFleets(request);

        for (FleetData fleetData: result.fleets()) {
            for (State state : states) {
                if (state.id.equals(fleetData.fleetId())) state.fleetData = fleetData;
            }
        }

        Map<String, FleetStateStats> r = new HashMap<>();
        for (State state : states) {
            if(state.fleetData != null) {
                r.put(state.id, new FleetStateStats(state.id,
                        state.fleetData.targetCapacitySpecification().totalTargetCapacity(),
                        new FleetStateStats.State(
                                isActive(state.fleetData),
                                isModifying(state.fleetData),
                                state.fleetData.fleetState().toString()),
                        state.instances,
                        Collections.<String, Double>emptyMap()));
            }
        }
        return r;
    }

    @Override
    public Boolean isAutoScalingGroup() {
        return false;
    }
}
