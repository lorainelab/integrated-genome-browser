package com.affymetrix.igb.thresholding;

import com.affymetrix.genoviz.swing.recordplayback.JRPMenuItem;
import com.affymetrix.igb.thresholding.action.ThresholdingAction;
import static com.affymetrix.igb.shared.Selections.*;

public class SelectionListener implements RefreshSelectionListener{
	private final ThresholdingAction thresholdingAction;
	private final JRPMenuItem thresholdingMenuItem;
	
	public SelectionListener(ThresholdingAction thresholdingAction, JRPMenuItem thresholdingMenuItem) {
		super();
		this.thresholdingAction = thresholdingAction;
		this.thresholdingMenuItem = thresholdingMenuItem;
	}

	@Override
	public void selectionRefreshed() {
		resetSelectedGraphGlyphs();
	}
	
	private void resetSelectedGraphGlyphs() {	
		thresholdingAction.setGraphs(graphGlyphs);
		thresholdingMenuItem.setEnabled(!graphGlyphs.isEmpty());
	}
	
}
