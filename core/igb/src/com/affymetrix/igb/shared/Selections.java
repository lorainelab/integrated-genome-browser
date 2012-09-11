package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.SeqMapRefreshed;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.IGBServiceImpl;
import com.affymetrix.igb.osgi.service.IGBService;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;
import javax.swing.event.EventListenerList;

/**
 *
 * @author hiralv
 */
public class Selections {
	public static final List<ITrackStyleExtended> allStyles = new ArrayList<ITrackStyleExtended>();
	public static final List<ITrackStyleExtended> annotStyles = new ArrayList<ITrackStyleExtended>();
	public static final List<StyledGlyph> allGlyphs = new ArrayList<StyledGlyph>();
	public static final List<GraphState> graphStates = new ArrayList<GraphState>();
	public static final List<GraphGlyph> graphGlyphs = new ArrayList<GraphGlyph>();
		
	private static final IGBService igbService;
	private static final EventListenerList listenerList;

	static{
		igbService = IGBServiceImpl.getInstance();
		listenerList = new EventListenerList();
		addListeners(new Listeners());
	}
	
	private Selections(){ }
	
	private static void addListeners(Listeners listeners){
		GenometryModel gmodel = GenometryModel.getGenometryModel();
		gmodel.addSeqSelectionListener(listeners);
		gmodel.addSymSelectionListener(listeners);
		igbService.getSeqMapView().addToRefreshList(listeners);
		TrackstylePropertyMonitor.getPropertyTracker().addPropertyListener(listeners);
//		igbService.addListSelectionListener(this);
	}
	
	private static void refreshSelection() {
		@SuppressWarnings({ "unchecked", "rawtypes", "cast" })
		List<StyledGlyph> selected = (List)igbService.getSeqMapView().getAllSelectedTiers();
		allStyles.clear();
		annotStyles.clear();
		graphStates.clear();
		graphGlyphs.clear();
		allGlyphs.clear();
		for (StyledGlyph useGlyph : selected) {
			FileTypeCategory category = useGlyph.getFileTypeCategory();
			if (useGlyph instanceof GraphGlyph){
				GraphGlyph gg = (GraphGlyph)useGlyph;
				graphStates.add(gg.getGraphState());
				allStyles.add(gg.getGraphState().getTierStyle());
				graphGlyphs.add(gg);
				allGlyphs.add(gg);
			}else if (useGlyph instanceof TierGlyph && ((TierGlyph)useGlyph).getTierType() == TierGlyph.TierType.GRAPH) {
				if (useGlyph.getChildCount() > 0) {
					for (GlyphI g : useGlyph.getChildren()) {
						if (g instanceof GraphGlyph) {
							GraphGlyph gg = (GraphGlyph)g;
							graphStates.add(gg.getGraphState());
							allStyles.add(gg.getGraphState().getTierStyle());
							graphGlyphs.add(gg);
							allGlyphs.add(gg);
						}
					}
				}
			}else if (category == FileTypeCategory.Annotation || category == FileTypeCategory.Alignment) {
				annotStyles.add(useGlyph.getAnnotStyle());
				allStyles.add(useGlyph.getAnnotStyle());
				allGlyphs.add(useGlyph);
			}
		}
		@SuppressWarnings({ "unchecked", "rawtypes", "cast" })
		List<GlyphI> selectedGraphs = (List)igbService.getSeqMapView().getFloatingGraphGlyphs();
		for (GlyphI glyph : selectedGraphs) {
			if (glyph instanceof GraphGlyph) {
				GraphGlyph gg = (GraphGlyph) glyph;
				graphStates.add(gg.getGraphState());
				allStyles.add(gg.getGraphState().getTierStyle());
				graphGlyphs.add(gg);
			}
		}
		notifyRefreshListener();
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
	}
	
	private static void notifyRefreshListener() {
		// Guaranteed to return a non-null array
		RefreshSelectionListener[] listeners = listenerList.getListeners(RefreshSelectionListener.class);
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 1; i >= 0; i -= 1) {
			listeners[i].selectionRefreshed();
		}
	}
	
	public static void addRefreshSelectionListener(RefreshSelectionListener listener){
		listenerList.add(RefreshSelectionListener.class, listener);
	}
	
	public static interface RefreshSelectionListener extends EventListener{
		public void selectionRefreshed();
	}
	
	private static class Listeners implements SeqSelectionListener, SymSelectionListener, TrackstylePropertyMonitor.TrackStylePropertyListener, SeqMapRefreshed {

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
		}

		@Override
		public void seqSelectionChanged(SeqSelectionEvent evt) {
			refreshSelection();
		}

		@Override
		public void trackstylePropertyChanged(EventObject eo) { // this is redundant when the source of the style change is this panel
			refreshSelection();
		}

		public void mapRefresh() {
			refreshSelection();
		}
	}
}
