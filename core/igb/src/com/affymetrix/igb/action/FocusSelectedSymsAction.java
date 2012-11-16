package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

/**
 *
 * @author hiralv
 */
public class FocusSelectedSymsAction extends SeqMapViewActionA {
	private static FocusSelectedSymsAction ACTION = new FocusSelectedSymsAction();
	
	public static FocusSelectedSymsAction getAction(){
		return ACTION;
	}
	
	private FocusSelectedSymsAction() {
		super("Focus on Selected", null, null);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		getSeqMapView().zoomToSelections();
	}
}
