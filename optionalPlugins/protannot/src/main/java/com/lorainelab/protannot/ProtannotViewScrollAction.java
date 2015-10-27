/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.protannot;

import com.affymetrix.genometry.event.GenericAction;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

/**
 *
 * @author Tarun
 */
public class ProtannotViewScrollAction extends GenericAction {
    
    GenomeView seqMapView;
    
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("protannot");
    
    public ProtannotViewScrollAction(GenomeView seqMapView) {
        super(BUNDLE.getString("scrollMode"),
                BUNDLE.getString("scrollModeToolTip"),
                "16x16/actions/open_hand.png", null, KeyEvent.VK_UNDEFINED);
        this.seqMapView = seqMapView;
        
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        seqMapView.setMapMode(GenomeView.MapMode.MapScrollMode);
    }
}
