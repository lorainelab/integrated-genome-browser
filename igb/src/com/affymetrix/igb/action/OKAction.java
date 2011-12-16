package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.genometryImpl.event.GenericAction;

public class OKAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final OKAction ACTION = new OKAction();

	public static OKAction getAction() {
		return ACTION;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
	}

	@Override
	public String getText() {
		return "OK";//BUNDLE.getString("ok");
	}
}
