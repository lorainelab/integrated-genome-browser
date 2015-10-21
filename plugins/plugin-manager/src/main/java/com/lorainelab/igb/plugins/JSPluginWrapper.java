/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.igb.plugins;

import com.lorainelab.igb.plugins.model.PluginListItemMetadata;
import javafx.scene.control.ListView;

/**
 *
 * @author dcnorris
 */
public class JSPluginWrapper {

    final PluginListItemMetadata plugin;
    BundleInfoManager bundleInfoManager;

    public JSPluginWrapper(ListView<PluginListItemMetadata> listView, BundleInfoManager bundleInfoManager) {
        plugin = listView.getSelectionModel().getSelectedItem();
        this.bundleInfoManager = bundleInfoManager;
    }

    public String getPluginName() {
        return plugin.getPluginName();
    }

    public String getRepository() {
        return plugin.getRepository();
    }

    public String getVersion() {
        return plugin.getVersion();
    }

    public String getLatestVersion() {
        return bundleInfoManager.getLatestBundle(plugin.getBundle()).getVersion().toString();
    }

    public String getDescription() {
        return plugin.getDescription();
    }

    public Boolean isUpdatable() {
        return plugin.isUpdatable();
    }

    public Boolean isInstalled() {
        return plugin.isInstalled();
    }

    public Boolean isBusy() {
        return plugin.isBusy();
    }

}
