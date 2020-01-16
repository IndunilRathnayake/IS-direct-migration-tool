package org.wso2.data.migration.execution;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.data.migration.common.DataMigrationException;
import org.wso2.data.migration.common.claim.LocalClaimMetaData;
import org.wso2.data.migration_510.ClaimData510;
import org.wso2.data.migration_510.SPData510;
import org.wso2.data.migration_570.ClaimData570;
import org.wso2.data.migration_570.SPData570;

import java.util.ArrayList;
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

    private static String destISHostname;
    private static String destISPort;
    private static String destTruststorePath;
    private static String destTruststorePassword;
    private static String destAdminUsername;
    private static String destAdminPassword;

    public static void main(String[] args) {

        args = new String[12];

        List<String> a = new ArrayList<String>() {
            {
                add("localhost");
                add("9443");
                add("/Applications/1_WSO2/Allocation/Offsite/wso2is-5.1.0_2/repository/resources/security/client-truststore.jks");
                add("wso2carbon");
                add("admin");
                add("admin");
                add("localhost");
                add("9444");
                add("/Applications/1_WSO2/Allocation/Offsite/wso2is-5.7.0/repository/resources/security/client-truststore.jks");
                add("wso2carbon");
                add("admin");
                add("admin");
            }
        };

        for (int j = 0; j < a.size(); j++) {
            args[j] = a.get(j);
        }

        // resolve to issue 'ORA-12705: Cannot access NLS data files or invalid environment specified'
        Locale.setDefault(Locale.ENGLISH);

        PropertyConfigurator.configure("log4j.properties");
        logger.info("*********Start of Database Migration Process********");

        sourceISHostname = args[0];
        sourceISPort = args[1];
        sourceTruststorePath = args[2];
        sourceTruststorePassword = args[3];
        sourceAdminUsername = args[4];
        sourceAdminPassword = args[5];

        destISHostname = args[6];
        destISPort = args[7];
        destTruststorePath = args[8];
        destTruststorePassword = args[9];
        destAdminUsername = args[10];
        destAdminPassword = args[11];

        if (args.length != 12) {
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

        Map<String, String> serviceProviderDTOMap;
        List<String> appNames;
        try {
            serviceProviderDTOMap = spData510.getSAMLSPConfiguration();
            appNames = spData510.getAllServiceProviderNames();
        } catch (DataMigrationException e) {
            throw new RuntimeException("Error while retrieving all the service provider information from IS 5.1.0", e);
        }

        for (String name : appNames) {
            try {
                String sp510 = spData510.getServiceProvider(name);
                List<LocalClaimMetaData> localClaimDTOs = claimData510.getLocalClaimsInSPConfig(sp510);
                claimData570.createLocalClaimsInSPConfig(localClaimDTOs);
                spData570.createServiceProvider(sp510, serviceProviderDTOMap);
            } catch (DataMigrationException e) {
                logger.error("Error while retrieving service provider information and creating of SP : " + name, e);
                continue;
            }
        }
        logger.info("*********End of Database Migration Process********");
    }
}
