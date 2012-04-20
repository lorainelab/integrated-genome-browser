package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.igb.IGBConstants;

public class ShowOneTierAction extends ShowStrandActionA implements SymSelectionListener {
	private static final long serialVersionUID = 1L;
	private static ShowOneTierAction ACTION;

	public static ShowOneTierAction getAction() {
		if (ACTION == null) {
			ACTION = new ShowOneTierAction();
		}
		return ACTION;
	}

	protected ShowOneTierAction() {
		super(IGBConstants.BUNDLE.getString("showSingleTierAction"), "images/strand_mixed.png");
		separateStrands = false;
	}

	@Override
	protected void processChange(boolean hasSeparate, boolean hasMixed) {
		setEnabled(hasSeparate);
	}
}
