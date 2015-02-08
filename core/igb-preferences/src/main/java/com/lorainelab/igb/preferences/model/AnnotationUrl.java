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
public class AnnotationUrl {

    @XmlValue
    protected String value;
    @XmlAttribute(name = "annot_id_regex")
    @SerializedName("annot_id_regex")
    @Expose
    protected String annotIdRegex;
    @Expose
    @XmlAttribute(name = "name")
    protected String name;
    @Expose
    @XmlAttribute(name = "url")
    protected String url;
    @Expose
    @XmlAttribute(name = "type")
    protected String type;
    @SerializedName("image_icon_path")
    @Expose
    @XmlAttribute(name = "image_icon_path")
    protected String imageIconPath;
    @Expose
    @XmlAttribute(name = "description")
    protected String description;
    @SerializedName("annot_type_regex")
    @Expose
    @XmlAttribute(name = "annot_type_regex")
    protected String annotTypeRegex;
    @SerializedName("id_field")
    @Expose
    @XmlAttribute(name = "id_field")
    protected String idField;

    @SerializedName("species")
    @Expose
    @XmlAttribute(name = "species")
    protected String species;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getAnnotIdRegex() {
        return annotIdRegex;
    }

    public void setAnnotIdRegex(String annotIdRegex) {
        this.annotIdRegex = annotIdRegex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getImageIconPath() {
        return imageIconPath;
    }

    public void setImageIconPath(String imageIconPath) {
        this.imageIconPath = imageIconPath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAnnotTypeRegex() {
        return annotTypeRegex;
    }

    public void setAnnotTypeRegex(String annotTypeRegex) {
        this.annotTypeRegex = annotTypeRegex;
    }

    public String getIdField() {
        return idField;
    }

    public void setIdField(String idField) {
        this.idField = idField;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(annotIdRegex).append(name).append(url).append(type).append(imageIconPath).append(description).append(annotTypeRegex).append(idField).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof AnnotationUrl) == false) {
            return false;
        }
        AnnotationUrl rhs = ((AnnotationUrl) other);
        return new EqualsBuilder().append(annotIdRegex, rhs.annotIdRegex).append(name, rhs.name).append(url, rhs.url).append(type, rhs.type).append(imageIconPath, rhs.imageIconPath).append(description, rhs.description).append(annotTypeRegex, rhs.annotTypeRegex).append(idField, rhs.idField).isEquals();
    }
}
