package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.igb.IGBConstants;

public class SelectParentAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static SelectParentAction ACTION;

	public static SelectParentAction getAction() {
		if (ACTION == null) {
			ACTION = new SelectParentAction();
		}
		return ACTION;
	}
	protected SelectParentAction() {
		super(IGBConstants.BUNDLE.getString("selectParentAction"), null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		getSeqMapView().selectParents();
	}
}
