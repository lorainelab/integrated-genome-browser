package com.affymetrix.igb.shared;

import com.affymetrix.igb.osgi.service.IGBService;
import java.awt.event.ActionEvent;

/**
 *
 * @author hiralv
 */
public class AnnotationTrackPanel extends TrackViewPanel {
	private static final long serialVersionUID = 1L;
	private static final int TAB_POSITION = 8;

	public AnnotationTrackPanel(IGBService _igbService) {
		super(_igbService, "Annotation", "Annotation", false, TAB_POSITION);
		
		StylePanelImpl stylePanel = new StylePanelImpl(igbService);
		AnnotationPanelImpl annotationPanel = new AnnotationPanelImpl(igbService);
		addPanel(stylePanel);
	    addPanel(annotationPanel);
		
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
}
