package org.wso2.data.migration.execution;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.data.migration.common.DataMigrationException;
import org.wso2.data.migration_510.SP510DataRetrieval;
import org.wso2.data.migration_570.SP570DataRetrieval;

import java.util.*;

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

        SP510DataRetrieval sp510DataRetrieval = SP510DataRetrieval.getInstance(sourceISHostname, sourceISPort,
                sourceTruststorePath, sourceTruststorePassword, sourceAdminUsername, sourceAdminPassword);
        SP570DataRetrieval sp570DataRetrieval = SP570DataRetrieval.getInstance(destISHostname, destISPort,
                destTruststorePath, destTruststorePassword, destAdminUsername, destAdminPassword);

        Map<String, String> serviceProviderDTOMap;
        List<String> appNames;
        try {
            serviceProviderDTOMap = sp510DataRetrieval.getSAMLSPConfiguration();
            appNames = sp510DataRetrieval.getAllServiceProviderNames();
        } catch (DataMigrationException e) {
            throw new RuntimeException("Error while retrieving all the service provider information from IS 5.1.0", e);
        }

        for (String name : appNames) {
            try {
                String sp510 = sp510DataRetrieval.getServiceProvider(name);
                sp570DataRetrieval.createServiceProvider(sp510, serviceProviderDTOMap);
            } catch (DataMigrationException e) {
                logger.error("Error while retrieving service provider information and creating of SP : " + name, e);
                continue;
            }
        }
        logger.info("*********End of Database Migration Process********");
    }
}
