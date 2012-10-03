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
import com.affymetrix.igb.tiers.TrackConstants;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.factories.DefaultTierGlyph;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;
import javax.swing.event.EventListenerList;

/**
 *
 * @author hiralv
 */
public abstract class Selections {
	public static final List<ITrackStyleExtended> allStyles = new ArrayList<ITrackStyleExtended>();
	public static final List<ITrackStyleExtended> annotStyles = new ArrayList<ITrackStyleExtended>();
	public static final List<StyledGlyph> allGlyphs = new ArrayList<StyledGlyph>();
	public static final List<GraphState> graphStates = new ArrayList<GraphState>();
	public static final List<GraphGlyph> graphGlyphs = new ArrayList<GraphGlyph>();
	public static final List<RootSeqSymmetry> rootSyms = new ArrayList<RootSeqSymmetry>();
	
	private static final SeqMapView smv;
	private static final EventListenerList listenerList;

	static{
		smv = (SeqMapView) IGBServiceImpl.getInstance().getSeqMapView();
		listenerList = new EventListenerList();
		addListeners(new Listeners());
	}
	
	private Selections(){ }
	
	private static void addListeners(Listeners listeners){
		GenometryModel gmodel = GenometryModel.getGenometryModel();
		gmodel.addSeqSelectionListener(listeners);
		gmodel.addSymSelectionListener(listeners);
		smv.addToRefreshList(listeners);
		TrackstylePropertyMonitor.getPropertyTracker().addPropertyListener(listeners);
//		igbService.addListSelectionListener(this);
	}
	
	private static void refreshSelection() {
		@SuppressWarnings({ "unchecked", "rawtypes", "cast" })
		List<StyledGlyph> selected = (List)smv.getAllSelectedTiers();
		allStyles.clear();
		annotStyles.clear();
		graphStates.clear();
		graphGlyphs.clear();
		allGlyphs.clear();
		rootSyms.clear();
		for (StyledGlyph useGlyph : selected) {
			FileTypeCategory category = useGlyph.getFileTypeCategory();
			if (useGlyph instanceof GraphGlyph){
				GraphGlyph gg = (GraphGlyph)useGlyph;
				graphStates.add(gg.getGraphState());
				allStyles.add(gg.getGraphState().getTierStyle());
				graphGlyphs.add(gg);
				allGlyphs.add(gg);
				rootSyms.add((RootSeqSymmetry)gg.getInfo());
			}else if (useGlyph instanceof TierGlyph && ((TierGlyph)useGlyph).getTierType() == TierGlyph.TierType.GRAPH) {
				if (useGlyph.getChildCount() > 0) {
					for (GlyphI g : useGlyph.getChildren()) {
						if (g instanceof GraphGlyph) {
							GraphGlyph gg = (GraphGlyph)g;
							graphStates.add(gg.getGraphState());
							allStyles.add(gg.getGraphState().getTierStyle());
							graphGlyphs.add(gg);
							allGlyphs.add(gg);
							rootSyms.add((RootSeqSymmetry)gg.getInfo());
						}
					}
				}
			}else if (category == FileTypeCategory.Annotation || category == FileTypeCategory.Alignment) {
				annotStyles.add(useGlyph.getAnnotStyle());
				allStyles.add(useGlyph.getAnnotStyle());
				allGlyphs.add(useGlyph);
				rootSyms.add((RootSeqSymmetry)useGlyph.getInfo());
			}
		}
		@SuppressWarnings({ "unchecked", "rawtypes", "cast" })
		List<GlyphI> selectedGraphs = (List)smv.getFloatingGraphGlyphs();
		for (GlyphI glyph : selectedGraphs) {
			if (glyph instanceof GraphGlyph) {
				GraphGlyph gg = (GraphGlyph) glyph;
				graphStates.add(gg.getGraphState());
				allStyles.add(gg.getGraphState().getTierStyle());
				graphGlyphs.add(gg);
				rootSyms.add((RootSeqSymmetry)gg.getInfo());
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
	
	public static boolean isAllGraph() {
		return allStyles.size() == graphStates.size() && graphStates.size() > 0;
	}

	public static boolean isAllAnnot() {
		return allStyles.size() == annotStyles.size() && annotStyles.size() > 0;
	}

	public static boolean isAnyJoined(){
		for (GraphState state : graphStates) {
			if (state.getComboStyle() != null) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isOneJoined(){
		if(graphStates.size() < 2)
			return false;
		
		Object comboStyle = graphStates.get(0).getComboStyle();
		if(comboStyle == null)
			return false;
		
		for(int i=1; i<graphStates.size(); i++){
			if(graphStates.get(i).getComboStyle() != comboStyle){
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean isAnyFloat() {
		for (ITrackStyleExtended style : allStyles) {
			if (style.getFloatTier()) {
				return true;
			}
		}
		return false;
	}

	public static boolean isAllSupportTwoTrack() {
		for (StyledGlyph glyph : allGlyphs) {
			if (!MapTierTypeHolder.getInstance().supportsTwoTrack(glyph.getFileTypeCategory())) {
				return false;
			}
		}
		return true;
	}

	public static boolean isAllGraphStyleLocked() {
		for(GraphState state : graphStates){
			if(!state.getGraphStyleLocked()){
				return false;
			}
		}
		return true;
	}
	
	public static boolean isAllRootSeqSymmetrySame(){
		if(rootSyms.isEmpty())
			return false;
		
		if(rootSyms.size() == 1)
			return true;
		
		for(int i=1; i<rootSyms.size(); i++){
			if(rootSyms.get(0).getCategory() != rootSyms.get(i).getCategory()){
				return false;
			}
		}
		
		return true;
	}
	
	
	public static boolean isAllStrandsColor() {
		boolean allColor = true;
		for (ITrackStyleExtended style : annotStyles) {
			if (!(style.getDirectionType() == TrackConstants.DIRECTION_TYPE.COLOR.ordinal() 
					|| style.getDirectionType() == TrackConstants.DIRECTION_TYPE.BOTH.ordinal())) {
				allColor = false;
				break;
			}
		}
		return allColor;
	}
	
	public static boolean isAllStrandsArrow() {
		boolean allArrow = true;
		for (ITrackStyleExtended style : annotStyles) {
			if (!(style.getDirectionType() == TrackConstants.DIRECTION_TYPE.ARROW.ordinal() 
					|| style.getDirectionType() == TrackConstants.DIRECTION_TYPE.BOTH.ordinal())) {
				allArrow = false;
				break;
			}
		}
		return allArrow;
	}
	
	public static boolean isAnyLocked(){
		for (StyledGlyph glyph : allGlyphs) {
			if(glyph instanceof DefaultTierGlyph && ((DefaultTierGlyph)glyph).isHeightFixed()){
				return true;
			}
		}
		return false;
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
		
	/*
	 * Interface to notify selection has been updated.
	 */
	public static interface RefreshSelectionListener extends EventListener{
		public void selectionRefreshed();
	}
	
	/**
	 * Inner class to fire selection refreshed events.
	 */
	private static class Listeners implements SeqSelectionListener, SymSelectionListener, TrackstylePropertyMonitor.TrackStylePropertyListener, SeqMapRefreshed {

		@Override
		public void symSelectionChanged(SymSelectionEvent evt) {
			// Only pay attention to selections from the main SeqMapView or its map.
			// Ignore the splice view as well as events coming from this class itself.

			Object src = evt.getSource();
			if (!(src == smv ||  src == smv.getSeqMap())
					  || smv == null || smv.getSeqMap() == null) {
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
