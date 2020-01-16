package org.wso2.data.migration_510;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.application.common.model.xsd.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.xsd.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.stub.IdentityApplicationManagementServiceIdentityApplicationManagementException;
import org.wso2.carbon.identity.application.mgt.stub.IdentityApplicationManagementServiceStub;
import org.wso2.carbon.identity.sso.saml.stub.IdentitySAMLSSOConfigServiceIdentityException;
import org.wso2.carbon.identity.sso.saml.stub.IdentitySAMLSSOConfigServiceStub;
import org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSOServiceProviderDTO;
import org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSOServiceProviderInfoDTO;
import org.wso2.data.migration.common.DataMigrationException;
import org.wso2.data.migration.common.DataMigrationUtil;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SPData510 {

    private static Logger logger = LoggerFactory.getLogger(SPData510.class);

    private static SPData510 spData510;
    private static IdentityApplicationManagementServiceStub applicationMgtServiceStub;
    private static IdentitySAMLSSOConfigServiceStub samlConfigServiceStub;

    private SPData510() {

    }

    private SPData510(String hostname, String port, String trustStorePath, String trustStorePassword,
                      String adminUsername, String adminPassword) {
        applicationMgtServiceStub = getIdentityApplicationManagementService(hostname, port, trustStorePath,
                trustStorePassword, adminUsername, adminPassword);
        samlConfigServiceStub = getSAMLSSOConfigService(hostname, port, trustStorePath, trustStorePassword,
                adminUsername, adminPassword);
    }

    public static SPData510 getInstance(String hostname, String port, String trustStorePath,
                                        String trustStorePassword, String adminUsername,
                                        String adminPassword) {
        if (spData510 == null) {
            synchronized (SPData510.class) {
                if (spData510 == null) {
                    spData510 = new SPData510(hostname, port, trustStorePath, trustStorePassword,
                            adminUsername, adminPassword);
                }
            }
        }
        return spData510;
    }

    public List<String> getAllServiceProviderNames() throws DataMigrationException {

        logger.info("Retrieving all the SAML Service Provider names of IS 5.1.0.");

        ApplicationBasicInfo[] applicationBasicInfos;
        List<String> appNames = new ArrayList<>();

        if (applicationMgtServiceStub == null) {
            throw new DataMigrationException("Couldn't find Application Management service stub");
        }
        try {
            applicationBasicInfos = applicationMgtServiceStub.getAllApplicationBasicInfo();
        } catch (RemoteException | IdentityApplicationManagementServiceIdentityApplicationManagementException e) {
            throw new DataMigrationException("Error in retrieving all the application basic information in IS 5.1.0", e);
        }

        for (ApplicationBasicInfo info : applicationBasicInfos) {
            appNames.add(info.getApplicationName());
        }
        return appNames;
    }

    public String getServiceProvider(String appName) throws DataMigrationException {

        logger.info("Retrieving the SAML Service Provider : " + appName);

        ServiceProvider serviceProvider;
        if (applicationMgtServiceStub == null) {
            throw new DataMigrationException("Couldn't find Application Management service stub");
        }
        try {
            serviceProvider = applicationMgtServiceStub.getApplication(appName);
        } catch (RemoteException | IdentityApplicationManagementServiceIdentityApplicationManagementException e) {
            throw new DataMigrationException("Error in retrieving the Service provider for SP name : " + appName + " in IS 5.1.0", e);
        }
        return DataMigrationUtil.getObjectInJson(serviceProvider);
    }

    public Map<String, String> getSAMLSPConfiguration() throws DataMigrationException {

        logger.info("Retrieving all the SAML Service Provider configurations of IS 5.1.0.");

        Map<String, String> samlSPConfigMap = new HashMap<>();
        SAMLSSOServiceProviderInfoDTO sameSPInfoDTO;
        if (samlConfigServiceStub == null) {
            throw new DataMigrationException("Couldn't find SAML Config service stub");
        }
        try {
            sameSPInfoDTO = samlConfigServiceStub.getServiceProviders();
        } catch (RemoteException | IdentitySAMLSSOConfigServiceIdentityException e) {
            throw new DataMigrationException("Error in retrieving all the application basic information in IS 5.1.0", e);
        }
        for (SAMLSSOServiceProviderDTO providerDTO : sameSPInfoDTO.getServiceProviders()) {
            enableSAMLAttributeProfile(providerDTO);
            samlSPConfigMap.put(providerDTO.getIssuer(), DataMigrationUtil.getObjectInJson(providerDTO));
        }
        return samlSPConfigMap;
    }

    private IdentityApplicationManagementServiceStub getIdentityApplicationManagementService(String hostName,
                                                                                             String port,
                                                                                             String trustStorePath,
                                                                                             String trustStorePassword,
                                                                                             String adminUsername,
                                                                                             String adminPassword) {

        DataMigrationUtil.setTrustStoreForSSL(trustStorePath, trustStorePassword);
        ConfigurationContext configContext = null;
        String serviceEndPoint = "https://" + hostName + ":" + port + "/services/IdentityApplicationManagementService";
        try {
            configContext = ConfigurationContextFactory
                    .createConfigurationContextFromFileSystem(null, null);
        } catch (AxisFault axisFault) {
            logger.error("Unable to create Configuration Context", axisFault);
        }

        try {
            applicationMgtServiceStub = new IdentityApplicationManagementServiceStub(configContext, serviceEndPoint);
        } catch (AxisFault axisFault) {
            logger.error("Unable to instantiate admin Service stub of " +
                    "IdentityApplicationManagementService", axisFault);
        }
        ServiceClient client = applicationMgtServiceStub._getServiceClient();
        DataMigrationUtil.authenticate(client, adminUsername, adminPassword);
        return applicationMgtServiceStub;
    }

    private IdentitySAMLSSOConfigServiceStub getSAMLSSOConfigService(String hostName, String port,
                                                                     String trustStorePath,
                                                                     String trustStorePassword,
                                                                     String adminUsername,
                                                                     String adminPassword) {

        DataMigrationUtil.setTrustStoreForSSL(trustStorePath, trustStorePassword);
        ConfigurationContext configContext = null;
        String serviceEndPoint = "https://" + hostName + ":" + port + "/services/IdentitySAMLSSOConfigService";
        try {
            configContext = ConfigurationContextFactory
                    .createConfigurationContextFromFileSystem(null, null);
        } catch (AxisFault axisFault) {
            logger.error("Unable to create Configuration Context", axisFault);
        }

        try {
            samlConfigServiceStub = new IdentitySAMLSSOConfigServiceStub(configContext, serviceEndPoint);
        } catch (AxisFault axisFault) {
            logger.error("Unable to instantiate admin Service stub of " +
                    "IdentitySAMLSSOConfigService", axisFault);

        }
        ServiceClient client = samlConfigServiceStub._getServiceClient();
        DataMigrationUtil.authenticate(client, adminUsername, adminPassword);
        return samlConfigServiceStub;
    }

    private void enableSAMLAttributeProfile(SAMLSSOServiceProviderDTO samlssoServiceProviderDTO) {

        if (samlssoServiceProviderDTO.getAttributeConsumingServiceIndex() != null) {
            samlssoServiceProviderDTO.setEnableAttributeProfile(true);
            samlssoServiceProviderDTO.setEnableAttributesByDefault(true);
        }
    }
}