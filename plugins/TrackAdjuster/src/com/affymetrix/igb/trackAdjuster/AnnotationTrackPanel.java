package com.affymetrix.igb.trackAdjuster;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.SelectAllAction;
import com.affymetrix.igb.shared.StylePanelImpl;
import com.affymetrix.igb.shared.TrackViewPanel;

/**
 *
 * @author hiralv
 */
public class AnnotationTrackPanel extends TrackViewPanel {
	private static final long serialVersionUID = 1L;
	private static final int TAB_POSITION = 4;
	
	public AnnotationTrackPanel(IGBService _igbService) {
		super(_igbService, "Annotation", "Annotation", false, TAB_POSITION);
		
		addPanel(new StylePanelImpl(igbService));
	    addPanel(new AnnotationPanelImpl(igbService));
	}
	
	
	@Override
	protected void selectAllButtonActionPerformedA(java.awt.event.ActionEvent evt) {
		SelectAllAction.getAction().execute(FileTypeCategory.Annotation, FileTypeCategory.Alignment, FileTypeCategory.ProbeSet);
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
