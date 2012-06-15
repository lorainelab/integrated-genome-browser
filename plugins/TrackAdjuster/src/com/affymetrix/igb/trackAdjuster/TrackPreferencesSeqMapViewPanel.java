package com.affymetrix.igb.trackAdjuster;

import java.util.Collections;
import java.util.List;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.MultiGraphGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackPreferencesA;
import com.affymetrix.igb.shared.ViewModeGlyph;

public class TrackPreferencesSeqMapViewPanel extends TrackPreferencesA implements SeqSelectionListener, SymSelectionListener {
	private static final long serialVersionUID = 1L;

	public TrackPreferencesSeqMapViewPanel(IGBService _igbService) {
		super(_igbService);
		GenometryModel gmodel = GenometryModel.getGenometryModel();
		gmodel.addSeqSelectionListener(this);
		gmodel.addSymSelectionListener(this);
//		TrackstylePropertyMonitor.getPropertyTracker().addPropertyListener(this);
//		igbService.addListSelectionListener(this);
	}

	private void collectSymsAndGlyphs(List<RootSeqSymmetry> selected_syms, int symcount) {
		if (rootSyms != selected_syms) {
			// in certain cases selected_syms arg and grafs list may be same, for example when method is being
			//     called to catch changes in glyphs representing selected sym, not the syms themselves)
			//     therefore don't want to change grafs list if same as selected_syms (especially don't want to clear it!)
			rootSyms.clear();
			annotSyms.clear();
			graphSyms.clear();
		}
		allGlyphs.clear();
		graphGlyphs.clear();
		annotGlyphs.clear();
		for (Glyph glyph : igbService.getSelectedTierGlyphs()) {
			ViewModeGlyph vg = ((TierGlyph)glyph).getViewModeGlyph();
			allGlyphs.add(vg);
			if (vg instanceof AbstractGraphGlyph) {
				if (vg instanceof MultiGraphGlyph) {
					for (GlyphI child : vg.getChildren()) {
						if (rootSyms.contains(child.getInfo())) {
							graphGlyphs.add((AbstractGraphGlyph) child);
						}
					}
				}else if (!graphGlyphs.contains(vg)) {
					graphGlyphs.add((AbstractGraphGlyph)vg);
				}
			}
			else if (vg.getInfo() != null && vg.getInfo() instanceof RootSeqSymmetry && (((RootSeqSymmetry)vg.getInfo()).getCategory() == FileTypeCategory.Annotation || ((RootSeqSymmetry)vg.getInfo()).getCategory() == FileTypeCategory.Alignment)) {
				annotGlyphs.add(((TierGlyph)glyph).getViewModeGlyph());
			}
		}
		// First loop through and collect graphs and glyphs
		for (int i = 0; i < symcount; i++) {
			RootSeqSymmetry rootSym = selected_syms.get(i);
			// only add to grafs if list is not identical to selected_syms arg
			if (rootSyms != selected_syms) {
				rootSyms.add(rootSym);
				if (rootSym.getCategory() == FileTypeCategory.Annotation || rootSym.getCategory() == FileTypeCategory.Alignment) {
					annotSyms.add(rootSym);
				}
				if (rootSym.getCategory() == FileTypeCategory.Graph || rootSym.getCategory() == FileTypeCategory.ScoredContainer) {
					graphSyms.add(rootSym);
				}
			}
			// add all graph glyphs representing graph sym
			//	  System.out.println("found multiple glyphs for graph sym: " + multigl.size());
//				for (Glyph g : igbService.getVisibleTierGlyphs()) {
//					ViewModeGlyph vg = ((TierGlyph) g).getViewModeGlyph();
//					if (vg instanceof MultiGraphGlyph) {
//						for (GlyphI child : vg.getChildren()) {
//							if (grafs.contains(child.getInfo())) {
//								glyphs.add((AbstractGraphGlyph) child);
//							}
//						}
//					} else if (grafs.contains(vg.getInfo())) {
//						glyphs.add((AbstractGraphGlyph) vg);
//					}
//				}
			
			Glyph glyph = igbService.getSeqMap().getItem(rootSym);
			if (glyph != null) {
				if (glyph instanceof AbstractGraphGlyph) {
					if (glyph instanceof MultiGraphGlyph) {
						for (GlyphI child : glyph.getChildren()) {
							if (rootSyms.contains(child.getInfo())) {
								graphGlyphs.add((AbstractGraphGlyph) child);
								allGlyphs.add((AbstractGraphGlyph) child);
							}
						}
					}else if (!graphGlyphs.contains(glyph)) {
						graphGlyphs.add((AbstractGraphGlyph)glyph);
						allGlyphs.add((AbstractGraphGlyph) glyph);
					}
				}
			}
		}
	}

	private void refreshSelection(List<RootSeqSymmetry> selected_syms) {
		is_listening = false; // turn off propagation of events from the GUI while we modify the settings
		int symcount = selected_syms.size();
		collectSymsAndGlyphs(selected_syms, symcount);
		@SuppressWarnings("unchecked")
		List<TierGlyph> tierList = (List<TierGlyph>) igbService.getSeqMapView().getSelectedTiers();
		if (selectedTiers != tierList) {
			selectedTiers.clear();
			for (TierGlyph tier : tierList) {
				if (!selectedTiers.contains(tier)) {
					selectedTiers.add(tier);
				}
			}
		}
		is_listening = true; // turn back on GUI events
	}

	@Override
	public void symSelectionChanged(SymSelectionEvent evt) {
		List<RootSeqSymmetry> selected_syms = evt.getAllSelectedSyms();
		
		// Selected sym contains graph sym
		for(SeqSymmetry sym: evt.getSelectedGraphSyms()){
			if(sym instanceof RootSeqSymmetry){
				selected_syms.add((RootSeqSymmetry)sym);
			}
		}
		// Only pay attention to selections from the main SeqMapView or its map.
		// Ignore the splice view as well as events coming from this class itself.

		Object src = evt.getSource();
		if (!(src == igbService.getSeqMapView() || src == igbService.getSeqMap())
				|| igbService.getSeqMap() == null || igbService.getSeqMapView() == null) {
			return;
		}

		refreshSelection(selected_syms);
		resetAll();
	}
	@Override
	public void seqSelectionChanged(SeqSelectionEvent evt) {
//		current_seq = evt.getSelectedSeq();
//		refreshSelection(gmodel.getSelectedSymmetries(current_seq));
		refreshSelection(Collections.<RootSeqSymmetry>emptyList());
		resetAll();
	}
}
