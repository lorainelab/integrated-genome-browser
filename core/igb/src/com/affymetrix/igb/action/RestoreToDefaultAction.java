package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TrackStyle;

public class RestoreToDefaultAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;

	private static final RestoreToDefaultAction ACTION = new RestoreToDefaultAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static RestoreToDefaultAction getAction() {
		return ACTION;
	}

	public RestoreToDefaultAction() {
		super("Restore to Default", null, null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		if (getTierManager().getSelectedTiers() == null) {
			return;
		}
		for (TierGlyph tierGlyph : getTierManager().getSelectedTiers()) {
			if (tierGlyph.getAnnotStyle() instanceof TrackStyle) {
				((TrackStyle)tierGlyph.getAnnotStyle()).restoreToDefault();
			}
			if(tierGlyph.getViewModeGlyph() instanceof AbstractGraphGlyph ){
				((AbstractGraphGlyph)tierGlyph.getViewModeGlyph()).getGraphGlyph().getGraphState().restoreToDefault();
			}
		}
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		getSeqMapView().updatePanel();
	}
}
