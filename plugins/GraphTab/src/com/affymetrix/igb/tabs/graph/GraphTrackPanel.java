package com.affymetrix.igb.tabs.graph;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.TrackViewPanel;
import static com.affymetrix.igb.shared.Selections.*;
import com.affymetrix.igb.thresholding.action.ThresholdingAction;

/**
 *
 * @author hiralv
 */
public class GraphTrackPanel extends TrackViewPanel {
	private static final long serialVersionUID = 1L;
	public static final java.util.ResourceBundle BUNDLE = java.util.ResourceBundle.getBundle("graph");
	private static final int TAB_POSITION = 4;
	
	public GraphTrackPanel(IGBService _igbService) {
		super(_igbService, BUNDLE.getString("graphTab"), BUNDLE.getString("graphTab"), false, TAB_POSITION);
		getCustomButton().setText("Thresholding...");
		getClearButton().setText("Clear Graph");
		getSaveButton().setText("Save Graph");
		getDeleteButton().setText(("Delete Graph"));
	}

	@Override
	protected void selectAllButtonReset() {
		
	}

	@Override
	protected void customButtonActionPerformedA(java.awt.event.ActionEvent evt) {
		ThresholdingAction.getAction().actionPerformed(evt);
	}
	
	@Override
	protected void clearButtonReset() {
		javax.swing.JButton clearButton = getClearButton();
		clearButton.setEnabled(graphSyms.size() > 0);
	}

	@Override
	protected void saveButtonReset() {
		javax.swing.JButton saveButton = getSaveButton();
		saveButton.setEnabled(graphSyms.size() > 0);
	}

	@Override
	protected void deleteButtonReset() {
		javax.swing.JButton deleteButton = getDeleteButton();
		deleteButton.setEnabled(graphSyms.size() > 0);
	}

	@Override
	protected void restoreButtonReset() {
		javax.swing.JButton restoreButton = getRestoreButton();
		restoreButton.setEnabled(graphSyms.size() > 0);
	}
	
	@Override
	protected void customButtonReset() {
		getCustomButton().setEnabled(!graphSyms.isEmpty());
	}
		
	@Override
	public boolean isEmbedded() {
		return true;
	}
}
