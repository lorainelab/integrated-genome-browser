package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import java.awt.Adjustable;
import java.awt.event.ActionEvent;
import javax.swing.JScrollBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class NewTrackStrechAction extends SeqMapViewActionA {

    private static final Logger logger = LoggerFactory.getLogger(NewTrackStrechAction.class);
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
        AffyTieredMap affyTieredMap = getSeqMapView().getSeqMap();
        if (affyTieredMap.getTiers().size() <= 3) {
            ((AffyLabelledTierMap) affyTieredMap).packTiers(false, true, true);
            affyTieredMap.stretchToFit(false, true);
        } else {
            ((AffyLabelledTierMap) affyTieredMap).packTiers(false, false, true);
            ((AffyLabelledTierMap) affyTieredMap).zoom(1, 1);
            JScrollBar scroller = affyTieredMap.getScroller(NeoMap.Y);
            scroller.setValue(0);
            ((AffyLabelledTierMap) affyTieredMap).repackTheTiers(true, false);
            stretchHack();
            scroller.setValue(0);
        }
        affyTieredMap.updateWidget();

    }

    //Briefly stretching the viewer resolves a bug with the Jslider becoming unusable due to the slider block disapearing
    private void stretchHack() {
        AffyTieredMap affyTieredMap = getSeqMapView().getSeqMap();
        Adjustable adj = affyTieredMap.getZoomer(NeoMap.Y);
        final int originalZoomPosition = adj.getValue();
        final int adjustment = originalZoomPosition + (adj.getMaximum() - adj.getMinimum()) / 10;
        adj.setValue(adjustment);
        adj.setValue(originalZoomPosition);
    }
}
