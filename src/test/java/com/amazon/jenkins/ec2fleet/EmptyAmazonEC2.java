package com.amazon.jenkins.ec2fleet;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ec2.waiters.Ec2Waiter;

import java.util.function.Consumer;

public class EmptyAmazonEC2 implements Ec2Client {

    @Override
    public AcceptAddressTransferResponse acceptAddressTransfer(AcceptAddressTransferRequest acceptAddressTransferRequest) {
        return null;
    }

    @Override
    public AcceptReservedInstancesExchangeQuoteResponse acceptReservedInstancesExchangeQuote(AcceptReservedInstancesExchangeQuoteRequest acceptReservedInstancesExchangeQuoteRequest) {
        return null;
    }

    @Override
    public AcceptTransitGatewayMulticastDomainAssociationsResponse acceptTransitGatewayMulticastDomainAssociations(AcceptTransitGatewayMulticastDomainAssociationsRequest acceptTransitGatewayMulticastDomainAssociationsRequest) {
        return null;
    }

    @Override
    public AcceptTransitGatewayPeeringAttachmentResponse acceptTransitGatewayPeeringAttachment(AcceptTransitGatewayPeeringAttachmentRequest acceptTransitGatewayPeeringAttachmentRequest) {
        return null;
    }

    @Override
    public AcceptTransitGatewayVpcAttachmentResponse acceptTransitGatewayVpcAttachment(AcceptTransitGatewayVpcAttachmentRequest acceptTransitGatewayVpcAttachmentRequest) {
        return null;
    }

    @Override
    public AcceptVpcEndpointConnectionsResponse acceptVpcEndpointConnections(AcceptVpcEndpointConnectionsRequest acceptVpcEndpointConnectionsRequest) {
        return null;
    }

    @Override
    public AcceptVpcPeeringConnectionResponse acceptVpcPeeringConnection(AcceptVpcPeeringConnectionRequest acceptVpcPeeringConnectionRequest) {
        return null;
    }

    @Override
    public AcceptVpcPeeringConnectionResponse acceptVpcPeeringConnection(Consumer<AcceptVpcPeeringConnectionRequest.Builder> acceptVpcPeeringConnectionRequest) {
        return null;
    }

    @Override
    public AdvertiseByoipCidrResponse advertiseByoipCidr(AdvertiseByoipCidrRequest advertiseByoipCidrRequest) {
        return null;
    }

    @Override
    public AllocateAddressResponse allocateAddress(AllocateAddressRequest allocateAddressRequest) {
        return null;
    }

    @Override
    public AllocateAddressResponse allocateAddress() {
        return null;
    }

    @Override
    public AllocateHostsResponse allocateHosts(AllocateHostsRequest allocateHostsRequest) {
        return null;
    }

    @Override
    public AllocateIpamPoolCidrResponse allocateIpamPoolCidr(AllocateIpamPoolCidrRequest allocateIpamPoolCidrRequest) {
        return null;
    }

    @Override
    public ApplySecurityGroupsToClientVpnTargetNetworkResponse applySecurityGroupsToClientVpnTargetNetwork(ApplySecurityGroupsToClientVpnTargetNetworkRequest applySecurityGroupsToClientVpnTargetNetworkRequest) {
        return null;
    }

    @Override
    public AssignIpv6AddressesResponse assignIpv6Addresses(AssignIpv6AddressesRequest assignIpv6AddressesRequest) {
        return null;
    }

    @Override
    public AssignPrivateIpAddressesResponse assignPrivateIpAddresses(AssignPrivateIpAddressesRequest assignPrivateIpAddressesRequest) {
        return null;
    }

    @Override
    public AssignPrivateNatGatewayAddressResponse assignPrivateNatGatewayAddress(AssignPrivateNatGatewayAddressRequest assignPrivateNatGatewayAddressRequest) {
        return null;
    }

    @Override
    public AssociateAddressResponse associateAddress(AssociateAddressRequest associateAddressRequest) {
        return null;
    }

    @Override
    public AssociateClientVpnTargetNetworkResponse associateClientVpnTargetNetwork(AssociateClientVpnTargetNetworkRequest associateClientVpnTargetNetworkRequest) {
        return null;
    }

    @Override
    public AssociateDhcpOptionsResponse associateDhcpOptions(AssociateDhcpOptionsRequest associateDhcpOptionsRequest) {
        return null;
    }

    @Override
    public AssociateEnclaveCertificateIamRoleResponse associateEnclaveCertificateIamRole(AssociateEnclaveCertificateIamRoleRequest associateEnclaveCertificateIamRoleRequest) {
        return null;
    }

    @Override
    public AssociateIamInstanceProfileResponse associateIamInstanceProfile(AssociateIamInstanceProfileRequest associateIamInstanceProfileRequest) {
        return null;
    }

    @Override
    public AssociateInstanceEventWindowResponse associateInstanceEventWindow(AssociateInstanceEventWindowRequest associateInstanceEventWindowRequest) {
        return null;
    }

    @Override
    public AssociateIpamByoasnResponse associateIpamByoasn(AssociateIpamByoasnRequest associateIpamByoasnRequest) {
        return null;
    }

    @Override
    public AssociateIpamResourceDiscoveryResponse associateIpamResourceDiscovery(AssociateIpamResourceDiscoveryRequest associateIpamResourceDiscoveryRequest) {
        return null;
    }

    @Override
    public AssociateNatGatewayAddressResponse associateNatGatewayAddress(AssociateNatGatewayAddressRequest associateNatGatewayAddressRequest) {
        return null;
    }

    @Override
    public AssociateRouteTableResponse associateRouteTable(AssociateRouteTableRequest associateRouteTableRequest) {
        return null;
    }

    @Override
    public AssociateSubnetCidrBlockResponse associateSubnetCidrBlock(AssociateSubnetCidrBlockRequest associateSubnetCidrBlockRequest) {
        return null;
    }

    @Override
    public AssociateTransitGatewayMulticastDomainResponse associateTransitGatewayMulticastDomain(AssociateTransitGatewayMulticastDomainRequest associateTransitGatewayMulticastDomainRequest) {
        return null;
    }

    @Override
    public AssociateTransitGatewayPolicyTableResponse associateTransitGatewayPolicyTable(AssociateTransitGatewayPolicyTableRequest associateTransitGatewayPolicyTableRequest) {
        return null;
    }

    @Override
    public AssociateTransitGatewayRouteTableResponse associateTransitGatewayRouteTable(AssociateTransitGatewayRouteTableRequest associateTransitGatewayRouteTableRequest) {
        return null;
    }

    @Override
    public AssociateTrunkInterfaceResponse associateTrunkInterface(AssociateTrunkInterfaceRequest associateTrunkInterfaceRequest) {
        return null;
    }

    @Override
    public AssociateVpcCidrBlockResponse associateVpcCidrBlock(AssociateVpcCidrBlockRequest associateVpcCidrBlockRequest) {
        return null;
    }

    @Override
    public AttachClassicLinkVpcResponse attachClassicLinkVpc(AttachClassicLinkVpcRequest attachClassicLinkVpcRequest) {
        return null;
    }

    @Override
    public AttachInternetGatewayResponse attachInternetGateway(AttachInternetGatewayRequest attachInternetGatewayRequest) {
        return null;
    }

    @Override
    public AttachNetworkInterfaceResponse attachNetworkInterface(AttachNetworkInterfaceRequest attachNetworkInterfaceRequest) {
        return null;
    }

    @Override
    public AttachVerifiedAccessTrustProviderResponse attachVerifiedAccessTrustProvider(AttachVerifiedAccessTrustProviderRequest attachVerifiedAccessTrustProviderRequest) {
        return null;
    }

    @Override
    public AttachVolumeResponse attachVolume(AttachVolumeRequest attachVolumeRequest) {
        return null;
    }

    @Override
    public AttachVpnGatewayResponse attachVpnGateway(AttachVpnGatewayRequest attachVpnGatewayRequest) {
        return null;
    }

    @Override
    public AuthorizeClientVpnIngressResponse authorizeClientVpnIngress(AuthorizeClientVpnIngressRequest authorizeClientVpnIngressRequest) {
        return null;
    }

    @Override
    public AuthorizeSecurityGroupEgressResponse authorizeSecurityGroupEgress(AuthorizeSecurityGroupEgressRequest authorizeSecurityGroupEgressRequest) {
        return null;
    }

    @Override
    public AuthorizeSecurityGroupIngressResponse authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest) {
        return null;
    }

    @Override
    public BundleInstanceResponse bundleInstance(BundleInstanceRequest bundleInstanceRequest) {
        return null;
    }

    @Override
    public CancelBundleTaskResponse cancelBundleTask(CancelBundleTaskRequest cancelBundleTaskRequest) {
        return null;
    }

    @Override
    public CancelCapacityReservationResponse cancelCapacityReservation(CancelCapacityReservationRequest cancelCapacityReservationRequest) {
        return null;
    }

    @Override
    public CancelCapacityReservationFleetsResponse cancelCapacityReservationFleets(CancelCapacityReservationFleetsRequest cancelCapacityReservationFleetsRequest) {
        return null;
    }

    @Override
    public CancelConversionTaskResponse cancelConversionTask(CancelConversionTaskRequest cancelConversionTaskRequest) {
        return null;
    }

    @Override
    public CancelExportTaskResponse cancelExportTask(CancelExportTaskRequest cancelExportTaskRequest) {
        return null;
    }

    @Override
    public CancelImageLaunchPermissionResponse cancelImageLaunchPermission(CancelImageLaunchPermissionRequest cancelImageLaunchPermissionRequest) {
        return null;
    }

    @Override
    public CancelImportTaskResponse cancelImportTask(CancelImportTaskRequest cancelImportTaskRequest) {
        return null;
    }

    @Override
    public CancelImportTaskResponse cancelImportTask(Consumer<CancelImportTaskRequest.Builder> cancelImportTaskRequest) {
        return null;
    }

    @Override
    public CancelReservedInstancesListingResponse cancelReservedInstancesListing(CancelReservedInstancesListingRequest cancelReservedInstancesListingRequest) {
        return null;
    }

    @Override
    public CancelSpotFleetRequestsResponse cancelSpotFleetRequests(CancelSpotFleetRequestsRequest cancelSpotFleetRequestsRequest) {
        return null;
    }

    @Override
    public CancelSpotInstanceRequestsResponse cancelSpotInstanceRequests(CancelSpotInstanceRequestsRequest cancelSpotInstanceRequestsRequest) {
        return null;
    }

    @Override
    public ConfirmProductInstanceResponse confirmProductInstance(ConfirmProductInstanceRequest confirmProductInstanceRequest) {
        return null;
    }

    @Override
    public CopyFpgaImageResponse copyFpgaImage(CopyFpgaImageRequest copyFpgaImageRequest) {
        return null;
    }

    @Override
    public CopyImageResponse copyImage(CopyImageRequest copyImageRequest) {
        return null;
    }

    @Override
    public CopySnapshotResponse copySnapshot(CopySnapshotRequest copySnapshotRequest) {
        return null;
    }

    @Override
    public CreateCapacityReservationResponse createCapacityReservation(CreateCapacityReservationRequest createCapacityReservationRequest) {
        return null;
    }

    @Override
    public CreateCapacityReservationFleetResponse createCapacityReservationFleet(CreateCapacityReservationFleetRequest createCapacityReservationFleetRequest) {
        return null;
    }

    @Override
    public CreateCarrierGatewayResponse createCarrierGateway(CreateCarrierGatewayRequest createCarrierGatewayRequest) {
        return null;
    }

    @Override
    public CreateClientVpnEndpointResponse createClientVpnEndpoint(CreateClientVpnEndpointRequest createClientVpnEndpointRequest) {
        return null;
    }

    @Override
    public CreateClientVpnRouteResponse createClientVpnRoute(CreateClientVpnRouteRequest createClientVpnRouteRequest) {
        return null;
    }

    @Override
    public CreateCoipCidrResponse createCoipCidr(CreateCoipCidrRequest createCoipCidrRequest) {
        return null;
    }

    @Override
    public CreateCoipPoolResponse createCoipPool(CreateCoipPoolRequest createCoipPoolRequest) {
        return null;
    }

    @Override
    public CreateCustomerGatewayResponse createCustomerGateway(CreateCustomerGatewayRequest createCustomerGatewayRequest) {
        return null;
    }

    @Override
    public CreateDefaultSubnetResponse createDefaultSubnet(CreateDefaultSubnetRequest createDefaultSubnetRequest) {
        return null;
    }

    @Override
    public CreateDefaultVpcResponse createDefaultVpc(CreateDefaultVpcRequest createDefaultVpcRequest) {
        return null;
    }

    @Override
    public CreateDhcpOptionsResponse createDhcpOptions(CreateDhcpOptionsRequest createDhcpOptionsRequest) {
        return null;
    }

    @Override
    public CreateEgressOnlyInternetGatewayResponse createEgressOnlyInternetGateway(CreateEgressOnlyInternetGatewayRequest createEgressOnlyInternetGatewayRequest) {
        return null;
    }

    @Override
    public CreateFleetResponse createFleet(CreateFleetRequest createFleetRequest) {
        return null;
    }

    @Override
    public CreateFlowLogsResponse createFlowLogs(CreateFlowLogsRequest createFlowLogsRequest) {
        return null;
    }

    @Override
    public CreateFpgaImageResponse createFpgaImage(CreateFpgaImageRequest createFpgaImageRequest) {
        return null;
    }

    @Override
    public CreateImageResponse createImage(CreateImageRequest createImageRequest) {
        return null;
    }

    @Override
    public CreateInstanceConnectEndpointResponse createInstanceConnectEndpoint(CreateInstanceConnectEndpointRequest createInstanceConnectEndpointRequest) {
        return null;
    }

    @Override
    public CreateInstanceEventWindowResponse createInstanceEventWindow(CreateInstanceEventWindowRequest createInstanceEventWindowRequest) {
        return null;
    }

    @Override
    public CreateInstanceExportTaskResponse createInstanceExportTask(CreateInstanceExportTaskRequest createInstanceExportTaskRequest) {
        return null;
    }

    @Override
    public CreateInternetGatewayResponse createInternetGateway(CreateInternetGatewayRequest createInternetGatewayRequest) {
        return null;
    }

    @Override
    public CreateInternetGatewayResponse createInternetGateway() {
        return null;
    }

    @Override
    public CreateIpamResponse createIpam(CreateIpamRequest createIpamRequest) {
        return null;
    }

    @Override
    public CreateIpamExternalResourceVerificationTokenResponse createIpamExternalResourceVerificationToken(CreateIpamExternalResourceVerificationTokenRequest createIpamExternalResourceVerificationTokenRequest) {
        return null;
    }

    @Override
    public CreateIpamPoolResponse createIpamPool(CreateIpamPoolRequest createIpamPoolRequest) {
        return null;
    }

    @Override
    public CreateIpamResourceDiscoveryResponse createIpamResourceDiscovery(CreateIpamResourceDiscoveryRequest createIpamResourceDiscoveryRequest) {
        return null;
    }

    @Override
    public CreateIpamScopeResponse createIpamScope(CreateIpamScopeRequest createIpamScopeRequest) {
        return null;
    }

    @Override
    public CreateKeyPairResponse createKeyPair(CreateKeyPairRequest createKeyPairRequest) {
        return null;
    }

    @Override
    public CreateLaunchTemplateResponse createLaunchTemplate(CreateLaunchTemplateRequest createLaunchTemplateRequest) {
        return null;
    }

    @Override
    public CreateLaunchTemplateVersionResponse createLaunchTemplateVersion(CreateLaunchTemplateVersionRequest createLaunchTemplateVersionRequest) {
        return null;
    }

    @Override
    public CreateLocalGatewayRouteResponse createLocalGatewayRoute(CreateLocalGatewayRouteRequest createLocalGatewayRouteRequest) {
        return null;
    }

    @Override
    public CreateLocalGatewayRouteTableResponse createLocalGatewayRouteTable(CreateLocalGatewayRouteTableRequest createLocalGatewayRouteTableRequest) {
        return null;
    }

    @Override
    public CreateLocalGatewayRouteTableVirtualInterfaceGroupAssociationResponse createLocalGatewayRouteTableVirtualInterfaceGroupAssociation(CreateLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest createLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest) {
        return null;
    }

    @Override
    public CreateLocalGatewayRouteTableVpcAssociationResponse createLocalGatewayRouteTableVpcAssociation(CreateLocalGatewayRouteTableVpcAssociationRequest createLocalGatewayRouteTableVpcAssociationRequest) {
        return null;
    }

    @Override
    public CreateManagedPrefixListResponse createManagedPrefixList(CreateManagedPrefixListRequest createManagedPrefixListRequest) {
        return null;
    }

    @Override
    public CreateNatGatewayResponse createNatGateway(CreateNatGatewayRequest createNatGatewayRequest) {
        return null;
    }

    @Override
    public CreateNetworkAclResponse createNetworkAcl(CreateNetworkAclRequest createNetworkAclRequest) {
        return null;
    }

    @Override
    public CreateNetworkAclEntryResponse createNetworkAclEntry(CreateNetworkAclEntryRequest createNetworkAclEntryRequest) {
        return null;
    }

    @Override
    public CreateNetworkInsightsAccessScopeResponse createNetworkInsightsAccessScope(CreateNetworkInsightsAccessScopeRequest createNetworkInsightsAccessScopeRequest) {
        return null;
    }

    @Override
    public CreateNetworkInsightsPathResponse createNetworkInsightsPath(CreateNetworkInsightsPathRequest createNetworkInsightsPathRequest) {
        return null;
    }

    @Override
    public CreateNetworkInterfaceResponse createNetworkInterface(CreateNetworkInterfaceRequest createNetworkInterfaceRequest) {
        return null;
    }

    @Override
    public CreateNetworkInterfacePermissionResponse createNetworkInterfacePermission(CreateNetworkInterfacePermissionRequest createNetworkInterfacePermissionRequest) {
        return null;
    }

    @Override
    public CreatePlacementGroupResponse createPlacementGroup(CreatePlacementGroupRequest createPlacementGroupRequest) {
        return null;
    }

    @Override
    public CreatePublicIpv4PoolResponse createPublicIpv4Pool(CreatePublicIpv4PoolRequest createPublicIpv4PoolRequest) {
        return null;
    }

    @Override
    public CreateReplaceRootVolumeTaskResponse createReplaceRootVolumeTask(CreateReplaceRootVolumeTaskRequest createReplaceRootVolumeTaskRequest) {
        return null;
    }

    @Override
    public CreateReservedInstancesListingResponse createReservedInstancesListing(CreateReservedInstancesListingRequest createReservedInstancesListingRequest) {
        return null;
    }

    @Override
    public CreateRestoreImageTaskResponse createRestoreImageTask(CreateRestoreImageTaskRequest createRestoreImageTaskRequest) {
        return null;
    }

    @Override
    public CreateRouteResponse createRoute(CreateRouteRequest createRouteRequest) {
        return null;
    }

    @Override
    public CreateRouteTableResponse createRouteTable(CreateRouteTableRequest createRouteTableRequest) {
        return null;
    }

    @Override
    public CreateSecurityGroupResponse createSecurityGroup(CreateSecurityGroupRequest createSecurityGroupRequest) {
        return null;
    }

    @Override
    public CreateSnapshotResponse createSnapshot(CreateSnapshotRequest createSnapshotRequest) {
        return null;
    }

    @Override
    public CreateSnapshotsResponse createSnapshots(CreateSnapshotsRequest createSnapshotsRequest) {
        return null;
    }

    @Override
    public CreateSpotDatafeedSubscriptionResponse createSpotDatafeedSubscription(CreateSpotDatafeedSubscriptionRequest createSpotDatafeedSubscriptionRequest) {
        return null;
    }

    @Override
    public CreateStoreImageTaskResponse createStoreImageTask(CreateStoreImageTaskRequest createStoreImageTaskRequest) {
        return null;
    }

    @Override
    public CreateSubnetResponse createSubnet(CreateSubnetRequest createSubnetRequest) {
        return null;
    }

    @Override
    public CreateSubnetCidrReservationResponse createSubnetCidrReservation(CreateSubnetCidrReservationRequest createSubnetCidrReservationRequest) {
        return null;
    }

    @Override
    public CreateTagsResponse createTags(CreateTagsRequest createTagsRequest) {
        return null;
    }

    @Override
    public CreateTrafficMirrorFilterResponse createTrafficMirrorFilter(CreateTrafficMirrorFilterRequest createTrafficMirrorFilterRequest) {
        return null;
    }

    @Override
    public CreateTrafficMirrorFilterRuleResponse createTrafficMirrorFilterRule(CreateTrafficMirrorFilterRuleRequest createTrafficMirrorFilterRuleRequest) {
        return null;
    }

    @Override
    public CreateTrafficMirrorSessionResponse createTrafficMirrorSession(CreateTrafficMirrorSessionRequest createTrafficMirrorSessionRequest) {
        return null;
    }

    @Override
    public CreateTrafficMirrorTargetResponse createTrafficMirrorTarget(CreateTrafficMirrorTargetRequest createTrafficMirrorTargetRequest) {
        return null;
    }

    @Override
    public CreateTransitGatewayResponse createTransitGateway(CreateTransitGatewayRequest createTransitGatewayRequest) {
        return null;
    }

    @Override
    public CreateTransitGatewayConnectResponse createTransitGatewayConnect(CreateTransitGatewayConnectRequest createTransitGatewayConnectRequest) {
        return null;
    }

    @Override
    public CreateTransitGatewayConnectPeerResponse createTransitGatewayConnectPeer(CreateTransitGatewayConnectPeerRequest createTransitGatewayConnectPeerRequest) {
        return null;
    }

    @Override
    public CreateTransitGatewayMulticastDomainResponse createTransitGatewayMulticastDomain(CreateTransitGatewayMulticastDomainRequest createTransitGatewayMulticastDomainRequest) {
        return null;
    }

    @Override
    public CreateTransitGatewayPeeringAttachmentResponse createTransitGatewayPeeringAttachment(CreateTransitGatewayPeeringAttachmentRequest createTransitGatewayPeeringAttachmentRequest) {
        return null;
    }

    @Override
    public CreateTransitGatewayPolicyTableResponse createTransitGatewayPolicyTable(CreateTransitGatewayPolicyTableRequest createTransitGatewayPolicyTableRequest) {
        return null;
    }

    @Override
    public CreateTransitGatewayPrefixListReferenceResponse createTransitGatewayPrefixListReference(CreateTransitGatewayPrefixListReferenceRequest createTransitGatewayPrefixListReferenceRequest) {
        return null;
    }

    @Override
    public CreateTransitGatewayRouteResponse createTransitGatewayRoute(CreateTransitGatewayRouteRequest createTransitGatewayRouteRequest) {
        return null;
    }

    @Override
    public CreateTransitGatewayRouteTableResponse createTransitGatewayRouteTable(CreateTransitGatewayRouteTableRequest createTransitGatewayRouteTableRequest) {
        return null;
    }

    @Override
    public CreateTransitGatewayRouteTableAnnouncementResponse createTransitGatewayRouteTableAnnouncement(CreateTransitGatewayRouteTableAnnouncementRequest createTransitGatewayRouteTableAnnouncementRequest) {
        return null;
    }

    @Override
    public CreateTransitGatewayVpcAttachmentResponse createTransitGatewayVpcAttachment(CreateTransitGatewayVpcAttachmentRequest createTransitGatewayVpcAttachmentRequest) {
        return null;
    }

    @Override
    public CreateVerifiedAccessEndpointResponse createVerifiedAccessEndpoint(CreateVerifiedAccessEndpointRequest createVerifiedAccessEndpointRequest) {
        return null;
    }

    @Override
    public CreateVerifiedAccessGroupResponse createVerifiedAccessGroup(CreateVerifiedAccessGroupRequest createVerifiedAccessGroupRequest) {
        return null;
    }

    @Override
    public CreateVerifiedAccessInstanceResponse createVerifiedAccessInstance(CreateVerifiedAccessInstanceRequest createVerifiedAccessInstanceRequest) {
        return null;
    }

    @Override
    public CreateVerifiedAccessTrustProviderResponse createVerifiedAccessTrustProvider(CreateVerifiedAccessTrustProviderRequest createVerifiedAccessTrustProviderRequest) {
        return null;
    }

    @Override
    public CreateVolumeResponse createVolume(CreateVolumeRequest createVolumeRequest) {
        return null;
    }

    @Override
    public CreateVpcResponse createVpc(CreateVpcRequest createVpcRequest) {
        return null;
    }

    @Override
    public CreateVpcEndpointResponse createVpcEndpoint(CreateVpcEndpointRequest createVpcEndpointRequest) {
        return null;
    }

    @Override
    public CreateVpcEndpointConnectionNotificationResponse createVpcEndpointConnectionNotification(CreateVpcEndpointConnectionNotificationRequest createVpcEndpointConnectionNotificationRequest) {
        return null;
    }

    @Override
    public CreateVpcEndpointServiceConfigurationResponse createVpcEndpointServiceConfiguration(CreateVpcEndpointServiceConfigurationRequest createVpcEndpointServiceConfigurationRequest) {
        return null;
    }

    @Override
    public CreateVpcPeeringConnectionResponse createVpcPeeringConnection(CreateVpcPeeringConnectionRequest createVpcPeeringConnectionRequest) {
        return null;
    }

    @Override
    public CreateVpcPeeringConnectionResponse createVpcPeeringConnection(Consumer<CreateVpcPeeringConnectionRequest.Builder> createVpcPeeringConnectionRequest) {
        return null;
    }

    @Override
    public CreateVpnConnectionResponse createVpnConnection(CreateVpnConnectionRequest createVpnConnectionRequest) {
        return null;
    }

    @Override
    public CreateVpnConnectionRouteResponse createVpnConnectionRoute(CreateVpnConnectionRouteRequest createVpnConnectionRouteRequest) {
        return null;
    }

    @Override
    public CreateVpnGatewayResponse createVpnGateway(CreateVpnGatewayRequest createVpnGatewayRequest) {
        return null;
    }

    @Override
    public DeleteCarrierGatewayResponse deleteCarrierGateway(DeleteCarrierGatewayRequest deleteCarrierGatewayRequest) {
        return null;
    }

    @Override
    public DeleteClientVpnEndpointResponse deleteClientVpnEndpoint(DeleteClientVpnEndpointRequest deleteClientVpnEndpointRequest) {
        return null;
    }

    @Override
    public DeleteClientVpnRouteResponse deleteClientVpnRoute(DeleteClientVpnRouteRequest deleteClientVpnRouteRequest) {
        return null;
    }

    @Override
    public DeleteCoipCidrResponse deleteCoipCidr(DeleteCoipCidrRequest deleteCoipCidrRequest) {
        return null;
    }

    @Override
    public DeleteCoipPoolResponse deleteCoipPool(DeleteCoipPoolRequest deleteCoipPoolRequest) {
        return null;
    }

    @Override
    public DeleteCustomerGatewayResponse deleteCustomerGateway(DeleteCustomerGatewayRequest deleteCustomerGatewayRequest) {
        return null;
    }

    @Override
    public DeleteDhcpOptionsResponse deleteDhcpOptions(DeleteDhcpOptionsRequest deleteDhcpOptionsRequest) {
        return null;
    }

    @Override
    public DeleteEgressOnlyInternetGatewayResponse deleteEgressOnlyInternetGateway(DeleteEgressOnlyInternetGatewayRequest deleteEgressOnlyInternetGatewayRequest) {
        return null;
    }

    @Override
    public DeleteFleetsResponse deleteFleets(DeleteFleetsRequest deleteFleetsRequest) {
        return null;
    }

    @Override
    public DeleteFlowLogsResponse deleteFlowLogs(DeleteFlowLogsRequest deleteFlowLogsRequest) {
        return null;
    }

    @Override
    public DeleteFpgaImageResponse deleteFpgaImage(DeleteFpgaImageRequest deleteFpgaImageRequest) {
        return null;
    }

    @Override
    public DeleteInstanceConnectEndpointResponse deleteInstanceConnectEndpoint(DeleteInstanceConnectEndpointRequest deleteInstanceConnectEndpointRequest) {
        return null;
    }

    @Override
    public DeleteInstanceEventWindowResponse deleteInstanceEventWindow(DeleteInstanceEventWindowRequest deleteInstanceEventWindowRequest) {
        return null;
    }

    @Override
    public DeleteInternetGatewayResponse deleteInternetGateway(DeleteInternetGatewayRequest deleteInternetGatewayRequest) {
        return null;
    }

    @Override
    public DeleteIpamResponse deleteIpam(DeleteIpamRequest deleteIpamRequest) {
        return null;
    }

    @Override
    public DeleteIpamExternalResourceVerificationTokenResponse deleteIpamExternalResourceVerificationToken(DeleteIpamExternalResourceVerificationTokenRequest deleteIpamExternalResourceVerificationTokenRequest) {
        return null;
    }

    @Override
    public DeleteIpamPoolResponse deleteIpamPool(DeleteIpamPoolRequest deleteIpamPoolRequest) {
        return null;
    }

    @Override
    public DeleteIpamResourceDiscoveryResponse deleteIpamResourceDiscovery(DeleteIpamResourceDiscoveryRequest deleteIpamResourceDiscoveryRequest) {
        return null;
    }

    @Override
    public DeleteIpamScopeResponse deleteIpamScope(DeleteIpamScopeRequest deleteIpamScopeRequest) {
        return null;
    }

    @Override
    public DeleteKeyPairResponse deleteKeyPair(DeleteKeyPairRequest deleteKeyPairRequest) {
        return null;
    }

    @Override
    public DeleteLaunchTemplateResponse deleteLaunchTemplate(DeleteLaunchTemplateRequest deleteLaunchTemplateRequest) {
        return null;
    }

    @Override
    public DeleteLaunchTemplateVersionsResponse deleteLaunchTemplateVersions(DeleteLaunchTemplateVersionsRequest deleteLaunchTemplateVersionsRequest) {
        return null;
    }

    @Override
    public DeleteLocalGatewayRouteResponse deleteLocalGatewayRoute(DeleteLocalGatewayRouteRequest deleteLocalGatewayRouteRequest) {
        return null;
    }

    @Override
    public DeleteLocalGatewayRouteTableResponse deleteLocalGatewayRouteTable(DeleteLocalGatewayRouteTableRequest deleteLocalGatewayRouteTableRequest) {
        return null;
    }

    @Override
    public DeleteLocalGatewayRouteTableVirtualInterfaceGroupAssociationResponse deleteLocalGatewayRouteTableVirtualInterfaceGroupAssociation(DeleteLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest deleteLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest) {
        return null;
    }

    @Override
    public DeleteLocalGatewayRouteTableVpcAssociationResponse deleteLocalGatewayRouteTableVpcAssociation(DeleteLocalGatewayRouteTableVpcAssociationRequest deleteLocalGatewayRouteTableVpcAssociationRequest) {
        return null;
    }

    @Override
    public DeleteManagedPrefixListResponse deleteManagedPrefixList(DeleteManagedPrefixListRequest deleteManagedPrefixListRequest) {
        return null;
    }

    @Override
    public DeleteNatGatewayResponse deleteNatGateway(DeleteNatGatewayRequest deleteNatGatewayRequest) {
        return null;
    }

    @Override
    public DeleteNetworkAclResponse deleteNetworkAcl(DeleteNetworkAclRequest deleteNetworkAclRequest) {
        return null;
    }

    @Override
    public DeleteNetworkAclEntryResponse deleteNetworkAclEntry(DeleteNetworkAclEntryRequest deleteNetworkAclEntryRequest) {
        return null;
    }

    @Override
    public DeleteNetworkInsightsAccessScopeResponse deleteNetworkInsightsAccessScope(DeleteNetworkInsightsAccessScopeRequest deleteNetworkInsightsAccessScopeRequest) {
        return null;
    }

    @Override
    public DeleteNetworkInsightsAccessScopeAnalysisResponse deleteNetworkInsightsAccessScopeAnalysis(DeleteNetworkInsightsAccessScopeAnalysisRequest deleteNetworkInsightsAccessScopeAnalysisRequest) {
        return null;
    }

    @Override
    public DeleteNetworkInsightsAnalysisResponse deleteNetworkInsightsAnalysis(DeleteNetworkInsightsAnalysisRequest deleteNetworkInsightsAnalysisRequest) {
        return null;
    }

    @Override
    public DeleteNetworkInsightsPathResponse deleteNetworkInsightsPath(DeleteNetworkInsightsPathRequest deleteNetworkInsightsPathRequest) {
        return null;
    }

    @Override
    public DeleteNetworkInterfaceResponse deleteNetworkInterface(DeleteNetworkInterfaceRequest deleteNetworkInterfaceRequest) {
        return null;
    }

    @Override
    public DeleteNetworkInterfacePermissionResponse deleteNetworkInterfacePermission(DeleteNetworkInterfacePermissionRequest deleteNetworkInterfacePermissionRequest) {
        return null;
    }

    @Override
    public DeletePlacementGroupResponse deletePlacementGroup(DeletePlacementGroupRequest deletePlacementGroupRequest) {
        return null;
    }

    @Override
    public DeletePublicIpv4PoolResponse deletePublicIpv4Pool(DeletePublicIpv4PoolRequest deletePublicIpv4PoolRequest) {
        return null;
    }

    @Override
    public DeleteQueuedReservedInstancesResponse deleteQueuedReservedInstances(DeleteQueuedReservedInstancesRequest deleteQueuedReservedInstancesRequest) {
        return null;
    }

    @Override
    public DeleteRouteResponse deleteRoute(DeleteRouteRequest deleteRouteRequest) {
        return null;
    }

    @Override
    public DeleteRouteTableResponse deleteRouteTable(DeleteRouteTableRequest deleteRouteTableRequest) {
        return null;
    }

    @Override
    public DeleteSecurityGroupResponse deleteSecurityGroup(DeleteSecurityGroupRequest deleteSecurityGroupRequest) {
        return null;
    }

    @Override
    public DeleteSnapshotResponse deleteSnapshot(DeleteSnapshotRequest deleteSnapshotRequest) {
        return null;
    }

    @Override
    public DeleteSpotDatafeedSubscriptionResponse deleteSpotDatafeedSubscription(DeleteSpotDatafeedSubscriptionRequest deleteSpotDatafeedSubscriptionRequest) {
        return null;
    }

    @Override
    public DeleteSpotDatafeedSubscriptionResponse deleteSpotDatafeedSubscription() {
        return null;
    }

    @Override
    public DeleteSubnetResponse deleteSubnet(DeleteSubnetRequest deleteSubnetRequest) {
        return null;
    }

    @Override
    public DeleteSubnetCidrReservationResponse deleteSubnetCidrReservation(DeleteSubnetCidrReservationRequest deleteSubnetCidrReservationRequest) {
        return null;
    }

    @Override
    public DeleteTagsResponse deleteTags(DeleteTagsRequest deleteTagsRequest) {
        return null;
    }

    @Override
    public DeleteTrafficMirrorFilterResponse deleteTrafficMirrorFilter(DeleteTrafficMirrorFilterRequest deleteTrafficMirrorFilterRequest) {
        return null;
    }

    @Override
    public DeleteTrafficMirrorFilterRuleResponse deleteTrafficMirrorFilterRule(DeleteTrafficMirrorFilterRuleRequest deleteTrafficMirrorFilterRuleRequest) {
        return null;
    }

    @Override
    public DeleteTrafficMirrorSessionResponse deleteTrafficMirrorSession(DeleteTrafficMirrorSessionRequest deleteTrafficMirrorSessionRequest) {
        return null;
    }

    @Override
    public DeleteTrafficMirrorTargetResponse deleteTrafficMirrorTarget(DeleteTrafficMirrorTargetRequest deleteTrafficMirrorTargetRequest) {
        return null;
    }

    @Override
    public DeleteTransitGatewayResponse deleteTransitGateway(DeleteTransitGatewayRequest deleteTransitGatewayRequest) {
        return null;
    }

    @Override
    public DeleteTransitGatewayConnectResponse deleteTransitGatewayConnect(DeleteTransitGatewayConnectRequest deleteTransitGatewayConnectRequest) {
        return null;
    }

    @Override
    public DeleteTransitGatewayConnectPeerResponse deleteTransitGatewayConnectPeer(DeleteTransitGatewayConnectPeerRequest deleteTransitGatewayConnectPeerRequest) {
        return null;
    }

    @Override
    public DeleteTransitGatewayMulticastDomainResponse deleteTransitGatewayMulticastDomain(DeleteTransitGatewayMulticastDomainRequest deleteTransitGatewayMulticastDomainRequest) {
        return null;
    }

    @Override
    public DeleteTransitGatewayPeeringAttachmentResponse deleteTransitGatewayPeeringAttachment(DeleteTransitGatewayPeeringAttachmentRequest deleteTransitGatewayPeeringAttachmentRequest) {
        return null;
    }

    @Override
    public DeleteTransitGatewayPolicyTableResponse deleteTransitGatewayPolicyTable(DeleteTransitGatewayPolicyTableRequest deleteTransitGatewayPolicyTableRequest) {
        return null;
    }

    @Override
    public DeleteTransitGatewayPrefixListReferenceResponse deleteTransitGatewayPrefixListReference(DeleteTransitGatewayPrefixListReferenceRequest deleteTransitGatewayPrefixListReferenceRequest) {
        return null;
    }

    @Override
    public DeleteTransitGatewayRouteResponse deleteTransitGatewayRoute(DeleteTransitGatewayRouteRequest deleteTransitGatewayRouteRequest) {
        return null;
    }

    @Override
    public DeleteTransitGatewayRouteTableResponse deleteTransitGatewayRouteTable(DeleteTransitGatewayRouteTableRequest deleteTransitGatewayRouteTableRequest) {
        return null;
    }

    @Override
    public DeleteTransitGatewayRouteTableAnnouncementResponse deleteTransitGatewayRouteTableAnnouncement(DeleteTransitGatewayRouteTableAnnouncementRequest deleteTransitGatewayRouteTableAnnouncementRequest) {
        return null;
    }

    @Override
    public DeleteTransitGatewayVpcAttachmentResponse deleteTransitGatewayVpcAttachment(DeleteTransitGatewayVpcAttachmentRequest deleteTransitGatewayVpcAttachmentRequest) {
        return null;
    }

    @Override
    public DeleteVerifiedAccessEndpointResponse deleteVerifiedAccessEndpoint(DeleteVerifiedAccessEndpointRequest deleteVerifiedAccessEndpointRequest) {
        return null;
    }

    @Override
    public DeleteVerifiedAccessGroupResponse deleteVerifiedAccessGroup(DeleteVerifiedAccessGroupRequest deleteVerifiedAccessGroupRequest) {
        return null;
    }

    @Override
    public DeleteVerifiedAccessInstanceResponse deleteVerifiedAccessInstance(DeleteVerifiedAccessInstanceRequest deleteVerifiedAccessInstanceRequest) {
        return null;
    }

    @Override
    public DeleteVerifiedAccessTrustProviderResponse deleteVerifiedAccessTrustProvider(DeleteVerifiedAccessTrustProviderRequest deleteVerifiedAccessTrustProviderRequest) {
        return null;
    }

    @Override
    public DeleteVolumeResponse deleteVolume(DeleteVolumeRequest deleteVolumeRequest) {
        return null;
    }

    @Override
    public DeleteVpcResponse deleteVpc(DeleteVpcRequest deleteVpcRequest) {
        return null;
    }

    @Override
    public DeleteVpcEndpointConnectionNotificationsResponse deleteVpcEndpointConnectionNotifications(DeleteVpcEndpointConnectionNotificationsRequest deleteVpcEndpointConnectionNotificationsRequest) {
        return null;
    }

    @Override
    public DeleteVpcEndpointServiceConfigurationsResponse deleteVpcEndpointServiceConfigurations(DeleteVpcEndpointServiceConfigurationsRequest deleteVpcEndpointServiceConfigurationsRequest) {
        return null;
    }

    @Override
    public DeleteVpcEndpointsResponse deleteVpcEndpoints(DeleteVpcEndpointsRequest deleteVpcEndpointsRequest) {
        return null;
    }

    @Override
    public DeleteVpcPeeringConnectionResponse deleteVpcPeeringConnection(DeleteVpcPeeringConnectionRequest deleteVpcPeeringConnectionRequest) {
        return null;
    }

    @Override
    public DeleteVpnConnectionResponse deleteVpnConnection(DeleteVpnConnectionRequest deleteVpnConnectionRequest) {
        return null;
    }

    @Override
    public DeleteVpnConnectionRouteResponse deleteVpnConnectionRoute(DeleteVpnConnectionRouteRequest deleteVpnConnectionRouteRequest) {
        return null;
    }

    @Override
    public DeleteVpnGatewayResponse deleteVpnGateway(DeleteVpnGatewayRequest deleteVpnGatewayRequest) {
        return null;
    }

    @Override
    public DeprovisionByoipCidrResponse deprovisionByoipCidr(DeprovisionByoipCidrRequest deprovisionByoipCidrRequest) {
        return null;
    }

    @Override
    public DeprovisionIpamByoasnResponse deprovisionIpamByoasn(DeprovisionIpamByoasnRequest deprovisionIpamByoasnRequest) {
        return null;
    }

    @Override
    public DeprovisionIpamPoolCidrResponse deprovisionIpamPoolCidr(DeprovisionIpamPoolCidrRequest deprovisionIpamPoolCidrRequest) {
        return null;
    }

    @Override
    public DeprovisionPublicIpv4PoolCidrResponse deprovisionPublicIpv4PoolCidr(DeprovisionPublicIpv4PoolCidrRequest deprovisionPublicIpv4PoolCidrRequest) {
        return null;
    }

    @Override
    public DeregisterImageResponse deregisterImage(DeregisterImageRequest deregisterImageRequest) {
        return null;
    }

    @Override
    public DeregisterInstanceEventNotificationAttributesResponse deregisterInstanceEventNotificationAttributes(DeregisterInstanceEventNotificationAttributesRequest deregisterInstanceEventNotificationAttributesRequest) {
        return null;
    }

    @Override
    public DeregisterTransitGatewayMulticastGroupMembersResponse deregisterTransitGatewayMulticastGroupMembers(DeregisterTransitGatewayMulticastGroupMembersRequest deregisterTransitGatewayMulticastGroupMembersRequest) {
        return null;
    }

    @Override
    public DeregisterTransitGatewayMulticastGroupSourcesResponse deregisterTransitGatewayMulticastGroupSources(DeregisterTransitGatewayMulticastGroupSourcesRequest deregisterTransitGatewayMulticastGroupSourcesRequest) {
        return null;
    }

    @Override
    public DescribeAccountAttributesResponse describeAccountAttributes(DescribeAccountAttributesRequest describeAccountAttributesRequest) {
        return null;
    }

    @Override
    public DescribeAccountAttributesResponse describeAccountAttributes() {
        return null;
    }

    @Override
    public DescribeAddressTransfersResponse describeAddressTransfers(DescribeAddressTransfersRequest describeAddressTransfersRequest) {
        return null;
    }

    @Override
    public DescribeAddressesResponse describeAddresses(DescribeAddressesRequest describeAddressesRequest) {
        return null;
    }

    @Override
    public DescribeAddressesResponse describeAddresses() {
        return null;
    }

    @Override
    public DescribeAddressesAttributeResponse describeAddressesAttribute(DescribeAddressesAttributeRequest describeAddressesAttributeRequest) {
        return null;
    }

    @Override
    public DescribeAggregateIdFormatResponse describeAggregateIdFormat(DescribeAggregateIdFormatRequest describeAggregateIdFormatRequest) {
        return null;
    }

    @Override
    public DescribeAvailabilityZonesResponse describeAvailabilityZones(DescribeAvailabilityZonesRequest describeAvailabilityZonesRequest) {
        return null;
    }

    @Override
    public DescribeAvailabilityZonesResponse describeAvailabilityZones() {
        return null;
    }

    @Override
    public DescribeAwsNetworkPerformanceMetricSubscriptionsResponse describeAwsNetworkPerformanceMetricSubscriptions(DescribeAwsNetworkPerformanceMetricSubscriptionsRequest describeAwsNetworkPerformanceMetricSubscriptionsRequest) {
        return null;
    }

    @Override
    public DescribeBundleTasksResponse describeBundleTasks(DescribeBundleTasksRequest describeBundleTasksRequest) {
        return null;
    }

    @Override
    public DescribeBundleTasksResponse describeBundleTasks() {
        return null;
    }

    @Override
    public DescribeByoipCidrsResponse describeByoipCidrs(DescribeByoipCidrsRequest describeByoipCidrsRequest) {
        return null;
    }

    @Override
    public DescribeCapacityBlockOfferingsResponse describeCapacityBlockOfferings(DescribeCapacityBlockOfferingsRequest describeCapacityBlockOfferingsRequest) {
        return null;
    }

    @Override
    public DescribeCapacityReservationFleetsResponse describeCapacityReservationFleets(DescribeCapacityReservationFleetsRequest describeCapacityReservationFleetsRequest) {
        return null;
    }

    @Override
    public DescribeCapacityReservationsResponse describeCapacityReservations(DescribeCapacityReservationsRequest describeCapacityReservationsRequest) {
        return null;
    }

    @Override
    public DescribeCarrierGatewaysResponse describeCarrierGateways(DescribeCarrierGatewaysRequest describeCarrierGatewaysRequest) {
        return null;
    }

    @Override
    public DescribeClassicLinkInstancesResponse describeClassicLinkInstances(DescribeClassicLinkInstancesRequest describeClassicLinkInstancesRequest) {
        return null;
    }

    @Override
    public DescribeClassicLinkInstancesResponse describeClassicLinkInstances() {
        return null;
    }

    @Override
    public DescribeClientVpnAuthorizationRulesResponse describeClientVpnAuthorizationRules(DescribeClientVpnAuthorizationRulesRequest describeClientVpnAuthorizationRulesRequest) {
        return null;
    }

    @Override
    public DescribeClientVpnConnectionsResponse describeClientVpnConnections(DescribeClientVpnConnectionsRequest describeClientVpnConnectionsRequest) {
        return null;
    }

    @Override
    public DescribeClientVpnEndpointsResponse describeClientVpnEndpoints(DescribeClientVpnEndpointsRequest describeClientVpnEndpointsRequest) {
        return null;
    }

    @Override
    public DescribeClientVpnRoutesResponse describeClientVpnRoutes(DescribeClientVpnRoutesRequest describeClientVpnRoutesRequest) {
        return null;
    }

    @Override
    public DescribeClientVpnTargetNetworksResponse describeClientVpnTargetNetworks(DescribeClientVpnTargetNetworksRequest describeClientVpnTargetNetworksRequest) {
        return null;
    }

    @Override
    public DescribeCoipPoolsResponse describeCoipPools(DescribeCoipPoolsRequest describeCoipPoolsRequest) {
        return null;
    }

    @Override
    public DescribeConversionTasksResponse describeConversionTasks(DescribeConversionTasksRequest describeConversionTasksRequest) {
        return null;
    }

    @Override
    public DescribeConversionTasksResponse describeConversionTasks() {
        return null;
    }

    @Override
    public DescribeCustomerGatewaysResponse describeCustomerGateways(DescribeCustomerGatewaysRequest describeCustomerGatewaysRequest) {
        return null;
    }

    @Override
    public DescribeCustomerGatewaysResponse describeCustomerGateways() {
        return null;
    }

    @Override
    public DescribeDhcpOptionsResponse describeDhcpOptions(DescribeDhcpOptionsRequest describeDhcpOptionsRequest) {
        return null;
    }

    @Override
    public DescribeDhcpOptionsResponse describeDhcpOptions() {
        return null;
    }

    @Override
    public DescribeEgressOnlyInternetGatewaysResponse describeEgressOnlyInternetGateways(DescribeEgressOnlyInternetGatewaysRequest describeEgressOnlyInternetGatewaysRequest) {
        return null;
    }

    @Override
    public DescribeElasticGpusResponse describeElasticGpus(DescribeElasticGpusRequest describeElasticGpusRequest) {
        return null;
    }

    @Override
    public DescribeExportImageTasksResponse describeExportImageTasks(DescribeExportImageTasksRequest describeExportImageTasksRequest) {
        return null;
    }

    @Override
    public DescribeExportTasksResponse describeExportTasks(DescribeExportTasksRequest describeExportTasksRequest) {
        return null;
    }

    @Override
    public DescribeExportTasksResponse describeExportTasks() {
        return null;
    }

    @Override
    public DescribeFastLaunchImagesResponse describeFastLaunchImages(DescribeFastLaunchImagesRequest describeFastLaunchImagesRequest) {
        return null;
    }

    @Override
    public DescribeFastSnapshotRestoresResponse describeFastSnapshotRestores(DescribeFastSnapshotRestoresRequest describeFastSnapshotRestoresRequest) {
        return null;
    }

    @Override
    public DescribeFleetHistoryResponse describeFleetHistory(DescribeFleetHistoryRequest describeFleetHistoryRequest) {
        return null;
    }

    @Override
    public DescribeFleetInstancesResponse describeFleetInstances(DescribeFleetInstancesRequest describeFleetInstancesRequest) {
        return null;
    }

    @Override
    public DescribeFleetsResponse describeFleets(DescribeFleetsRequest describeFleetsRequest) {
        return null;
    }

    @Override
    public DescribeFlowLogsResponse describeFlowLogs(DescribeFlowLogsRequest describeFlowLogsRequest) {
        return null;
    }

    @Override
    public DescribeFlowLogsResponse describeFlowLogs() {
        return null;
    }

    @Override
    public DescribeFpgaImageAttributeResponse describeFpgaImageAttribute(DescribeFpgaImageAttributeRequest describeFpgaImageAttributeRequest) {
        return null;
    }

    @Override
    public DescribeFpgaImagesResponse describeFpgaImages(DescribeFpgaImagesRequest describeFpgaImagesRequest) {
        return null;
    }

    @Override
    public DescribeHostReservationOfferingsResponse describeHostReservationOfferings(DescribeHostReservationOfferingsRequest describeHostReservationOfferingsRequest) {
        return null;
    }

    @Override
    public DescribeHostReservationsResponse describeHostReservations(DescribeHostReservationsRequest describeHostReservationsRequest) {
        return null;
    }

    @Override
    public DescribeHostsResponse describeHosts(DescribeHostsRequest describeHostsRequest) {
        return null;
    }

    @Override
    public DescribeHostsResponse describeHosts() {
        return null;
    }

    @Override
    public DescribeIamInstanceProfileAssociationsResponse describeIamInstanceProfileAssociations(DescribeIamInstanceProfileAssociationsRequest describeIamInstanceProfileAssociationsRequest) {
        return null;
    }

    @Override
    public DescribeIdFormatResponse describeIdFormat(DescribeIdFormatRequest describeIdFormatRequest) {
        return null;
    }

    @Override
    public DescribeIdFormatResponse describeIdFormat() {
        return null;
    }

    @Override
    public DescribeIdentityIdFormatResponse describeIdentityIdFormat(DescribeIdentityIdFormatRequest describeIdentityIdFormatRequest) {
        return null;
    }

    @Override
    public DescribeImageAttributeResponse describeImageAttribute(DescribeImageAttributeRequest describeImageAttributeRequest) {
        return null;
    }

    @Override
    public DescribeImagesResponse describeImages(DescribeImagesRequest describeImagesRequest) {
        return null;
    }

    @Override
    public DescribeImagesResponse describeImages() {
        return null;
    }

    @Override
    public DescribeImportImageTasksResponse describeImportImageTasks(DescribeImportImageTasksRequest describeImportImageTasksRequest) {
        return null;
    }

    @Override
    public DescribeImportImageTasksResponse describeImportImageTasks() {
        return null;
    }

    @Override
    public DescribeImportSnapshotTasksResponse describeImportSnapshotTasks(DescribeImportSnapshotTasksRequest describeImportSnapshotTasksRequest) {
        return null;
    }

    @Override
    public DescribeImportSnapshotTasksResponse describeImportSnapshotTasks() {
        return null;
    }

    @Override
    public DescribeInstanceAttributeResponse describeInstanceAttribute(DescribeInstanceAttributeRequest describeInstanceAttributeRequest) {
        return null;
    }

    @Override
    public DescribeInstanceConnectEndpointsResponse describeInstanceConnectEndpoints(DescribeInstanceConnectEndpointsRequest describeInstanceConnectEndpointsRequest) {
        return null;
    }

    @Override
    public DescribeInstanceCreditSpecificationsResponse describeInstanceCreditSpecifications(DescribeInstanceCreditSpecificationsRequest describeInstanceCreditSpecificationsRequest) {
        return null;
    }

    @Override
    public DescribeInstanceEventNotificationAttributesResponse describeInstanceEventNotificationAttributes(DescribeInstanceEventNotificationAttributesRequest describeInstanceEventNotificationAttributesRequest) {
        return null;
    }

    @Override
    public DescribeInstanceEventWindowsResponse describeInstanceEventWindows(DescribeInstanceEventWindowsRequest describeInstanceEventWindowsRequest) {
        return null;
    }

    @Override
    public DescribeInstanceStatusResponse describeInstanceStatus(DescribeInstanceStatusRequest describeInstanceStatusRequest) {
        return null;
    }

    @Override
    public DescribeInstanceStatusResponse describeInstanceStatus() {
        return null;
    }

    @Override
    public DescribeInstanceTopologyResponse describeInstanceTopology(DescribeInstanceTopologyRequest describeInstanceTopologyRequest) {
        return null;
    }

    @Override
    public DescribeInstanceTypeOfferingsResponse describeInstanceTypeOfferings(DescribeInstanceTypeOfferingsRequest describeInstanceTypeOfferingsRequest) {
        return null;
    }

    @Override
    public DescribeInstanceTypesResponse describeInstanceTypes(DescribeInstanceTypesRequest describeInstanceTypesRequest) {
        return null;
    }

    @Override
    public DescribeInstancesResponse describeInstances(DescribeInstancesRequest describeInstancesRequest) {
        return null;
    }

    @Override
    public DescribeInstancesResponse describeInstances() {
        return null;
    }

    @Override
    public DescribeInternetGatewaysResponse describeInternetGateways(DescribeInternetGatewaysRequest describeInternetGatewaysRequest) {
        return null;
    }

    @Override
    public DescribeInternetGatewaysResponse describeInternetGateways() {
        return null;
    }

    @Override
    public DescribeIpamByoasnResponse describeIpamByoasn(DescribeIpamByoasnRequest describeIpamByoasnRequest) {
        return null;
    }

    @Override
    public DescribeIpamExternalResourceVerificationTokensResponse describeIpamExternalResourceVerificationTokens(DescribeIpamExternalResourceVerificationTokensRequest describeIpamExternalResourceVerificationTokensRequest) {
        return null;
    }

    @Override
    public DescribeIpamPoolsResponse describeIpamPools(DescribeIpamPoolsRequest describeIpamPoolsRequest) {
        return null;
    }

    @Override
    public DescribeIpamResourceDiscoveriesResponse describeIpamResourceDiscoveries(DescribeIpamResourceDiscoveriesRequest describeIpamResourceDiscoveriesRequest) {
        return null;
    }

    @Override
    public DescribeIpamResourceDiscoveryAssociationsResponse describeIpamResourceDiscoveryAssociations(DescribeIpamResourceDiscoveryAssociationsRequest describeIpamResourceDiscoveryAssociationsRequest) {
        return null;
    }

    @Override
    public DescribeIpamScopesResponse describeIpamScopes(DescribeIpamScopesRequest describeIpamScopesRequest) {
        return null;
    }

    @Override
    public DescribeIpamsResponse describeIpams(DescribeIpamsRequest describeIpamsRequest) {
        return null;
    }

    @Override
    public DescribeIpv6PoolsResponse describeIpv6Pools(DescribeIpv6PoolsRequest describeIpv6PoolsRequest) {
        return null;
    }

    @Override
    public DescribeKeyPairsResponse describeKeyPairs(DescribeKeyPairsRequest describeKeyPairsRequest) {
        return null;
    }

    @Override
    public DescribeKeyPairsResponse describeKeyPairs() {
        return null;
    }

    @Override
    public DescribeLaunchTemplateVersionsResponse describeLaunchTemplateVersions(DescribeLaunchTemplateVersionsRequest describeLaunchTemplateVersionsRequest) {
        return null;
    }

    @Override
    public DescribeLaunchTemplatesResponse describeLaunchTemplates(DescribeLaunchTemplatesRequest describeLaunchTemplatesRequest) {
        return null;
    }

    @Override
    public DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse describeLocalGatewayRouteTableVirtualInterfaceGroupAssociations(DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest describeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest) {
        return null;
    }

    @Override
    public DescribeLocalGatewayRouteTableVpcAssociationsResponse describeLocalGatewayRouteTableVpcAssociations(DescribeLocalGatewayRouteTableVpcAssociationsRequest describeLocalGatewayRouteTableVpcAssociationsRequest) {
        return null;
    }

    @Override
    public DescribeLocalGatewayRouteTablesResponse describeLocalGatewayRouteTables(DescribeLocalGatewayRouteTablesRequest describeLocalGatewayRouteTablesRequest) {
        return null;
    }

    @Override
    public DescribeLocalGatewayVirtualInterfaceGroupsResponse describeLocalGatewayVirtualInterfaceGroups(DescribeLocalGatewayVirtualInterfaceGroupsRequest describeLocalGatewayVirtualInterfaceGroupsRequest) {
        return null;
    }

    @Override
    public DescribeLocalGatewayVirtualInterfacesResponse describeLocalGatewayVirtualInterfaces(DescribeLocalGatewayVirtualInterfacesRequest describeLocalGatewayVirtualInterfacesRequest) {
        return null;
    }

    @Override
    public DescribeLocalGatewaysResponse describeLocalGateways(DescribeLocalGatewaysRequest describeLocalGatewaysRequest) {
        return null;
    }

    @Override
    public DescribeLockedSnapshotsResponse describeLockedSnapshots(DescribeLockedSnapshotsRequest describeLockedSnapshotsRequest) {
        return null;
    }

    @Override
    public DescribeMacHostsResponse describeMacHosts(DescribeMacHostsRequest describeMacHostsRequest) {
        return null;
    }

    @Override
    public DescribeManagedPrefixListsResponse describeManagedPrefixLists(DescribeManagedPrefixListsRequest describeManagedPrefixListsRequest) {
        return null;
    }

    @Override
    public DescribeMovingAddressesResponse describeMovingAddresses(DescribeMovingAddressesRequest describeMovingAddressesRequest) {
        return null;
    }

    @Override
    public DescribeMovingAddressesResponse describeMovingAddresses() {
        return null;
    }

    @Override
    public DescribeNatGatewaysResponse describeNatGateways(DescribeNatGatewaysRequest describeNatGatewaysRequest) {
        return null;
    }

    @Override
    public DescribeNetworkAclsResponse describeNetworkAcls(DescribeNetworkAclsRequest describeNetworkAclsRequest) {
        return null;
    }

    @Override
    public DescribeNetworkAclsResponse describeNetworkAcls() {
        return null;
    }

    @Override
    public DescribeNetworkInsightsAccessScopeAnalysesResponse describeNetworkInsightsAccessScopeAnalyses(DescribeNetworkInsightsAccessScopeAnalysesRequest describeNetworkInsightsAccessScopeAnalysesRequest) {
        return null;
    }

    @Override
    public DescribeNetworkInsightsAccessScopesResponse describeNetworkInsightsAccessScopes(DescribeNetworkInsightsAccessScopesRequest describeNetworkInsightsAccessScopesRequest) {
        return null;
    }

    @Override
    public DescribeNetworkInsightsAnalysesResponse describeNetworkInsightsAnalyses(DescribeNetworkInsightsAnalysesRequest describeNetworkInsightsAnalysesRequest) {
        return null;
    }

    @Override
    public DescribeNetworkInsightsPathsResponse describeNetworkInsightsPaths(DescribeNetworkInsightsPathsRequest describeNetworkInsightsPathsRequest) {
        return null;
    }

    @Override
    public DescribeNetworkInterfaceAttributeResponse describeNetworkInterfaceAttribute(DescribeNetworkInterfaceAttributeRequest describeNetworkInterfaceAttributeRequest) {
        return null;
    }

    @Override
    public DescribeNetworkInterfacePermissionsResponse describeNetworkInterfacePermissions(DescribeNetworkInterfacePermissionsRequest describeNetworkInterfacePermissionsRequest) {
        return null;
    }

    @Override
    public DescribeNetworkInterfacesResponse describeNetworkInterfaces(DescribeNetworkInterfacesRequest describeNetworkInterfacesRequest) {
        return null;
    }

    @Override
    public DescribeNetworkInterfacesResponse describeNetworkInterfaces() {
        return null;
    }

    @Override
    public DescribePlacementGroupsResponse describePlacementGroups(DescribePlacementGroupsRequest describePlacementGroupsRequest) {
        return null;
    }

    @Override
    public DescribePlacementGroupsResponse describePlacementGroups() {
        return null;
    }

    @Override
    public DescribePrefixListsResponse describePrefixLists(DescribePrefixListsRequest describePrefixListsRequest) {
        return null;
    }

    @Override
    public DescribePrefixListsResponse describePrefixLists() {
        return null;
    }

    @Override
    public DescribePrincipalIdFormatResponse describePrincipalIdFormat(DescribePrincipalIdFormatRequest describePrincipalIdFormatRequest) {
        return null;
    }

    @Override
    public DescribePublicIpv4PoolsResponse describePublicIpv4Pools(DescribePublicIpv4PoolsRequest describePublicIpv4PoolsRequest) {
        return null;
    }

    @Override
    public DescribeRegionsResponse describeRegions(DescribeRegionsRequest describeRegionsRequest) {
        return null;
    }

    @Override
    public DescribeRegionsResponse describeRegions() {
        return null;
    }

    @Override
    public DescribeReplaceRootVolumeTasksResponse describeReplaceRootVolumeTasks(DescribeReplaceRootVolumeTasksRequest describeReplaceRootVolumeTasksRequest) {
        return null;
    }

    @Override
    public DescribeReservedInstancesResponse describeReservedInstances(DescribeReservedInstancesRequest describeReservedInstancesRequest) {
        return null;
    }

    @Override
    public DescribeReservedInstancesResponse describeReservedInstances() {
        return null;
    }

    @Override
    public DescribeReservedInstancesListingsResponse describeReservedInstancesListings(DescribeReservedInstancesListingsRequest describeReservedInstancesListingsRequest) {
        return null;
    }

    @Override
    public DescribeReservedInstancesListingsResponse describeReservedInstancesListings() {
        return null;
    }

    @Override
    public DescribeReservedInstancesModificationsResponse describeReservedInstancesModifications(DescribeReservedInstancesModificationsRequest describeReservedInstancesModificationsRequest) {
        return null;
    }

    @Override
    public DescribeReservedInstancesModificationsResponse describeReservedInstancesModifications() {
        return null;
    }

    @Override
    public DescribeReservedInstancesOfferingsResponse describeReservedInstancesOfferings(DescribeReservedInstancesOfferingsRequest describeReservedInstancesOfferingsRequest) {
        return null;
    }

    @Override
    public DescribeReservedInstancesOfferingsResponse describeReservedInstancesOfferings() {
        return null;
    }

    @Override
    public DescribeRouteTablesResponse describeRouteTables(DescribeRouteTablesRequest describeRouteTablesRequest) {
        return null;
    }

    @Override
    public DescribeRouteTablesResponse describeRouteTables() {
        return null;
    }

    @Override
    public DescribeScheduledInstanceAvailabilityResponse describeScheduledInstanceAvailability(DescribeScheduledInstanceAvailabilityRequest describeScheduledInstanceAvailabilityRequest) {
        return null;
    }

    @Override
    public DescribeScheduledInstancesResponse describeScheduledInstances(DescribeScheduledInstancesRequest describeScheduledInstancesRequest) {
        return null;
    }

    @Override
    public DescribeSecurityGroupReferencesResponse describeSecurityGroupReferences(DescribeSecurityGroupReferencesRequest describeSecurityGroupReferencesRequest) {
        return null;
    }

    @Override
    public DescribeSecurityGroupRulesResponse describeSecurityGroupRules(DescribeSecurityGroupRulesRequest describeSecurityGroupRulesRequest) {
        return null;
    }

    @Override
    public DescribeSecurityGroupsResponse describeSecurityGroups(DescribeSecurityGroupsRequest describeSecurityGroupsRequest) {
        return null;
    }

    @Override
    public DescribeSecurityGroupsResponse describeSecurityGroups() {
        return null;
    }

    @Override
    public DescribeSnapshotAttributeResponse describeSnapshotAttribute(DescribeSnapshotAttributeRequest describeSnapshotAttributeRequest) {
        return null;
    }

    @Override
    public DescribeSnapshotTierStatusResponse describeSnapshotTierStatus(DescribeSnapshotTierStatusRequest describeSnapshotTierStatusRequest) {
        return null;
    }

    @Override
    public DescribeSnapshotsResponse describeSnapshots(DescribeSnapshotsRequest describeSnapshotsRequest) {
        return null;
    }

    @Override
    public DescribeSnapshotsResponse describeSnapshots() {
        return null;
    }

    @Override
    public DescribeSpotDatafeedSubscriptionResponse describeSpotDatafeedSubscription(DescribeSpotDatafeedSubscriptionRequest describeSpotDatafeedSubscriptionRequest) {
        return null;
    }

    @Override
    public DescribeSpotDatafeedSubscriptionResponse describeSpotDatafeedSubscription() {
        return null;
    }

    @Override
    public DescribeSpotFleetInstancesResponse describeSpotFleetInstances(DescribeSpotFleetInstancesRequest describeSpotFleetInstancesRequest) {
        return null;
    }

    @Override
    public DescribeSpotFleetRequestHistoryResponse describeSpotFleetRequestHistory(DescribeSpotFleetRequestHistoryRequest describeSpotFleetRequestHistoryRequest) {
        return null;
    }

    @Override
    public DescribeSpotFleetRequestsResponse describeSpotFleetRequests(DescribeSpotFleetRequestsRequest describeSpotFleetRequestsRequest) {
        return null;
    }

    @Override
    public DescribeSpotFleetRequestsResponse describeSpotFleetRequests() {
        return null;
    }

    @Override
    public DescribeSpotInstanceRequestsResponse describeSpotInstanceRequests(DescribeSpotInstanceRequestsRequest describeSpotInstanceRequestsRequest) {
        return null;
    }

    @Override
    public DescribeSpotInstanceRequestsResponse describeSpotInstanceRequests() {
        return null;
    }

    @Override
    public DescribeSpotPriceHistoryResponse describeSpotPriceHistory(DescribeSpotPriceHistoryRequest describeSpotPriceHistoryRequest) {
        return null;
    }

    @Override
    public DescribeSpotPriceHistoryResponse describeSpotPriceHistory() {
        return null;
    }

    @Override
    public DescribeStaleSecurityGroupsResponse describeStaleSecurityGroups(DescribeStaleSecurityGroupsRequest describeStaleSecurityGroupsRequest) {
        return null;
    }

    @Override
    public DescribeStoreImageTasksResponse describeStoreImageTasks(DescribeStoreImageTasksRequest describeStoreImageTasksRequest) {
        return null;
    }

    @Override
    public DescribeSubnetsResponse describeSubnets(DescribeSubnetsRequest describeSubnetsRequest) {
        return null;
    }

    @Override
    public DescribeSubnetsResponse describeSubnets() {
        return null;
    }

    @Override
    public DescribeTagsResponse describeTags(DescribeTagsRequest describeTagsRequest) {
        return null;
    }

    @Override
    public DescribeTagsResponse describeTags() {
        return null;
    }

    @Override
    public DescribeTrafficMirrorFilterRulesResponse describeTrafficMirrorFilterRules(DescribeTrafficMirrorFilterRulesRequest describeTrafficMirrorFilterRulesRequest) {
        return null;
    }

    @Override
    public DescribeTrafficMirrorFiltersResponse describeTrafficMirrorFilters(DescribeTrafficMirrorFiltersRequest describeTrafficMirrorFiltersRequest) {
        return null;
    }

    @Override
    public DescribeTrafficMirrorSessionsResponse describeTrafficMirrorSessions(DescribeTrafficMirrorSessionsRequest describeTrafficMirrorSessionsRequest) {
        return null;
    }

    @Override
    public DescribeTrafficMirrorTargetsResponse describeTrafficMirrorTargets(DescribeTrafficMirrorTargetsRequest describeTrafficMirrorTargetsRequest) {
        return null;
    }

    @Override
    public DescribeTransitGatewayAttachmentsResponse describeTransitGatewayAttachments(DescribeTransitGatewayAttachmentsRequest describeTransitGatewayAttachmentsRequest) {
        return null;
    }

    @Override
    public DescribeTransitGatewayConnectPeersResponse describeTransitGatewayConnectPeers(DescribeTransitGatewayConnectPeersRequest describeTransitGatewayConnectPeersRequest) {
        return null;
    }

    @Override
    public DescribeTransitGatewayConnectsResponse describeTransitGatewayConnects(DescribeTransitGatewayConnectsRequest describeTransitGatewayConnectsRequest) {
        return null;
    }

    @Override
    public DescribeTransitGatewayMulticastDomainsResponse describeTransitGatewayMulticastDomains(DescribeTransitGatewayMulticastDomainsRequest describeTransitGatewayMulticastDomainsRequest) {
        return null;
    }

    @Override
    public DescribeTransitGatewayPeeringAttachmentsResponse describeTransitGatewayPeeringAttachments(DescribeTransitGatewayPeeringAttachmentsRequest describeTransitGatewayPeeringAttachmentsRequest) {
        return null;
    }

    @Override
    public DescribeTransitGatewayPolicyTablesResponse describeTransitGatewayPolicyTables(DescribeTransitGatewayPolicyTablesRequest describeTransitGatewayPolicyTablesRequest) {
        return null;
    }

    @Override
    public DescribeTransitGatewayRouteTableAnnouncementsResponse describeTransitGatewayRouteTableAnnouncements(DescribeTransitGatewayRouteTableAnnouncementsRequest describeTransitGatewayRouteTableAnnouncementsRequest) {
        return null;
    }

    @Override
    public DescribeTransitGatewayRouteTablesResponse describeTransitGatewayRouteTables(DescribeTransitGatewayRouteTablesRequest describeTransitGatewayRouteTablesRequest) {
        return null;
    }

    @Override
    public DescribeTransitGatewayVpcAttachmentsResponse describeTransitGatewayVpcAttachments(DescribeTransitGatewayVpcAttachmentsRequest describeTransitGatewayVpcAttachmentsRequest) {
        return null;
    }

    @Override
    public DescribeTransitGatewaysResponse describeTransitGateways(DescribeTransitGatewaysRequest describeTransitGatewaysRequest) {
        return null;
    }

    @Override
    public DescribeTrunkInterfaceAssociationsResponse describeTrunkInterfaceAssociations(DescribeTrunkInterfaceAssociationsRequest describeTrunkInterfaceAssociationsRequest) {
        return null;
    }

    @Override
    public DescribeVerifiedAccessEndpointsResponse describeVerifiedAccessEndpoints(DescribeVerifiedAccessEndpointsRequest describeVerifiedAccessEndpointsRequest) {
        return null;
    }

    @Override
    public DescribeVerifiedAccessGroupsResponse describeVerifiedAccessGroups(DescribeVerifiedAccessGroupsRequest describeVerifiedAccessGroupsRequest) {
        return null;
    }

    @Override
    public DescribeVerifiedAccessInstanceLoggingConfigurationsResponse describeVerifiedAccessInstanceLoggingConfigurations(DescribeVerifiedAccessInstanceLoggingConfigurationsRequest describeVerifiedAccessInstanceLoggingConfigurationsRequest) {
        return null;
    }

    @Override
    public DescribeVerifiedAccessInstancesResponse describeVerifiedAccessInstances(DescribeVerifiedAccessInstancesRequest describeVerifiedAccessInstancesRequest) {
        return null;
    }

    @Override
    public DescribeVerifiedAccessTrustProvidersResponse describeVerifiedAccessTrustProviders(DescribeVerifiedAccessTrustProvidersRequest describeVerifiedAccessTrustProvidersRequest) {
        return null;
    }

    @Override
    public DescribeVolumeAttributeResponse describeVolumeAttribute(DescribeVolumeAttributeRequest describeVolumeAttributeRequest) {
        return null;
    }

    @Override
    public DescribeVolumeStatusResponse describeVolumeStatus(DescribeVolumeStatusRequest describeVolumeStatusRequest) {
        return null;
    }

    @Override
    public DescribeVolumeStatusResponse describeVolumeStatus() {
        return null;
    }

    @Override
    public DescribeVolumesResponse describeVolumes(DescribeVolumesRequest describeVolumesRequest) {
        return null;
    }

    @Override
    public DescribeVolumesResponse describeVolumes() {
        return null;
    }

    @Override
    public DescribeVolumesModificationsResponse describeVolumesModifications(DescribeVolumesModificationsRequest describeVolumesModificationsRequest) {
        return null;
    }

    @Override
    public DescribeVpcAttributeResponse describeVpcAttribute(DescribeVpcAttributeRequest describeVpcAttributeRequest) {
        return null;
    }

    @Override
    public DescribeVpcClassicLinkResponse describeVpcClassicLink(DescribeVpcClassicLinkRequest describeVpcClassicLinkRequest) {
        return null;
    }

    @Override
    public DescribeVpcClassicLinkResponse describeVpcClassicLink() {
        return null;
    }

    @Override
    public DescribeVpcClassicLinkDnsSupportResponse describeVpcClassicLinkDnsSupport(DescribeVpcClassicLinkDnsSupportRequest describeVpcClassicLinkDnsSupportRequest) {
        return null;
    }

    @Override
    public DescribeVpcEndpointConnectionNotificationsResponse describeVpcEndpointConnectionNotifications(DescribeVpcEndpointConnectionNotificationsRequest describeVpcEndpointConnectionNotificationsRequest) {
        return null;
    }

    @Override
    public DescribeVpcEndpointConnectionsResponse describeVpcEndpointConnections(DescribeVpcEndpointConnectionsRequest describeVpcEndpointConnectionsRequest) {
        return null;
    }

    @Override
    public DescribeVpcEndpointServiceConfigurationsResponse describeVpcEndpointServiceConfigurations(DescribeVpcEndpointServiceConfigurationsRequest describeVpcEndpointServiceConfigurationsRequest) {
        return null;
    }

    @Override
    public DescribeVpcEndpointServicePermissionsResponse describeVpcEndpointServicePermissions(DescribeVpcEndpointServicePermissionsRequest describeVpcEndpointServicePermissionsRequest) {
        return null;
    }

    @Override
    public DescribeVpcEndpointServicesResponse describeVpcEndpointServices(DescribeVpcEndpointServicesRequest describeVpcEndpointServicesRequest) {
        return null;
    }

    @Override
    public DescribeVpcEndpointServicesResponse describeVpcEndpointServices() {
        return null;
    }

    @Override
    public DescribeVpcEndpointsResponse describeVpcEndpoints(DescribeVpcEndpointsRequest describeVpcEndpointsRequest) {
        return null;
    }

    @Override
    public DescribeVpcEndpointsResponse describeVpcEndpoints() {
        return null;
    }

    @Override
    public DescribeVpcPeeringConnectionsResponse describeVpcPeeringConnections(DescribeVpcPeeringConnectionsRequest describeVpcPeeringConnectionsRequest) {
        return null;
    }

    @Override
    public DescribeVpcPeeringConnectionsResponse describeVpcPeeringConnections() {
        return null;
    }

    @Override
    public DescribeVpcsResponse describeVpcs(DescribeVpcsRequest describeVpcsRequest) {
        return null;
    }

    @Override
    public DescribeVpcsResponse describeVpcs() {
        return null;
    }

    @Override
    public DescribeVpnConnectionsResponse describeVpnConnections(DescribeVpnConnectionsRequest describeVpnConnectionsRequest) {
        return null;
    }

    @Override
    public DescribeVpnConnectionsResponse describeVpnConnections() {
        return null;
    }

    @Override
    public DescribeVpnGatewaysResponse describeVpnGateways(DescribeVpnGatewaysRequest describeVpnGatewaysRequest) {
        return null;
    }

    @Override
    public DescribeVpnGatewaysResponse describeVpnGateways() {
        return null;
    }

    @Override
    public DetachClassicLinkVpcResponse detachClassicLinkVpc(DetachClassicLinkVpcRequest detachClassicLinkVpcRequest) {
        return null;
    }

    @Override
    public DetachInternetGatewayResponse detachInternetGateway(DetachInternetGatewayRequest detachInternetGatewayRequest) {
        return null;
    }

    @Override
    public DetachNetworkInterfaceResponse detachNetworkInterface(DetachNetworkInterfaceRequest detachNetworkInterfaceRequest) {
        return null;
    }

    @Override
    public DetachVerifiedAccessTrustProviderResponse detachVerifiedAccessTrustProvider(DetachVerifiedAccessTrustProviderRequest detachVerifiedAccessTrustProviderRequest) {
        return null;
    }

    @Override
    public DetachVolumeResponse detachVolume(DetachVolumeRequest detachVolumeRequest) {
        return null;
    }

    @Override
    public DetachVpnGatewayResponse detachVpnGateway(DetachVpnGatewayRequest detachVpnGatewayRequest) {
        return null;
    }

    @Override
    public DisableAddressTransferResponse disableAddressTransfer(DisableAddressTransferRequest disableAddressTransferRequest) {
        return null;
    }

    @Override
    public DisableAwsNetworkPerformanceMetricSubscriptionResponse disableAwsNetworkPerformanceMetricSubscription(DisableAwsNetworkPerformanceMetricSubscriptionRequest disableAwsNetworkPerformanceMetricSubscriptionRequest) {
        return null;
    }

    @Override
    public DisableEbsEncryptionByDefaultResponse disableEbsEncryptionByDefault(DisableEbsEncryptionByDefaultRequest disableEbsEncryptionByDefaultRequest) {
        return null;
    }

    @Override
    public DisableFastLaunchResponse disableFastLaunch(DisableFastLaunchRequest disableFastLaunchRequest) {
        return null;
    }

    @Override
    public DisableFastSnapshotRestoresResponse disableFastSnapshotRestores(DisableFastSnapshotRestoresRequest disableFastSnapshotRestoresRequest) {
        return null;
    }

    @Override
    public DisableImageResponse disableImage(DisableImageRequest disableImageRequest) {
        return null;
    }

    @Override
    public DisableImageBlockPublicAccessResponse disableImageBlockPublicAccess(DisableImageBlockPublicAccessRequest disableImageBlockPublicAccessRequest) {
        return null;
    }

    @Override
    public DisableImageDeprecationResponse disableImageDeprecation(DisableImageDeprecationRequest disableImageDeprecationRequest) {
        return null;
    }

    @Override
    public DisableImageDeregistrationProtectionResponse disableImageDeregistrationProtection(DisableImageDeregistrationProtectionRequest disableImageDeregistrationProtectionRequest) {
        return null;
    }

    @Override
    public DisableIpamOrganizationAdminAccountResponse disableIpamOrganizationAdminAccount(DisableIpamOrganizationAdminAccountRequest disableIpamOrganizationAdminAccountRequest) {
        return null;
    }

    @Override
    public DisableSerialConsoleAccessResponse disableSerialConsoleAccess(DisableSerialConsoleAccessRequest disableSerialConsoleAccessRequest) {
        return null;
    }

    @Override
    public DisableSnapshotBlockPublicAccessResponse disableSnapshotBlockPublicAccess(DisableSnapshotBlockPublicAccessRequest disableSnapshotBlockPublicAccessRequest) {
        return null;
    }

    @Override
    public DisableTransitGatewayRouteTablePropagationResponse disableTransitGatewayRouteTablePropagation(DisableTransitGatewayRouteTablePropagationRequest disableTransitGatewayRouteTablePropagationRequest) {
        return null;
    }

    @Override
    public DisableVgwRoutePropagationResponse disableVgwRoutePropagation(DisableVgwRoutePropagationRequest disableVgwRoutePropagationRequest) {
        return null;
    }

    @Override
    public DisableVpcClassicLinkResponse disableVpcClassicLink(DisableVpcClassicLinkRequest disableVpcClassicLinkRequest) {
        return null;
    }

    @Override
    public DisableVpcClassicLinkDnsSupportResponse disableVpcClassicLinkDnsSupport(DisableVpcClassicLinkDnsSupportRequest disableVpcClassicLinkDnsSupportRequest) {
        return null;
    }

    @Override
    public DisassociateAddressResponse disassociateAddress(DisassociateAddressRequest disassociateAddressRequest) {
        return null;
    }

    @Override
    public DisassociateClientVpnTargetNetworkResponse disassociateClientVpnTargetNetwork(DisassociateClientVpnTargetNetworkRequest disassociateClientVpnTargetNetworkRequest) {
        return null;
    }

    @Override
    public DisassociateEnclaveCertificateIamRoleResponse disassociateEnclaveCertificateIamRole(DisassociateEnclaveCertificateIamRoleRequest disassociateEnclaveCertificateIamRoleRequest) {
        return null;
    }

    @Override
    public DisassociateIamInstanceProfileResponse disassociateIamInstanceProfile(DisassociateIamInstanceProfileRequest disassociateIamInstanceProfileRequest) {
        return null;
    }

    @Override
    public DisassociateInstanceEventWindowResponse disassociateInstanceEventWindow(DisassociateInstanceEventWindowRequest disassociateInstanceEventWindowRequest) {
        return null;
    }

    @Override
    public DisassociateIpamByoasnResponse disassociateIpamByoasn(DisassociateIpamByoasnRequest disassociateIpamByoasnRequest) {
        return null;
    }

    @Override
    public DisassociateIpamResourceDiscoveryResponse disassociateIpamResourceDiscovery(DisassociateIpamResourceDiscoveryRequest disassociateIpamResourceDiscoveryRequest) {
        return null;
    }

    @Override
    public DisassociateNatGatewayAddressResponse disassociateNatGatewayAddress(DisassociateNatGatewayAddressRequest disassociateNatGatewayAddressRequest) {
        return null;
    }

    @Override
    public DisassociateRouteTableResponse disassociateRouteTable(DisassociateRouteTableRequest disassociateRouteTableRequest) {
        return null;
    }

    @Override
    public DisassociateSubnetCidrBlockResponse disassociateSubnetCidrBlock(DisassociateSubnetCidrBlockRequest disassociateSubnetCidrBlockRequest) {
        return null;
    }

    @Override
    public DisassociateTransitGatewayMulticastDomainResponse disassociateTransitGatewayMulticastDomain(DisassociateTransitGatewayMulticastDomainRequest disassociateTransitGatewayMulticastDomainRequest) {
        return null;
    }

    @Override
    public DisassociateTransitGatewayPolicyTableResponse disassociateTransitGatewayPolicyTable(DisassociateTransitGatewayPolicyTableRequest disassociateTransitGatewayPolicyTableRequest) {
        return null;
    }

    @Override
    public DisassociateTransitGatewayRouteTableResponse disassociateTransitGatewayRouteTable(DisassociateTransitGatewayRouteTableRequest disassociateTransitGatewayRouteTableRequest) {
        return null;
    }

    @Override
    public DisassociateTrunkInterfaceResponse disassociateTrunkInterface(DisassociateTrunkInterfaceRequest disassociateTrunkInterfaceRequest) {
        return null;
    }

    @Override
    public DisassociateVpcCidrBlockResponse disassociateVpcCidrBlock(DisassociateVpcCidrBlockRequest disassociateVpcCidrBlockRequest) {
        return null;
    }

    @Override
    public EnableAddressTransferResponse enableAddressTransfer(EnableAddressTransferRequest enableAddressTransferRequest) {
        return null;
    }

    @Override
    public EnableAwsNetworkPerformanceMetricSubscriptionResponse enableAwsNetworkPerformanceMetricSubscription(EnableAwsNetworkPerformanceMetricSubscriptionRequest enableAwsNetworkPerformanceMetricSubscriptionRequest) {
        return null;
    }

    @Override
    public EnableEbsEncryptionByDefaultResponse enableEbsEncryptionByDefault(EnableEbsEncryptionByDefaultRequest enableEbsEncryptionByDefaultRequest) {
        return null;
    }

    @Override
    public EnableFastLaunchResponse enableFastLaunch(EnableFastLaunchRequest enableFastLaunchRequest) {
        return null;
    }

    @Override
    public EnableFastSnapshotRestoresResponse enableFastSnapshotRestores(EnableFastSnapshotRestoresRequest enableFastSnapshotRestoresRequest) {
        return null;
    }

    @Override
    public EnableImageResponse enableImage(EnableImageRequest enableImageRequest) {
        return null;
    }

    @Override
    public EnableImageBlockPublicAccessResponse enableImageBlockPublicAccess(EnableImageBlockPublicAccessRequest enableImageBlockPublicAccessRequest) {
        return null;
    }

    @Override
    public EnableImageDeprecationResponse enableImageDeprecation(EnableImageDeprecationRequest enableImageDeprecationRequest) {
        return null;
    }

    @Override
    public EnableImageDeregistrationProtectionResponse enableImageDeregistrationProtection(EnableImageDeregistrationProtectionRequest enableImageDeregistrationProtectionRequest) {
        return null;
    }

    @Override
    public EnableIpamOrganizationAdminAccountResponse enableIpamOrganizationAdminAccount(EnableIpamOrganizationAdminAccountRequest enableIpamOrganizationAdminAccountRequest) {
        return null;
    }

    @Override
    public EnableReachabilityAnalyzerOrganizationSharingResponse enableReachabilityAnalyzerOrganizationSharing(EnableReachabilityAnalyzerOrganizationSharingRequest enableReachabilityAnalyzerOrganizationSharingRequest) {
        return null;
    }

    @Override
    public EnableSerialConsoleAccessResponse enableSerialConsoleAccess(EnableSerialConsoleAccessRequest enableSerialConsoleAccessRequest) {
        return null;
    }

    @Override
    public EnableSnapshotBlockPublicAccessResponse enableSnapshotBlockPublicAccess(EnableSnapshotBlockPublicAccessRequest enableSnapshotBlockPublicAccessRequest) {
        return null;
    }

    @Override
    public EnableTransitGatewayRouteTablePropagationResponse enableTransitGatewayRouteTablePropagation(EnableTransitGatewayRouteTablePropagationRequest enableTransitGatewayRouteTablePropagationRequest) {
        return null;
    }

    @Override
    public EnableVgwRoutePropagationResponse enableVgwRoutePropagation(EnableVgwRoutePropagationRequest enableVgwRoutePropagationRequest) {
        return null;
    }

    @Override
    public EnableVolumeIoResponse enableVolumeIO(EnableVolumeIoRequest enableVolumeIORequest) {
        return null;
    }

    @Override
    public EnableVpcClassicLinkResponse enableVpcClassicLink(EnableVpcClassicLinkRequest enableVpcClassicLinkRequest) {
        return null;
    }

    @Override
    public EnableVpcClassicLinkDnsSupportResponse enableVpcClassicLinkDnsSupport(EnableVpcClassicLinkDnsSupportRequest enableVpcClassicLinkDnsSupportRequest) {
        return null;
    }

    @Override
    public ExportClientVpnClientCertificateRevocationListResponse exportClientVpnClientCertificateRevocationList(ExportClientVpnClientCertificateRevocationListRequest exportClientVpnClientCertificateRevocationListRequest) {
        return null;
    }

    @Override
    public ExportClientVpnClientConfigurationResponse exportClientVpnClientConfiguration(ExportClientVpnClientConfigurationRequest exportClientVpnClientConfigurationRequest) {
        return null;
    }

    @Override
    public ExportImageResponse exportImage(ExportImageRequest exportImageRequest) {
        return null;
    }

    @Override
    public ExportTransitGatewayRoutesResponse exportTransitGatewayRoutes(ExportTransitGatewayRoutesRequest exportTransitGatewayRoutesRequest) {
        return null;
    }

    @Override
    public GetAssociatedEnclaveCertificateIamRolesResponse getAssociatedEnclaveCertificateIamRoles(GetAssociatedEnclaveCertificateIamRolesRequest getAssociatedEnclaveCertificateIamRolesRequest) {
        return null;
    }

    @Override
    public GetAssociatedIpv6PoolCidrsResponse getAssociatedIpv6PoolCidrs(GetAssociatedIpv6PoolCidrsRequest getAssociatedIpv6PoolCidrsRequest) {
        return null;
    }

    @Override
    public GetAwsNetworkPerformanceDataResponse getAwsNetworkPerformanceData(GetAwsNetworkPerformanceDataRequest getAwsNetworkPerformanceDataRequest) {
        return null;
    }

    @Override
    public GetCapacityReservationUsageResponse getCapacityReservationUsage(GetCapacityReservationUsageRequest getCapacityReservationUsageRequest) {
        return null;
    }

    @Override
    public GetCoipPoolUsageResponse getCoipPoolUsage(GetCoipPoolUsageRequest getCoipPoolUsageRequest) {
        return null;
    }

    @Override
    public GetConsoleOutputResponse getConsoleOutput(GetConsoleOutputRequest getConsoleOutputRequest) {
        return null;
    }

    @Override
    public GetConsoleScreenshotResponse getConsoleScreenshot(GetConsoleScreenshotRequest getConsoleScreenshotRequest) {
        return null;
    }

    @Override
    public GetDefaultCreditSpecificationResponse getDefaultCreditSpecification(GetDefaultCreditSpecificationRequest getDefaultCreditSpecificationRequest) {
        return null;
    }

    @Override
    public GetEbsDefaultKmsKeyIdResponse getEbsDefaultKmsKeyId(GetEbsDefaultKmsKeyIdRequest getEbsDefaultKmsKeyIdRequest) {
        return null;
    }

    @Override
    public GetEbsEncryptionByDefaultResponse getEbsEncryptionByDefault(GetEbsEncryptionByDefaultRequest getEbsEncryptionByDefaultRequest) {
        return null;
    }

    @Override
    public GetFlowLogsIntegrationTemplateResponse getFlowLogsIntegrationTemplate(GetFlowLogsIntegrationTemplateRequest getFlowLogsIntegrationTemplateRequest) {
        return null;
    }

    @Override
    public GetGroupsForCapacityReservationResponse getGroupsForCapacityReservation(GetGroupsForCapacityReservationRequest getGroupsForCapacityReservationRequest) {
        return null;
    }

    @Override
    public GetHostReservationPurchasePreviewResponse getHostReservationPurchasePreview(GetHostReservationPurchasePreviewRequest getHostReservationPurchasePreviewRequest) {
        return null;
    }

    @Override
    public GetImageBlockPublicAccessStateResponse getImageBlockPublicAccessState(GetImageBlockPublicAccessStateRequest getImageBlockPublicAccessStateRequest) {
        return null;
    }

    @Override
    public GetInstanceMetadataDefaultsResponse getInstanceMetadataDefaults(GetInstanceMetadataDefaultsRequest getInstanceMetadataDefaultsRequest) {
        return null;
    }

    @Override
    public GetInstanceTpmEkPubResponse getInstanceTpmEkPub(GetInstanceTpmEkPubRequest getInstanceTpmEkPubRequest) {
        return null;
    }

    @Override
    public GetInstanceTypesFromInstanceRequirementsResponse getInstanceTypesFromInstanceRequirements(GetInstanceTypesFromInstanceRequirementsRequest getInstanceTypesFromInstanceRequirementsRequest) {
        return null;
    }

    @Override
    public GetInstanceUefiDataResponse getInstanceUefiData(GetInstanceUefiDataRequest getInstanceUefiDataRequest) {
        return null;
    }

    @Override
    public GetIpamAddressHistoryResponse getIpamAddressHistory(GetIpamAddressHistoryRequest getIpamAddressHistoryRequest) {
        return null;
    }

    @Override
    public GetIpamDiscoveredAccountsResponse getIpamDiscoveredAccounts(GetIpamDiscoveredAccountsRequest getIpamDiscoveredAccountsRequest) {
        return null;
    }

    @Override
    public GetIpamDiscoveredPublicAddressesResponse getIpamDiscoveredPublicAddresses(GetIpamDiscoveredPublicAddressesRequest getIpamDiscoveredPublicAddressesRequest) {
        return null;
    }

    @Override
    public GetIpamDiscoveredResourceCidrsResponse getIpamDiscoveredResourceCidrs(GetIpamDiscoveredResourceCidrsRequest getIpamDiscoveredResourceCidrsRequest) {
        return null;
    }

    @Override
    public GetIpamPoolAllocationsResponse getIpamPoolAllocations(GetIpamPoolAllocationsRequest getIpamPoolAllocationsRequest) {
        return null;
    }

    @Override
    public GetIpamPoolCidrsResponse getIpamPoolCidrs(GetIpamPoolCidrsRequest getIpamPoolCidrsRequest) {
        return null;
    }

    @Override
    public GetIpamResourceCidrsResponse getIpamResourceCidrs(GetIpamResourceCidrsRequest getIpamResourceCidrsRequest) {
        return null;
    }

    @Override
    public GetLaunchTemplateDataResponse getLaunchTemplateData(GetLaunchTemplateDataRequest getLaunchTemplateDataRequest) {
        return null;
    }

    @Override
    public GetManagedPrefixListAssociationsResponse getManagedPrefixListAssociations(GetManagedPrefixListAssociationsRequest getManagedPrefixListAssociationsRequest) {
        return null;
    }

    @Override
    public GetManagedPrefixListEntriesResponse getManagedPrefixListEntries(GetManagedPrefixListEntriesRequest getManagedPrefixListEntriesRequest) {
        return null;
    }

    @Override
    public GetNetworkInsightsAccessScopeAnalysisFindingsResponse getNetworkInsightsAccessScopeAnalysisFindings(GetNetworkInsightsAccessScopeAnalysisFindingsRequest getNetworkInsightsAccessScopeAnalysisFindingsRequest) {
        return null;
    }

    @Override
    public GetNetworkInsightsAccessScopeContentResponse getNetworkInsightsAccessScopeContent(GetNetworkInsightsAccessScopeContentRequest getNetworkInsightsAccessScopeContentRequest) {
        return null;
    }

    @Override
    public GetPasswordDataResponse getPasswordData(GetPasswordDataRequest getPasswordDataRequest) {
        return null;
    }

    @Override
    public GetReservedInstancesExchangeQuoteResponse getReservedInstancesExchangeQuote(GetReservedInstancesExchangeQuoteRequest getReservedInstancesExchangeQuoteRequest) {
        return null;
    }

    @Override
    public GetSecurityGroupsForVpcResponse getSecurityGroupsForVpc(GetSecurityGroupsForVpcRequest getSecurityGroupsForVpcRequest) {
        return null;
    }

    @Override
    public GetSerialConsoleAccessStatusResponse getSerialConsoleAccessStatus(GetSerialConsoleAccessStatusRequest getSerialConsoleAccessStatusRequest) {
        return null;
    }

    @Override
    public GetSnapshotBlockPublicAccessStateResponse getSnapshotBlockPublicAccessState(GetSnapshotBlockPublicAccessStateRequest getSnapshotBlockPublicAccessStateRequest) {
        return null;
    }

    @Override
    public GetSpotPlacementScoresResponse getSpotPlacementScores(GetSpotPlacementScoresRequest getSpotPlacementScoresRequest) {
        return null;
    }

    @Override
    public GetSubnetCidrReservationsResponse getSubnetCidrReservations(GetSubnetCidrReservationsRequest getSubnetCidrReservationsRequest) {
        return null;
    }

    @Override
    public GetTransitGatewayAttachmentPropagationsResponse getTransitGatewayAttachmentPropagations(GetTransitGatewayAttachmentPropagationsRequest getTransitGatewayAttachmentPropagationsRequest) {
        return null;
    }

    @Override
    public GetTransitGatewayMulticastDomainAssociationsResponse getTransitGatewayMulticastDomainAssociations(GetTransitGatewayMulticastDomainAssociationsRequest getTransitGatewayMulticastDomainAssociationsRequest) {
        return null;
    }

    @Override
    public GetTransitGatewayPolicyTableAssociationsResponse getTransitGatewayPolicyTableAssociations(GetTransitGatewayPolicyTableAssociationsRequest getTransitGatewayPolicyTableAssociationsRequest) {
        return null;
    }

    @Override
    public GetTransitGatewayPolicyTableEntriesResponse getTransitGatewayPolicyTableEntries(GetTransitGatewayPolicyTableEntriesRequest getTransitGatewayPolicyTableEntriesRequest) {
        return null;
    }

    @Override
    public GetTransitGatewayPrefixListReferencesResponse getTransitGatewayPrefixListReferences(GetTransitGatewayPrefixListReferencesRequest getTransitGatewayPrefixListReferencesRequest) {
        return null;
    }

    @Override
    public GetTransitGatewayRouteTableAssociationsResponse getTransitGatewayRouteTableAssociations(GetTransitGatewayRouteTableAssociationsRequest getTransitGatewayRouteTableAssociationsRequest) {
        return null;
    }

    @Override
    public GetTransitGatewayRouteTablePropagationsResponse getTransitGatewayRouteTablePropagations(GetTransitGatewayRouteTablePropagationsRequest getTransitGatewayRouteTablePropagationsRequest) {
        return null;
    }

    @Override
    public GetVerifiedAccessEndpointPolicyResponse getVerifiedAccessEndpointPolicy(GetVerifiedAccessEndpointPolicyRequest getVerifiedAccessEndpointPolicyRequest) {
        return null;
    }

    @Override
    public GetVerifiedAccessGroupPolicyResponse getVerifiedAccessGroupPolicy(GetVerifiedAccessGroupPolicyRequest getVerifiedAccessGroupPolicyRequest) {
        return null;
    }

    @Override
    public GetVpnConnectionDeviceSampleConfigurationResponse getVpnConnectionDeviceSampleConfiguration(GetVpnConnectionDeviceSampleConfigurationRequest getVpnConnectionDeviceSampleConfigurationRequest) {
        return null;
    }

    @Override
    public GetVpnConnectionDeviceTypesResponse getVpnConnectionDeviceTypes(GetVpnConnectionDeviceTypesRequest getVpnConnectionDeviceTypesRequest) {
        return null;
    }

    @Override
    public GetVpnTunnelReplacementStatusResponse getVpnTunnelReplacementStatus(GetVpnTunnelReplacementStatusRequest getVpnTunnelReplacementStatusRequest) {
        return null;
    }

    @Override
    public ImportClientVpnClientCertificateRevocationListResponse importClientVpnClientCertificateRevocationList(ImportClientVpnClientCertificateRevocationListRequest importClientVpnClientCertificateRevocationListRequest) {
        return null;
    }

    @Override
    public ImportImageResponse importImage(ImportImageRequest importImageRequest) {
        return null;
    }

    @Override
    public ImportImageResponse importImage(Consumer<ImportImageRequest.Builder> importImageRequest) {
        return null;
    }

    @Override
    public ImportInstanceResponse importInstance(ImportInstanceRequest importInstanceRequest) {
        return null;
    }

    @Override
    public ImportKeyPairResponse importKeyPair(ImportKeyPairRequest importKeyPairRequest) {
        return null;
    }

    @Override
    public ImportSnapshotResponse importSnapshot(ImportSnapshotRequest importSnapshotRequest) {
        return null;
    }

    @Override
    public ImportSnapshotResponse importSnapshot(Consumer<ImportSnapshotRequest.Builder> importSnapshotRequest) {
        return null;
    }

    @Override
    public ImportVolumeResponse importVolume(ImportVolumeRequest importVolumeRequest) {
        return null;
    }

    @Override
    public ListImagesInRecycleBinResponse listImagesInRecycleBin(ListImagesInRecycleBinRequest listImagesInRecycleBinRequest) {
        return null;
    }

    @Override
    public ListSnapshotsInRecycleBinResponse listSnapshotsInRecycleBin(ListSnapshotsInRecycleBinRequest listSnapshotsInRecycleBinRequest) {
        return null;
    }

    @Override
    public LockSnapshotResponse lockSnapshot(LockSnapshotRequest lockSnapshotRequest) {
        return null;
    }

    @Override
    public ModifyAddressAttributeResponse modifyAddressAttribute(ModifyAddressAttributeRequest modifyAddressAttributeRequest) {
        return null;
    }

    @Override
    public ModifyAvailabilityZoneGroupResponse modifyAvailabilityZoneGroup(ModifyAvailabilityZoneGroupRequest modifyAvailabilityZoneGroupRequest) {
        return null;
    }

    @Override
    public ModifyCapacityReservationResponse modifyCapacityReservation(ModifyCapacityReservationRequest modifyCapacityReservationRequest) {
        return null;
    }

    @Override
    public ModifyCapacityReservationFleetResponse modifyCapacityReservationFleet(ModifyCapacityReservationFleetRequest modifyCapacityReservationFleetRequest) {
        return null;
    }

    @Override
    public ModifyClientVpnEndpointResponse modifyClientVpnEndpoint(ModifyClientVpnEndpointRequest modifyClientVpnEndpointRequest) {
        return null;
    }

    @Override
    public ModifyDefaultCreditSpecificationResponse modifyDefaultCreditSpecification(ModifyDefaultCreditSpecificationRequest modifyDefaultCreditSpecificationRequest) {
        return null;
    }

    @Override
    public ModifyEbsDefaultKmsKeyIdResponse modifyEbsDefaultKmsKeyId(ModifyEbsDefaultKmsKeyIdRequest modifyEbsDefaultKmsKeyIdRequest) {
        return null;
    }

    @Override
    public ModifyFleetResponse modifyFleet(ModifyFleetRequest modifyFleetRequest) {
        return null;
    }

    @Override
    public ModifyFpgaImageAttributeResponse modifyFpgaImageAttribute(ModifyFpgaImageAttributeRequest modifyFpgaImageAttributeRequest) {
        return null;
    }

    @Override
    public ModifyHostsResponse modifyHosts(ModifyHostsRequest modifyHostsRequest) {
        return null;
    }

    @Override
    public ModifyIdFormatResponse modifyIdFormat(ModifyIdFormatRequest modifyIdFormatRequest) {
        return null;
    }

    @Override
    public ModifyIdentityIdFormatResponse modifyIdentityIdFormat(ModifyIdentityIdFormatRequest modifyIdentityIdFormatRequest) {
        return null;
    }

    @Override
    public ModifyImageAttributeResponse modifyImageAttribute(ModifyImageAttributeRequest modifyImageAttributeRequest) {
        return null;
    }

    @Override
    public ModifyInstanceAttributeResponse modifyInstanceAttribute(ModifyInstanceAttributeRequest modifyInstanceAttributeRequest) {
        return null;
    }

    @Override
    public ModifyInstanceCapacityReservationAttributesResponse modifyInstanceCapacityReservationAttributes(ModifyInstanceCapacityReservationAttributesRequest modifyInstanceCapacityReservationAttributesRequest) {
        return null;
    }

    @Override
    public ModifyInstanceCreditSpecificationResponse modifyInstanceCreditSpecification(ModifyInstanceCreditSpecificationRequest modifyInstanceCreditSpecificationRequest) {
        return null;
    }

    @Override
    public ModifyInstanceEventStartTimeResponse modifyInstanceEventStartTime(ModifyInstanceEventStartTimeRequest modifyInstanceEventStartTimeRequest) {
        return null;
    }

    @Override
    public ModifyInstanceEventWindowResponse modifyInstanceEventWindow(ModifyInstanceEventWindowRequest modifyInstanceEventWindowRequest) {
        return null;
    }

    @Override
    public ModifyInstanceMaintenanceOptionsResponse modifyInstanceMaintenanceOptions(ModifyInstanceMaintenanceOptionsRequest modifyInstanceMaintenanceOptionsRequest) {
        return null;
    }

    @Override
    public ModifyInstanceMetadataDefaultsResponse modifyInstanceMetadataDefaults(ModifyInstanceMetadataDefaultsRequest modifyInstanceMetadataDefaultsRequest) {
        return null;
    }

    @Override
    public ModifyInstanceMetadataOptionsResponse modifyInstanceMetadataOptions(ModifyInstanceMetadataOptionsRequest modifyInstanceMetadataOptionsRequest) {
        return null;
    }

    @Override
    public ModifyInstancePlacementResponse modifyInstancePlacement(ModifyInstancePlacementRequest modifyInstancePlacementRequest) {
        return null;
    }

    @Override
    public ModifyIpamResponse modifyIpam(ModifyIpamRequest modifyIpamRequest) {
        return null;
    }

    @Override
    public ModifyIpamPoolResponse modifyIpamPool(ModifyIpamPoolRequest modifyIpamPoolRequest) {
        return null;
    }

    @Override
    public ModifyIpamResourceCidrResponse modifyIpamResourceCidr(ModifyIpamResourceCidrRequest modifyIpamResourceCidrRequest) {
        return null;
    }

    @Override
    public ModifyIpamResourceDiscoveryResponse modifyIpamResourceDiscovery(ModifyIpamResourceDiscoveryRequest modifyIpamResourceDiscoveryRequest) {
        return null;
    }

    @Override
    public ModifyIpamScopeResponse modifyIpamScope(ModifyIpamScopeRequest modifyIpamScopeRequest) {
        return null;
    }

    @Override
    public ModifyLaunchTemplateResponse modifyLaunchTemplate(ModifyLaunchTemplateRequest modifyLaunchTemplateRequest) {
        return null;
    }

    @Override
    public ModifyLocalGatewayRouteResponse modifyLocalGatewayRoute(ModifyLocalGatewayRouteRequest modifyLocalGatewayRouteRequest) {
        return null;
    }

    @Override
    public ModifyManagedPrefixListResponse modifyManagedPrefixList(ModifyManagedPrefixListRequest modifyManagedPrefixListRequest) {
        return null;
    }

    @Override
    public ModifyNetworkInterfaceAttributeResponse modifyNetworkInterfaceAttribute(ModifyNetworkInterfaceAttributeRequest modifyNetworkInterfaceAttributeRequest) {
        return null;
    }

    @Override
    public ModifyPrivateDnsNameOptionsResponse modifyPrivateDnsNameOptions(ModifyPrivateDnsNameOptionsRequest modifyPrivateDnsNameOptionsRequest) {
        return null;
    }

    @Override
    public ModifyReservedInstancesResponse modifyReservedInstances(ModifyReservedInstancesRequest modifyReservedInstancesRequest) {
        return null;
    }

    @Override
    public ModifySecurityGroupRulesResponse modifySecurityGroupRules(ModifySecurityGroupRulesRequest modifySecurityGroupRulesRequest) {
        return null;
    }

    @Override
    public ModifySnapshotAttributeResponse modifySnapshotAttribute(ModifySnapshotAttributeRequest modifySnapshotAttributeRequest) {
        return null;
    }

    @Override
    public ModifySnapshotTierResponse modifySnapshotTier(ModifySnapshotTierRequest modifySnapshotTierRequest) {
        return null;
    }

    @Override
    public ModifySpotFleetRequestResponse modifySpotFleetRequest(ModifySpotFleetRequestRequest modifySpotFleetRequestRequest) {
        return null;
    }

    @Override
    public ModifySubnetAttributeResponse modifySubnetAttribute(ModifySubnetAttributeRequest modifySubnetAttributeRequest) {
        return null;
    }

    @Override
    public ModifyTrafficMirrorFilterNetworkServicesResponse modifyTrafficMirrorFilterNetworkServices(ModifyTrafficMirrorFilterNetworkServicesRequest modifyTrafficMirrorFilterNetworkServicesRequest) {
        return null;
    }

    @Override
    public ModifyTrafficMirrorFilterRuleResponse modifyTrafficMirrorFilterRule(ModifyTrafficMirrorFilterRuleRequest modifyTrafficMirrorFilterRuleRequest) {
        return null;
    }

    @Override
    public ModifyTrafficMirrorSessionResponse modifyTrafficMirrorSession(ModifyTrafficMirrorSessionRequest modifyTrafficMirrorSessionRequest) {
        return null;
    }

    @Override
    public ModifyTransitGatewayResponse modifyTransitGateway(ModifyTransitGatewayRequest modifyTransitGatewayRequest) {
        return null;
    }

    @Override
    public ModifyTransitGatewayPrefixListReferenceResponse modifyTransitGatewayPrefixListReference(ModifyTransitGatewayPrefixListReferenceRequest modifyTransitGatewayPrefixListReferenceRequest) {
        return null;
    }

    @Override
    public ModifyTransitGatewayVpcAttachmentResponse modifyTransitGatewayVpcAttachment(ModifyTransitGatewayVpcAttachmentRequest modifyTransitGatewayVpcAttachmentRequest) {
        return null;
    }

    @Override
    public ModifyVerifiedAccessEndpointResponse modifyVerifiedAccessEndpoint(ModifyVerifiedAccessEndpointRequest modifyVerifiedAccessEndpointRequest) {
        return null;
    }

    @Override
    public ModifyVerifiedAccessEndpointPolicyResponse modifyVerifiedAccessEndpointPolicy(ModifyVerifiedAccessEndpointPolicyRequest modifyVerifiedAccessEndpointPolicyRequest) {
        return null;
    }

    @Override
    public ModifyVerifiedAccessGroupResponse modifyVerifiedAccessGroup(ModifyVerifiedAccessGroupRequest modifyVerifiedAccessGroupRequest) {
        return null;
    }

    @Override
    public ModifyVerifiedAccessGroupPolicyResponse modifyVerifiedAccessGroupPolicy(ModifyVerifiedAccessGroupPolicyRequest modifyVerifiedAccessGroupPolicyRequest) {
        return null;
    }

    @Override
    public ModifyVerifiedAccessInstanceResponse modifyVerifiedAccessInstance(ModifyVerifiedAccessInstanceRequest modifyVerifiedAccessInstanceRequest) {
        return null;
    }

    @Override
    public ModifyVerifiedAccessInstanceLoggingConfigurationResponse modifyVerifiedAccessInstanceLoggingConfiguration(ModifyVerifiedAccessInstanceLoggingConfigurationRequest modifyVerifiedAccessInstanceLoggingConfigurationRequest) {
        return null;
    }

    @Override
    public ModifyVerifiedAccessTrustProviderResponse modifyVerifiedAccessTrustProvider(ModifyVerifiedAccessTrustProviderRequest modifyVerifiedAccessTrustProviderRequest) {
        return null;
    }

    @Override
    public ModifyVolumeResponse modifyVolume(ModifyVolumeRequest modifyVolumeRequest) {
        return null;
    }

    @Override
    public ModifyVolumeAttributeResponse modifyVolumeAttribute(ModifyVolumeAttributeRequest modifyVolumeAttributeRequest) {
        return null;
    }

    @Override
    public ModifyVpcAttributeResponse modifyVpcAttribute(ModifyVpcAttributeRequest modifyVpcAttributeRequest) {
        return null;
    }

    @Override
    public ModifyVpcEndpointResponse modifyVpcEndpoint(ModifyVpcEndpointRequest modifyVpcEndpointRequest) {
        return null;
    }

    @Override
    public ModifyVpcEndpointConnectionNotificationResponse modifyVpcEndpointConnectionNotification(ModifyVpcEndpointConnectionNotificationRequest modifyVpcEndpointConnectionNotificationRequest) {
        return null;
    }

    @Override
    public ModifyVpcEndpointServiceConfigurationResponse modifyVpcEndpointServiceConfiguration(ModifyVpcEndpointServiceConfigurationRequest modifyVpcEndpointServiceConfigurationRequest) {
        return null;
    }

    @Override
    public ModifyVpcEndpointServicePayerResponsibilityResponse modifyVpcEndpointServicePayerResponsibility(ModifyVpcEndpointServicePayerResponsibilityRequest modifyVpcEndpointServicePayerResponsibilityRequest) {
        return null;
    }

    @Override
    public ModifyVpcEndpointServicePermissionsResponse modifyVpcEndpointServicePermissions(ModifyVpcEndpointServicePermissionsRequest modifyVpcEndpointServicePermissionsRequest) {
        return null;
    }

    @Override
    public ModifyVpcPeeringConnectionOptionsResponse modifyVpcPeeringConnectionOptions(ModifyVpcPeeringConnectionOptionsRequest modifyVpcPeeringConnectionOptionsRequest) {
        return null;
    }

    @Override
    public ModifyVpcTenancyResponse modifyVpcTenancy(ModifyVpcTenancyRequest modifyVpcTenancyRequest) {
        return null;
    }

    @Override
    public ModifyVpnConnectionResponse modifyVpnConnection(ModifyVpnConnectionRequest modifyVpnConnectionRequest) {
        return null;
    }

    @Override
    public ModifyVpnConnectionOptionsResponse modifyVpnConnectionOptions(ModifyVpnConnectionOptionsRequest modifyVpnConnectionOptionsRequest) {
        return null;
    }

    @Override
    public ModifyVpnTunnelCertificateResponse modifyVpnTunnelCertificate(ModifyVpnTunnelCertificateRequest modifyVpnTunnelCertificateRequest) {
        return null;
    }

    @Override
    public ModifyVpnTunnelOptionsResponse modifyVpnTunnelOptions(ModifyVpnTunnelOptionsRequest modifyVpnTunnelOptionsRequest) {
        return null;
    }

    @Override
    public MonitorInstancesResponse monitorInstances(MonitorInstancesRequest monitorInstancesRequest) {
        return null;
    }

    @Override
    public MoveAddressToVpcResponse moveAddressToVpc(MoveAddressToVpcRequest moveAddressToVpcRequest) {
        return null;
    }

    @Override
    public MoveByoipCidrToIpamResponse moveByoipCidrToIpam(MoveByoipCidrToIpamRequest moveByoipCidrToIpamRequest) {
        return null;
    }

    @Override
    public ProvisionByoipCidrResponse provisionByoipCidr(ProvisionByoipCidrRequest provisionByoipCidrRequest) {
        return null;
    }

    @Override
    public ProvisionIpamByoasnResponse provisionIpamByoasn(ProvisionIpamByoasnRequest provisionIpamByoasnRequest) {
        return null;
    }

    @Override
    public ProvisionIpamPoolCidrResponse provisionIpamPoolCidr(ProvisionIpamPoolCidrRequest provisionIpamPoolCidrRequest) {
        return null;
    }

    @Override
    public ProvisionPublicIpv4PoolCidrResponse provisionPublicIpv4PoolCidr(ProvisionPublicIpv4PoolCidrRequest provisionPublicIpv4PoolCidrRequest) {
        return null;
    }

    @Override
    public PurchaseCapacityBlockResponse purchaseCapacityBlock(PurchaseCapacityBlockRequest purchaseCapacityBlockRequest) {
        return null;
    }

    @Override
    public PurchaseHostReservationResponse purchaseHostReservation(PurchaseHostReservationRequest purchaseHostReservationRequest) {
        return null;
    }

    @Override
    public PurchaseReservedInstancesOfferingResponse purchaseReservedInstancesOffering(PurchaseReservedInstancesOfferingRequest purchaseReservedInstancesOfferingRequest) {
        return null;
    }

    @Override
    public PurchaseScheduledInstancesResponse purchaseScheduledInstances(PurchaseScheduledInstancesRequest purchaseScheduledInstancesRequest) {
        return null;
    }

    @Override
    public RebootInstancesResponse rebootInstances(RebootInstancesRequest rebootInstancesRequest) {
        return null;
    }

    @Override
    public RegisterImageResponse registerImage(RegisterImageRequest registerImageRequest) {
        return null;
    }

    @Override
    public RegisterInstanceEventNotificationAttributesResponse registerInstanceEventNotificationAttributes(RegisterInstanceEventNotificationAttributesRequest registerInstanceEventNotificationAttributesRequest) {
        return null;
    }

    @Override
    public RegisterTransitGatewayMulticastGroupMembersResponse registerTransitGatewayMulticastGroupMembers(RegisterTransitGatewayMulticastGroupMembersRequest registerTransitGatewayMulticastGroupMembersRequest) {
        return null;
    }

    @Override
    public RegisterTransitGatewayMulticastGroupSourcesResponse registerTransitGatewayMulticastGroupSources(RegisterTransitGatewayMulticastGroupSourcesRequest registerTransitGatewayMulticastGroupSourcesRequest) {
        return null;
    }

    @Override
    public RejectTransitGatewayMulticastDomainAssociationsResponse rejectTransitGatewayMulticastDomainAssociations(RejectTransitGatewayMulticastDomainAssociationsRequest rejectTransitGatewayMulticastDomainAssociationsRequest) {
        return null;
    }

    @Override
    public RejectTransitGatewayPeeringAttachmentResponse rejectTransitGatewayPeeringAttachment(RejectTransitGatewayPeeringAttachmentRequest rejectTransitGatewayPeeringAttachmentRequest) {
        return null;
    }

    @Override
    public RejectTransitGatewayVpcAttachmentResponse rejectTransitGatewayVpcAttachment(RejectTransitGatewayVpcAttachmentRequest rejectTransitGatewayVpcAttachmentRequest) {
        return null;
    }

    @Override
    public RejectVpcEndpointConnectionsResponse rejectVpcEndpointConnections(RejectVpcEndpointConnectionsRequest rejectVpcEndpointConnectionsRequest) {
        return null;
    }

    @Override
    public RejectVpcPeeringConnectionResponse rejectVpcPeeringConnection(RejectVpcPeeringConnectionRequest rejectVpcPeeringConnectionRequest) {
        return null;
    }

    @Override
    public ReleaseAddressResponse releaseAddress(ReleaseAddressRequest releaseAddressRequest) {
        return null;
    }

    @Override
    public ReleaseHostsResponse releaseHosts(ReleaseHostsRequest releaseHostsRequest) {
        return null;
    }

    @Override
    public ReleaseIpamPoolAllocationResponse releaseIpamPoolAllocation(ReleaseIpamPoolAllocationRequest releaseIpamPoolAllocationRequest) {
        return null;
    }

    @Override
    public ReplaceIamInstanceProfileAssociationResponse replaceIamInstanceProfileAssociation(ReplaceIamInstanceProfileAssociationRequest replaceIamInstanceProfileAssociationRequest) {
        return null;
    }

    @Override
    public ReplaceNetworkAclAssociationResponse replaceNetworkAclAssociation(ReplaceNetworkAclAssociationRequest replaceNetworkAclAssociationRequest) {
        return null;
    }

    @Override
    public ReplaceNetworkAclEntryResponse replaceNetworkAclEntry(ReplaceNetworkAclEntryRequest replaceNetworkAclEntryRequest) {
        return null;
    }

    @Override
    public ReplaceRouteResponse replaceRoute(ReplaceRouteRequest replaceRouteRequest) {
        return null;
    }

    @Override
    public ReplaceRouteTableAssociationResponse replaceRouteTableAssociation(ReplaceRouteTableAssociationRequest replaceRouteTableAssociationRequest) {
        return null;
    }

    @Override
    public ReplaceTransitGatewayRouteResponse replaceTransitGatewayRoute(ReplaceTransitGatewayRouteRequest replaceTransitGatewayRouteRequest) {
        return null;
    }

    @Override
    public ReplaceVpnTunnelResponse replaceVpnTunnel(ReplaceVpnTunnelRequest replaceVpnTunnelRequest) {
        return null;
    }

    @Override
    public ReportInstanceStatusResponse reportInstanceStatus(ReportInstanceStatusRequest reportInstanceStatusRequest) {
        return null;
    }

    @Override
    public RequestSpotFleetResponse requestSpotFleet(RequestSpotFleetRequest requestSpotFleetRequest) {
        return null;
    }

    @Override
    public RequestSpotInstancesResponse requestSpotInstances(RequestSpotInstancesRequest requestSpotInstancesRequest) {
        return null;
    }

    @Override
    public ResetAddressAttributeResponse resetAddressAttribute(ResetAddressAttributeRequest resetAddressAttributeRequest) {
        return null;
    }

    @Override
    public ResetEbsDefaultKmsKeyIdResponse resetEbsDefaultKmsKeyId(ResetEbsDefaultKmsKeyIdRequest resetEbsDefaultKmsKeyIdRequest) {
        return null;
    }

    @Override
    public ResetFpgaImageAttributeResponse resetFpgaImageAttribute(ResetFpgaImageAttributeRequest resetFpgaImageAttributeRequest) {
        return null;
    }

    @Override
    public ResetImageAttributeResponse resetImageAttribute(ResetImageAttributeRequest resetImageAttributeRequest) {
        return null;
    }

    @Override
    public ResetInstanceAttributeResponse resetInstanceAttribute(ResetInstanceAttributeRequest resetInstanceAttributeRequest) {
        return null;
    }

    @Override
    public ResetNetworkInterfaceAttributeResponse resetNetworkInterfaceAttribute(ResetNetworkInterfaceAttributeRequest resetNetworkInterfaceAttributeRequest) {
        return null;
    }

    @Override
    public ResetSnapshotAttributeResponse resetSnapshotAttribute(ResetSnapshotAttributeRequest resetSnapshotAttributeRequest) {
        return null;
    }

    @Override
    public RestoreAddressToClassicResponse restoreAddressToClassic(RestoreAddressToClassicRequest restoreAddressToClassicRequest) {
        return null;
    }

    @Override
    public RestoreImageFromRecycleBinResponse restoreImageFromRecycleBin(RestoreImageFromRecycleBinRequest restoreImageFromRecycleBinRequest) {
        return null;
    }

    @Override
    public RestoreManagedPrefixListVersionResponse restoreManagedPrefixListVersion(RestoreManagedPrefixListVersionRequest restoreManagedPrefixListVersionRequest) {
        return null;
    }

    @Override
    public RestoreSnapshotFromRecycleBinResponse restoreSnapshotFromRecycleBin(RestoreSnapshotFromRecycleBinRequest restoreSnapshotFromRecycleBinRequest) {
        return null;
    }

    @Override
    public RestoreSnapshotTierResponse restoreSnapshotTier(RestoreSnapshotTierRequest restoreSnapshotTierRequest) {
        return null;
    }

    @Override
    public RevokeClientVpnIngressResponse revokeClientVpnIngress(RevokeClientVpnIngressRequest revokeClientVpnIngressRequest) {
        return null;
    }

    @Override
    public RevokeSecurityGroupEgressResponse revokeSecurityGroupEgress(RevokeSecurityGroupEgressRequest revokeSecurityGroupEgressRequest) {
        return null;
    }

    @Override
    public RevokeSecurityGroupIngressResponse revokeSecurityGroupIngress(RevokeSecurityGroupIngressRequest revokeSecurityGroupIngressRequest) {
        return null;
    }

    @Override
    public RevokeSecurityGroupIngressResponse revokeSecurityGroupIngress(Consumer<RevokeSecurityGroupIngressRequest.Builder> revokeSecurityGroupIngressRequest) {
        return null;
    }

    @Override
    public RunInstancesResponse runInstances(RunInstancesRequest runInstancesRequest) {
        return null;
    }

    @Override
    public RunScheduledInstancesResponse runScheduledInstances(RunScheduledInstancesRequest runScheduledInstancesRequest) {
        return null;
    }

    @Override
    public SearchLocalGatewayRoutesResponse searchLocalGatewayRoutes(SearchLocalGatewayRoutesRequest searchLocalGatewayRoutesRequest) {
        return null;
    }

    @Override
    public SearchTransitGatewayMulticastGroupsResponse searchTransitGatewayMulticastGroups(SearchTransitGatewayMulticastGroupsRequest searchTransitGatewayMulticastGroupsRequest) {
        return null;
    }

    @Override
    public SearchTransitGatewayRoutesResponse searchTransitGatewayRoutes(SearchTransitGatewayRoutesRequest searchTransitGatewayRoutesRequest) {
        return null;
    }

    @Override
    public SendDiagnosticInterruptResponse sendDiagnosticInterrupt(SendDiagnosticInterruptRequest sendDiagnosticInterruptRequest) {
        return null;
    }

    @Override
    public StartInstancesResponse startInstances(StartInstancesRequest startInstancesRequest) {
        return null;
    }

    @Override
    public StartNetworkInsightsAccessScopeAnalysisResponse startNetworkInsightsAccessScopeAnalysis(StartNetworkInsightsAccessScopeAnalysisRequest startNetworkInsightsAccessScopeAnalysisRequest) {
        return null;
    }

    @Override
    public StartNetworkInsightsAnalysisResponse startNetworkInsightsAnalysis(StartNetworkInsightsAnalysisRequest startNetworkInsightsAnalysisRequest) {
        return null;
    }

    @Override
    public StartVpcEndpointServicePrivateDnsVerificationResponse startVpcEndpointServicePrivateDnsVerification(StartVpcEndpointServicePrivateDnsVerificationRequest startVpcEndpointServicePrivateDnsVerificationRequest) {
        return null;
    }

    @Override
    public StopInstancesResponse stopInstances(StopInstancesRequest stopInstancesRequest) {
        return null;
    }

    @Override
    public TerminateClientVpnConnectionsResponse terminateClientVpnConnections(TerminateClientVpnConnectionsRequest terminateClientVpnConnectionsRequest) {
        return null;
    }

    @Override
    public TerminateInstancesResponse terminateInstances(TerminateInstancesRequest terminateInstancesRequest) {
        return null;
    }

    @Override
    public UnassignIpv6AddressesResponse unassignIpv6Addresses(UnassignIpv6AddressesRequest unassignIpv6AddressesRequest) {
        return null;
    }

    @Override
    public UnassignPrivateIpAddressesResponse unassignPrivateIpAddresses(UnassignPrivateIpAddressesRequest unassignPrivateIpAddressesRequest) {
        return null;
    }

    @Override
    public UnassignPrivateNatGatewayAddressResponse unassignPrivateNatGatewayAddress(UnassignPrivateNatGatewayAddressRequest unassignPrivateNatGatewayAddressRequest) {
        return null;
    }

    @Override
    public UnlockSnapshotResponse unlockSnapshot(UnlockSnapshotRequest unlockSnapshotRequest) {
        return null;
    }

    @Override
    public UnmonitorInstancesResponse unmonitorInstances(UnmonitorInstancesRequest unmonitorInstancesRequest) {
        return null;
    }

    @Override
    public UpdateSecurityGroupRuleDescriptionsEgressResponse updateSecurityGroupRuleDescriptionsEgress(UpdateSecurityGroupRuleDescriptionsEgressRequest updateSecurityGroupRuleDescriptionsEgressRequest) {
        return null;
    }

    @Override
    public UpdateSecurityGroupRuleDescriptionsIngressResponse updateSecurityGroupRuleDescriptionsIngress(UpdateSecurityGroupRuleDescriptionsIngressRequest updateSecurityGroupRuleDescriptionsIngressRequest) {
        return null;
    }

    @Override
    public WithdrawByoipCidrResponse withdrawByoipCidr(WithdrawByoipCidrRequest withdrawByoipCidrRequest) {
        return null;
    }


    @Override
    public Ec2Waiter waiter() {
        return null;
    }

    @Override
    public String serviceName() {
        return "";
    }

    @Override
    public void close() {

    }
}
