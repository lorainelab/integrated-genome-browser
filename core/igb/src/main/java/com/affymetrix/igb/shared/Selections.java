package com.affymetrix.igb.shared;

import java.util.EventListener;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.event.EventListenerList;

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
import com.affymetrix.genoviz.glyph.SolidGlyph;

import com.affymetrix.igb.IGBServiceImpl;
import com.affymetrix.igb.tiers.CoordinateStyle;
import com.affymetrix.igb.tiers.TrackConstants;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.factories.DefaultTierGlyph;

/**
 *
 * @author hiralv
 */
public abstract class Selections {

    public static final List<ITrackStyleExtended> allStyles = new CopyOnWriteArrayList<ITrackStyleExtended>();
    public static final List<ITrackStyleExtended> annotStyles = new CopyOnWriteArrayList<ITrackStyleExtended>();
    public static final List<ITrackStyleExtended> graphStyles = new CopyOnWriteArrayList<ITrackStyleExtended>();
    public static final List<StyledGlyph> allGlyphs = new CopyOnWriteArrayList<StyledGlyph>();
    public static final List<GraphState> graphStates = new CopyOnWriteArrayList<GraphState>();
    public static final List<GraphGlyph> graphGlyphs = new CopyOnWriteArrayList<GraphGlyph>();
    public static final List<RootSeqSymmetry> rootSyms = new CopyOnWriteArrayList<RootSeqSymmetry>();
    public static final List<RootSeqSymmetry> annotSyms = new CopyOnWriteArrayList<RootSeqSymmetry>();
    public static final List<RootSeqSymmetry> graphSyms = new CopyOnWriteArrayList<RootSeqSymmetry>();
    public static final List<ITrackStyleExtended> axisStyles = new CopyOnWriteArrayList<ITrackStyleExtended>();

    private static final SeqMapView smv;
    private static final EventListenerList listenerList;

    static {
        smv = (SeqMapView) IGBServiceImpl.getInstance().getSeqMapView();
        listenerList = new EventListenerList();
        addListeners(new Listeners());
    }

    private Selections() {
    }

    private static void addListeners(Listeners listeners) {
        GenometryModel gmodel = GenometryModel.getGenometryModel();
        gmodel.addSeqSelectionListener(listeners);
        gmodel.addSymSelectionListener(listeners);
        smv.addToRefreshList(listeners);
        TrackstylePropertyMonitor.getPropertyTracker().addPropertyListener(listeners);
//		igbService.addListSelectionListener(this);
    }

    private synchronized static void refreshSelection() {
        @SuppressWarnings({"unchecked", "rawtypes", "cast"})
        List<StyledGlyph> selected = (List) smv.getAllSelectedTiers();
        allStyles.clear();
        annotStyles.clear();
        graphStyles.clear();
        graphStates.clear();
        graphGlyphs.clear();
        allGlyphs.clear();
        rootSyms.clear();
        annotSyms.clear();
        graphSyms.clear();
        axisStyles.clear();
        for (StyledGlyph useGlyph : selected) {
            FileTypeCategory category = useGlyph.getFileTypeCategory();
            if (useGlyph instanceof GraphGlyph) {
                GraphGlyph gg = (GraphGlyph) useGlyph;
                graphStates.add(gg.getGraphState());
                graphStyles.add(gg.getGraphState().getTierStyle());
                allStyles.add(gg.getGraphState().getTierStyle());
                graphGlyphs.add(gg);
                allGlyphs.add(gg);
                if (gg.getInfo() != null) {
                    rootSyms.add((RootSeqSymmetry) gg.getInfo());
                    graphSyms.add((RootSeqSymmetry) gg.getInfo());
                }
            } else if (useGlyph instanceof TierGlyph && ((TierGlyph) useGlyph).getTierType() == TierGlyph.TierType.GRAPH) {
                if (useGlyph.getChildCount() > 0) {
                    for (GlyphI g : useGlyph.getChildren()) {
                        if (g instanceof GraphGlyph) {
                            GraphGlyph gg = (GraphGlyph) g;
                            graphStates.add(gg.getGraphState());
                            graphStyles.add(gg.getGraphState().getTierStyle());
                            allStyles.add(gg.getGraphState().getTierStyle());
                            graphGlyphs.add(gg);
                            allGlyphs.add(gg);
                            if (gg.getInfo() != null) {
                                rootSyms.add((RootSeqSymmetry) gg.getInfo());
                                graphSyms.add((RootSeqSymmetry) gg.getInfo());
                            }
                        } else if (useGlyph.getChildCount() == 1 && g instanceof SolidGlyph) { // This happens for graph when the data is cleared
                            allStyles.add(useGlyph.getAnnotStyle());
                            allGlyphs.add(useGlyph);
                        }
                    }
                }
            } else if (category == FileTypeCategory.Annotation || category == FileTypeCategory.Alignment
                    || category == FileTypeCategory.ProbeSet || category == FileTypeCategory.PairedRead) {
                annotStyles.add(useGlyph.getAnnotStyle());
                allStyles.add(useGlyph.getAnnotStyle());
                allGlyphs.add(useGlyph);
                if (useGlyph.getInfo() != null && category != FileTypeCategory.PairedRead) {
                    rootSyms.add((RootSeqSymmetry) useGlyph.getInfo());
                    annotSyms.add((RootSeqSymmetry) useGlyph.getInfo());
                }
            } else if (category == FileTypeCategory.Axis) {
                allGlyphs.add(useGlyph);
                axisStyles.add(useGlyph.getAnnotStyle());
            } else if (category == null) { // This happens when feature checked but data is not loaded
                allStyles.add(useGlyph.getAnnotStyle());
                allGlyphs.add(useGlyph);
                if (useGlyph.getAnnotStyle().isGraphTier()) {
                    graphStyles.add(useGlyph.getAnnotStyle());
                } else {
                    annotStyles.add(useGlyph.getAnnotStyle());
                }
            }
        }
        @SuppressWarnings({"unchecked", "rawtypes", "cast"})
        List<GlyphI> selectedGraphs = (List) smv.getSelectedFloatingGraphGlyphs();
        for (GlyphI glyph : selectedGraphs) {
            if (glyph instanceof GraphGlyph) {
                GraphGlyph gg = (GraphGlyph) glyph;
                graphStates.add(gg.getGraphState());
                graphStyles.add(gg.getGraphState().getTierStyle());
                allStyles.add(gg.getGraphState().getTierStyle());
                graphGlyphs.add(gg);
                allGlyphs.add(gg);
                if (gg.getInfo() != null) {
                    rootSyms.add((RootSeqSymmetry) gg.getInfo());
                    graphSyms.add((RootSeqSymmetry) gg.getInfo());
                }
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

    public static boolean isAnyJoined() {
        for (GraphState state : graphStates) {
            if (state.getComboStyle() != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOneJoined() {
        if (graphStates.size() < 2) {
            return false;
        }

        Object comboStyle = graphStates.get(0).getComboStyle();
        if (comboStyle == null) {
            return false;
        }

        for (int i = 1; i < graphStates.size(); i++) {
            if (graphStates.get(i).getComboStyle() != comboStyle) {
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
        for (GraphState state : graphStates) {
            if (!state.getGraphStyleLocked()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAllRootSeqSymmetrySame() {
        if (rootSyms.isEmpty()) {
            return false;
        }

        if (rootSyms.size() == 1) {
            return true;
        }

        for (int i = 1; i < rootSyms.size(); i++) {
            if (rootSyms.get(0).getCategory() != rootSyms.get(i).getCategory()) {
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

    public static boolean isAnyLocked() {
        for (StyledGlyph glyph : allGlyphs) {
            if (glyph.getAnnotStyle() != CoordinateStyle.coordinate_annot_style
                    && glyph instanceof DefaultTierGlyph && ((DefaultTierGlyph) glyph).isHeightFixed()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAnyLockable() {
        for (StyledGlyph glyph : allGlyphs) {
            if (glyph.getAnnotStyle() != CoordinateStyle.coordinate_annot_style
                    && glyph instanceof TierGlyph && ((TierGlyph) glyph).getTierType() == TierGlyph.TierType.ANNOTATION) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAllButOneLocked() {
        return getTotalLocked() == smv.getTierManager().getVisibleTierGlyphs().size() - 2;
    }

    public static int getLockedHeight() {
        for (StyledGlyph glyph : allGlyphs) {
            if (glyph.getAnnotStyle() != CoordinateStyle.coordinate_annot_style
                    && glyph instanceof DefaultTierGlyph && ((DefaultTierGlyph) glyph).isHeightFixed()) {
                return ((DefaultTierGlyph) glyph).getFixedPixHeight();
            }
        }
        return -1;
    }

    public static int getOptimum() {
        int ourOptimum = -1;
        boolean optimumSet = false;
        for (StyledGlyph glyph : allGlyphs) {
            if (glyph instanceof TierGlyph) {
                TierGlyph tg = (TierGlyph) glyph;
                int slotNeeded = tg.getSlotsNeeded(smv.getSeqMap().getView());
                if (optimumSet && ourOptimum != slotNeeded) {
                    ourOptimum = -1;
                    break;
                }
                ourOptimum = slotNeeded;
                optimumSet = true;
            }
        }
        return ourOptimum;
    }

    private static int getTotalLocked() {
        int no_of_locked = 0;
        for (TierGlyph tier : smv.getSeqMap().getTiers()) {
            ITrackStyleExtended style = tier.getAnnotStyle();
            if (style != CoordinateStyle.coordinate_annot_style && style.getShow()
                    && tier instanceof DefaultTierGlyph && ((DefaultTierGlyph) tier).isHeightFixed()) {
                no_of_locked++;
            }
        }
        return no_of_locked;
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

    public static void addRefreshSelectionListener(RefreshSelectionListener listener) {
        listenerList.add(RefreshSelectionListener.class, listener);
    }

    /*
     * Interface to notify selection has been updated.
     */
    public static interface RefreshSelectionListener extends EventListener {

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
            if (!(src == smv || src == smv.getSeqMap())
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
