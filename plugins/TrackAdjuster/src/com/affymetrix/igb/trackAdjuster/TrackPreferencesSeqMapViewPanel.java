package com.affymetrix.igb.trackAdjuster;

import java.util.EventObject;
import java.util.List;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.*;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.*;

public class TrackPreferencesSeqMapViewPanel extends TrackPreferencesA implements SeqSelectionListener, SymSelectionListener, TrackstylePropertyMonitor.TrackStylePropertyListener, SeqMapRefreshed {
	private static final long serialVersionUID = 1L;

	public TrackPreferencesSeqMapViewPanel(IGBService _igbService) {
		super(_igbService);
		GenometryModel gmodel = GenometryModel.getGenometryModel();
		gmodel.addSeqSelectionListener(this);
		gmodel.addSymSelectionListener(this);
		igbService.getSeqMapView().addToRefreshList(this);
		TrackstylePropertyMonitor.getPropertyTracker().addPropertyListener(this);
//		igbService.addListSelectionListener(this);
	}

	private void refreshSelection() {
		is_listening = false; // turn off propagation of events from the GUI while we modify the settings
		@SuppressWarnings({ "unchecked", "rawtypes", "cast" })
		List<TierGlyph> selected = (List)igbService.getSeqMapView().getAllSelectedTiers();
		allStyles.clear();
		//allGlyphs.addAll(selected);
		graphState.clear();
		annotStyles.clear();
		for (TierGlyph useGlyph : selected) {
			FileTypeCategory category = null;
			if (useGlyph.getInfo() instanceof RootSeqSymmetry) {
				category = ((RootSeqSymmetry)useGlyph.getInfo()).getCategory();
			}
			if (category == null && useGlyph.getAnnotStyle() != null) {
				category = useGlyph.getAnnotStyle().getFileTypeCategory();
			}
			if (useGlyph instanceof AbstractGraphGlyph) {
				if (useGlyph instanceof MultiGraphGlyph && useGlyph.getChildCount() > 0) {
					for (GlyphI g : useGlyph.getChildren()) {
						if (g instanceof AbstractGraphGlyph) {
							graphState.add(((AbstractGraphGlyph) g).getGraphGlyph().getGraphState());
							allStyles.add(((ViewModeGlyph) g).getAnnotStyle());
						}
					}
				}else{
					graphState.add(((AbstractGraphGlyph) useGlyph).getGraphGlyph().getGraphState());
					allStyles.add(useGlyph.getAnnotStyle());
				}
			}
			else if (category == FileTypeCategory.Annotation || category == FileTypeCategory.Alignment) {
				annotStyles.add(useGlyph.getAnnotStyle());
				allStyles.add(useGlyph.getAnnotStyle());
			}
		}
//		rootSyms.clear();
//		rootSyms.addAll(TrackUtils.getInstance().getSymsFromViewModeGlyphs(allGlyphs));
//		// First loop through and collect graphs and glyphs
//		for (RootSeqSymmetry rootSym : rootSyms) {
//			if (rootSym.getCategory() == FileTypeCategory.Annotation || rootSym.getCategory() == FileTypeCategory.Alignment) {
//				annotSyms.add(rootSym);
//			}
//			if (rootSym.getCategory() == FileTypeCategory.Graph || rootSym.getCategory() == FileTypeCategory.ScoredContainer) {
//				graphSyms.add(rootSym);
//			}
//		}
		is_listening = true; // turn back on GUI events
	}

	@Override
	public void symSelectionChanged(SymSelectionEvent evt) {
		// Only pay attention to selections from the main SeqMapView or its map.
		// Ignore the splice view as well as events coming from this class itself.

		Object src = evt.getSource();
		if (!(src == igbService.getSeqMapView() || src == igbService.getSeqMap())
				|| igbService.getSeqMap() == null || igbService.getSeqMapView() == null) {
			return;
		}

		refreshSelection();
		resetAll();
	}
	@Override
	public void seqSelectionChanged(SeqSelectionEvent evt) {
		refreshSelection();
		resetAll();
	}

	@Override
	public void trackstylePropertyChanged(EventObject eo) { // this is redundant when the source of the style change is this panel
		refreshSelection();
		resetAll();
	}

	public void mapRefresh() {
		refreshSelection();
		resetAll();
		//selectAllButtonReset();
	}
}
