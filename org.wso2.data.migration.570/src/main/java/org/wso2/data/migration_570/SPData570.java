package org.wso2.data.migration_570;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.application.common.model.xsd.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.xsd.Property;
import org.wso2.carbon.identity.application.common.model.xsd.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.stub.IdentityApplicationManagementServiceIdentityApplicationManagementException;
import org.wso2.carbon.identity.application.mgt.stub.IdentityApplicationManagementServiceStub;
import org.wso2.carbon.identity.sso.saml.stub.IdentitySAMLSSOConfigServiceIdentityException;
import org.wso2.carbon.identity.sso.saml.stub.IdentitySAMLSSOConfigServiceStub;
import org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSOServiceProviderDTO;
import org.wso2.data.migration.common.DataMigrationException;
import org.wso2.data.migration.common.DataMigrationUtil;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.wso2.data.migration.common.DataMigrationConstants.*;

public class SPData570 {

    private static Logger logger = LoggerFactory.getLogger(SPData570.class);

    private static SPData570 spData570;
    IdentitySAMLSSOConfigServiceStub samlConfigServiceStub;
    private IdentityApplicationManagementServiceStub applicationMgtServiceStub;

    private SPData570() {

    }

    private SPData570(String hostname, String port, String trustStorePath, String trustStorePassword,
                      String adminUsername, String adminPassword) {
        applicationMgtServiceStub = getIdentityApplicationManagementService(hostname, port, trustStorePath,
                trustStorePassword, adminUsername, adminPassword);
        samlConfigServiceStub = getSAMLSSOConfigService(hostname, port, trustStorePath, trustStorePassword,
                adminUsername, adminPassword);
    }

    public static SPData570 getInstance(String hostname, String port, String trustStorePath,
                                        String trustStorePassword, String adminUsername,
                                        String adminPassword) {
        if (spData570 == null) {
            synchronized (SPData570.class) {
                if (spData570 == null) {
                    spData570 = new SPData570(hostname, port, trustStorePath, trustStorePassword,
                            adminUsername, adminPassword);
                }
            }
        }
        return spData570;
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

            updateCasConfiguration(serviceProvider);

            applicationMgtServiceStub.updateApplication(serviceProvider);
            logger.info("Service provider other metadata is added for SP : " +
                    serviceProvider_created.getApplicationName());
        } catch (RemoteException | IdentityApplicationManagementServiceIdentityApplicationManagementException e) {
            throw new DataMigrationException("Error in adding Service Provider Basic Info and other Metadata in " +
                    "IS 5.7.0", e);
        }
        return serviceProvider_created;
    }

    private void updateCasConfiguration(ServiceProvider serviceProvider) {

        if (serviceProvider.getInboundAuthenticationConfig() != null &&
                serviceProvider.getInboundAuthenticationConfig().getInboundAuthenticationRequestConfigs() != null) {
            List<InboundAuthenticationRequestConfig> requestConfig = new ArrayList(Arrays.asList(serviceProvider.getInboundAuthenticationConfig()
                    .getInboundAuthenticationRequestConfigs()));
            InboundAuthenticationRequestConfig updatedCASConfig = new InboundAuthenticationRequestConfig();
            updatedCASConfig.setInboundAuthType(CAS_570_INBOUND_AUTH_TYPE);
            updatedCASConfig.setInboundConfigType(CAS_570_INBOUND_CONFIG_TYPE);
            updatedCASConfig.setFriendlyName(CAS_570_INBOUND_CONFIG_FRIENDLY_NAME);

            Iterator itr = requestConfig.iterator();
            while (itr.hasNext()) {
                InboundAuthenticationRequestConfig config = (InboundAuthenticationRequestConfig) itr.next();
                if (CAS_510_INBOUND_AUTH_NAME.equals(config.getInboundAuthType())) {
                    itr.remove();
                } else if (CAS_510_INBOUND_AUTH_URL.equals(config.getInboundAuthType())) {
                    Property property = new Property();
                    property.setName(CAS_570_INBOUND_AUTH_PROPERTY_URL);
                    property.setValue(config.getInboundAuthKey());
                    property.setDisplayName(CAS_570_INBOUND_AUTH_PROPERTY_URL_DISPLAY_NAME);
                    property.setRequired(false);
                    property.setDescription(CAS_570_INBOUND_AUTH_PROPERTY_URL_DESCRIPTION);
                    property.setDisplayOrder(0);
                    updatedCASConfig.addProperties(property);
                    itr.remove();
                } else if (CAS_510_INBOUND_AUTH_SLO.equals(config.getInboundAuthType())) {
                    Property property = new Property();
                    property.setName(CAS_570_INBOUND_AUTH_PROPERTY_SLO);
                    property.setValue(config.getInboundAuthKey());
                    property.setDisplayName(CAS_570_INBOUND_AUTH_PROPERTY_SLO_DISPLAY_NAME);
                    property.setRequired(false);
                    property.setType(CAS_570_INBOUND_AUTH_PROPERTY_SLO_TYPE);
                    property.setDisplayOrder(1);
                    updatedCASConfig.addProperties(property);
                    itr.remove();
                }
            }
            requestConfig.add(updatedCASConfig);
            serviceProvider.getInboundAuthenticationConfig().setInboundAuthenticationRequestConfigs(
                    requestConfig.toArray(new InboundAuthenticationRequestConfig[requestConfig.size()]));
        }
    }

    private String retrieveIssuerName(ServiceProvider serviceProvider) {

        String issuerName = null;
        InboundAuthenticationRequestConfig[] inboundAuthenticationRequestConfigs =
                serviceProvider.getInboundAuthenticationConfig().getInboundAuthenticationRequestConfigs();
        if (inboundAuthenticationRequestConfigs != null) {
            for (InboundAuthenticationRequestConfig config : inboundAuthenticationRequestConfigs) {
                if (SAML_INBOUND_AUTH_TYPE.equals(config.getInboundAuthType())) {
                    issuerName = config.getInboundAuthKey();
                }
            }
        }
        return issuerName;
    }

    private void createSAMLSPConfiguration(String serviceProviderDTO) throws DataMigrationException {

        logger.info("Creating the SAML Service Provider configurations in IS 5.7.0");

        if (samlConfigServiceStub == null) {
            throw new DataMigrationException("Couldn't find SAML Config service stub");
        }
        if (serviceProviderDTO != null) {
            SAMLSSOServiceProviderDTO samlssoServiceProviderDTO = (SAMLSSOServiceProviderDTO)
                    DataMigrationUtil.getObjectFromJson(serviceProviderDTO, SAMLSSOServiceProviderDTO.class);
            try {
                samlConfigServiceStub.addRPServiceProvider(samlssoServiceProviderDTO);
            } catch (RemoteException | IdentitySAMLSSOConfigServiceIdentityException e) {
                throw new DataMigrationException("Error in adding Service Provider SAML configuration in " +
                        "IS 5.7.0", e);
            }
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