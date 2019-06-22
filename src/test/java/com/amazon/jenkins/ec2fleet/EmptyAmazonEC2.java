package com.amazon.jenkins.ec2fleet;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.regions.Region;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ec2.waiters.AmazonEC2Waiters;

public class EmptyAmazonEC2 implements AmazonEC2 {
    @Override
    public void setEndpoint(String endpoint) {

    }

    @Override
    public void setRegion(Region region) {

    }

    @Override
    public AcceptReservedInstancesExchangeQuoteResult acceptReservedInstancesExchangeQuote(AcceptReservedInstancesExchangeQuoteRequest acceptReservedInstancesExchangeQuoteRequest) {
        return null;
    }

    @Override
    public AcceptVpcEndpointConnectionsResult acceptVpcEndpointConnections(AcceptVpcEndpointConnectionsRequest acceptVpcEndpointConnectionsRequest) {
        return null;
    }

    @Override
    public AcceptVpcPeeringConnectionResult acceptVpcPeeringConnection(AcceptVpcPeeringConnectionRequest acceptVpcPeeringConnectionRequest) {
        return null;
    }

    @Override
    public AcceptVpcPeeringConnectionResult acceptVpcPeeringConnection() {
        return null;
    }

    @Override
    public AllocateAddressResult allocateAddress(AllocateAddressRequest allocateAddressRequest) {
        return null;
    }

    @Override
    public AllocateAddressResult allocateAddress() {
        return null;
    }

    @Override
    public AllocateHostsResult allocateHosts(AllocateHostsRequest allocateHostsRequest) {
        return null;
    }

    @Override
    public AssignIpv6AddressesResult assignIpv6Addresses(AssignIpv6AddressesRequest assignIpv6AddressesRequest) {
        return null;
    }

    @Override
    public AssignPrivateIpAddressesResult assignPrivateIpAddresses(AssignPrivateIpAddressesRequest assignPrivateIpAddressesRequest) {
        return null;
    }

    @Override
    public AssociateAddressResult associateAddress(AssociateAddressRequest associateAddressRequest) {
        return null;
    }

    @Override
    public AssociateDhcpOptionsResult associateDhcpOptions(AssociateDhcpOptionsRequest associateDhcpOptionsRequest) {
        return null;
    }

    @Override
    public AssociateIamInstanceProfileResult associateIamInstanceProfile(AssociateIamInstanceProfileRequest associateIamInstanceProfileRequest) {
        return null;
    }

    @Override
    public AssociateRouteTableResult associateRouteTable(AssociateRouteTableRequest associateRouteTableRequest) {
        return null;
    }

    @Override
    public AssociateSubnetCidrBlockResult associateSubnetCidrBlock(AssociateSubnetCidrBlockRequest associateSubnetCidrBlockRequest) {
        return null;
    }

    @Override
    public AssociateVpcCidrBlockResult associateVpcCidrBlock(AssociateVpcCidrBlockRequest associateVpcCidrBlockRequest) {
        return null;
    }

    @Override
    public AttachClassicLinkVpcResult attachClassicLinkVpc(AttachClassicLinkVpcRequest attachClassicLinkVpcRequest) {
        return null;
    }

    @Override
    public AttachInternetGatewayResult attachInternetGateway(AttachInternetGatewayRequest attachInternetGatewayRequest) {
        return null;
    }

    @Override
    public AttachNetworkInterfaceResult attachNetworkInterface(AttachNetworkInterfaceRequest attachNetworkInterfaceRequest) {
        return null;
    }

    @Override
    public AttachVolumeResult attachVolume(AttachVolumeRequest attachVolumeRequest) {
        return null;
    }

    @Override
    public AttachVpnGatewayResult attachVpnGateway(AttachVpnGatewayRequest attachVpnGatewayRequest) {
        return null;
    }

    @Override
    public AuthorizeSecurityGroupEgressResult authorizeSecurityGroupEgress(AuthorizeSecurityGroupEgressRequest authorizeSecurityGroupEgressRequest) {
        return null;
    }

    @Override
    public AuthorizeSecurityGroupIngressResult authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest) {
        return null;
    }

    @Override
    public BundleInstanceResult bundleInstance(BundleInstanceRequest bundleInstanceRequest) {
        return null;
    }

    @Override
    public CancelBundleTaskResult cancelBundleTask(CancelBundleTaskRequest cancelBundleTaskRequest) {
        return null;
    }

    @Override
    public CancelConversionTaskResult cancelConversionTask(CancelConversionTaskRequest cancelConversionTaskRequest) {
        return null;
    }

    @Override
    public CancelExportTaskResult cancelExportTask(CancelExportTaskRequest cancelExportTaskRequest) {
        return null;
    }

    @Override
    public CancelImportTaskResult cancelImportTask(CancelImportTaskRequest cancelImportTaskRequest) {
        return null;
    }

    @Override
    public CancelImportTaskResult cancelImportTask() {
        return null;
    }

    @Override
    public CancelReservedInstancesListingResult cancelReservedInstancesListing(CancelReservedInstancesListingRequest cancelReservedInstancesListingRequest) {
        return null;
    }

    @Override
    public CancelSpotFleetRequestsResult cancelSpotFleetRequests(CancelSpotFleetRequestsRequest cancelSpotFleetRequestsRequest) {
        return null;
    }

    @Override
    public CancelSpotInstanceRequestsResult cancelSpotInstanceRequests(CancelSpotInstanceRequestsRequest cancelSpotInstanceRequestsRequest) {
        return null;
    }

    @Override
    public ConfirmProductInstanceResult confirmProductInstance(ConfirmProductInstanceRequest confirmProductInstanceRequest) {
        return null;
    }

    @Override
    public CopyFpgaImageResult copyFpgaImage(CopyFpgaImageRequest copyFpgaImageRequest) {
        return null;
    }

    @Override
    public CopyImageResult copyImage(CopyImageRequest copyImageRequest) {
        return null;
    }

    @Override
    public CopySnapshotResult copySnapshot(CopySnapshotRequest copySnapshotRequest) {
        return null;
    }

    @Override
    public CreateCustomerGatewayResult createCustomerGateway(CreateCustomerGatewayRequest createCustomerGatewayRequest) {
        return null;
    }

    @Override
    public CreateDefaultSubnetResult createDefaultSubnet(CreateDefaultSubnetRequest createDefaultSubnetRequest) {
        return null;
    }

    @Override
    public CreateDefaultVpcResult createDefaultVpc(CreateDefaultVpcRequest createDefaultVpcRequest) {
        return null;
    }

    @Override
    public CreateDhcpOptionsResult createDhcpOptions(CreateDhcpOptionsRequest createDhcpOptionsRequest) {
        return null;
    }

    @Override
    public CreateEgressOnlyInternetGatewayResult createEgressOnlyInternetGateway(CreateEgressOnlyInternetGatewayRequest createEgressOnlyInternetGatewayRequest) {
        return null;
    }

    @Override
    public CreateFleetResult createFleet(CreateFleetRequest createFleetRequest) {
        return null;
    }

    @Override
    public CreateFlowLogsResult createFlowLogs(CreateFlowLogsRequest createFlowLogsRequest) {
        return null;
    }

    @Override
    public CreateFpgaImageResult createFpgaImage(CreateFpgaImageRequest createFpgaImageRequest) {
        return null;
    }

    @Override
    public CreateImageResult createImage(CreateImageRequest createImageRequest) {
        return null;
    }

    @Override
    public CreateInstanceExportTaskResult createInstanceExportTask(CreateInstanceExportTaskRequest createInstanceExportTaskRequest) {
        return null;
    }

    @Override
    public CreateInternetGatewayResult createInternetGateway(CreateInternetGatewayRequest createInternetGatewayRequest) {
        return null;
    }

    @Override
    public CreateInternetGatewayResult createInternetGateway() {
        return null;
    }

    @Override
    public CreateKeyPairResult createKeyPair(CreateKeyPairRequest createKeyPairRequest) {
        return null;
    }

    @Override
    public CreateLaunchTemplateResult createLaunchTemplate(CreateLaunchTemplateRequest createLaunchTemplateRequest) {
        return null;
    }

    @Override
    public CreateLaunchTemplateVersionResult createLaunchTemplateVersion(CreateLaunchTemplateVersionRequest createLaunchTemplateVersionRequest) {
        return null;
    }

    @Override
    public CreateNatGatewayResult createNatGateway(CreateNatGatewayRequest createNatGatewayRequest) {
        return null;
    }

    @Override
    public CreateNetworkAclResult createNetworkAcl(CreateNetworkAclRequest createNetworkAclRequest) {
        return null;
    }

    @Override
    public CreateNetworkAclEntryResult createNetworkAclEntry(CreateNetworkAclEntryRequest createNetworkAclEntryRequest) {
        return null;
    }

    @Override
    public CreateNetworkInterfaceResult createNetworkInterface(CreateNetworkInterfaceRequest createNetworkInterfaceRequest) {
        return null;
    }

    @Override
    public CreateNetworkInterfacePermissionResult createNetworkInterfacePermission(CreateNetworkInterfacePermissionRequest createNetworkInterfacePermissionRequest) {
        return null;
    }

    @Override
    public CreatePlacementGroupResult createPlacementGroup(CreatePlacementGroupRequest createPlacementGroupRequest) {
        return null;
    }

    @Override
    public CreateReservedInstancesListingResult createReservedInstancesListing(CreateReservedInstancesListingRequest createReservedInstancesListingRequest) {
        return null;
    }

    @Override
    public CreateRouteResult createRoute(CreateRouteRequest createRouteRequest) {
        return null;
    }

    @Override
    public CreateRouteTableResult createRouteTable(CreateRouteTableRequest createRouteTableRequest) {
        return null;
    }

    @Override
    public CreateSecurityGroupResult createSecurityGroup(CreateSecurityGroupRequest createSecurityGroupRequest) {
        return null;
    }

    @Override
    public CreateSnapshotResult createSnapshot(CreateSnapshotRequest createSnapshotRequest) {
        return null;
    }

    @Override
    public CreateSpotDatafeedSubscriptionResult createSpotDatafeedSubscription(CreateSpotDatafeedSubscriptionRequest createSpotDatafeedSubscriptionRequest) {
        return null;
    }

    @Override
    public CreateSubnetResult createSubnet(CreateSubnetRequest createSubnetRequest) {
        return null;
    }

    @Override
    public CreateTagsResult createTags(CreateTagsRequest createTagsRequest) {
        return null;
    }

    @Override
    public CreateVolumeResult createVolume(CreateVolumeRequest createVolumeRequest) {
        return null;
    }

    @Override
    public CreateVpcResult createVpc(CreateVpcRequest createVpcRequest) {
        return null;
    }

    @Override
    public CreateVpcEndpointResult createVpcEndpoint(CreateVpcEndpointRequest createVpcEndpointRequest) {
        return null;
    }

    @Override
    public CreateVpcEndpointConnectionNotificationResult createVpcEndpointConnectionNotification(CreateVpcEndpointConnectionNotificationRequest createVpcEndpointConnectionNotificationRequest) {
        return null;
    }

    @Override
    public CreateVpcEndpointServiceConfigurationResult createVpcEndpointServiceConfiguration(CreateVpcEndpointServiceConfigurationRequest createVpcEndpointServiceConfigurationRequest) {
        return null;
    }

    @Override
    public CreateVpcPeeringConnectionResult createVpcPeeringConnection(CreateVpcPeeringConnectionRequest createVpcPeeringConnectionRequest) {
        return null;
    }

    @Override
    public CreateVpcPeeringConnectionResult createVpcPeeringConnection() {
        return null;
    }

    @Override
    public CreateVpnConnectionResult createVpnConnection(CreateVpnConnectionRequest createVpnConnectionRequest) {
        return null;
    }

    @Override
    public CreateVpnConnectionRouteResult createVpnConnectionRoute(CreateVpnConnectionRouteRequest createVpnConnectionRouteRequest) {
        return null;
    }

    @Override
    public CreateVpnGatewayResult createVpnGateway(CreateVpnGatewayRequest createVpnGatewayRequest) {
        return null;
    }

    @Override
    public DeleteCustomerGatewayResult deleteCustomerGateway(DeleteCustomerGatewayRequest deleteCustomerGatewayRequest) {
        return null;
    }

    @Override
    public DeleteDhcpOptionsResult deleteDhcpOptions(DeleteDhcpOptionsRequest deleteDhcpOptionsRequest) {
        return null;
    }

    @Override
    public DeleteEgressOnlyInternetGatewayResult deleteEgressOnlyInternetGateway(DeleteEgressOnlyInternetGatewayRequest deleteEgressOnlyInternetGatewayRequest) {
        return null;
    }

    @Override
    public DeleteFleetsResult deleteFleets(DeleteFleetsRequest deleteFleetsRequest) {
        return null;
    }

    @Override
    public DeleteFlowLogsResult deleteFlowLogs(DeleteFlowLogsRequest deleteFlowLogsRequest) {
        return null;
    }

    @Override
    public DeleteFpgaImageResult deleteFpgaImage(DeleteFpgaImageRequest deleteFpgaImageRequest) {
        return null;
    }

    @Override
    public DeleteInternetGatewayResult deleteInternetGateway(DeleteInternetGatewayRequest deleteInternetGatewayRequest) {
        return null;
    }

    @Override
    public DeleteKeyPairResult deleteKeyPair(DeleteKeyPairRequest deleteKeyPairRequest) {
        return null;
    }

    @Override
    public DeleteLaunchTemplateResult deleteLaunchTemplate(DeleteLaunchTemplateRequest deleteLaunchTemplateRequest) {
        return null;
    }

    @Override
    public DeleteLaunchTemplateVersionsResult deleteLaunchTemplateVersions(DeleteLaunchTemplateVersionsRequest deleteLaunchTemplateVersionsRequest) {
        return null;
    }

    @Override
    public DeleteNatGatewayResult deleteNatGateway(DeleteNatGatewayRequest deleteNatGatewayRequest) {
        return null;
    }

    @Override
    public DeleteNetworkAclResult deleteNetworkAcl(DeleteNetworkAclRequest deleteNetworkAclRequest) {
        return null;
    }

    @Override
    public DeleteNetworkAclEntryResult deleteNetworkAclEntry(DeleteNetworkAclEntryRequest deleteNetworkAclEntryRequest) {
        return null;
    }

    @Override
    public DeleteNetworkInterfaceResult deleteNetworkInterface(DeleteNetworkInterfaceRequest deleteNetworkInterfaceRequest) {
        return null;
    }

    @Override
    public DeleteNetworkInterfacePermissionResult deleteNetworkInterfacePermission(DeleteNetworkInterfacePermissionRequest deleteNetworkInterfacePermissionRequest) {
        return null;
    }

    @Override
    public DeletePlacementGroupResult deletePlacementGroup(DeletePlacementGroupRequest deletePlacementGroupRequest) {
        return null;
    }

    @Override
    public DeleteRouteResult deleteRoute(DeleteRouteRequest deleteRouteRequest) {
        return null;
    }

    @Override
    public DeleteRouteTableResult deleteRouteTable(DeleteRouteTableRequest deleteRouteTableRequest) {
        return null;
    }

    @Override
    public DeleteSecurityGroupResult deleteSecurityGroup(DeleteSecurityGroupRequest deleteSecurityGroupRequest) {
        return null;
    }

    @Override
    public DeleteSnapshotResult deleteSnapshot(DeleteSnapshotRequest deleteSnapshotRequest) {
        return null;
    }

    @Override
    public DeleteSpotDatafeedSubscriptionResult deleteSpotDatafeedSubscription(DeleteSpotDatafeedSubscriptionRequest deleteSpotDatafeedSubscriptionRequest) {
        return null;
    }

    @Override
    public DeleteSpotDatafeedSubscriptionResult deleteSpotDatafeedSubscription() {
        return null;
    }

    @Override
    public DeleteSubnetResult deleteSubnet(DeleteSubnetRequest deleteSubnetRequest) {
        return null;
    }

    @Override
    public DeleteTagsResult deleteTags(DeleteTagsRequest deleteTagsRequest) {
        return null;
    }

    @Override
    public DeleteVolumeResult deleteVolume(DeleteVolumeRequest deleteVolumeRequest) {
        return null;
    }

    @Override
    public DeleteVpcResult deleteVpc(DeleteVpcRequest deleteVpcRequest) {
        return null;
    }

    @Override
    public DeleteVpcEndpointConnectionNotificationsResult deleteVpcEndpointConnectionNotifications(DeleteVpcEndpointConnectionNotificationsRequest deleteVpcEndpointConnectionNotificationsRequest) {
        return null;
    }

    @Override
    public DeleteVpcEndpointServiceConfigurationsResult deleteVpcEndpointServiceConfigurations(DeleteVpcEndpointServiceConfigurationsRequest deleteVpcEndpointServiceConfigurationsRequest) {
        return null;
    }

    @Override
    public DeleteVpcEndpointsResult deleteVpcEndpoints(DeleteVpcEndpointsRequest deleteVpcEndpointsRequest) {
        return null;
    }

    @Override
    public DeleteVpcPeeringConnectionResult deleteVpcPeeringConnection(DeleteVpcPeeringConnectionRequest deleteVpcPeeringConnectionRequest) {
        return null;
    }

    @Override
    public DeleteVpnConnectionResult deleteVpnConnection(DeleteVpnConnectionRequest deleteVpnConnectionRequest) {
        return null;
    }

    @Override
    public DeleteVpnConnectionRouteResult deleteVpnConnectionRoute(DeleteVpnConnectionRouteRequest deleteVpnConnectionRouteRequest) {
        return null;
    }

    @Override
    public DeleteVpnGatewayResult deleteVpnGateway(DeleteVpnGatewayRequest deleteVpnGatewayRequest) {
        return null;
    }

    @Override
    public DeregisterImageResult deregisterImage(DeregisterImageRequest deregisterImageRequest) {
        return null;
    }

    @Override
    public DescribeAccountAttributesResult describeAccountAttributes(DescribeAccountAttributesRequest describeAccountAttributesRequest) {
        return null;
    }

    @Override
    public DescribeAccountAttributesResult describeAccountAttributes() {
        return null;
    }

    @Override
    public DescribeAddressesResult describeAddresses(DescribeAddressesRequest describeAddressesRequest) {
        return null;
    }

    @Override
    public DescribeAddressesResult describeAddresses() {
        return null;
    }

    @Override
    public DescribeAggregateIdFormatResult describeAggregateIdFormat(DescribeAggregateIdFormatRequest describeAggregateIdFormatRequest) {
        return null;
    }

    @Override
    public DescribeAvailabilityZonesResult describeAvailabilityZones(DescribeAvailabilityZonesRequest describeAvailabilityZonesRequest) {
        return null;
    }

    @Override
    public DescribeAvailabilityZonesResult describeAvailabilityZones() {
        return null;
    }

    @Override
    public DescribeBundleTasksResult describeBundleTasks(DescribeBundleTasksRequest describeBundleTasksRequest) {
        return null;
    }

    @Override
    public DescribeBundleTasksResult describeBundleTasks() {
        return null;
    }

    @Override
    public DescribeClassicLinkInstancesResult describeClassicLinkInstances(DescribeClassicLinkInstancesRequest describeClassicLinkInstancesRequest) {
        return null;
    }

    @Override
    public DescribeClassicLinkInstancesResult describeClassicLinkInstances() {
        return null;
    }

    @Override
    public DescribeConversionTasksResult describeConversionTasks(DescribeConversionTasksRequest describeConversionTasksRequest) {
        return null;
    }

    @Override
    public DescribeConversionTasksResult describeConversionTasks() {
        return null;
    }

    @Override
    public DescribeCustomerGatewaysResult describeCustomerGateways(DescribeCustomerGatewaysRequest describeCustomerGatewaysRequest) {
        return null;
    }

    @Override
    public DescribeCustomerGatewaysResult describeCustomerGateways() {
        return null;
    }

    @Override
    public DescribeDhcpOptionsResult describeDhcpOptions(DescribeDhcpOptionsRequest describeDhcpOptionsRequest) {
        return null;
    }

    @Override
    public DescribeDhcpOptionsResult describeDhcpOptions() {
        return null;
    }

    @Override
    public DescribeEgressOnlyInternetGatewaysResult describeEgressOnlyInternetGateways(DescribeEgressOnlyInternetGatewaysRequest describeEgressOnlyInternetGatewaysRequest) {
        return null;
    }

    @Override
    public DescribeElasticGpusResult describeElasticGpus(DescribeElasticGpusRequest describeElasticGpusRequest) {
        return null;
    }

    @Override
    public DescribeExportTasksResult describeExportTasks(DescribeExportTasksRequest describeExportTasksRequest) {
        return null;
    }

    @Override
    public DescribeExportTasksResult describeExportTasks() {
        return null;
    }

    @Override
    public DescribeFleetHistoryResult describeFleetHistory(DescribeFleetHistoryRequest describeFleetHistoryRequest) {
        return null;
    }

    @Override
    public DescribeFleetInstancesResult describeFleetInstances(DescribeFleetInstancesRequest describeFleetInstancesRequest) {
        return null;
    }

    @Override
    public DescribeFleetsResult describeFleets(DescribeFleetsRequest describeFleetsRequest) {
        return null;
    }

    @Override
    public DescribeFlowLogsResult describeFlowLogs(DescribeFlowLogsRequest describeFlowLogsRequest) {
        return null;
    }

    @Override
    public DescribeFlowLogsResult describeFlowLogs() {
        return null;
    }

    @Override
    public DescribeFpgaImageAttributeResult describeFpgaImageAttribute(DescribeFpgaImageAttributeRequest describeFpgaImageAttributeRequest) {
        return null;
    }

    @Override
    public DescribeFpgaImagesResult describeFpgaImages(DescribeFpgaImagesRequest describeFpgaImagesRequest) {
        return null;
    }

    @Override
    public DescribeHostReservationOfferingsResult describeHostReservationOfferings(DescribeHostReservationOfferingsRequest describeHostReservationOfferingsRequest) {
        return null;
    }

    @Override
    public DescribeHostReservationsResult describeHostReservations(DescribeHostReservationsRequest describeHostReservationsRequest) {
        return null;
    }

    @Override
    public DescribeHostsResult describeHosts(DescribeHostsRequest describeHostsRequest) {
        return null;
    }

    @Override
    public DescribeHostsResult describeHosts() {
        return null;
    }

    @Override
    public DescribeIamInstanceProfileAssociationsResult describeIamInstanceProfileAssociations(DescribeIamInstanceProfileAssociationsRequest describeIamInstanceProfileAssociationsRequest) {
        return null;
    }

    @Override
    public DescribeIdFormatResult describeIdFormat(DescribeIdFormatRequest describeIdFormatRequest) {
        return null;
    }

    @Override
    public DescribeIdFormatResult describeIdFormat() {
        return null;
    }

    @Override
    public DescribeIdentityIdFormatResult describeIdentityIdFormat(DescribeIdentityIdFormatRequest describeIdentityIdFormatRequest) {
        return null;
    }

    @Override
    public DescribeImageAttributeResult describeImageAttribute(DescribeImageAttributeRequest describeImageAttributeRequest) {
        return null;
    }

    @Override
    public DescribeImagesResult describeImages(DescribeImagesRequest describeImagesRequest) {
        return null;
    }

    @Override
    public DescribeImagesResult describeImages() {
        return null;
    }

    @Override
    public DescribeImportImageTasksResult describeImportImageTasks(DescribeImportImageTasksRequest describeImportImageTasksRequest) {
        return null;
    }

    @Override
    public DescribeImportImageTasksResult describeImportImageTasks() {
        return null;
    }

    @Override
    public DescribeImportSnapshotTasksResult describeImportSnapshotTasks(DescribeImportSnapshotTasksRequest describeImportSnapshotTasksRequest) {
        return null;
    }

    @Override
    public DescribeImportSnapshotTasksResult describeImportSnapshotTasks() {
        return null;
    }

    @Override
    public DescribeInstanceAttributeResult describeInstanceAttribute(DescribeInstanceAttributeRequest describeInstanceAttributeRequest) {
        return null;
    }

    @Override
    public DescribeInstanceCreditSpecificationsResult describeInstanceCreditSpecifications(DescribeInstanceCreditSpecificationsRequest describeInstanceCreditSpecificationsRequest) {
        return null;
    }

    @Override
    public DescribeInstanceStatusResult describeInstanceStatus(DescribeInstanceStatusRequest describeInstanceStatusRequest) {
        return null;
    }

    @Override
    public DescribeInstanceStatusResult describeInstanceStatus() {
        return null;
    }

    @Override
    public DescribeInstancesResult describeInstances(DescribeInstancesRequest describeInstancesRequest) {
        return null;
    }

    @Override
    public DescribeInstancesResult describeInstances() {
        return null;
    }

    @Override
    public DescribeInternetGatewaysResult describeInternetGateways(DescribeInternetGatewaysRequest describeInternetGatewaysRequest) {
        return null;
    }

    @Override
    public DescribeInternetGatewaysResult describeInternetGateways() {
        return null;
    }

    @Override
    public DescribeKeyPairsResult describeKeyPairs(DescribeKeyPairsRequest describeKeyPairsRequest) {
        return null;
    }

    @Override
    public DescribeKeyPairsResult describeKeyPairs() {
        return null;
    }

    @Override
    public DescribeLaunchTemplateVersionsResult describeLaunchTemplateVersions(DescribeLaunchTemplateVersionsRequest describeLaunchTemplateVersionsRequest) {
        return null;
    }

    @Override
    public DescribeLaunchTemplatesResult describeLaunchTemplates(DescribeLaunchTemplatesRequest describeLaunchTemplatesRequest) {
        return null;
    }

    @Override
    public DescribeMovingAddressesResult describeMovingAddresses(DescribeMovingAddressesRequest describeMovingAddressesRequest) {
        return null;
    }

    @Override
    public DescribeMovingAddressesResult describeMovingAddresses() {
        return null;
    }

    @Override
    public DescribeNatGatewaysResult describeNatGateways(DescribeNatGatewaysRequest describeNatGatewaysRequest) {
        return null;
    }

    @Override
    public DescribeNetworkAclsResult describeNetworkAcls(DescribeNetworkAclsRequest describeNetworkAclsRequest) {
        return null;
    }

    @Override
    public DescribeNetworkAclsResult describeNetworkAcls() {
        return null;
    }

    @Override
    public DescribeNetworkInterfaceAttributeResult describeNetworkInterfaceAttribute(DescribeNetworkInterfaceAttributeRequest describeNetworkInterfaceAttributeRequest) {
        return null;
    }

    @Override
    public DescribeNetworkInterfacePermissionsResult describeNetworkInterfacePermissions(DescribeNetworkInterfacePermissionsRequest describeNetworkInterfacePermissionsRequest) {
        return null;
    }

    @Override
    public DescribeNetworkInterfacesResult describeNetworkInterfaces(DescribeNetworkInterfacesRequest describeNetworkInterfacesRequest) {
        return null;
    }

    @Override
    public DescribeNetworkInterfacesResult describeNetworkInterfaces() {
        return null;
    }

    @Override
    public DescribePlacementGroupsResult describePlacementGroups(DescribePlacementGroupsRequest describePlacementGroupsRequest) {
        return null;
    }

    @Override
    public DescribePlacementGroupsResult describePlacementGroups() {
        return null;
    }

    @Override
    public DescribePrefixListsResult describePrefixLists(DescribePrefixListsRequest describePrefixListsRequest) {
        return null;
    }

    @Override
    public DescribePrefixListsResult describePrefixLists() {
        return null;
    }

    @Override
    public DescribePrincipalIdFormatResult describePrincipalIdFormat(DescribePrincipalIdFormatRequest describePrincipalIdFormatRequest) {
        return null;
    }

    @Override
    public DescribeRegionsResult describeRegions(DescribeRegionsRequest describeRegionsRequest) {
        return null;
    }

    @Override
    public DescribeRegionsResult describeRegions() {
        return null;
    }

    @Override
    public DescribeReservedInstancesResult describeReservedInstances(DescribeReservedInstancesRequest describeReservedInstancesRequest) {
        return null;
    }

    @Override
    public DescribeReservedInstancesResult describeReservedInstances() {
        return null;
    }

    @Override
    public DescribeReservedInstancesListingsResult describeReservedInstancesListings(DescribeReservedInstancesListingsRequest describeReservedInstancesListingsRequest) {
        return null;
    }

    @Override
    public DescribeReservedInstancesListingsResult describeReservedInstancesListings() {
        return null;
    }

    @Override
    public DescribeReservedInstancesModificationsResult describeReservedInstancesModifications(DescribeReservedInstancesModificationsRequest describeReservedInstancesModificationsRequest) {
        return null;
    }

    @Override
    public DescribeReservedInstancesModificationsResult describeReservedInstancesModifications() {
        return null;
    }

    @Override
    public DescribeReservedInstancesOfferingsResult describeReservedInstancesOfferings(DescribeReservedInstancesOfferingsRequest describeReservedInstancesOfferingsRequest) {
        return null;
    }

    @Override
    public DescribeReservedInstancesOfferingsResult describeReservedInstancesOfferings() {
        return null;
    }

    @Override
    public DescribeRouteTablesResult describeRouteTables(DescribeRouteTablesRequest describeRouteTablesRequest) {
        return null;
    }

    @Override
    public DescribeRouteTablesResult describeRouteTables() {
        return null;
    }

    @Override
    public DescribeScheduledInstanceAvailabilityResult describeScheduledInstanceAvailability(DescribeScheduledInstanceAvailabilityRequest describeScheduledInstanceAvailabilityRequest) {
        return null;
    }

    @Override
    public DescribeScheduledInstancesResult describeScheduledInstances(DescribeScheduledInstancesRequest describeScheduledInstancesRequest) {
        return null;
    }

    @Override
    public DescribeSecurityGroupReferencesResult describeSecurityGroupReferences(DescribeSecurityGroupReferencesRequest describeSecurityGroupReferencesRequest) {
        return null;
    }

    @Override
    public DescribeSecurityGroupsResult describeSecurityGroups(DescribeSecurityGroupsRequest describeSecurityGroupsRequest) {
        return null;
    }

    @Override
    public DescribeSecurityGroupsResult describeSecurityGroups() {
        return null;
    }

    @Override
    public DescribeSnapshotAttributeResult describeSnapshotAttribute(DescribeSnapshotAttributeRequest describeSnapshotAttributeRequest) {
        return null;
    }

    @Override
    public DescribeSnapshotsResult describeSnapshots(DescribeSnapshotsRequest describeSnapshotsRequest) {
        return null;
    }

    @Override
    public DescribeSnapshotsResult describeSnapshots() {
        return null;
    }

    @Override
    public DescribeSpotDatafeedSubscriptionResult describeSpotDatafeedSubscription(DescribeSpotDatafeedSubscriptionRequest describeSpotDatafeedSubscriptionRequest) {
        return null;
    }

    @Override
    public DescribeSpotDatafeedSubscriptionResult describeSpotDatafeedSubscription() {
        return null;
    }

    @Override
    public DescribeSpotFleetInstancesResult describeSpotFleetInstances(DescribeSpotFleetInstancesRequest describeSpotFleetInstancesRequest) {
        return null;
    }

    @Override
    public DescribeSpotFleetRequestHistoryResult describeSpotFleetRequestHistory(DescribeSpotFleetRequestHistoryRequest describeSpotFleetRequestHistoryRequest) {
        return null;
    }

    @Override
    public DescribeSpotFleetRequestsResult describeSpotFleetRequests(DescribeSpotFleetRequestsRequest describeSpotFleetRequestsRequest) {
        return null;
    }

    @Override
    public DescribeSpotFleetRequestsResult describeSpotFleetRequests() {
        return null;
    }

    @Override
    public DescribeSpotInstanceRequestsResult describeSpotInstanceRequests(DescribeSpotInstanceRequestsRequest describeSpotInstanceRequestsRequest) {
        return null;
    }

    @Override
    public DescribeSpotInstanceRequestsResult describeSpotInstanceRequests() {
        return null;
    }

    @Override
    public DescribeSpotPriceHistoryResult describeSpotPriceHistory(DescribeSpotPriceHistoryRequest describeSpotPriceHistoryRequest) {
        return null;
    }

    @Override
    public DescribeSpotPriceHistoryResult describeSpotPriceHistory() {
        return null;
    }

    @Override
    public DescribeStaleSecurityGroupsResult describeStaleSecurityGroups(DescribeStaleSecurityGroupsRequest describeStaleSecurityGroupsRequest) {
        return null;
    }

    @Override
    public DescribeSubnetsResult describeSubnets(DescribeSubnetsRequest describeSubnetsRequest) {
        return null;
    }

    @Override
    public DescribeSubnetsResult describeSubnets() {
        return null;
    }

    @Override
    public DescribeTagsResult describeTags(DescribeTagsRequest describeTagsRequest) {
        return null;
    }

    @Override
    public DescribeTagsResult describeTags() {
        return null;
    }

    @Override
    public DescribeVolumeAttributeResult describeVolumeAttribute(DescribeVolumeAttributeRequest describeVolumeAttributeRequest) {
        return null;
    }

    @Override
    public DescribeVolumeStatusResult describeVolumeStatus(DescribeVolumeStatusRequest describeVolumeStatusRequest) {
        return null;
    }

    @Override
    public DescribeVolumeStatusResult describeVolumeStatus() {
        return null;
    }

    @Override
    public DescribeVolumesResult describeVolumes(DescribeVolumesRequest describeVolumesRequest) {
        return null;
    }

    @Override
    public DescribeVolumesResult describeVolumes() {
        return null;
    }

    @Override
    public DescribeVolumesModificationsResult describeVolumesModifications(DescribeVolumesModificationsRequest describeVolumesModificationsRequest) {
        return null;
    }

    @Override
    public DescribeVpcAttributeResult describeVpcAttribute(DescribeVpcAttributeRequest describeVpcAttributeRequest) {
        return null;
    }

    @Override
    public DescribeVpcClassicLinkResult describeVpcClassicLink(DescribeVpcClassicLinkRequest describeVpcClassicLinkRequest) {
        return null;
    }

    @Override
    public DescribeVpcClassicLinkResult describeVpcClassicLink() {
        return null;
    }

    @Override
    public DescribeVpcClassicLinkDnsSupportResult describeVpcClassicLinkDnsSupport(DescribeVpcClassicLinkDnsSupportRequest describeVpcClassicLinkDnsSupportRequest) {
        return null;
    }

    @Override
    public DescribeVpcEndpointConnectionNotificationsResult describeVpcEndpointConnectionNotifications(DescribeVpcEndpointConnectionNotificationsRequest describeVpcEndpointConnectionNotificationsRequest) {
        return null;
    }

    @Override
    public DescribeVpcEndpointConnectionsResult describeVpcEndpointConnections(DescribeVpcEndpointConnectionsRequest describeVpcEndpointConnectionsRequest) {
        return null;
    }

    @Override
    public DescribeVpcEndpointServiceConfigurationsResult describeVpcEndpointServiceConfigurations(DescribeVpcEndpointServiceConfigurationsRequest describeVpcEndpointServiceConfigurationsRequest) {
        return null;
    }

    @Override
    public DescribeVpcEndpointServicePermissionsResult describeVpcEndpointServicePermissions(DescribeVpcEndpointServicePermissionsRequest describeVpcEndpointServicePermissionsRequest) {
        return null;
    }

    @Override
    public DescribeVpcEndpointServicesResult describeVpcEndpointServices(DescribeVpcEndpointServicesRequest describeVpcEndpointServicesRequest) {
        return null;
    }

    @Override
    public DescribeVpcEndpointServicesResult describeVpcEndpointServices() {
        return null;
    }

    @Override
    public DescribeVpcEndpointsResult describeVpcEndpoints(DescribeVpcEndpointsRequest describeVpcEndpointsRequest) {
        return null;
    }

    @Override
    public DescribeVpcEndpointsResult describeVpcEndpoints() {
        return null;
    }

    @Override
    public DescribeVpcPeeringConnectionsResult describeVpcPeeringConnections(DescribeVpcPeeringConnectionsRequest describeVpcPeeringConnectionsRequest) {
        return null;
    }

    @Override
    public DescribeVpcPeeringConnectionsResult describeVpcPeeringConnections() {
        return null;
    }

    @Override
    public DescribeVpcsResult describeVpcs(DescribeVpcsRequest describeVpcsRequest) {
        return null;
    }

    @Override
    public DescribeVpcsResult describeVpcs() {
        return null;
    }

    @Override
    public DescribeVpnConnectionsResult describeVpnConnections(DescribeVpnConnectionsRequest describeVpnConnectionsRequest) {
        return null;
    }

    @Override
    public DescribeVpnConnectionsResult describeVpnConnections() {
        return null;
    }

    @Override
    public DescribeVpnGatewaysResult describeVpnGateways(DescribeVpnGatewaysRequest describeVpnGatewaysRequest) {
        return null;
    }

    @Override
    public DescribeVpnGatewaysResult describeVpnGateways() {
        return null;
    }

    @Override
    public DetachClassicLinkVpcResult detachClassicLinkVpc(DetachClassicLinkVpcRequest detachClassicLinkVpcRequest) {
        return null;
    }

    @Override
    public DetachInternetGatewayResult detachInternetGateway(DetachInternetGatewayRequest detachInternetGatewayRequest) {
        return null;
    }

    @Override
    public DetachNetworkInterfaceResult detachNetworkInterface(DetachNetworkInterfaceRequest detachNetworkInterfaceRequest) {
        return null;
    }

    @Override
    public DetachVolumeResult detachVolume(DetachVolumeRequest detachVolumeRequest) {
        return null;
    }

    @Override
    public DetachVpnGatewayResult detachVpnGateway(DetachVpnGatewayRequest detachVpnGatewayRequest) {
        return null;
    }

    @Override
    public DisableVgwRoutePropagationResult disableVgwRoutePropagation(DisableVgwRoutePropagationRequest disableVgwRoutePropagationRequest) {
        return null;
    }

    @Override
    public DisableVpcClassicLinkResult disableVpcClassicLink(DisableVpcClassicLinkRequest disableVpcClassicLinkRequest) {
        return null;
    }

    @Override
    public DisableVpcClassicLinkDnsSupportResult disableVpcClassicLinkDnsSupport(DisableVpcClassicLinkDnsSupportRequest disableVpcClassicLinkDnsSupportRequest) {
        return null;
    }

    @Override
    public DisassociateAddressResult disassociateAddress(DisassociateAddressRequest disassociateAddressRequest) {
        return null;
    }

    @Override
    public DisassociateIamInstanceProfileResult disassociateIamInstanceProfile(DisassociateIamInstanceProfileRequest disassociateIamInstanceProfileRequest) {
        return null;
    }

    @Override
    public DisassociateRouteTableResult disassociateRouteTable(DisassociateRouteTableRequest disassociateRouteTableRequest) {
        return null;
    }

    @Override
    public DisassociateSubnetCidrBlockResult disassociateSubnetCidrBlock(DisassociateSubnetCidrBlockRequest disassociateSubnetCidrBlockRequest) {
        return null;
    }

    @Override
    public DisassociateVpcCidrBlockResult disassociateVpcCidrBlock(DisassociateVpcCidrBlockRequest disassociateVpcCidrBlockRequest) {
        return null;
    }

    @Override
    public EnableVgwRoutePropagationResult enableVgwRoutePropagation(EnableVgwRoutePropagationRequest enableVgwRoutePropagationRequest) {
        return null;
    }

    @Override
    public EnableVolumeIOResult enableVolumeIO(EnableVolumeIORequest enableVolumeIORequest) {
        return null;
    }

    @Override
    public EnableVpcClassicLinkResult enableVpcClassicLink(EnableVpcClassicLinkRequest enableVpcClassicLinkRequest) {
        return null;
    }

    @Override
    public EnableVpcClassicLinkDnsSupportResult enableVpcClassicLinkDnsSupport(EnableVpcClassicLinkDnsSupportRequest enableVpcClassicLinkDnsSupportRequest) {
        return null;
    }

    @Override
    public GetConsoleOutputResult getConsoleOutput(GetConsoleOutputRequest getConsoleOutputRequest) {
        return null;
    }

    @Override
    public GetConsoleScreenshotResult getConsoleScreenshot(GetConsoleScreenshotRequest getConsoleScreenshotRequest) {
        return null;
    }

    @Override
    public GetHostReservationPurchasePreviewResult getHostReservationPurchasePreview(GetHostReservationPurchasePreviewRequest getHostReservationPurchasePreviewRequest) {
        return null;
    }

    @Override
    public GetLaunchTemplateDataResult getLaunchTemplateData(GetLaunchTemplateDataRequest getLaunchTemplateDataRequest) {
        return null;
    }

    @Override
    public GetPasswordDataResult getPasswordData(GetPasswordDataRequest getPasswordDataRequest) {
        return null;
    }

    @Override
    public GetReservedInstancesExchangeQuoteResult getReservedInstancesExchangeQuote(GetReservedInstancesExchangeQuoteRequest getReservedInstancesExchangeQuoteRequest) {
        return null;
    }

    @Override
    public ImportImageResult importImage(ImportImageRequest importImageRequest) {
        return null;
    }

    @Override
    public ImportImageResult importImage() {
        return null;
    }

    @Override
    public ImportInstanceResult importInstance(ImportInstanceRequest importInstanceRequest) {
        return null;
    }

    @Override
    public ImportKeyPairResult importKeyPair(ImportKeyPairRequest importKeyPairRequest) {
        return null;
    }

    @Override
    public ImportSnapshotResult importSnapshot(ImportSnapshotRequest importSnapshotRequest) {
        return null;
    }

    @Override
    public ImportSnapshotResult importSnapshot() {
        return null;
    }

    @Override
    public ImportVolumeResult importVolume(ImportVolumeRequest importVolumeRequest) {
        return null;
    }

    @Override
    public ModifyFleetResult modifyFleet(ModifyFleetRequest modifyFleetRequest) {
        return null;
    }

    @Override
    public ModifyFpgaImageAttributeResult modifyFpgaImageAttribute(ModifyFpgaImageAttributeRequest modifyFpgaImageAttributeRequest) {
        return null;
    }

    @Override
    public ModifyHostsResult modifyHosts(ModifyHostsRequest modifyHostsRequest) {
        return null;
    }

    @Override
    public ModifyIdFormatResult modifyIdFormat(ModifyIdFormatRequest modifyIdFormatRequest) {
        return null;
    }

    @Override
    public ModifyIdentityIdFormatResult modifyIdentityIdFormat(ModifyIdentityIdFormatRequest modifyIdentityIdFormatRequest) {
        return null;
    }

    @Override
    public ModifyImageAttributeResult modifyImageAttribute(ModifyImageAttributeRequest modifyImageAttributeRequest) {
        return null;
    }

    @Override
    public ModifyInstanceAttributeResult modifyInstanceAttribute(ModifyInstanceAttributeRequest modifyInstanceAttributeRequest) {
        return null;
    }

    @Override
    public ModifyInstanceCreditSpecificationResult modifyInstanceCreditSpecification(ModifyInstanceCreditSpecificationRequest modifyInstanceCreditSpecificationRequest) {
        return null;
    }

    @Override
    public ModifyInstancePlacementResult modifyInstancePlacement(ModifyInstancePlacementRequest modifyInstancePlacementRequest) {
        return null;
    }

    @Override
    public ModifyLaunchTemplateResult modifyLaunchTemplate(ModifyLaunchTemplateRequest modifyLaunchTemplateRequest) {
        return null;
    }

    @Override
    public ModifyNetworkInterfaceAttributeResult modifyNetworkInterfaceAttribute(ModifyNetworkInterfaceAttributeRequest modifyNetworkInterfaceAttributeRequest) {
        return null;
    }

    @Override
    public ModifyReservedInstancesResult modifyReservedInstances(ModifyReservedInstancesRequest modifyReservedInstancesRequest) {
        return null;
    }

    @Override
    public ModifySnapshotAttributeResult modifySnapshotAttribute(ModifySnapshotAttributeRequest modifySnapshotAttributeRequest) {
        return null;
    }

    @Override
    public ModifySpotFleetRequestResult modifySpotFleetRequest(ModifySpotFleetRequestRequest modifySpotFleetRequestRequest) {
        return null;
    }

    @Override
    public ModifySubnetAttributeResult modifySubnetAttribute(ModifySubnetAttributeRequest modifySubnetAttributeRequest) {
        return null;
    }

    @Override
    public ModifyVolumeResult modifyVolume(ModifyVolumeRequest modifyVolumeRequest) {
        return null;
    }

    @Override
    public ModifyVolumeAttributeResult modifyVolumeAttribute(ModifyVolumeAttributeRequest modifyVolumeAttributeRequest) {
        return null;
    }

    @Override
    public ModifyVpcAttributeResult modifyVpcAttribute(ModifyVpcAttributeRequest modifyVpcAttributeRequest) {
        return null;
    }

    @Override
    public ModifyVpcEndpointResult modifyVpcEndpoint(ModifyVpcEndpointRequest modifyVpcEndpointRequest) {
        return null;
    }

    @Override
    public ModifyVpcEndpointConnectionNotificationResult modifyVpcEndpointConnectionNotification(ModifyVpcEndpointConnectionNotificationRequest modifyVpcEndpointConnectionNotificationRequest) {
        return null;
    }

    @Override
    public ModifyVpcEndpointServiceConfigurationResult modifyVpcEndpointServiceConfiguration(ModifyVpcEndpointServiceConfigurationRequest modifyVpcEndpointServiceConfigurationRequest) {
        return null;
    }

    @Override
    public ModifyVpcEndpointServicePermissionsResult modifyVpcEndpointServicePermissions(ModifyVpcEndpointServicePermissionsRequest modifyVpcEndpointServicePermissionsRequest) {
        return null;
    }

    @Override
    public ModifyVpcPeeringConnectionOptionsResult modifyVpcPeeringConnectionOptions(ModifyVpcPeeringConnectionOptionsRequest modifyVpcPeeringConnectionOptionsRequest) {
        return null;
    }

    @Override
    public ModifyVpcTenancyResult modifyVpcTenancy(ModifyVpcTenancyRequest modifyVpcTenancyRequest) {
        return null;
    }

    @Override
    public MonitorInstancesResult monitorInstances(MonitorInstancesRequest monitorInstancesRequest) {
        return null;
    }

    @Override
    public MoveAddressToVpcResult moveAddressToVpc(MoveAddressToVpcRequest moveAddressToVpcRequest) {
        return null;
    }

    @Override
    public PurchaseHostReservationResult purchaseHostReservation(PurchaseHostReservationRequest purchaseHostReservationRequest) {
        return null;
    }

    @Override
    public PurchaseReservedInstancesOfferingResult purchaseReservedInstancesOffering(PurchaseReservedInstancesOfferingRequest purchaseReservedInstancesOfferingRequest) {
        return null;
    }

    @Override
    public PurchaseScheduledInstancesResult purchaseScheduledInstances(PurchaseScheduledInstancesRequest purchaseScheduledInstancesRequest) {
        return null;
    }

    @Override
    public RebootInstancesResult rebootInstances(RebootInstancesRequest rebootInstancesRequest) {
        return null;
    }

    @Override
    public RegisterImageResult registerImage(RegisterImageRequest registerImageRequest) {
        return null;
    }

    @Override
    public RejectVpcEndpointConnectionsResult rejectVpcEndpointConnections(RejectVpcEndpointConnectionsRequest rejectVpcEndpointConnectionsRequest) {
        return null;
    }

    @Override
    public RejectVpcPeeringConnectionResult rejectVpcPeeringConnection(RejectVpcPeeringConnectionRequest rejectVpcPeeringConnectionRequest) {
        return null;
    }

    @Override
    public ReleaseAddressResult releaseAddress(ReleaseAddressRequest releaseAddressRequest) {
        return null;
    }

    @Override
    public ReleaseHostsResult releaseHosts(ReleaseHostsRequest releaseHostsRequest) {
        return null;
    }

    @Override
    public ReplaceIamInstanceProfileAssociationResult replaceIamInstanceProfileAssociation(ReplaceIamInstanceProfileAssociationRequest replaceIamInstanceProfileAssociationRequest) {
        return null;
    }

    @Override
    public ReplaceNetworkAclAssociationResult replaceNetworkAclAssociation(ReplaceNetworkAclAssociationRequest replaceNetworkAclAssociationRequest) {
        return null;
    }

    @Override
    public ReplaceNetworkAclEntryResult replaceNetworkAclEntry(ReplaceNetworkAclEntryRequest replaceNetworkAclEntryRequest) {
        return null;
    }

    @Override
    public ReplaceRouteResult replaceRoute(ReplaceRouteRequest replaceRouteRequest) {
        return null;
    }

    @Override
    public ReplaceRouteTableAssociationResult replaceRouteTableAssociation(ReplaceRouteTableAssociationRequest replaceRouteTableAssociationRequest) {
        return null;
    }

    @Override
    public ReportInstanceStatusResult reportInstanceStatus(ReportInstanceStatusRequest reportInstanceStatusRequest) {
        return null;
    }

    @Override
    public RequestSpotFleetResult requestSpotFleet(RequestSpotFleetRequest requestSpotFleetRequest) {
        return null;
    }

    @Override
    public RequestSpotInstancesResult requestSpotInstances(RequestSpotInstancesRequest requestSpotInstancesRequest) {
        return null;
    }

    @Override
    public ResetFpgaImageAttributeResult resetFpgaImageAttribute(ResetFpgaImageAttributeRequest resetFpgaImageAttributeRequest) {
        return null;
    }

    @Override
    public ResetImageAttributeResult resetImageAttribute(ResetImageAttributeRequest resetImageAttributeRequest) {
        return null;
    }

    @Override
    public ResetInstanceAttributeResult resetInstanceAttribute(ResetInstanceAttributeRequest resetInstanceAttributeRequest) {
        return null;
    }

    @Override
    public ResetNetworkInterfaceAttributeResult resetNetworkInterfaceAttribute(ResetNetworkInterfaceAttributeRequest resetNetworkInterfaceAttributeRequest) {
        return null;
    }

    @Override
    public ResetSnapshotAttributeResult resetSnapshotAttribute(ResetSnapshotAttributeRequest resetSnapshotAttributeRequest) {
        return null;
    }

    @Override
    public RestoreAddressToClassicResult restoreAddressToClassic(RestoreAddressToClassicRequest restoreAddressToClassicRequest) {
        return null;
    }

    @Override
    public RevokeSecurityGroupEgressResult revokeSecurityGroupEgress(RevokeSecurityGroupEgressRequest revokeSecurityGroupEgressRequest) {
        return null;
    }

    @Override
    public RevokeSecurityGroupIngressResult revokeSecurityGroupIngress(RevokeSecurityGroupIngressRequest revokeSecurityGroupIngressRequest) {
        return null;
    }

    @Override
    public RevokeSecurityGroupIngressResult revokeSecurityGroupIngress() {
        return null;
    }

    @Override
    public RunInstancesResult runInstances(RunInstancesRequest runInstancesRequest) {
        return null;
    }

    @Override
    public RunScheduledInstancesResult runScheduledInstances(RunScheduledInstancesRequest runScheduledInstancesRequest) {
        return null;
    }

    @Override
    public StartInstancesResult startInstances(StartInstancesRequest startInstancesRequest) {
        return null;
    }

    @Override
    public StopInstancesResult stopInstances(StopInstancesRequest stopInstancesRequest) {
        return null;
    }

    @Override
    public TerminateInstancesResult terminateInstances(TerminateInstancesRequest terminateInstancesRequest) {
        return null;
    }

    @Override
    public UnassignIpv6AddressesResult unassignIpv6Addresses(UnassignIpv6AddressesRequest unassignIpv6AddressesRequest) {
        return null;
    }

    @Override
    public UnassignPrivateIpAddressesResult unassignPrivateIpAddresses(UnassignPrivateIpAddressesRequest unassignPrivateIpAddressesRequest) {
        return null;
    }

    @Override
    public UnmonitorInstancesResult unmonitorInstances(UnmonitorInstancesRequest unmonitorInstancesRequest) {
        return null;
    }

    @Override
    public UpdateSecurityGroupRuleDescriptionsEgressResult updateSecurityGroupRuleDescriptionsEgress(UpdateSecurityGroupRuleDescriptionsEgressRequest updateSecurityGroupRuleDescriptionsEgressRequest) {
        return null;
    }

    @Override
    public UpdateSecurityGroupRuleDescriptionsIngressResult updateSecurityGroupRuleDescriptionsIngress(UpdateSecurityGroupRuleDescriptionsIngressRequest updateSecurityGroupRuleDescriptionsIngressRequest) {
        return null;
    }

    @Override
    public <X extends AmazonWebServiceRequest> DryRunResult<X> dryRun(DryRunSupportedRequest<X> request) throws AmazonServiceException, AmazonClientException {
        return null;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request) {
        return null;
    }

    @Override
    public AmazonEC2Waiters waiters() {
        return null;
    }
}
