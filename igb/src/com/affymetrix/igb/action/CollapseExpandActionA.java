package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.MultiGraphGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.shared.ViewModeGlyph;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.viewmode.AnnotationGlyph;

public abstract class CollapseExpandActionA extends SeqMapViewActionA implements SymSelectionListener {
	private static final long serialVersionUID = 1L;
	protected boolean collapsedTracks;

	protected CollapseExpandActionA(SeqMapView gviewer, String text,
			String tooltip, String iconPath) {
		super(gviewer, text, tooltip, iconPath);
		GenometryModel.getGenometryModel().addSymSelectionListener(this);
	}

	private void setTiersCollapsed(List<TierLabelGlyph> tier_labels, boolean collapsed) {
		handler.setTiersCollapsed(tier_labels, collapsed);
		gviewer.getSeqMap().updateWidget();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		setTiersCollapsed(handler.getSelectedTierLabels(), collapsedTracks);
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		changeActionDisplay(new ArrayList<SeqSymmetry>());
	}

	@SuppressWarnings("unchecked")
	@Override
	public void symSelectionChanged(SymSelectionEvent evt) {
		List<SeqSymmetry> selected_syms = SeqMapView.glyphsToSyms((List<GlyphI>)gviewer.getSelectedTiers());
		// Only pay attention to selections from the main SeqMapView or its map.
		// Ignore the splice view as well as events coming from this class itself.

		Object src = evt.getSource();
		if (!(src == gviewer || src == gviewer.getSeqMap())) {
			return;
		}

		changeActionDisplay(selected_syms);
	}

	protected abstract void processChange(boolean hasCollapsed, boolean hasExpanded);

	private void changeActionDisplay(List<SeqSymmetry> selected_syms) {
		boolean hasCollapsed = false;
		boolean hasExpanded = false;
		for (TierGlyph tg : gviewer.getTierManager().getVisibleTierGlyphs()) {
			ViewModeGlyph vg = tg.getViewModeGlyph();
			if (selected_syms.contains(vg.getInfo()) && !(vg instanceof MultiGraphGlyph) && vg instanceof AnnotationGlyph) {
				boolean collapsed = vg.getAnnotStyle().getCollapsed();
				hasCollapsed |= collapsed;
				hasExpanded |= !collapsed;
			}
		}
		processChange(hasCollapsed, hasExpanded);
	}
}
