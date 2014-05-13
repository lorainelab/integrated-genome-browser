package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author hiralv
 */
public class ZoomOnSelectedSymsAction extends SeqMapViewActionA {
	private static ZoomOnSelectedSymsAction ACTION = new ZoomOnSelectedSymsAction();
	
	public static ZoomOnSelectedSymsAction getAction(){
		return ACTION;
	}
	
	private ZoomOnSelectedSymsAction() {
		super(BUNDLE.getString("zoomOnSelected"), "toolbarButtonGraphics/general/Zoom16.gif", "toolbarButtonGraphics/general/Zoom24.gif");
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		getSeqMapView().zoomToSelections();
	}
}
