package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.IGBConstants;

public class ShowOneTierAction extends ShowStrandActionA {
	private static final long serialVersionUID = 1L;
	private static final ShowOneTierAction ACTION = new ShowOneTierAction();

	static{
		GenericActionHolder.getInstance().addGenericActionSilently(ACTION);
	}
	
	public static ShowOneTierAction getAction() {
		return ACTION;
	}

	protected ShowOneTierAction() {
		super(IGBConstants.BUNDLE.getString("showSingleTierAction"), "16x16/actions/strandstogether.png", "22x22/actions/strandstogether.png");
		separateStrands = false;
		setEnabled(false);
	}

	@Override
	protected void processChange(boolean hasSeparate, boolean hasMixed) {
		setEnabled(hasSeparate);
		ShowTwoTiersAction.getAction().setEnabled(hasMixed);
	}
}
