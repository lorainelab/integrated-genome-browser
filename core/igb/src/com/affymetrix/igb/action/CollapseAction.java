package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.igb.IGBConstants;

public class CollapseAction extends CollapseExpandActionA implements SymSelectionListener {
	private static final long serialVersionUID = 1L;
	private static final CollapseAction ACTION = new CollapseAction();

	static{
		GenericActionHolder.getInstance().addGenericActionSilently(ACTION);
	}
	
	public static CollapseAction getAction() {
		return ACTION;
	}

	protected CollapseAction() {
		super(IGBConstants.BUNDLE.getString("collapseAction"), "images/collapse.png", "22x22/actions/collapse.png");
		collapsedTracks = true;
	}

	@Override
	protected void processChange(boolean hasCollapsed, boolean hasExpanded) {
		setEnabled(hasCollapsed);
	}
}
