package org.wso2.data.migration.common;

public class DataMigrationException extends Exception {

    public DataMigrationException() {
        super();
    }

    public DataMigrationException(String msg, Exception e) {
        super(msg, e);
    }

    public DataMigrationException(String msg) {
        super(msg);
    }
}
