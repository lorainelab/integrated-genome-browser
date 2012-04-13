package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.view.SeqMapView;

public class RepackAllTiersAction extends RepackTiersAction {
	private static final long serialVersionUID = 1L;
	private static RepackAllTiersAction ACTION;

	public RepackAllTiersAction(SeqMapView gviewer) {
		super(gviewer, IGBConstants.BUNDLE.getString("repackAllTracksAction"), null, "toolbarButtonGraphics/general/AlignJustifyHorizontal16.gif");
	}

	public static RepackAllTiersAction getAction() {
		if (ACTION == null) {
			ACTION = new RepackAllTiersAction(Application.getSingleton().getMapView());
		}
		return ACTION;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		repackTiers(handler.getAllTierLabels());
	}
}
