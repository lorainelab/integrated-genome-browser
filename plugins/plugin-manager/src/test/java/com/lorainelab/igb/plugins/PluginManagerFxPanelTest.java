/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.igb.plugins;

import com.google.common.collect.ImmutableMap;
import com.lorainelab.igb.plugins.model.PluginListItemMetadata;
import java.awt.Dimension;
import java.util.Map;
import javax.swing.JFrame;
import net.miginfocom.swing.MigLayout;
import org.junit.Test;

/**
 *
 * @author dcnorris
 */
public class PluginManagerFxPanelTest {

    @Test
    public void testPanelUI() throws InterruptedException {
        JFrame testFrame = new JFrame("");
        MigLayout migLayout = new MigLayout("fill");
        testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        testFrame.setLayout(migLayout);
        testFrame.setSize(new Dimension(885, 541));
        PluginManagerFxPanel fxPanel = new PluginManagerFxPanel();
        fxPanel.updateListContent(getListItems());
        testFrame.add(fxPanel, "grow");
        testFrame.setVisible(true);
        Thread.sleep(10000);
    }

    private Map<String, PluginListItemMetadata> getListItems() {
        return ImmutableMap.of(
                "ProtAnnot.1.0.0", new PluginListItemMetadata("ProtAnnot", Boolean.FALSE, Boolean.TRUE),
                "23 and me.1.0.0", new PluginListItemMetadata("23 and me", Boolean.FALSE, Boolean.FALSE),
                "Crisper Cas.1.0.0", new PluginListItemMetadata("Crisper Cas", Boolean.FALSE, Boolean.FALSE),
                "Command Socket.1.0.0", new PluginListItemMetadata("Command Socket", Boolean.TRUE, Boolean.TRUE)
        );
    }
}
