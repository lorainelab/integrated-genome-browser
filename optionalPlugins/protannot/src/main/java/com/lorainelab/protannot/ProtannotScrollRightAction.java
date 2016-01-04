/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.protannot;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.genoviz.widget.TieredNeoMap;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

/**
 *
 * @author Tarun
 */
public class ProtannotScrollRightAction extends GenericAction {
    TieredNeoMap seqMapView;
    
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("protannot");

    public ProtannotScrollRightAction(TieredNeoMap seqMapView) {
        super(BUNDLE.getString("scrollRight"), 
                BUNDLE.getString("scrollRightTooltip"),
                "16x16/actions/go-next.png", null, KeyEvent.VK_UNDEFINED);
        this.seqMapView = seqMapView;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int[] visible = seqMapView.getVisibleRange();
        seqMapView.scroll(NeoAbstractWidget.X, visible[0] + (visible[1] - visible[0]) / 10);
        seqMapView.updateWidget();
    }
}
