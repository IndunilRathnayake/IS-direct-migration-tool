sourceISHostname="localhost"
sourceISPort="9443"
sourceTruststorePath="/Applications/1_WSO2/Allocation/Offsite/wso2is-5.1.0_2/repository/resources/security/client-truststore.jks"
sourceTruststorePassword="wso2carbon"
sourceAdminUsername="admin"
sourceAdminPassword="admin"
assignAllAppRoles="true"

destISHostname="localhost"
destISPort="9444"
destTruststorePath="/Applications/1_WSO2/Allocation/Offsite/wso2is-5.7.0/repository/resources/security/client-truststore.jks"
destTruststorePassword="wso2carbon"
destAdminUsername="admin"
destAdminPassword="admin"

java -jar /Applications/1_WSO2/GIT_Repo/Support/data-migration-tool/org.wso2.data.migration.execution/target/org.wso2.data.migration.execution-1.0.0-jar-with-dependencies.jar \
  $sourceISHostname $sourceISPort $sourceTruststorePath $sourceTruststorePassword $sourceAdminUsername $sourceAdminPassword $assignAllAppRoles\
  $destISHostname $destISPort $destTruststorePath $destTruststorePassword $destAdminUsername $destAdminPassword
