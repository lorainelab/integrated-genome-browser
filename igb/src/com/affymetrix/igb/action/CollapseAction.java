package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.igb.IGBConstants;

public class CollapseAction extends CollapseExpandActionA implements SymSelectionListener {
	private static final long serialVersionUID = 1L;
	private static CollapseAction ACTION;

	public static CollapseAction getAction() {
		if (ACTION == null) {
			ACTION = new CollapseAction();
		}
		return ACTION;
	}

	protected CollapseAction() {
		super(IGBConstants.BUNDLE.getString("collapseAction"), "images/collapse.png", null);
		collapsedTracks = true;
	}

	@Override
	protected void processChange(boolean hasCollapsed, boolean hasExpanded) {
		setEnabled(hasCollapsed);
	}
}
