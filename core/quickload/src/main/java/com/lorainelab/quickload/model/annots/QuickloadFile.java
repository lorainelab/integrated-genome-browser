package com.lorainelab.quickload.model.annots;

import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "file")
public class QuickloadFile {

    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "title")
    protected String title;
    @XmlAttribute(name = "url")
    @XmlSchemaType(name = "anyURI")
    protected String url;
    @XmlAttribute(name = "description")
    protected String description;
    @XmlAttribute(name = "load_hint")
    protected String loadHint;
    @XmlAttribute(name = "show2tracks")
    protected String show2Tracks;
    @XmlAttribute(name = "label_field")
    protected String labelField;
    @XmlAttribute(name = "foreground")
    protected String foreground;
    @XmlAttribute(name = "background")
    protected String background;
    @XmlAttribute(name = "positive_strand_color")
    protected String positiveStrandColor;
    @XmlAttribute(name = "negative_strand_color")
    protected String negativeStrandColor;
    @XmlAttribute(name = "name_size")
    protected String nameSize;
    @XmlAttribute(name = "direction_type")
    protected String directionType;
    @XmlAttribute(name = "max_depth")
    protected String maxDepth;
    @XmlAttribute(name = "connected")
    protected String connected;
    @XmlAttribute(name = "view_mode")
    protected String viewMode;
    @XmlAttribute(name = "serverURL")
    protected String serverURL;
    @XmlAttribute(name = "collapsed")
    protected String collapsed;

    private final Map<String, String> props = new HashMap<>();

    public Map<String, String> getProps() {
        return props;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
        if (!Strings.isNullOrEmpty(value)) {
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String value) {
        this.title = value;
        if (!Strings.isNullOrEmpty(value)) {
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String value) {
        this.url = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("url", value);
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        this.description = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("description", description);
        }
    }

    public String getLoadHint() {
        return loadHint;
    }

    public void setLoadHint(String value) {
        this.loadHint = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("load_hint", value);
        }
    }

    public String getShow2Tracks() {
        return show2Tracks;
    }

    public void setShow2Tracks(String value) {
        this.show2Tracks = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("show2tracks", value);
        }
    }

    public String getLabelField() {
        return labelField;
    }

    public void setLabelField(String value) {
        this.labelField = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("label_field", value);
        }
    }

    public String getForeground() {
        return foreground;
    }

    public void setForeground(String value) {
        this.foreground = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("foreground", value);
        }
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String value) {
        this.background = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("background", value);
        }
    }

    public String getPositiveStrandColor() {
        return positiveStrandColor;
    }

    public void setPositiveStrandColor(String value) {
        this.positiveStrandColor = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("positive_strand_color", value);
        }
    }

    public String getNegativeStrandColor() {
        return negativeStrandColor;
    }

    public void setNegativeStrandColor(String value) {
        this.negativeStrandColor = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("negative_strand_color", value);
        }
    }

    public String getNameSize() {
        return nameSize;
    }

    public void setNameSize(String value) {
        this.nameSize = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("name_size", value);
        }
    }

    public String getDirectionType() {
        return directionType;
    }

    public void setDirectionType(String value) {
        this.directionType = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("direction_type", value);
        }
    }

    public String getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(String value) {
        this.maxDepth = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("max_depth", value);
        }
    }

    public String getConnected() {
        return connected;
    }

    public void setConnected(String value) {
        this.connected = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("connected", value);
        }
    }

    public String getViewMode() {
        return viewMode;
    }

    public void setViewMode(String value) {
        this.viewMode = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("view_mode", value);
        }
    }

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String value) {
        this.serverURL = value;
        if (!Strings.isNullOrEmpty(value)) {
        }
    }

    public String getCollapsed() {
        return collapsed;
    }

    public void setCollapsed(String value) {
        this.collapsed = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("collapsed", value);
        }
    }

}
