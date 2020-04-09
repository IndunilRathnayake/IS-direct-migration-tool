package org.wso2.data.migration.execution;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.data.migration.common.DataMigrationException;
import org.wso2.data.migration.common.claim.LocalClaimMetaData;
import org.wso2.data.migration_510.ClaimData510;
import org.wso2.data.migration_510.SPData510;
import org.wso2.data.migration_510.UserData510;
import org.wso2.data.migration_570.ClaimData570;
import org.wso2.data.migration_570.SPData570;
import org.wso2.data.migration_570.UserData570;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DataMigrationExecution {

    private static Logger logger = LoggerFactory.getLogger(DataMigrationExecution.class);

    private static String sourceISHostname;
    private static String sourceISPort;
    private static String sourceTruststorePath;
    private static String sourceTruststorePassword;
    private static String sourceAdminUsername;
    private static String sourceAdminPassword;
    private static String assignAllAppRoles;

    private static String destISHostname;
    private static String destISPort;
    private static String destTruststorePath;
    private static String destTruststorePassword;
    private static String destAdminUsername;
    private static String destAdminPassword;

    public static void main(String[] args) {

        // resolve to issue 'ORA-12705: Cannot access NLS data files or invalid environment specified'
        Locale.setDefault(Locale.ENGLISH);

        PropertyConfigurator.configure("log4j.properties");

        sourceISHostname = args[0];
        sourceISPort = args[1];
        sourceTruststorePath = args[2];
        sourceTruststorePassword = args[3];
        sourceAdminUsername = args[4];
        sourceAdminPassword = args[5];
        assignAllAppRoles = args[6];

        destISHostname = args[7];
        destISPort = args[8];
        destTruststorePath = args[9];
        destTruststorePassword = args[10];
        destAdminUsername = args[11];
        destAdminPassword = args[12];

        if (args.length != 13) {
            throw new RuntimeException("Required arguments not provided");
        }

        SPData510 spData510 = SPData510.getInstance(sourceISHostname, sourceISPort,
                sourceTruststorePath, sourceTruststorePassword, sourceAdminUsername, sourceAdminPassword);
        SPData570 spData570 = SPData570.getInstance(destISHostname, destISPort,
                destTruststorePath, destTruststorePassword, destAdminUsername, destAdminPassword);

        ClaimData510 claimData510 = ClaimData510.getInstance(sourceISHostname, sourceISPort,
                sourceTruststorePath, sourceTruststorePassword, sourceAdminUsername, sourceAdminPassword);
        ClaimData570 claimData570 = ClaimData570.getInstance(destISHostname, destISPort,
                destTruststorePath, destTruststorePassword, destAdminUsername, destAdminPassword);

        UserData510 userData510 = UserData510.getInstance(sourceISHostname, sourceISPort, sourceTruststorePath,
                sourceTruststorePassword, sourceAdminUsername, sourceAdminPassword);
        UserData570 userData570 = UserData570.getInstance(destISHostname, destISPort,
                destTruststorePath, destTruststorePassword, destAdminUsername, destAdminPassword);

        if (Boolean.parseBoolean(assignAllAppRoles)) {
            try {
                userData510.assignAllApplicationRoles(sourceAdminUsername);
            } catch (DataMigrationException e) {
                logger.error("Error while assigning all the application roles to " + sourceAdminUsername +
                        " in IS 5.1.0");
            }
        } else {
            logger.info("**************************Start of Database Migration Process**************************");

            Map<String, String> serviceProviderDTOMap;
            List<String> appNames;
            try {
                serviceProviderDTOMap = spData510.getSAMLSPConfiguration();
                appNames = spData510.getAllServiceProviderNames();
            } catch (DataMigrationException e) {
                throw new RuntimeException("Error while retrieving all the service provider information from IS 5.1.0", e);
            }

            for (String name : appNames) {
                logger.info("*********Start Data Migration of Application : " + name + "********");
                String sp510 = null;
                List<LocalClaimMetaData> localClaimDTOs = null;
                List<String> usersOfApplicationRole = null;
                try {
                    sp510 = spData510.getServiceProvider(name);
                } catch (DataMigrationException e) {
                    logger.error("Error while retrieving service provider information of : " + name);
                }

                try {
                    localClaimDTOs = claimData510.getLocalClaimsInSPConfig(sp510);
                } catch (DataMigrationException e) {
                    logger.error("Error while retrieving all the local claims configured in the service provider");
                }

                try {
                    usersOfApplicationRole = userData510.getUserListOfAppRole(name);
                } catch (DataMigrationException e) {
                    logger.error("Error while retrieving user list of application role of : " + name);
                }

                if (sp510 != null) {
                    try {
                        claimData570.createLocalClaimsInSPConfig(localClaimDTOs);
                        spData570.createServiceProvider(sp510, serviceProviderDTOMap, usersOfApplicationRole);
                        userData570.assignApplicationRoleToUsers(usersOfApplicationRole, name);
                    } catch (DataMigrationException e) {
                        logger.error("Error while creating service provider : " + name);
                        try {
                            userData570.assignApplicationRoleToUsers(usersOfApplicationRole, name);
                        } catch (DataMigrationException ex) {
                            logger.error("Error while assigning application role of : " + name + " to users.");
                        }
                    }
                }
            }
            logger.info("**************************End of Database Migration Process**************************");
        }
    }
}
