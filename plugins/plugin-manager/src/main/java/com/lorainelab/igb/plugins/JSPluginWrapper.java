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

    public JSPluginWrapper(ListView<PluginListItemMetadata> listView) {
        plugin = listView.getSelectionModel().getSelectedItem();
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

    public String getDescription() {
        return plugin.getDescription();
    }

    public Boolean isUpdatable() {
        return plugin.isUpdatable();
    }

    public Boolean isInstalled() {
        return plugin.isInstalled();
    }

}
