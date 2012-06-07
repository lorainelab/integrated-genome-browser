package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.igb.IGBConstants;

public class ShowOneTierAction extends ShowStrandActionA implements SymSelectionListener {
	private static final long serialVersionUID = 1L;
	private static final ShowOneTierAction ACTION = new ShowOneTierAction();

	static{
		GenericActionHolder.getInstance().addGenericActionSilently(ACTION);
	}
	
	public static ShowOneTierAction getAction() {
		return ACTION;
	}

	protected ShowOneTierAction() {
		super(IGBConstants.BUNDLE.getString("showSingleTierAction"), "images/strand_mixed.png", "22x22/actions/strandstogether.png");
		separateStrands = false;
	}

	@Override
	protected void processChange(boolean hasSeparate, boolean hasMixed) {
		setEnabled(hasSeparate);
	}
}
