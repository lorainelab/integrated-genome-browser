package com.lorainelab.igb.plugins.model;

import com.lorainelab.igb.plugins.AppController;
import java.util.Base64;
import org.osgi.framework.Bundle;

/**
 *
 * @author jeckstei
 */
public class PluginListItemMetadata {

    private final String pluginName;
    private final String repository;
    private final String version;
    private final String description;
    private final Boolean isUpdatable;
    private final Boolean isInstalled;

    public PluginListItemMetadata(Bundle bundle, String repository, Boolean isUpdatable) {
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

}
