package com.lorainelab.igb.plugins.model;

import com.lorainelab.igb.plugins.AppController;
import java.util.Base64;
import java.util.Objects;
import org.osgi.framework.Bundle;

/**
 *
 * @author jeckstei
 */
public class PluginListItemMetadata {

    private final String pluginName;
    private final String repository;
    private String version;
    private final String description;
    private Boolean isUpdatable;
    private Boolean isInstalled;
    private Bundle bundle;

    public PluginListItemMetadata(Bundle bundle, String repository, Boolean isUpdatable) {
        this.bundle = bundle;
        this.pluginName = bundle.getSymbolicName();
        this.version = bundle.getVersion().toString();
        this.repository = repository;
        this.isUpdatable = isUpdatable;
        this.isInstalled = AppController.isInstalled(bundle);
        this.description = getBundleDescription(bundle);
    }

    //for unit testing...
    public PluginListItemMetadata(String pluginName, String repository, String version, String description, Boolean isUpdatable, Boolean isInstalled) {
        this.pluginName = pluginName;
        this.repository = repository;
        this.version = version;
        this.description = description;
        this.isUpdatable = isUpdatable;
        this.isInstalled = isInstalled;
    }

    public String getPluginName() {
        return pluginName;
    }

    public String getRepository() {
        return repository;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public Boolean isUpdatable() {
        return isUpdatable;
    }

    public Boolean isInstalled() {
        return isInstalled;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public static String getBundleDescription(Bundle bundle) {
        String bundleDescription = bundle.getSymbolicName();
        try {
            bundleDescription = bundle.getHeaders().get("Bundle-Description");
            byte[] decode = Base64.getDecoder().decode(bundleDescription);
            bundleDescription = new String(decode);
        } catch (Exception ex) {
        }
        return bundleDescription;
    }

    public void setIsUpdatable(Boolean isUpdatable) {
        this.isUpdatable = isUpdatable;
    }

    public void setIsInstalled(Boolean isInstalled) {
        this.isInstalled = isInstalled;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.pluginName);
        hash = 67 * hash + Objects.hashCode(this.repository);
        hash = 67 * hash + Objects.hashCode(this.version);
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
        final PluginListItemMetadata other = (PluginListItemMetadata) obj;
        if (!Objects.equals(this.pluginName, other.pluginName)) {
            return false;
        }
        if (!Objects.equals(this.repository, other.repository)) {
            return false;
        }
        if (!Objects.equals(this.version, other.version)) {
            return false;
        }
        return true;
    }


}
