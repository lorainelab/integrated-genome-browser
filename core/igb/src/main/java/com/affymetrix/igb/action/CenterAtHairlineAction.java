package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.view.SeqMapView;

public class CenterAtHairlineAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	private static final CenterAtHairlineAction ACTION = new CenterAtHairlineAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static CenterAtHairlineAction getAction() {
		return ACTION;
	}

	private CenterAtHairlineAction() {
		super(IGBConstants.BUNDLE.getString("centerZoomStripe"), "16x16/actions/center_on_zoom_stripe.png", "22x22/actions/center_on_zoom_stripe.png");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		SeqMapView gviewer = IGB.getSingleton().getMapView();
		gviewer.centerAtHairline();
	}
}
