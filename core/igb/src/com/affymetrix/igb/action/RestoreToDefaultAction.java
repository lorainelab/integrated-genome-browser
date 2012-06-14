package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genoviz.bioviews.GlyphI;
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
		if (getSeqMapView().getSelectedTiers() == null) {
			return;
		}
		for (GlyphI glyph : getSeqMapView().getSelectedTiers()) {
			TierGlyph tierGlyph = (TierGlyph)glyph;
			if (tierGlyph.getAnnotStyle() instanceof TrackStyle) {
				((TrackStyle)tierGlyph.getAnnotStyle()).restoreToDefault();
			}
		}
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		getSeqMapView().updatePanel();
	}
}
