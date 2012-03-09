package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.igb.action.TrackFunctionOperationA;
import com.affymetrix.igb.osgi.service.SeqMapViewI;

public class TrackOperationAction extends TrackFunctionOperationA {
	private static final long serialVersionUID = 1L;

	public TrackOperationAction(SeqMapViewI gviewer, Operator operator) {
		super(gviewer, operator);
	}

	@Override
	public void actionPerformed(java.awt.event.ActionEvent e) {
		super.actionPerformed(e);
		addTier(gviewer.getSelectedTiers());
	}
}
