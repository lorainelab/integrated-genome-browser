package com.affymetrix.igb.action;


import com.affymetrix.igb.shared.RepackTiersAction;
import java.awt.event.ActionEvent;

import com.affymetrix.igb.IGBConstants;

public class RepackSelectedTiersAction extends RepackTiersAction {
	private static final long serialVersionUID = 1L;
	private static RepackSelectedTiersAction ACTION;

	public RepackSelectedTiersAction() {
		super(IGBConstants.BUNDLE.getString("repackSelectedTracksAction"), "16x16/actions/view-refresh.png", "22x22/actions/view-refresh.png");
	}

	public static RepackSelectedTiersAction getAction() {
		if (ACTION == null) {
			ACTION = new RepackSelectedTiersAction();
		}
		return ACTION;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		repackTiers(getTierManager().getSelectedTierLabels());
	}
}
