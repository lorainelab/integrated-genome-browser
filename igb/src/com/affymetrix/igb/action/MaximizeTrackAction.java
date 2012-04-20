package com.affymetrix.igb.action;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

import java.awt.event.ActionEvent;

import com.affymetrix.igb.shared.TierGlyph;

public class MaximizeTrackAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static final MaximizeTrackAction ACTION = new MaximizeTrackAction();

	public static MaximizeTrackAction getAction() {
		return ACTION;
	}

	private MaximizeTrackAction() {
		super(BUNDLE.getString("maximizeTrackAction"), null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		TierGlyph current_tier = getTierManager().getSelectedTiers().get(0);
		getSeqMapView().focusTrack(current_tier);
	}

}
