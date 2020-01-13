package org.wso2.data.migration_570;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.application.common.model.xsd.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.xsd.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.stub.IdentityApplicationManagementServiceIdentityApplicationManagementException;
import org.wso2.carbon.identity.application.mgt.stub.IdentityApplicationManagementServiceStub;
import org.wso2.carbon.identity.sso.saml.stub.IdentitySAMLSSOConfigServiceIdentityException;
import org.wso2.carbon.identity.sso.saml.stub.IdentitySAMLSSOConfigServiceStub;
import org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSOServiceProviderDTO;
import org.wso2.data.migration.common.DataMigrationConstants;
import org.wso2.data.migration.common.DataMigrationException;
import org.wso2.data.migration.common.DataMigrationUtil;

import java.rmi.RemoteException;
import java.util.Map;


public class SP570DataRetrieval {

    private static Logger logger = LoggerFactory.getLogger(SP570DataRetrieval.class);

    private static SP570DataRetrieval sp570DataRetrieval;
    private IdentityApplicationManagementServiceStub applicationMgtServiceStub;
    IdentitySAMLSSOConfigServiceStub samlConfigServiceStub;

    private SP570DataRetrieval() {

    }

    private SP570DataRetrieval(String hostname, String port, String trustStorePath, String trustStorePassword,
                               String adminUsername, String adminPassword) {
        applicationMgtServiceStub = getIdentityApplicationManagementService(hostname, port, trustStorePath,
                trustStorePassword, adminUsername, adminPassword);
        samlConfigServiceStub = getSAMLSSOConfigService(hostname, port, trustStorePath, trustStorePassword,
                adminUsername, adminPassword);
    }

    public static SP570DataRetrieval getInstance(String hostname, String port, String trustStorePath,
                                                 String trustStorePassword, String adminUsername,
                                                 String adminPassword) {
        if (sp570DataRetrieval == null) {
            synchronized (SP570DataRetrieval.class) {
                if (sp570DataRetrieval == null) {
                    sp570DataRetrieval = new SP570DataRetrieval(hostname, port, trustStorePath, trustStorePassword,
                            adminUsername, adminPassword);
                }
            }
        }
        return sp570DataRetrieval;
    }

    public void createServiceProvider(String serviceProviderAsJson, Map<String, String> serviceProviderDTOMap)
            throws DataMigrationException {

        logger.info("Creating the Service Provider in IS 5.7.0");

        ServiceProvider serviceProvider_created = createSPMetadata(serviceProviderAsJson);
        createSAMLSPConfiguration(serviceProviderDTOMap.get(retrieveIssuerName(serviceProvider_created)));
    }

    private ServiceProvider createSPMetadata(String serviceProviderAsJson) throws DataMigrationException {

        ServiceProvider serviceProvider_created;
        try {
            if (applicationMgtServiceStub == null) {
                throw new DataMigrationException("Couldn't find Application Management service stub");
            }
            ServiceProvider serviceProvider = (ServiceProvider) DataMigrationUtil.getObjectFromJson(
                    serviceProviderAsJson, ServiceProvider.class);
            serviceProvider_created = applicationMgtServiceStub.createApplicationWithTemplate(serviceProvider, null);
            logger.info("Service provider basic info is added for SP : " + serviceProvider_created.getApplicationName());

            serviceProvider.setApplicationID(serviceProvider_created.getApplicationID());
            applicationMgtServiceStub.updateApplication(serviceProvider);
            logger.info("Service provider other metadata is added for SP : " +
                    serviceProvider_created.getApplicationName());
        } catch (RemoteException | IdentityApplicationManagementServiceIdentityApplicationManagementException e) {
            throw new DataMigrationException("Error in adding Service Provider Basic Info and other Metadata in " +
                    "IS 5.7.0", e);
        }
        return serviceProvider_created;
    }

    private String retrieveIssuerName(ServiceProvider serviceProvider) {

        String issuerName = null;
        InboundAuthenticationRequestConfig[] inboundAuthenticationRequestConfigs =
                serviceProvider.getInboundAuthenticationConfig().getInboundAuthenticationRequestConfigs();
        for (InboundAuthenticationRequestConfig config : inboundAuthenticationRequestConfigs) {
            if (DataMigrationConstants.SAML_INBOUND_AUTH_TYPE.equals(config.getInboundAuthType())) {
                issuerName = config.getInboundAuthKey();
            }
        }
        return issuerName;
    }

    private void createSAMLSPConfiguration(String serviceProviderDTO) throws DataMigrationException {

        logger.info("Creating the SAML Service Provider configurations in IS 5.7.0");

        if (samlConfigServiceStub == null) {
            throw new DataMigrationException("Couldn't find SAML Config service stub");
        }
        SAMLSSOServiceProviderDTO samlssoServiceProviderDTO = (SAMLSSOServiceProviderDTO)
                DataMigrationUtil.getObjectFromJson(serviceProviderDTO, SAMLSSOServiceProviderDTO.class);
        try {
            samlConfigServiceStub.addRPServiceProvider(samlssoServiceProviderDTO);
        } catch (RemoteException | IdentitySAMLSSOConfigServiceIdentityException e) {
            throw new DataMigrationException("Error in adding Service Provider SAML configuration in " +
                    "IS 5.7.0", e);
        }
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
}