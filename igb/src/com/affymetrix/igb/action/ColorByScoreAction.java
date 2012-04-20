package com.affymetrix.igb.action;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

import java.awt.event.ActionEvent;
import java.util.List;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TierLabelGlyph;

public class ColorByScoreAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static final ColorByScoreAction ON_ACTION = new ColorByScoreAction("colorByScoreONAction", true);
	private static final ColorByScoreAction OFF_ACTION = new ColorByScoreAction("colorByScoreOFFAction", false);
	private final boolean on;
	
	public static ColorByScoreAction getOnAction() {
		return ON_ACTION;
	}

	public static ColorByScoreAction getOffAction() {
		return OFF_ACTION;
	}

	private ColorByScoreAction(String transKey, boolean on) {
		super(BUNDLE.getString(transKey), null);
		this.on = on;
	}

	private void setColorByScore(List<TierLabelGlyph> tier_labels, boolean b) {
		for (TierLabelGlyph tlg : tier_labels) {
			ITrackStyleExtended style = tlg.getReferenceTier().getAnnotStyle();
			style.setColorByScore(b);
		}

		refreshMap(false, false);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		setColorByScore(getTierManager().getSelectedTierLabels(), on);
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}

}
