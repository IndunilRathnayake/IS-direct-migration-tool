package org.wso2.data.migration.common.claim;

public class DomainClaimAttribute {

    private String attributeName;

    private String domainName;

    public DomainClaimAttribute(String attributeName, String domainName) {
        this.attributeName = attributeName;
        this.domainName = domainName;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }
}
