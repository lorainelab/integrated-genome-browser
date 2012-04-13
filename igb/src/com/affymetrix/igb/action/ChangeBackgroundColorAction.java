package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.view.SeqMapView;

public class ChangeBackgroundColorAction extends ChangeColorActionA {
	private static final long serialVersionUID = 1L;
	private static ChangeBackgroundColorAction ACTION;

	public static ChangeBackgroundColorAction getAction() {
		if (ACTION == null) {
			ACTION = new ChangeBackgroundColorAction(Application.getSingleton().getMapView());
		}
		return ACTION;
	}

	public ChangeBackgroundColorAction(SeqMapView gviewer) {
		super(gviewer, IGBConstants.BUNDLE.getString("changeBGColorAction"), null, "images/change_color.png");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		changeColor(handler.getSelectedTierLabels(), false);
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}
}
