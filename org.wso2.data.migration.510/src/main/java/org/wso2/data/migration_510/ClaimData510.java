package org.wso2.data.migration_510;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.claim.mgt.stub.ClaimManagementServiceException;
import org.wso2.carbon.claim.mgt.stub.ClaimManagementServiceStub;
import org.wso2.carbon.claim.mgt.stub.dto.ClaimDTO;
import org.wso2.carbon.claim.mgt.stub.dto.ClaimDialectDTO;
import org.wso2.carbon.claim.mgt.stub.dto.ClaimMappingDTO;
import org.wso2.carbon.identity.application.common.model.xsd.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.xsd.ServiceProvider;
import org.wso2.data.migration.common.DataMigrationConstants;
import org.wso2.data.migration.common.DataMigrationException;
import org.wso2.data.migration.common.DataMigrationUtil;
import org.wso2.data.migration.common.claim.DomainClaimAttribute;
import org.wso2.data.migration.common.claim.LocalClaimMetaData;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClaimData510 {

    private static Logger logger = LoggerFactory.getLogger(ClaimData510.class);

    private static ClaimData510 claimData510;
    private static ClaimManagementServiceStub claimMgtServiceStub;
    private static Map<String, LocalClaimMetaData> localClaimMap = new HashMap<>();

    private ClaimData510() {

    }

    private ClaimData510(String hostname, String port, String trustStorePath, String trustStorePassword,
                         String adminUsername, String adminPassword) {
        claimMgtServiceStub = getClaimManagementService(hostname, port, trustStorePath,
                trustStorePassword, adminUsername, adminPassword);
    }

    public static ClaimData510 getInstance(String hostname, String port, String trustStorePath,
                                           String trustStorePassword, String adminUsername,
                                           String adminPassword) {
        if (claimData510 == null) {
            synchronized (ClaimData510.class) {
                if (claimData510 == null) {
                    claimData510 = new ClaimData510(hostname, port, trustStorePath, trustStorePassword,
                            adminUsername, adminPassword);
                }
            }
        }
        return claimData510;
    }

    public Map<String, LocalClaimMetaData> getAllLocalClaims() throws DataMigrationException {

        logger.info("Retrieving all the Local Claims of IS 5.1.0.");

        ClaimDialectDTO claimDialectDTO;
        if (claimMgtServiceStub == null) {
            throw new DataMigrationException("Couldn't find Claim Management service stub");
        }
        try {
            claimDialectDTO = claimMgtServiceStub.getClaimMappingByDialect(DataMigrationConstants.LOCAL_CLAIM_DIALECT);
        } catch (RemoteException | ClaimManagementServiceException e) {
            throw new DataMigrationException("Error in retrieving all the local claims in IS 5.1.0", e);
        }

        for (ClaimMappingDTO claim : claimDialectDTO.getClaimMappings()) {
            LocalClaimMetaData claimDTO = new LocalClaimMetaData();
            if (ArrayUtils.isEmpty(claim.getMappedAttributes())) {

            }
            claimDTO.addMappedAttribute(new DomainClaimAttribute(claim.getMappedAttribute(), "PRIMARY"));

            ClaimDTO claimMetadata = claim.getClaim();
            claimDTO.setClaimUri(claimMetadata.getClaimUri());
            claimDTO.setDisplayName(claimMetadata.getDisplayTag());
            claimDTO.setDescription(claimMetadata.getDescription());
            claimDTO.setDisplayOrder(String.valueOf(claimMetadata.getDisplayOrder()));
            claimDTO.setReadOnly(String.valueOf(claimMetadata.getReadOnly()));
            claimDTO.setRequired(String.valueOf(claimMetadata.getRequired()));
            claimDTO.setRegEx(claimMetadata.getRegEx());
            claimDTO.setSupportedByDefault(String.valueOf(claimMetadata.getSupportedByDefault()));
            claimDTO.setValue(claimMetadata.getValue());
            claimDTO.setCheckedAttribute(String.valueOf(claimMetadata.getCheckedAttribute()));
            localClaimMap.put(claimMetadata.getClaimUri(), claimDTO);
        }
        return localClaimMap;
    }

    public List<LocalClaimMetaData> getLocalClaimsInSPConfig(String serviceProviderInJson) throws DataMigrationException {

        List<LocalClaimMetaData> spLocalClaims = new ArrayList<>();
        ServiceProvider serviceProvider = (ServiceProvider) DataMigrationUtil.getObjectFromJson(serviceProviderInJson, ServiceProvider.class);
        if (MapUtils.isEmpty(localClaimMap)) {
            getAllLocalClaims();
        }

        if (serviceProvider.getClaimConfig() != null && serviceProvider.getClaimConfig().getClaimMappings() != null) {
            for (ClaimMapping claimMapping : serviceProvider.getClaimConfig().getClaimMappings()) {
                spLocalClaims.add(localClaimMap.get(claimMapping.getLocalClaim().getClaimUri()));
            }
        }
        return spLocalClaims;
    }

    private ClaimManagementServiceStub getClaimManagementService(String hostName,
                                                                 String port,
                                                                 String trustStorePath,
                                                                 String trustStorePassword,
                                                                 String adminUsername,
                                                                 String adminPassword) {

        DataMigrationUtil.setTrustStoreForSSL(trustStorePath, trustStorePassword);
        ConfigurationContext configContext = null;
        String serviceEndPoint = "https://" + hostName + ":" + port + "/services/ClaimManagementService";
        try {
            configContext = ConfigurationContextFactory
                    .createConfigurationContextFromFileSystem(null, null);
        } catch (AxisFault axisFault) {
            logger.error("Unable to create Configuration Context", axisFault);
        }

        try {
            claimMgtServiceStub = new ClaimManagementServiceStub(configContext, serviceEndPoint);
        } catch (AxisFault axisFault) {
            logger.error("Unable to instantiate admin Service stub of " +
                    "IdentityApplicationManagementService", axisFault);
        }
        ServiceClient client = claimMgtServiceStub._getServiceClient();
        DataMigrationUtil.authenticate(client, adminUsername, adminPassword);
        return claimMgtServiceStub;
    }
}