package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import java.awt.event.ActionEvent;

import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.action.SeqMapViewActionA;

public class DeselectAllAction extends SeqMapViewActionA {

	private static final long serialVersionUID = 1L;
	private static DeselectAllAction ACTION = new DeselectAllAction();

	public static DeselectAllAction getAction() {
		return ACTION;
	}

	protected DeselectAllAction() {
		super(IGBConstants.BUNDLE.getString("selectNone"), "16x16/actions/blank_placeholder.png", null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		execute();
	}

	public void execute() {
		getSeqMapView().deselectAll();
	}
}
