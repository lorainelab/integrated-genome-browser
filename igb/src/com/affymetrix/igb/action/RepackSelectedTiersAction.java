package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.igb.IGBConstants;

public class RepackSelectedTiersAction extends RepackTiersAction {
	private static final long serialVersionUID = 1L;
	private static RepackSelectedTiersAction ACTION;

	public RepackSelectedTiersAction() {
		super(IGBConstants.BUNDLE.getString("repackSelectedTracksAction"), "toolbarButtonGraphics/general/AlignJustifyHorizontal16.gif");
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
