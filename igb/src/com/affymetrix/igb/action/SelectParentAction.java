package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import java.awt.event.ActionEvent;

import com.affymetrix.igb.IGBConstants;

public class SelectParentAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static SelectParentAction ACTION = new SelectParentAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static SelectParentAction getAction() {
		return ACTION;
	}
	protected SelectParentAction() {
		super(IGBConstants.BUNDLE.getString("selectParentAction"), null, null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		getSeqMapView().selectParents();
	}
}
