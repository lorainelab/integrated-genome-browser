package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.igb.IGBConstants;

public class ExpandAction extends CollapseExpandActionA implements SymSelectionListener {
	private static final long serialVersionUID = 1L;
	private static final ExpandAction ACTION = new ExpandAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ExpandAction getAction() {
		return ACTION;
	}

	protected ExpandAction() {
		super(IGBConstants.BUNDLE.getString("expandAction"), "16x16/actions/view-fullscreen.png", "22x22/actions/expand.png");
		collapsedTracks = false;
	}

	@Override
	protected void processChange(boolean hasCollapsed, boolean hasExpanded) {
		setEnabled(hasExpanded);
	}
}
