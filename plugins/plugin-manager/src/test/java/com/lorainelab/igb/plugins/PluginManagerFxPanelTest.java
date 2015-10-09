/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.igb.plugins;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.lorainelab.igb.plugins.model.PluginListItemMetadata;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import javafx.scene.paint.Color;
import javax.swing.JFrame;
import net.miginfocom.swing.MigLayout;
import org.junit.Test;

/**
 *
 * @author dcnorris
 */
public class PluginManagerFxPanelTest {

    @Test
    public void testPanelUI() throws InterruptedException, IOException {
        JFrame testFrame = new JFrame("");
        MigLayout migLayout = new MigLayout("fill");
        testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        testFrame.setLayout(migLayout);
        testFrame.setSize(new Dimension(885, 541));
        PluginManagerFxPanel fxPanel = new PluginManagerFxPanel();
        fxPanel.setMaterialDesignColors(getColors());
        fxPanel.updateListContent(getListItems());
        testFrame.add(fxPanel, "grow");
        testFrame.setVisible(true);
        Thread.sleep(150000);
    }

    private List<Color> getColors() {
        return ImmutableList.of(
                Color.rgb(156, 39, 176),
                Color.rgb(233, 30, 99),
                Color.rgb(244, 67, 54),
                Color.rgb(33, 150, 243),
                Color.rgb(63, 81, 181),
                Color.rgb(96, 125, 139),
                Color.rgb(255, 87, 34),
                Color.rgb(121, 85, 72),
                Color.rgb(158, 158, 158),
                Color.rgb(255, 235, 59),
                Color.rgb(255, 193, 7),
                Color.rgb(255, 152, 0),
                Color.rgb(76, 175, 80),
                Color.rgb(139, 195, 74),
                Color.rgb(205, 220, 57),
                Color.rgb(3, 169, 244),
                Color.rgb(0, 188, 212),
                Color.rgb(0, 150, 136),
                Color.rgb(103, 58, 183)
        );
    }

    private List<PluginListItemMetadata> getListItems() throws IOException {
        String readmeMarkdown = CharStreams.toString(new InputStreamReader(PluginManagerFxPanelTest.class.getClassLoader().getResourceAsStream("README.md")));
        return ImmutableList.of(
                new PluginListItemMetadata("ProtAnnot", "bioviz", "1.0.1", readmeMarkdown, Boolean.FALSE, Boolean.TRUE),
                new PluginListItemMetadata("Crisper Cas", "bioviz", "1.0.1", "## test", Boolean.FALSE, Boolean.FALSE),
                new PluginListItemMetadata("Command Socket", "test", "1.0.1", "### test", Boolean.TRUE, Boolean.TRUE),
                new PluginListItemMetadata("ProtAnnot", "bioviz2", "1.0.1", readmeMarkdown, Boolean.FALSE, Boolean.TRUE),
                new PluginListItemMetadata("Crisper Cas", "bioviz2", "1.0.1", "## test", Boolean.FALSE, Boolean.FALSE),
                new PluginListItemMetadata("Command Socket", "community", "1.0.1", "### test", Boolean.TRUE, Boolean.TRUE));
    }
}
