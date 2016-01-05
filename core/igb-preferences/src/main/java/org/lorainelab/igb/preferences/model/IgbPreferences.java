package org.lorainelab.igb.preferences.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "dataProviders",
    "repository",
    "annotationUrl"
})
@XmlRootElement(name = "prefs")
public class IgbPreferences {

    @SerializedName("server")
    @Expose
    @XmlElement(name = "server")
    protected List<DataProviderConfig> dataProviders = new ArrayList<>();
    @Expose
    protected List<PluginRepository> repository = new ArrayList<>();
    @SerializedName("annotation_url")
    @Expose
    @XmlElement(name = "annotation_url")
    protected List<AnnotationUrl> annotationUrl = new ArrayList<>();

    public List<DataProviderConfig> getDataProviders() {
        return dataProviders;
    }

    public void setDataProviders(List<DataProviderConfig> dataProviders) {
        this.dataProviders = dataProviders;
    }

    public void setRepository(List<PluginRepository> repository) {
        this.repository = repository;
    }

    public void setAnnotationUrl(List<AnnotationUrl> annotationUrl) {
        this.annotationUrl = annotationUrl;
    }

    public List<PluginRepository> getRepository() {
        if (repository == null) {
            repository = new ArrayList<>();
        }
        return this.repository;
    }

    public List<AnnotationUrl> getAnnotationUrl() {
        if (annotationUrl == null) {
            annotationUrl = new ArrayList<>();
        }
        return this.annotationUrl;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(dataProviders).append(repository).append(annotationUrl).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof IgbPreferences) == false) {
            return false;
        }
        IgbPreferences otherPreferences = ((IgbPreferences) other);
        return new EqualsBuilder().append(dataProviders, otherPreferences.dataProviders).append(repository, otherPreferences.repository).append(annotationUrl, otherPreferences.annotationUrl).isEquals();
    }
}
