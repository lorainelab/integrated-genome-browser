/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.igb.plugins.model;

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

    public Boolean isChecked() {
        return isInstalled;
    }

    public String toString() {
        return "{" + "pluginName:\"" + pluginName + "\", repository:\"" + repository + "\", version:\"" + version + "\", description:\"" + description + "\", isUpdatable:" + isUpdatable + ", isInstalled:" + isInstalled + "}";
    }

}
