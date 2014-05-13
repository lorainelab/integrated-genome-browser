package com.affymetrix.igb.thresholding;

import com.affymetrix.igb.swing.JRPMenuItem;
import com.affymetrix.igb.thresholding.action.ThresholdingAction;
import static com.affymetrix.igb.shared.Selections.*;

public class SelectionListener implements RefreshSelectionListener {

    private final JRPMenuItem thresholdingMenuItem;

    public SelectionListener(JRPMenuItem thresholdingMenuItem) {
        super();
        this.thresholdingMenuItem = thresholdingMenuItem;
    }

    @Override
    public void selectionRefreshed() {
        resetSelectedGraphGlyphs();
    }

    private void resetSelectedGraphGlyphs() {
        ThresholdingAction.getAction().setGraphs(graphGlyphs);
        thresholdingMenuItem.setEnabled(!graphGlyphs.isEmpty());
    }

}
