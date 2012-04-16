package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.view.SeqMapView;

public class SelectParentAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static SelectParentAction ACTION;

	public static SelectParentAction getAction() {
		if (ACTION == null) {
			ACTION = new SelectParentAction(Application.getSingleton().getMapView());
		}
		return ACTION;
	}
	protected SelectParentAction(SeqMapView gviewer) {
		super(gviewer, IGBConstants.BUNDLE.getString("selectParentAction"), null, null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		gviewer.selectParents();
	}
}
