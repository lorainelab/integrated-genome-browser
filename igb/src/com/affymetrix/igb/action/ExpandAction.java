package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.view.SeqMapView;

public class ExpandAction extends CollapseExpandActionA implements SymSelectionListener {
	private static final long serialVersionUID = 1L;
	private static ExpandAction ACTION;

	public static ExpandAction getAction() {
		if (ACTION == null) {
			ACTION = new ExpandAction(Application.getSingleton().getMapView());
		}
		return ACTION;
	}

	protected ExpandAction(SeqMapView gviewer) {
		super(gviewer, IGBConstants.BUNDLE.getString("expandAction"), null, "images/expand.png");
		collapsedTracks = false;
	}

	@Override
	protected void processChange(boolean hasCollapsed, boolean hasExpanded) {
		setEnabled(hasExpanded);
	}
}
