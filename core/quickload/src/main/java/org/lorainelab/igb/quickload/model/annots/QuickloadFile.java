package org.lorainelab.igb.quickload.model.annots;

import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(name = "")
@XmlRootElement(name = "file")
public class QuickloadFile {

    protected String name;
    protected String title;
    protected String url;
    protected String description;
    protected String loadHint;
    protected String show2Tracks;
    protected String labelField;
    protected String foreground;
    protected String background;
    protected String positiveStrandColor;
    protected String negativeStrandColor;
    protected String nameSize;
    protected String directionType;
    protected String maxDepth;
    protected String connected;
    protected String viewMode;
    protected String serverURL;
    protected String collapsed;
    protected String index;

    private final Map<String, String> props = new HashMap<>();

    public Map<String, String> getProps() {
        return props;
    }

    public String getName() {
        return name;
    }

    @XmlAttribute(name = "name", required = true)
    public void setName(String value) {
        this.name = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("name", value);
        }
    }

    public String getTitle() {
        return title;
    }

    @XmlAttribute(name = "title")
    public void setTitle(String value) {
        this.title = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("title", value);
        }
    }

    public String getUrl() {
        return url;
    }

    @XmlAttribute(name = "url")
    @XmlSchemaType(name = "anyURI")
    public void setUrl(String value) {
        this.url = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("url", value);
        }
    }

    public String getDescription() {
        return description;
    }

    @XmlAttribute(name = "description")
    public void setDescription(String value) {
        this.description = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("description", description);
        }
    }

    public String getLoadHint() {
        return loadHint;
    }

    @XmlAttribute(name = "load_hint")
    public void setLoadHint(String value) {
        this.loadHint = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("load_hint", value);
        }
    }

    public String getIndex() {
        return index;
    }

    @XmlAttribute(name = "index")
    public void setIndex(String value) {
        this.index = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("index", value);
        }
    }

    public String getShow2Tracks() {
        return show2Tracks;
    }

    @XmlAttribute(name = "show2tracks")
    public void setShow2Tracks(String value) {
        this.show2Tracks = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("show2tracks", value);
        }
    }

    public String getLabelField() {
        return labelField;
    }

    @XmlAttribute(name = "label_field")
    public void setLabelField(String value) {
        this.labelField = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("label_field", value);
        }
    }

    public String getForeground() {
        return foreground;
    }

    private String formatColor(String color){
        if(color.charAt(0)=='#'){
            color=color.substring(1);
        }
        return color;
    }

    @XmlAttribute(name = "foreground")
    public void setForeground(String value) {
        this.foreground = value;
        if (!Strings.isNullOrEmpty(value)) {
            value= formatColor(value);
            props.put("foreground", value);
        }
    }

    public String getBackground() {
        return background;
    }

    @XmlAttribute(name = "background")
    public void setBackground(String value) {
        this.background = value;
        if (!Strings.isNullOrEmpty(value)) {
            value= formatColor(value);
            props.put("background", value);
        }
    }

    public String getPositiveStrandColor() {
        return positiveStrandColor;
    }

    @XmlAttribute(name = "positive_strand_color")
    public void setPositiveStrandColor(String value) {
        this.positiveStrandColor = value;
        if (!Strings.isNullOrEmpty(value)) {
            value= formatColor(value);
            props.put("positive_strand_color", value);
        }
    }

    public String getNegativeStrandColor() {
        return negativeStrandColor;
    }

    @XmlAttribute(name = "negative_strand_color")
    public void setNegativeStrandColor(String value) {
        this.negativeStrandColor = value;
        if (!Strings.isNullOrEmpty(value)) {
            value= formatColor(value);
            props.put("negative_strand_color", value);
        }
    }

    public String getNameSize() {
        return nameSize;
    }

    @XmlAttribute(name = "name_size")
    public void setNameSize(String value) {
        this.nameSize = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("name_size", value);
        }
    }

    public String getDirectionType() {
        return directionType;
    }

    @XmlAttribute(name = "direction_type")
    public void setDirectionType(String value) {
        this.directionType = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("direction_type", value);
        }
    }

    public String getMaxDepth() {
        return maxDepth;
    }

    @XmlAttribute(name = "max_depth")
    public void setMaxDepth(String value) {
        this.maxDepth = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("max_depth", value);
        }
    }

    public String getConnected() {
        return connected;
    }

    @XmlAttribute(name = "connected")
    public void setConnected(String value) {
        this.connected = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("connected", value);
        }
    }

    public String getViewMode() {
        return viewMode;
    }

    @XmlAttribute(name = "view_mode")
    public void setViewMode(String value) {
        this.viewMode = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("view_mode", value);
        }
    }

    public String getServerURL() {
        return serverURL;
    }

    @XmlAttribute(name = "serverURL")
    public void setServerURL(String value) {
        this.serverURL = value;
        if (!Strings.isNullOrEmpty(value)) {
        }
    }

    public String getCollapsed() {
        return collapsed;
    }

    @XmlAttribute(name = "collapsed")
    public void setCollapsed(String value) {
        this.collapsed = value;
        if (!Strings.isNullOrEmpty(value)) {
            props.put("collapsed", value);
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 61 * hash + Objects.hashCode(this.name);
        hash = 61 * hash + Objects.hashCode(this.title);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final QuickloadFile other = (QuickloadFile) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.title, other.title)) {
            return false;
        }
        return true;
    }

}
