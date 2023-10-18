package org.lorainelab.igb.preferences.model;

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
public class PluginRepository {

    @XmlValue
    protected String value;
    @Expose
    @XmlAttribute(name = "name")
    protected String name;
    @Expose
    @XmlAttribute(name = "url")
    protected String url;
    @Expose
    @XmlAttribute(name = "enabled")
    protected String enabled;
    @Expose
    @XmlAttribute(name = "default")
    @SerializedName("default")
    protected String _default;

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String value) {
        this.url = value;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String value) {
        this.enabled = value;
    }

    public void setEnabled(boolean value) {
        this.enabled = Boolean.toString(value);
    }

    public boolean isEnabled() {
        return Boolean.valueOf(enabled);
    }

    public String getDefault() {
        return _default;
    }

    public void setDefault(String value) {
        this._default = value;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).append(url).append(enabled).append(_default).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof PluginRepository) == false) {
            return false;
        }
        PluginRepository rhs = ((PluginRepository) other);
        return new EqualsBuilder().append(name, rhs.name).append(url, rhs.url).append(enabled, rhs.enabled).append(_default, rhs._default).isEquals();
    }
}
