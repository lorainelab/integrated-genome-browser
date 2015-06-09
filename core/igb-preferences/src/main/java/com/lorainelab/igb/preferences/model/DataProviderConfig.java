package com.lorainelab.igb.preferences.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "value"
})
public class DataProviderConfig {

    @XmlValue
    protected String value;
    @Expose
    @XmlAttribute(name = "name")
    protected String name;
    @Expose
    @XmlAttribute(name = "factoryName")
    protected String factoryName;
    @Expose
    @XmlAttribute(name = "url")
    protected String url;
    @Expose
    @XmlAttribute(name = "loadPriority")
    protected Integer loadPriority;
    @SerializedName("default")
    @Expose
    @XmlAttribute(name = "default")
    protected String _default;
    @Expose
    @XmlAttribute(name = "mirror")
    protected String mirror;
    @Expose
    @XmlAttribute(name = "enabled")
    protected String enabled;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getFactoryName() {
        return factoryName;
    }

    public void setFactoryName(String factoryName) {
        this.factoryName = factoryName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String value) {
        this.url = value;
    }

    public Integer getLoadPriority() {
        return loadPriority;
    }

    public void setLoadPriority(Integer value) {
        this.loadPriority = value;
    }

    public String getDefault() {
        return _default;
    }

    public void setDefault(String value) {
        this._default = value;
    }

    public String getMirror() {
        return mirror;
    }

    public void setMirror(String value) {
        this.mirror = value;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String value) {
        this.enabled = value;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).append(url).append(loadPriority).append(_default).append(mirror).append(enabled).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof DataProviderConfig) == false) {
            return false;
        }
        DataProviderConfig rhs = ((DataProviderConfig) other);
        return new EqualsBuilder().append(name, rhs.name).append(url, rhs.url).append(loadPriority, rhs.loadPriority).append(_default, rhs._default).append(mirror, rhs.mirror).append(enabled, rhs.enabled).isEquals();
    }
}
