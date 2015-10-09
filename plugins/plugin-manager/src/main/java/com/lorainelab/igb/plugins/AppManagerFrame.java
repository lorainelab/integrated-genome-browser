/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.igb.plugins;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import java.awt.Dimension;
import javax.swing.JFrame;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author dcnorris
 */
@Component(immediate = true, provide = AppManagerFrame.class)
public class AppManagerFrame extends JFrame {

    private PluginManagerFxPanel fxPanel;

    public AppManagerFrame() {
        init();
    }

    private void init() {
        MigLayout migLayout = new MigLayout("fill");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLayout(migLayout);
        setSize(new Dimension(885, 541));
    }

    @Activate
    private void activate() {
        add(fxPanel, "grow");
    }

    @Reference
    public void setFxPanel(PluginManagerFxPanel fxPanel) {
        this.fxPanel = fxPanel;
    }

}
