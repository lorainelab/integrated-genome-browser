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
		List<ViewModeGlyph> selected = (List)igbService.getSeqMapView().getAllSelectedTiers();
		allGlyphs.clear();
		//allGlyphs.addAll(selected);
		graphGlyphs.clear();
		annotGlyphs.clear();
		for (ViewModeGlyph useGlyph : selected) {
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
							graphGlyphs.add(((AbstractGraphGlyph) g).getGraphGlyph());
							allGlyphs.add((ViewModeGlyph) g);
						}
					}
				}else{
					graphGlyphs.add(((AbstractGraphGlyph) useGlyph).getGraphGlyph());
					allGlyphs.add(useGlyph);
				}
			}
			else if (category == FileTypeCategory.Annotation || category == FileTypeCategory.Alignment) {
				annotGlyphs.add(useGlyph);
				allGlyphs.add(useGlyph);
			}
		}
		rootSyms.clear();
		rootSyms.addAll(TrackUtils.getInstance().getSymsFromViewModeGlyphs(allGlyphs));
		// First loop through and collect graphs and glyphs
		for (RootSeqSymmetry rootSym : rootSyms) {
			if (rootSym.getCategory() == FileTypeCategory.Annotation || rootSym.getCategory() == FileTypeCategory.Alignment) {
				annotSyms.add(rootSym);
			}
			if (rootSym.getCategory() == FileTypeCategory.Graph || rootSym.getCategory() == FileTypeCategory.ScoredContainer) {
				graphSyms.add(rootSym);
			}
		}
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
