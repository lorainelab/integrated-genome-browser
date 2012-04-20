package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.MultiGraphGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.shared.ViewModeGlyph;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.viewmode.MapViewModeHolder;

public abstract class ShowStrandActionA extends SeqMapViewActionA implements SymSelectionListener {
	private static final long serialVersionUID = 1L;
	protected boolean separateStrands;

	protected ShowStrandActionA(String text, String iconPath) {
		super(text, iconPath);
		GenometryModel.getGenometryModel().addSymSelectionListener(this);
	}

	private void setTwoTiers(List<TierLabelGlyph> tier_label_glyphs, boolean b) {
		for (TierLabelGlyph tlg : tier_label_glyphs) {
			TierGlyph tier = (TierGlyph) tlg.getInfo();
			ITrackStyleExtended style = tier.getAnnotStyle();
			if (!b || MapViewModeHolder.getInstance().styleSupportsTwoTrack(style)) {
				style.setSeparate(b);
			}
		}
		refreshMap(false, true);
		getTierManager().sortTiers();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		setTwoTiers(getTierManager().getSelectedTierLabels(), separateStrands);
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		changeStrandActionDisplay(new ArrayList<SeqSymmetry>());
	}

	@SuppressWarnings("unchecked")
	@Override
	public void symSelectionChanged(SymSelectionEvent evt) {
		SeqMapView gviewer = getSeqMapView();
		List<SeqSymmetry> selected_syms = SeqMapView.glyphsToSyms((List<GlyphI>)gviewer.getSelectedTiers());
		// Only pay attention to selections from the main SeqMapView or its map.
		// Ignore the splice view as well as events coming from this class itself.

		Object src = evt.getSource();
		if (!(src == gviewer || src == gviewer.getSeqMap())) {
			return;
		}

		changeStrandActionDisplay(selected_syms);
	}

	protected abstract void processChange(boolean hasSeparate, boolean hasMixed);

	private void changeStrandActionDisplay(List<SeqSymmetry> selected_syms) {
		boolean hasSeparate = false;
		boolean hasMixed = false;
		for (TierGlyph tg : getSeqMapView().getTierManager().getVisibleTierGlyphs()) {
			ViewModeGlyph vg = tg.getViewModeGlyph();
			if (selected_syms.contains(vg.getInfo()) && !(vg instanceof MultiGraphGlyph) && MapViewModeHolder.getInstance().styleSupportsTwoTrack(vg.getAnnotStyle())) {
				boolean separate = vg.getAnnotStyle().getSeparate();
				hasSeparate |= separate;
				hasMixed |= !separate;
			}
		}
		processChange(hasSeparate, hasMixed);
	}
}
