package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.view.SeqMapView;

public class CollapseAction extends CollapseExpandActionA implements SymSelectionListener {
	private static final long serialVersionUID = 1L;
	private static CollapseAction ACTION;

	public static CollapseAction getAction() {
		if (ACTION == null) {
			ACTION = new CollapseAction(Application.getSingleton().getMapView());
		}
		return ACTION;
	}

	protected CollapseAction(SeqMapView gviewer) {
		super(gviewer, IGBConstants.BUNDLE.getString("collapseAction"), null, "images/collapse.png");
		collapsedTracks = true;
	}

	@Override
	protected void processChange(boolean hasCollapsed, boolean hasExpanded) {
		setEnabled(hasCollapsed);
	}
}
