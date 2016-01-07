package org.lorainelab.igb.error.reporter.model;

import java.util.Objects;

/**
 *
 * @author dcnorris
 */
public class BundleInfo {

    private final String version;
    private final String symbolicName;

    public BundleInfo(String version, String symbolicName) {
        this.version = version;
        this.symbolicName = symbolicName;
    }

    public String getVersion() {
        return version;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.version);
        hash = 89 * hash + Objects.hashCode(this.symbolicName);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BundleInfo other = (BundleInfo) obj;
        if (!Objects.equals(this.version, other.version)) {
            return false;
        }
        if (!Objects.equals(this.symbolicName, other.symbolicName)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "BundleInfo{" + "version=" + version + ", symbolicName=" + symbolicName + '}';
    }

}
