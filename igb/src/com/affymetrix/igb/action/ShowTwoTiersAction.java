package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.view.SeqMapView;

public class ShowTwoTiersAction extends ShowStrandActionA implements SymSelectionListener {
	private static final long serialVersionUID = 1L;
	private static ShowTwoTiersAction ACTION;

	public static ShowTwoTiersAction getAction() {
		if (ACTION == null) {
			ACTION = new ShowTwoTiersAction(Application.getSingleton().getMapView());
		}
		return ACTION;
	}

	protected ShowTwoTiersAction(SeqMapView gviewer) {
		super(gviewer, IGBConstants.BUNDLE.getString("showTwoTiersAction"), null, "images/strand_separate.png");
		separateStrands = true;
	}

	@Override
	protected void processChange(boolean hasSeparate, boolean hasMixed) {
		setEnabled(hasMixed);
	}
}
