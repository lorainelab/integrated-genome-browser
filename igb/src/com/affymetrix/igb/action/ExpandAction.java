package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.igb.IGBConstants;

public class ExpandAction extends CollapseExpandActionA implements SymSelectionListener {
	private static final long serialVersionUID = 1L;
	private static ExpandAction ACTION;

	public static ExpandAction getAction() {
		if (ACTION == null) {
			ACTION = new ExpandAction();
		}
		return ACTION;
	}

	protected ExpandAction() {
		super(IGBConstants.BUNDLE.getString("expandAction"), "images/expand.png");
		collapsedTracks = false;
	}

	@Override
	protected void processChange(boolean hasCollapsed, boolean hasExpanded) {
		setEnabled(hasExpanded);
	}
}
