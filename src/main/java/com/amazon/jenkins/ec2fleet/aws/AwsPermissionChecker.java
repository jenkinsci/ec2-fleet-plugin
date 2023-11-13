package com.amazon.jenkins.ec2fleet.aws;

import com.amazon.jenkins.ec2fleet.Registry;
import com.amazon.jenkins.ec2fleet.fleet.AutoScalingGroupFleet;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleets;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AmazonAutoScalingException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class AwsPermissionChecker {
    private static final int UNAUTHORIZED_STATUS_CODE = 403;

    private String awsCrendentialsId;
    private String regionName;
    private String endpoint;

    public AwsPermissionChecker(final String awsCrendentialsId, final String regionName, final String endpoint) {
        this.awsCrendentialsId = awsCrendentialsId;
        this.regionName = regionName;
        this.endpoint = endpoint;
    }

    public enum FleetAPI {
        DescribeInstances,
        DescribeSpotFleetInstances,
        CreateTags,
        ModifySpotFleetRequest,
        DescribeSpotFleetRequests,
        DescribeAutoScalingGroups,
        DescribeFleets,
        DescribeFleetInstances,
        ModifyFleet,
        DescribeInstanceTypes,
        TerminateInstances, // TODO: Dry-run throws invalid instanceID first then AuthZ error. We need to find a better way to test
        UpdateAutoScalingGroup; // TODO: There is no dry-run for AutoScalingClient
    };

    public List<String> getMissingPermissions(final String fleet) {
        final AmazonEC2 ec2Client = Registry.getEc2Api().connect(awsCrendentialsId, regionName, endpoint);
        final List<String> missingPermissions = new ArrayList<>(getMissingCommonPermissions(ec2Client));
        if(StringUtils.isBlank(fleet)) { // Since we don't know the fleet type, show all permissions
            missingPermissions.addAll(getMissingPermissionsForEC2SpotFleet(ec2Client, fleet));
            missingPermissions.addAll(getMissingPermissionsForEC2EC2Fleet(ec2Client, fleet));
            missingPermissions.addAll(getMissingPermissionsForASG());
        } else if(EC2Fleets.isEC2SpotFleet(fleet)) {
            missingPermissions.addAll(getMissingPermissionsForEC2SpotFleet(ec2Client, fleet));
        } else if(EC2Fleets.isEC2EC2Fleet(fleet)) {
            missingPermissions.addAll(getMissingPermissionsForEC2EC2Fleet(ec2Client, fleet));
        } else {
            missingPermissions.addAll(getMissingPermissionsForASG());
        }
        return missingPermissions;
    }

    private List<String> getMissingPermissionsForEC2SpotFleet(final AmazonEC2 ec2Client, final String fleet) {
        final List<String> missingEC2SpotFleetPermissions = new ArrayList<>();
        if(!hasDescribeSpotFleetRequestsPermission(ec2Client, fleet)) {
            missingEC2SpotFleetPermissions.add(FleetAPI.DescribeSpotFleetRequests.name());
        }
        if(!hasDescribeSpotFleetInstancesPermission(ec2Client, fleet)) {
            missingEC2SpotFleetPermissions.add(FleetAPI.DescribeSpotFleetInstances.name());
        }
        if(!hasModifySpotFleetRequestPermission(ec2Client, fleet)) {
            missingEC2SpotFleetPermissions.add(FleetAPI.ModifySpotFleetRequest.name());
        }
        return missingEC2SpotFleetPermissions;
    }

    private List<String> getMissingCommonPermissions(final AmazonEC2 ec2Client) {
        final List<String> missingCommonPermissions = new ArrayList<>();
        if(!hasDescribeInstancePermission(ec2Client)) {
            missingCommonPermissions.add(FleetAPI.DescribeInstances.name());
        }
        if(!hasCreateTagsPermissions(ec2Client)) {
            missingCommonPermissions.add(FleetAPI.CreateTags.name());
        }
        if(!hasDescribeInstanceTypesPermission(ec2Client)) {
            missingCommonPermissions.add(FleetAPI.DescribeInstanceTypes.name());
        }
        return missingCommonPermissions;
    }

    private List<String> getMissingPermissionsForASG() {
        final AmazonAutoScalingClient asgClient = new AutoScalingGroupFleet().createClient(awsCrendentialsId, regionName, endpoint);
        List<String> missingAsgPermissions = new ArrayList<>();
        if(!hasDescribeAutoScalingGroupsPermission(asgClient)) {
            missingAsgPermissions.add(FleetAPI.DescribeAutoScalingGroups.name());
        }
        return missingAsgPermissions;
    }

    private List<String> getMissingPermissionsForEC2EC2Fleet(final AmazonEC2 ec2Client, final String fleet) {
        final List<String> missingFleetPermissions = new ArrayList<>();
        if(!hasDescribeEC2EC2FleetRequestsPermission(ec2Client, fleet)) {
            missingFleetPermissions.add(FleetAPI.DescribeFleets.name());
        }
        if(!hasDescribeEC2EC2FleetInstancesPermission(ec2Client, fleet)) {
            missingFleetPermissions.add(FleetAPI.DescribeFleetInstances.name());
        }
        if(!hasModifyEC2EC2FleetRequestPermission(ec2Client, fleet)) {
            missingFleetPermissions.add(FleetAPI.ModifyFleet.name());
        }
        return missingFleetPermissions;
    }

    private boolean hasModifyEC2EC2FleetRequestPermission(final AmazonEC2 ec2Client, final String fleet) {
        final DryRunResult<ModifyFleetRequest> dryRunResult = ec2Client.dryRun(new ModifyFleetRequest().withFleetId(fleet));
        return dryRunResult.getDryRunResponse().getStatusCode() != UNAUTHORIZED_STATUS_CODE;
    }

    private boolean hasDescribeEC2EC2FleetInstancesPermission(final AmazonEC2 ec2Client, final String fleet) {
        final DryRunResult<DescribeFleetInstancesRequest> dryRunResult = ec2Client.dryRun(new DescribeFleetInstancesRequest().withFleetId(fleet));
        return dryRunResult.getDryRunResponse().getStatusCode() != UNAUTHORIZED_STATUS_CODE;
    }

    private boolean hasDescribeEC2EC2FleetRequestsPermission(final AmazonEC2 ec2Client, final String fleet) {
        final DryRunResult<DescribeFleetsRequest> dryRunResult = ec2Client.dryRun(new DescribeFleetsRequest().withFleetIds(fleet));
        return dryRunResult.getDryRunResponse().getStatusCode() != UNAUTHORIZED_STATUS_CODE;
    }

    private boolean hasModifySpotFleetRequestPermission(final AmazonEC2 ec2Client, final String fleet) {
        final DryRunResult<ModifySpotFleetRequestRequest> dryRunResult = ec2Client.dryRun(new ModifySpotFleetRequestRequest().withSpotFleetRequestId(fleet));
        return dryRunResult.getDryRunResponse().getStatusCode() != UNAUTHORIZED_STATUS_CODE;
    }

    private boolean hasDescribeSpotFleetInstancesPermission(final AmazonEC2 ec2Client, final String fleet) {
        final DryRunResult<DescribeSpotFleetInstancesRequest> dryRunResult = ec2Client.dryRun(new DescribeSpotFleetInstancesRequest().withSpotFleetRequestId(fleet));
        return dryRunResult.getDryRunResponse().getStatusCode() != UNAUTHORIZED_STATUS_CODE;
    }

    private boolean hasDescribeSpotFleetRequestsPermission(final AmazonEC2 ec2Client, final String fleet) {
        final DryRunResult<DescribeSpotFleetRequestsRequest> dryRunResult = ec2Client.dryRun(new DescribeSpotFleetRequestsRequest().withSpotFleetRequestIds(fleet));
        return dryRunResult.getDryRunResponse().getStatusCode() != UNAUTHORIZED_STATUS_CODE;
    }

    private boolean hasDescribeAutoScalingGroupsPermission(final AmazonAutoScalingClient asgClient) {
        try {
            asgClient.describeAutoScalingGroups();
        } catch (final AmazonAutoScalingException ex) {
            return ex.getStatusCode() != UNAUTHORIZED_STATUS_CODE;
        }
        return Boolean.TRUE;
    }

    private boolean hasCreateTagsPermissions(final AmazonEC2 ec2Client) {
        final DryRunResult<CreateTagsRequest> dryRunResult = ec2Client.dryRun(new CreateTagsRequest().withTags(new Tag().withKey("instanceId").withValue("i-1234")));
        return dryRunResult.getDryRunResponse().getStatusCode() != UNAUTHORIZED_STATUS_CODE;
    }

    private boolean hasDescribeInstancePermission(final AmazonEC2 ec2Client) {
        final DryRunResult<DescribeInstancesRequest> dryRunResult = ec2Client.dryRun(new DescribeInstancesRequest());
        return dryRunResult.getDryRunResponse().getStatusCode() != UNAUTHORIZED_STATUS_CODE;
    }

    private boolean hasDescribeInstanceTypesPermission(final AmazonEC2 ec2Client) {
        final DryRunResult<DescribeInstanceTypesRequest> dryRunResult = ec2Client.dryRun(new DescribeInstanceTypesRequest());
        return dryRunResult.getDryRunResponse().getStatusCode() != UNAUTHORIZED_STATUS_CODE;
    }
}
