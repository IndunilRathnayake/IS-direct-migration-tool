package org.wso2.data.migration.common;

import com.google.gson.Gson;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;

public class DataMigrationUtil {

    public static String getObjectInJson(Object object) {
        return new Gson().toJson(object);
    }

    public static Object getObjectFromJson(String objectInJson, Class objectClass) {
        return new Gson().fromJson(objectInJson, objectClass);
    }

    public static void authenticate(ServiceClient client, String adminUserName, String adminPassword) {
        Options option = client.getOptions();
        option.setProperty(HTTPConstants.COOKIE_STRING, null);
        HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
        auth.setUsername(adminUserName);
        auth.setPassword(adminPassword);
        auth.setPreemptiveAuthentication(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, auth);
        option.setManageSession(true);
    }

    public static void setTrustStoreForSSL(String trustStorePath, String trustStorePassword) {
        /**
         * trust store path - this must contains server's  certificate or Server's CA chain
         *
         * Call to IS services uses HTTPS protocol.
         * Therefore we to validate the server certificate or CA chain. The server certificate is looked up in the
         * trust store.
         * Following code sets what trust-store to look for and its JKs password.
         */
        System.setProperty("javax.net.ssl.trustStore", trustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);

    }
}
