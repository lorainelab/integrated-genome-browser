package org.lorainelab.igb.plugin.manager.model;

import java.util.Base64;
import java.util.Objects;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 *
 * @author jeckstei
 */
public class PluginListItemMetadata implements Comparable<PluginListItemMetadata> {

    private String pluginName;
    private String repository;
    private String version;
    private String description;
    private boolean updateable;
    private boolean installed;
    private Bundle bundle;
    private int weight;
    private boolean busy;

    public PluginListItemMetadata(Bundle bundle, String version, String repository, Boolean installed, Boolean updateable) {
        this.bundle = bundle;
        this.pluginName = getpluginName(bundle);
        this.version = version;
        this.repository = repository;
        this.updateable = updateable;
        this.installed = installed;
        this.description = getBundleDescription(bundle);
        this.weight = 0;
        this.busy = false;
    }

    private String getpluginName(Bundle bundle) {
        try {
            return bundle.getHeaders().get(Constants.BUNDLE_NAME);
        } catch (Exception ex) {
            return bundle.getSymbolicName();
        }
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public String getRepository() {
        return repository;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isUpdateable() {
        return updateable;
    }

    public void setUpdateable(boolean updateable) {
        this.updateable = updateable;
    }

    public boolean isInstalled() {
        return installed;
    }

    public void setInstalled(boolean installed) {
        this.installed = installed;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public static String getBundleDescription(Bundle bundle) {
        String bundleDescription;
        try {
            bundleDescription = bundle.getHeaders().get("Bundle-Description");
            byte[] decode = Base64.getDecoder().decode(bundleDescription);
            bundleDescription = new String(decode, "UTF-8");
        } catch (Exception ex) {
            bundleDescription = bundle.getSymbolicName();
        }
        return bundleDescription;
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

    @Override
    public int compareTo(PluginListItemMetadata o) {
        return o.getWeight() - this.getWeight();
    }

}
