/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.protannot;

import com.affymetrix.genometry.event.GenericAction;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

/**
 *
 * @author Tarun
 */
public class ProtannotViewSelectAction extends GenericAction {
    
    GenomeView seqMapView;
    
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("protannot");
    
    public ProtannotViewSelectAction(GenomeView seqMapView) {
        super(BUNDLE.getString("selectMode"),
                BUNDLE.getString("selectModeToolTip"),
                "16x16/actions/arrow.png", null, KeyEvent.VK_UNDEFINED);
        this.seqMapView = seqMapView;
        
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        seqMapView.setMapMode(GenomeView.MapMode.MapSelectMode);
    }
    
}
