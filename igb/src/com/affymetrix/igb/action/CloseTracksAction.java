package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.util.List;

import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.view.load.GeneralLoadView;

public class CloseTracksAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static CloseTracksAction ACTION;

	public static CloseTracksAction getAction() {
		if (ACTION == null) {
			ACTION = new CloseTracksAction();
		}
		return ACTION;
	}

	protected CloseTracksAction() {
		super(IGBConstants.BUNDLE.getString("closeTracksAction"), "16x16/status/user-trash-full.png", "32x32/status/user-trash-full.png");
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		List<TierLabelGlyph> tiers = getTierManager().getSelectedTierLabels();
		for (TierLabelGlyph tlg : tiers) {
			TierGlyph tg = (TierGlyph)tlg.getInfo();
			GenericFeature gFeature = tg.getAnnotStyle().getFeature();
			if (gFeature != null) {
				GeneralLoadView.getLoadView().removeFeature(gFeature, true);
			}
		}
	}
}
