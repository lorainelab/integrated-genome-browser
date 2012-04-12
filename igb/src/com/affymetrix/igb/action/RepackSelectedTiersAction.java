package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.view.SeqMapView;

public class RepackSelectedTiersAction extends RepackTiersAction {
	private static final long serialVersionUID = 1L;
	private static RepackSelectedTiersAction ACTION;

	public RepackSelectedTiersAction(SeqMapView gviewer) {
		super(gviewer);
	}

	public static RepackSelectedTiersAction getAction() {
		if (ACTION == null) {
			ACTION = new RepackSelectedTiersAction(Application.getSingleton().getMapView());
		}
		return ACTION;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		repackTiers(handler.getSelectedTierLabels());
	}

	@Override
	public String getText() {
		return IGBConstants.BUNDLE.getString("repackSelectedTracksAction");
	}

	@Override
	public String getIconPath() {
		return "toolbarButtonGraphics/general/AlignJustifyHorizontal16.gif";
	}
}
