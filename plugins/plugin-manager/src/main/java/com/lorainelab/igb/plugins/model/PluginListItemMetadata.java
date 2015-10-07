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
    private final Boolean isUpdatable;
    private final Boolean isChecked;

    public PluginListItemMetadata(String pluginName, Boolean isUpdatable, Boolean isChecked) {
        this.pluginName = pluginName;
        this.isUpdatable = isUpdatable;
        this.isChecked = isChecked;
    }

    public String getPluginName() {
        return pluginName;
    }

    public Boolean isUpdatable() {
        return isUpdatable;
    }

    public Boolean isChecked() {
        return isChecked;
    }

}
