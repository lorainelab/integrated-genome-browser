package com.affymetrix.igb.action;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

import java.awt.event.ActionEvent;

import com.affymetrix.igb.shared.TrackstylePropertyMonitor;

public class ChangeExpandMaxAction extends ChangeExpandMaxActionA {
	private static final long serialVersionUID = 1L;
	private static final ChangeExpandMaxAction ACTION = new ChangeExpandMaxAction();

	public static ChangeExpandMaxAction getAction() {
		return ACTION;
	}

	private ChangeExpandMaxAction() {
		super(BUNDLE.getString("changeExpandMaxAction"), null, null);
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		changeExpandMax(getTierManager().getSelectedTierLabels());
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}
}
