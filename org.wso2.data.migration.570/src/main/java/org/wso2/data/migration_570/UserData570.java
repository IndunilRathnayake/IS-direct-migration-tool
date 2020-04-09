package org.wso2.data.migration_570;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.application.mgt.ApplicationConstants;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.mgt.stub.UserAdminStub;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;
import org.wso2.data.migration.common.DataMigrationException;
import org.wso2.data.migration.common.DataMigrationUtil;

import java.rmi.RemoteException;
import java.util.List;

public class UserData570 {

    private static Logger logger = LoggerFactory.getLogger(UserData570.class);

    private static UserData570 userData570;
    private UserAdminStub userAdminStub;

    private UserData570() {

    }

    private UserData570(String hostname, String port, String trustStorePath, String trustStorePassword,
                        String adminUsername, String adminPassword) {
        userAdminStub = getUserAdminService(hostname, port, trustStorePath, trustStorePassword, adminUsername,
                adminPassword);
    }

    public static UserData570 getInstance(String hostname, String port, String trustStorePath,
                                          String trustStorePassword, String adminUsername,
                                          String adminPassword) {
        if (userData570 == null) {
            synchronized (UserData570.class) {
                if (userData570 == null) {
                    userData570 = new UserData570(hostname, port, trustStorePath, trustStorePassword,
                            adminUsername, adminPassword);
                }
            }
        }
        return userData570;
    }

    public void assignApplicationRoleToUsers(List<String> userNames, String appName) throws DataMigrationException {

        logger.info("Assigning application role to the necessary users.");

        if (userNames != null) {
            try {
                userAdminStub.addRemoveUsersOfRole(getAppRoleName(appName), userNames.toArray(new String[userNames.size()]), null);
            } catch (RemoteException | UserAdminUserAdminException e) {
                throw new DataMigrationException("Error while assigning application role to the necessary users in " +
                        "IS 5.7.0", e);
            }
        }
    }

    private static String getAppRoleName(String applicationName) {

        return ApplicationConstants.APPLICATION_DOMAIN + UserCoreConstants.DOMAIN_SEPARATOR + applicationName;
    }

    private UserAdminStub getUserAdminService(String hostName, String port,
                                              String trustStorePath,
                                              String trustStorePassword,
                                              String adminUsername,
                                              String adminPassword) {

        DataMigrationUtil.setTrustStoreForSSL(trustStorePath, trustStorePassword);
        ConfigurationContext configContext = null;
        String serviceEndPoint = "https://" + hostName + ":" + port + "/services/UserAdmin";
        try {
            configContext = ConfigurationContextFactory
                    .createConfigurationContextFromFileSystem(null, null);
        } catch (AxisFault axisFault) {
            logger.error("Unable to create Configuration Context");
        }

        try {
            userAdminStub = new UserAdminStub(configContext, serviceEndPoint);
        } catch (AxisFault axisFault) {
            logger.error("Unable to instantiate admin Service stub of UserAdmin");

        }
        ServiceClient client = userAdminStub._getServiceClient();
        DataMigrationUtil.authenticate(client, adminUsername, adminPassword);
        return userAdminStub;
    }
}