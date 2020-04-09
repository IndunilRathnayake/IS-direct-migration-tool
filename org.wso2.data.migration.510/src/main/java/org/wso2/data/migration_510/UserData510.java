package org.wso2.data.migration_510;

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
import org.wso2.carbon.user.mgt.stub.types.carbon.FlaggedName;
import org.wso2.data.migration.common.DataMigrationException;
import org.wso2.data.migration.common.DataMigrationUtil;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class UserData510 {

    private static Logger logger = LoggerFactory.getLogger(UserData510.class);

    private static UserData510 userData510;
    private static UserAdminStub userAdminStub;

    private UserData510() {

    }

    private UserData510(String hostname, String port, String trustStorePath, String trustStorePassword,
                        String adminUsername, String adminPassword) {
        userAdminStub = getUserAdminService(hostname, port, trustStorePath, trustStorePassword, adminUsername,
                adminPassword);
    }

    public static UserData510 getInstance(String hostname, String port, String trustStorePath,
                                          String trustStorePassword, String adminUsername, String adminPassword) {
        if (userData510 == null) {
            synchronized (UserData510.class) {
                if (userData510 == null) {
                    userData510 = new UserData510(hostname, port, trustStorePath, trustStorePassword,
                            adminUsername, adminPassword);
                }
            }
        }
        return userData510;
    }

    public List<String> getUserListOfAppRole(String appName) throws DataMigrationException {

        FlaggedName[] flaggedNames;
        List<String> userList = new ArrayList<>();

        logger.info("Retrieving the user list of the application role created for application : " + appName);
        try {
            flaggedNames = userAdminStub.getUsersOfRole(getAppRoleName(appName), "*", 0);
        } catch (RemoteException | UserAdminUserAdminException e) {
            throw new DataMigrationException("Error in retrieving the user list of the application role in IS 5.1.0", e);
        }

        if (flaggedNames != null) {
            for (FlaggedName flaggedName : flaggedNames) {
                if (flaggedName.getSelected())
                    userList.add(flaggedName.getItemName());
            }
        }
        return userList;
    }

    public void assignAllApplicationRoles(String userName) throws DataMigrationException {

        logger.info("Assigning all the application role list");

        List<String> appRoleNames = getAllAppRoles();

        for (String appRole : appRoleNames) {
            try {
                userAdminStub.addRemoveRolesOfUser(userName, new String[]{appRole}, null);
            } catch (RemoteException | UserAdminUserAdminException e) {
                logger.error("Error in assigning the application role " + appRole + " in IS 5.1.0");
            }
        }
    }

    private static List<String> getAllAppRoles() throws DataMigrationException {

        logger.info("Retrieving all the application roles");

        List<String> applicationRoleNames = new ArrayList<>();
        FlaggedName[] flaggedNames;
        try {
            flaggedNames = userAdminStub.getAllRolesNames("Application/*", 0);
        } catch (RemoteException | UserAdminUserAdminException e) {
            throw new DataMigrationException("Error in retrieving all the application roles in IS 5.1.0", e);
        }

        if (flaggedNames != null) {
            for (FlaggedName flaggedName : flaggedNames) {
                if ("Application".equals(flaggedName.getRoleType())) {
                    applicationRoleNames.add(flaggedName.getItemName());
                }
            }
        }
        return applicationRoleNames;
    }

    private static String getAppRoleName(String applicationName) {
        return ApplicationConstants.APPLICATION_DOMAIN + UserCoreConstants.DOMAIN_SEPARATOR + applicationName;
    }

    private UserAdminStub getUserAdminService(String hostName, String port, String trustStorePath,
                                              String trustStorePassword, String adminUsername, String adminPassword) {

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