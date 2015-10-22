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
        if (plugin != null) {
            return plugin.getPluginName().get();
        }
        return "";
    }

    public String getRepository() {
        if (plugin != null) {
            return plugin.getRepository().get();
        }
        return "";
    }

    public String getVersion() {
        if (plugin != null) {
            return plugin.getVersion().get();
        }
        return "";
    }

    public String getLatestVersion() {
        if (plugin != null) {
            return bundleInfoManager.getLatestBundle(plugin.getBundle()).getVersion().toString();
        }
        return "";
    }

    public String getDescription() {
        if (plugin != null) {
            return plugin.getDescription().get();
        }
        return "";
    }

    public Boolean isUpdatable() {
        if (plugin != null) {
            return plugin.getIsUpdatable().get();
        }
        return false;
    }

    public Boolean isInstalled() {
        if (plugin != null) {
            return plugin.getIsInstalled().get();
        }
        return false;
    }

    public Boolean isBusy() {
        if (plugin != null) {
            return plugin.getIsBusy().get();
        }
        return false;
    }

}
