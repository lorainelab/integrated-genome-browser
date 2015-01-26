package com.affymetrix.igb.tabs.graph;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.util.ThreadUtils;
import com.affymetrix.igb.service.api.IGBService;
import com.affymetrix.igb.shared.GraphGlyph;

import static com.affymetrix.igb.shared.Selections.*;

/**
 * Puts all selected graphs in separate tiers by setting the combo state of each
 * graph's state to null.
 */
public class SplitGraphsAction extends GenericAction {

    private static final long serialVersionUID = 1l;

    public SplitGraphsAction(IGBService igbService) {
        super("Split", null, null);
        this.igbService = igbService;
    }

    private final IGBService igbService;

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        super.actionPerformed(e);

        for (GraphGlyph gg : graphGlyphs) {
            igbService.getSeqMapView().split(gg);
        }
        //igbService.getSeqMapView().postSelections();
        updateDisplay();
    }

    private void updateDisplay() {
        ThreadUtils.runOnEventQueue(() -> {
//				igbService.getSeqMap().updateWidget();
//				igbService.getSeqMapView().setTierStyles();
//				igbService.getSeqMapView().repackTheTiers(true, true);
            igbService.getSeqMapView().updatePanel(true, true);
        });
    }

}
