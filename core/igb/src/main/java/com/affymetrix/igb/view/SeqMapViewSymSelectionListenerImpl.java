/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view;

import com.affymetrix.genometry.event.SymSelectionEvent;
import com.affymetrix.genometry.event.SymSelectionListener;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.igb.tiers.AffyTieredMap;
import java.util.List;

/**
 *
 * @author tarun
 */
public class SeqMapViewSymSelectionListenerImpl implements SymSelectionListener {

    SeqMapView seqMapView;
    AffyTieredMap seqmap;

    public SeqMapViewSymSelectionListenerImpl(SeqMapView seqMapView) {
        this.seqMapView = seqMapView;
        this.seqmap = seqMapView.getSeqMap();
    }
    
    @Override
    public void symSelectionChanged(SymSelectionEvent evt) {
        Object src = evt.getSource();

        // ignore self-generated xym selection -- already handled internally
        if (src == seqMapView) {
            String title = seqMapView.getSelectionTitle(seqmap.getSelected());
            seqMapView.setSelectionStatus(title);
        } // ignore sym selection originating from AltSpliceView, don't want to change internal selection based on this
        else if ((src instanceof AltSpliceView) || (src instanceof SeqMapView)) {
            // catching SeqMapView as source of event because currently sym selection events actually originating
            //    from AltSpliceView have their source set to the AltSpliceView's internal SeqMapView...
        } else {
            List<SeqSymmetry> symlist = evt.getSelectedGraphSyms();
            // select:
            //   add_to_previous ==> false
            //   call_listeners ==> false
            //   update_widget ==>  false   (zoomToSelections() will make an updateWidget() call...)
            seqMapView.select(symlist, true, true, false);
            // Zoom to selections, unless the selection was caused by the TierLabelManager
            // (which sets the selection source as the AffyTieredMap, i.e. getSeqMap())
            if (src != seqMapView.getSeqMap() && src != seqMapView.getTierManager()) {
                seqMapView.zoomToSelections();
            }
            String title = seqMapView.getSelectionTitle(seqmap.getSelected());
            seqMapView.setSelectionStatus(title);
        }
    }
    
}
