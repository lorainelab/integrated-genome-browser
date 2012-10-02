package com.affymetrix.igb.trackOperations;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.GraphPanelImpl;
import com.affymetrix.igb.shared.StylePanelImpl;
import com.affymetrix.igb.shared.TrackViewPanel;

/**
 *
 * @author hiralv
 */
public class GraphTrackPanel extends TrackViewPanel {
	private static final long serialVersionUID = 1L;
	private static final int TAB_POSITION = 4;

	public GraphTrackPanel(IGBService _igbService) {
		super(_igbService, "Graph", "Graph", false, TAB_POSITION);
		
		addPanel(new StylePanelImpl(igbService));
		addPanel( new GraphPanelImpl(igbService));
	    addPanel(new YScaleAxisGUI(igbService));
	}

	@Override
	protected void selectAllButtonActionPerformedA(java.awt.event.ActionEvent evt) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	protected void clearButtonActionPerformedA(java.awt.event.ActionEvent evt) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	protected void saveButtonActionPerformedA(java.awt.event.ActionEvent evt) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	protected void deleteButtonActionPerformedA(java.awt.event.ActionEvent evt) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	protected void restoreButtonActionPerformedA(java.awt.event.ActionEvent evt) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	protected void selectAllButtonReset() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	protected void clearButtonReset() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	protected void saveButtonReset() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	protected void deleteButtonReset() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	protected void restoreButtonReset() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
	@Override
	public boolean isEmbedded() {
		return true;
	}
}
