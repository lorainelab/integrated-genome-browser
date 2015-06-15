package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import java.awt.Adjustable;
import java.awt.event.ActionEvent;
import javax.swing.JScrollBar;

/**
 *
 * @author dcnorris
 */
public class NewTrackStrechAction extends SeqMapViewActionA {

    private static final NewTrackStrechAction ACTION = new NewTrackStrechAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static NewTrackStrechAction getAction() {
        return ACTION;
    }

    public NewTrackStrechAction() {
        super("Stretch Main View to fit new track", null, null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        AffyTieredMap seqmap = getSeqMapView().getSeqMap();
        JScrollBar scroller = seqmap.getScroller(NeoMap.Y);
        scroller.setValue(0);
        stretchHack(seqmap);
    }

    //Briefly stretching the viewer resolves a bug with the Jslider becoming unusable due to the slider block disapearing
    private void stretchHack(AffyTieredMap seqmap) {
        Adjustable adj = seqmap.getZoomer(NeoMap.Y);
        final int originalZoomPosition = adj.getValue();
        final int adjustment = originalZoomPosition + (adj.getMaximum() - adj.getMinimum()) / 10;
        adj.setValue(adjustment);
        adj.setValue(originalZoomPosition);
    }
}
