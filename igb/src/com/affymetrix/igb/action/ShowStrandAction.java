package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.MultiGraphGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.shared.ViewModeGlyph;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.viewmode.MapViewModeHolder;

public class ShowStrandAction extends SeqMapViewActionA implements SymSelectionListener {
	private static final long serialVersionUID = 1L;
	private static ShowStrandAction ACTION;

	public static ShowStrandAction getAction() {
		if (ACTION == null) {
			ACTION = new ShowStrandAction(Application.getSingleton().getMapView());
		}
		return ACTION;
	}
	private boolean separateStrands;

	protected ShowStrandAction(SeqMapView gviewer) {
		super(gviewer, IGBConstants.BUNDLE.getString("showTwoTiersAction"), null, "images/strand_separate.png");
		GenometryModel.getGenometryModel().addSymSelectionListener(this);
	}

	protected void setTwoTiers(List<TierLabelGlyph> tier_label_glyphs, boolean b) {
		for (TierLabelGlyph tlg : tier_label_glyphs) {
			TierGlyph tier = (TierGlyph) tlg.getInfo();
			ITrackStyleExtended style = tier.getAnnotStyle();
			if (!b || MapViewModeHolder.getInstance().viewModeSupportsTwoTrack(style.getViewMode())) {
				style.setSeparate(b);
			}
		}
		refreshMap(false, true);
		handler.sortTiers();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		setTwoTiers(handler.getSelectedTierLabels(), separateStrands);
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		setStrand(new ArrayList<SeqSymmetry>());
	}

//	@Override
//	public String getText() {
//		return separateStrands ? IGBConstants.BUNDLE.getString("showTwoTiersAction") : IGBConstants.BUNDLE.getString("showSingleTierAction");
//	}

//	@Override
//	public String getIconPath() {
//		return separateStrands ? "images/strand_separate.png" : "images/strand_mixed.png";
//	}

	@Override
	public void symSelectionChanged(SymSelectionEvent evt) {
		List<SeqSymmetry> selected_syms = evt.getSelectedSyms();
		// Only pay attention to selections from the main SeqMapView or its map.
		// Ignore the splice view as well as events coming from this class itself.

		Object src = evt.getSource();
		if (!(src == gviewer || src == gviewer.getSeqMap())) {
			return;
		}

		setStrand(selected_syms);
	}

	private void setStrand(List<SeqSymmetry> selected_syms) {
		boolean hasSeparate = false;
		boolean hasMixed = false;
		for (TierGlyph tg : gviewer.getTierManager().getVisibleTierGlyphs()) {
			ViewModeGlyph vg = tg.getViewModeGlyph();
			if (vg instanceof MultiGraphGlyph) {
				for (GlyphI child : vg.getChildren()) {
					if (selected_syms.contains(child.getInfo())) {
						boolean separate = ((AbstractGraphGlyph) child).getAnnotStyle().getSeparate();
						hasSeparate |= separate;
						hasMixed |= !separate;
					}
				}
			}
			else if (selected_syms.contains(vg.getInfo())) {
				boolean separate = vg.getAnnotStyle().getSeparate();
				hasSeparate |= separate;
				hasMixed |= !separate;
			}
		}
		separateStrands = !separateStrands;//!hasSeparate || hasMixed;
		Object oldValue = getValue(Action.SMALL_ICON);
		setProperties(false);
		Object newValue = getValue(Action.SMALL_ICON);
		this.firePropertyChange(Action.SMALL_ICON, oldValue, newValue);
	}
}
