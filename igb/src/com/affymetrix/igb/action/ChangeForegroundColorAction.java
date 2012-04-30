package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;

public class ChangeForegroundColorAction extends ChangeColorActionA {
	private static final long serialVersionUID = 1L;
	private static ChangeForegroundColorAction ACTION;

	public static ChangeForegroundColorAction getAction() {
		if (ACTION == null) {
			ACTION = new ChangeForegroundColorAction();
		}
		return ACTION;
	}

	public ChangeForegroundColorAction() {
		super(IGBConstants.BUNDLE.getString("changeColorAction"), "16x16/categories/applications-graphics.png", "22x22/categories/applications-graphics.png");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		changeColor(getTierManager().getSelectedTierLabels(), true);
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}
}
