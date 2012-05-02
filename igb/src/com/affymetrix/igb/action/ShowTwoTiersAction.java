package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.igb.IGBConstants;

public class ShowTwoTiersAction extends ShowStrandActionA implements SymSelectionListener {
	private static final long serialVersionUID = 1L;
	private static final ShowTwoTiersAction ACTION = new ShowTwoTiersAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ShowTwoTiersAction getAction() {
		return ACTION;
	}

	protected ShowTwoTiersAction() {
		super(IGBConstants.BUNDLE.getString("showTwoTiersAction"), "images/strand_separate.png", null);
		separateStrands = true;
	}

	@Override
	protected void processChange(boolean hasSeparate, boolean hasMixed) {
		setEnabled(hasMixed);
	}
}
