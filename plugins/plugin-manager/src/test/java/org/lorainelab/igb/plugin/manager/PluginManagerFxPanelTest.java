/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.plugin.manager;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import org.lorainelab.igb.plugin.manager.model.PluginListItemMetadata;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import javax.swing.JFrame;
import net.miginfocom.swing.MigLayout;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author dcnorris
 */
public class PluginManagerFxPanelTest {

    @Test
    @Ignore
    public void testPanelUI() throws InterruptedException, IOException {
        JFrame testFrame = new JFrame("");
        MigLayout migLayout = new MigLayout("fill");
        testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        testFrame.setLayout(migLayout);
        testFrame.setSize(new Dimension(885, 541));
        AppManagerFrame fxPanel = new AppManagerFrame();
        testFrame.add(fxPanel, "grow");
        testFrame.setVisible(true);
        Thread.sleep(150000);
    }

    

    private List<PluginListItemMetadata> getListItems() throws IOException {
        String readmeMarkdown = CharStreams.toString(new InputStreamReader(PluginManagerFxPanelTest.class.getClassLoader().getResourceAsStream("README.md")));
        return ImmutableList.of(
                new PluginListItemMetadata("ProtAnnot some really long name that is strange afdsasdfasdfsadfsadfsadfasdfasdfasdfasdfasfdasdfasdfasdfasfasdfasdfadsfadsfadsf", "bioviz", "1.0.1", readmeMarkdown, Boolean.FALSE, Boolean.TRUE),
                new PluginListItemMetadata("Crisper Cas", "bioviz", "1.0.1", "## test", Boolean.FALSE, Boolean.FALSE),
                new PluginListItemMetadata("Command Socket", "test", "1.0.1", "### test", Boolean.TRUE, Boolean.TRUE),
                new PluginListItemMetadata("ProtAnnot", "bioviz2", "1.0.1", readmeMarkdown, Boolean.FALSE, Boolean.TRUE),
                new PluginListItemMetadata("Crisper Cas", "bioviz2", "1.0.1", "## test", Boolean.FALSE, Boolean.FALSE),
                new PluginListItemMetadata("23 and me", "bioviz2", "1.0.1", "## test", Boolean.FALSE, Boolean.FALSE),
                new PluginListItemMetadata("Web Links", "bioviz2", "1.0.1", "## test", Boolean.FALSE, Boolean.FALSE),
                new PluginListItemMetadata("Demo", "bioviz2", "1.0.1", "## test", Boolean.FALSE, Boolean.FALSE),
                new PluginListItemMetadata("Viewer", "bioviz2", "1.0.1", "## test", Boolean.FALSE, Boolean.FALSE),
                new PluginListItemMetadata("Test", "bioviz2", "1.0.1", "## test", Boolean.FALSE, Boolean.FALSE),
                new PluginListItemMetadata("Help", "bioviz2", "1.0.1", "## test", Boolean.FALSE, Boolean.FALSE),
                new PluginListItemMetadata("Crisper Cas", "bioviz2", "1.0.1", "## test", Boolean.FALSE, Boolean.FALSE),
                new PluginListItemMetadata("Command Socket", "community", "1.0.1", "### test", Boolean.TRUE, Boolean.TRUE));
    }
}
