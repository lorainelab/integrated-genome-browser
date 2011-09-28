package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.IGB;

public class NextSearchSpanAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	private static final NextSearchSpanAction ACTION = new NextSearchSpanAction();

	public static NextSearchSpanAction getAction() {
		return ACTION;
	}

	@Override
	public String getText() {
		return "Next Search Span";
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		IGB.getSingleton().getMapView().getMapRangeBox().nextSpan();
	}
}
