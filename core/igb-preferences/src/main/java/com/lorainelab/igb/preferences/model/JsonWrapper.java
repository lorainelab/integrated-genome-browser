package org.lorainelab.igb.igb.preferences.model;

import com.google.gson.annotations.Expose;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class JsonWrapper {

    @Expose
    protected IgbPreferences prefs;

    public IgbPreferences getPrefs() {
        return prefs;
    }

    public void setPrefs(IgbPreferences prefs) {
        this.prefs = prefs;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(prefs).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof JsonWrapper) == false) {
            return false;
        }
        JsonWrapper rhs = ((JsonWrapper) other);
        return new EqualsBuilder().append(prefs, rhs.prefs).isEquals();
    }
}
