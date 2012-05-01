package com.affymetrix.igb.action;

import com.affymetrix.igb.shared.RepackTiersAction;
import java.awt.event.ActionEvent;

import com.affymetrix.igb.IGBConstants;

public class RepackAllTiersAction extends RepackTiersAction {
	private static final long serialVersionUID = 1L;
	private static RepackAllTiersAction ACTION;

	public RepackAllTiersAction() {
		super(IGBConstants.BUNDLE.getString("repackAllTracksAction"), "16x16/actions/view-refresh.png", "22x22/actions/view-refresh.png");
	}

	public static RepackAllTiersAction getAction() {
		if (ACTION == null) {
			ACTION = new RepackAllTiersAction();
		}
		return ACTION;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		repackTiers(getTierManager().getAllTierLabels());
	}
}
