/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.igb.plugins;

import java.awt.Dimension;
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
        testFrame.add(fxPanel, "grow");
        testFrame.setVisible(true);
        Thread.sleep(10000);
    }
}
