package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.view.SeqMapView;

public class ShowOneTierAction extends ShowStrandActionA implements SymSelectionListener {
	private static final long serialVersionUID = 1L;
	private static ShowOneTierAction ACTION;

	public static ShowOneTierAction getAction() {
		if (ACTION == null) {
			ACTION = new ShowOneTierAction(Application.getSingleton().getMapView());
		}
		return ACTION;
	}

	protected ShowOneTierAction(SeqMapView gviewer) {
		super(gviewer, IGBConstants.BUNDLE.getString("showSingleTierAction"), null, "images/strand_mixed.png");
		separateStrands = false;
	}

	@Override
	protected void processChange(boolean hasSeparate, boolean hasMixed) {
		setEnabled(hasSeparate);
	}
}
