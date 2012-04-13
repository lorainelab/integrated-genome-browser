package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.view.SeqMapView;

public class ChangeForegroundColorAction extends ChangeColorActionA {
	private static final long serialVersionUID = 1L;
	private static ChangeForegroundColorAction ACTION;

	public static ChangeForegroundColorAction getAction() {
		if (ACTION == null) {
			ACTION = new ChangeForegroundColorAction(Application.getSingleton().getMapView());
		}
		return ACTION;
	}

	public ChangeForegroundColorAction(SeqMapView gviewer) {
		super(gviewer, IGBConstants.BUNDLE.getString("changeColorAction"), "images/change_color.png");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		changeColor(handler.getSelectedTierLabels(), true);
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}
}
