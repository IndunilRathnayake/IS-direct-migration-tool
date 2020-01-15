package org.wso2.data.migration.common.claim;

import java.util.ArrayList;
import java.util.List;

public class LocalClaimMetaData {

    private String claimUri;

    private List<DomainClaimAttribute> mappedAttributes;

    private String displayName;

    private String description;

    private String supportedByDefault;

    private String required;

    private String readOnly;

    private String checkedAttribute;

    private String regEx;

    private String dialectURI;

    private String value;

    private String displayOrder;

    public String getClaimUri() {
        return claimUri;
    }

    public void setClaimUri(String claimUri) {
        this.claimUri = claimUri;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<DomainClaimAttribute> getMappedAttributes() {
        return mappedAttributes;
    }

    public void setMappedAttributes(List<DomainClaimAttribute> mappedAttributes) {
        this.mappedAttributes = mappedAttributes;
    }

    public void addMappedAttribute(DomainClaimAttribute mappedAttribute) {
        if (mappedAttributes == null) {
            setMappedAttributes(new ArrayList<DomainClaimAttribute>());
        }
        this.mappedAttributes.add(mappedAttribute);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(String displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getDialectURI() {
        return dialectURI;
    }

    public void setDialectURI(String dialectURI) {
        this.dialectURI = dialectURI;
    }

    public String getRegEx() {
        return regEx;
    }

    public void setRegEx(String regEx) {
        this.regEx = regEx;
    }

    public String isSupportedByDefault() {
        return supportedByDefault;
    }

    public void setSupportedByDefault(String supportedByDefault) {
        this.supportedByDefault = supportedByDefault;
    }

    public String isCheckedAttribute() {
        return checkedAttribute;
    }

    public void setCheckedAttribute(String checkedAttribute) {
        this.checkedAttribute = checkedAttribute;
    }

    public String isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(String readOnly) {
        this.readOnly = readOnly;
    }

    public String isRequired() {
        return required;
    }

    public void setRequired(String required) {
        this.required = required;
    }
}
