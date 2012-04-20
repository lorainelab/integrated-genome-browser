package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.igb.IGBConstants;

public class RepackAllTiersAction extends RepackTiersAction {
	private static final long serialVersionUID = 1L;
	private static RepackAllTiersAction ACTION;

	public RepackAllTiersAction() {
		super(IGBConstants.BUNDLE.getString("repackAllTracksAction"), "toolbarButtonGraphics/general/AlignJustifyHorizontal16.gif");
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
