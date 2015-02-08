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
public class DataProvider {

    @XmlValue
    protected String value;
    @Expose
    @XmlAttribute(name = "type")
    protected String type;
    @Expose
    @XmlAttribute(name = "name")
    protected String name;
    @Expose
    @XmlAttribute(name = "url")
    protected String url;
    @Expose
    @XmlAttribute(name = "order")
    protected Integer order;
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
    @Expose
    @XmlAttribute(name = "primary")
    protected String primary;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String value) {
        this.type = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String value) {
        this.url = value;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer value) {
        this.order = value;
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

    public String getPrimary() {
        return primary;
    }

    public void setPrimary(String value) {
        this.primary = value;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(type).append(name).append(url).append(order).append(_default).append(mirror).append(enabled).append(primary).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof DataProvider) == false) {
            return false;
        }
        DataProvider rhs = ((DataProvider) other);
        return new EqualsBuilder().append(type, rhs.type).append(name, rhs.name).append(url, rhs.url).append(order, rhs.order).append(_default, rhs._default).append(mirror, rhs.mirror).append(enabled, rhs.enabled).append(primary, rhs.primary).isEquals();
    }
}
