package org.wso2.data.migration_570;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.claim.metadata.mgt.stub.ClaimMetadataManagementServiceClaimMetadataException;
import org.wso2.carbon.identity.claim.metadata.mgt.stub.ClaimMetadataManagementServiceStub;
import org.wso2.carbon.identity.claim.metadata.mgt.stub.dto.AttributeMappingDTO;
import org.wso2.carbon.identity.claim.metadata.mgt.stub.dto.ClaimPropertyDTO;
import org.wso2.carbon.identity.claim.metadata.mgt.stub.dto.LocalClaimDTO;
import org.wso2.data.migration.common.DataMigrationException;
import org.wso2.data.migration.common.DataMigrationUtil;
import org.wso2.data.migration.common.claim.DomainClaimAttribute;
import org.wso2.data.migration.common.claim.LocalClaimMetaData;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import static org.wso2.data.migration.common.DataMigrationConstants.*;

public class ClaimData570 {

    private static Logger logger = LoggerFactory.getLogger(ClaimData570.class);

    private static ClaimData570 claimData570;
    private static ClaimMetadataManagementServiceStub claimMetadataMgtServiceStub;
    private static List<String> localClaimUris = new ArrayList<>();

    private ClaimData570() {

    }

    private ClaimData570(String hostname, String port, String trustStorePath, String trustStorePassword,
                         String adminUsername, String adminPassword) {
        claimMetadataMgtServiceStub = getClaimMetadataMgtService(hostname, port, trustStorePath,
                trustStorePassword, adminUsername, adminPassword);
    }

    public static ClaimData570 getInstance(String hostname, String port, String trustStorePath,
                                           String trustStorePassword, String adminUsername,
                                           String adminPassword) {
        if (claimData570 == null) {
            synchronized (ClaimData570.class) {
                if (claimData570 == null) {
                    claimData570 = new ClaimData570(hostname, port, trustStorePath, trustStorePassword,
                            adminUsername, adminPassword);
                }
            }
        }
        return claimData570;
    }

    public List<String> getAllLocalClaimUris() throws DataMigrationException {

        logger.info("Retrieving all the Local Claims of IS 5.1.0.");

        LocalClaimDTO[] localClaimDTOS;
        if (claimMetadataMgtServiceStub == null) {
            throw new DataMigrationException("Couldn't find Claim Meta data Management service stub");
        }
        try {
            localClaimDTOS = claimMetadataMgtServiceStub.getLocalClaims();
        } catch (RemoteException | ClaimMetadataManagementServiceClaimMetadataException e) {
            throw new DataMigrationException("Error in retrieving all the local claims in IS 5.7.0", e);
        }

        for (LocalClaimDTO claim : localClaimDTOS) {
            localClaimUris.add(claim.getLocalClaimURI());
        }
        return localClaimUris;
    }

    public void createLocalClaimsInSPConfig(List<LocalClaimMetaData> spLocalClaims) throws DataMigrationException {

        logger.info("Creating Missing Local Claims configured in SP.");

        if (CollectionUtils.isEmpty(localClaimUris)) {
            getAllLocalClaimUris();
        }

        if (spLocalClaims != null) {
            for (LocalClaimMetaData spLocalClaim : spLocalClaims) {
                if (!localClaimUris.contains(spLocalClaim.getClaimUri())) {
                    if (claimMetadataMgtServiceStub == null) {
                        throw new DataMigrationException("Couldn't find Claim Meta data Management service stub");
                    }

                    LocalClaimDTO claimDTO570 = new LocalClaimDTO();
                    claimDTO570.setLocalClaimURI(spLocalClaim.getClaimUri());

                    for (DomainClaimAttribute claimAttribute : spLocalClaim.getMappedAttributes()) {
                        claimDTO570.addAttributeMappings(getAttributeMapping(claimAttribute.getAttributeName(), claimAttribute.getDomainName()));
                    }

                    List<ClaimPropertyDTO> claimProperties = new ArrayList<>();
                    claimProperties.add(getClaimProperty(DISPLAY_NAME_PROPERTY_570, spLocalClaim.getDisplayName()));
                    claimProperties.add(getClaimProperty(DESCRIPTION_PROPERTY_570, spLocalClaim.getDescription()));
                    claimProperties.add(getClaimProperty(DISPLAY_ORDER_PROPERTY_570, spLocalClaim.getDisplayOrder()));
                    claimProperties.add(getClaimProperty(READ_ONLY_PROPERTY_570, spLocalClaim.isReadOnly()));
                    claimProperties.add(getClaimProperty(REQUIRED_PROPERTY_570, spLocalClaim.isRequired()));
                    claimProperties.add(getClaimProperty(SUPPORTED_BY_DEFAULT_PROPERTY_570, spLocalClaim.isSupportedByDefault()));
                    if (spLocalClaim.getRegEx() != null) {
                        claimProperties.add(getClaimProperty(REGULAR_EXPRESSION_PROPERTY_570, spLocalClaim.getRegEx()));
                    }
                    claimDTO570.setClaimProperties(claimProperties.toArray(new ClaimPropertyDTO[claimProperties.size()]));

                    try {
                        claimMetadataMgtServiceStub.addLocalClaim(claimDTO570);
                    } catch (RemoteException | ClaimMetadataManagementServiceClaimMetadataException e) {
                        throw new DataMigrationException("Error in adding local claim : " + spLocalClaim.getClaimUri() +
                                " in IS 5.7.0", e);
                    }
                    localClaimUris.add(spLocalClaim.getClaimUri());
                }
            }
        }
    }

    private AttributeMappingDTO getAttributeMapping(String attribute, String domain) {
        AttributeMappingDTO attributeMappingDTO = new AttributeMappingDTO();
        attributeMappingDTO.setAttributeName(attribute);
        attributeMappingDTO.setUserStoreDomain(domain);
        return attributeMappingDTO;
    }

    private ClaimPropertyDTO getClaimProperty(String propName, String propValue) {
        ClaimPropertyDTO claimProperty = new ClaimPropertyDTO();
        claimProperty.setPropertyName(propName);
        claimProperty.setPropertyValue(propValue);
        return claimProperty;
    }

    private ClaimMetadataManagementServiceStub getClaimMetadataMgtService(String hostName,
                                                                          String port,
                                                                          String trustStorePath,
                                                                          String trustStorePassword,
                                                                          String adminUsername,
                                                                          String adminPassword) {

        DataMigrationUtil.setTrustStoreForSSL(trustStorePath, trustStorePassword);
        ConfigurationContext configContext = null;
        String serviceEndPoint = "https://" + hostName + ":" + port + "/services/ClaimMetadataManagementService";
        try {
            configContext = ConfigurationContextFactory
                    .createConfigurationContextFromFileSystem(null, null);
        } catch (AxisFault axisFault) {
            logger.error("Unable to create Configuration Context");
        }

        try {
            claimMetadataMgtServiceStub = new ClaimMetadataManagementServiceStub(configContext, serviceEndPoint);
        } catch (AxisFault axisFault) {
            logger.error("Unable to instantiate admin Service stub of " +
                    "IdentityApplicationManagementService");
        }
        ServiceClient client = claimMetadataMgtServiceStub._getServiceClient();
        DataMigrationUtil.authenticate(client, adminUsername, adminPassword);
        return claimMetadataMgtServiceStub;
    }
}