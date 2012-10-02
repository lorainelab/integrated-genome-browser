package com.affymetrix.igb.trackOperations;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.GraphPanelImpl;
import com.affymetrix.igb.shared.StylePanelImpl;
import com.affymetrix.igb.shared.TrackViewPanel;
import java.awt.event.ActionEvent;

/**
 *
 * @author hiralv
 */
public class GraphTrackPanel extends TrackViewPanel {
	private static final long serialVersionUID = 1L;
	private static final int TAB_POSITION = 4;

	public GraphTrackPanel(IGBService _igbService) {
		super(_igbService, "Graph", "Graph", false, TAB_POSITION);
		
		StylePanelImpl stylePanel = new StylePanelImpl(igbService);
		GraphPanelImpl annotationPanel = new GraphPanelImpl(igbService);
		YScaleAxisGUI yAxisPanel = new YScaleAxisGUI(igbService);
		TrackOperationsTab trackOperation = new TrackOperationsTab(igbService);
		addPanel(stylePanel);
		addPanel(yAxisPanel);
	    addPanel(annotationPanel);
		addPanel(trackOperation);
	}

	@Override
	protected void selectAllButtonActionPerformedA(ActionEvent evt) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	protected void clearButtonActionPerformedA(ActionEvent evt) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	protected void saveButtonActionPerformedA(ActionEvent evt) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	protected void deleteButtonActionPerformedA(ActionEvent evt) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	protected void restoreButtonActionPerformedA(ActionEvent evt) {
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
