package com.affymetrix.igb.tiers;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.EventUtils;
import com.affymetrix.genometryImpl.event.PropertyHolder;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.impl.CdsSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.DerivedSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.GraphSym;
import com.affymetrix.genometryImpl.symmetry.impl.MisMatchGraphSym;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.MultiGlyphDragger;
import com.affymetrix.genoviz.bioviews.SceneI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.comparator.GlyphMinYComparator;
import com.affymetrix.genoviz.event.NeoGlyphDragEvent;
import com.affymetrix.genoviz.event.NeoGlyphDragListener;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.shared.GraphGlyph;
import com.lorainelab.igb.genoviz.extensions.api.StyledGlyph;
import com.lorainelab.igb.genoviz.extensions.api.TierGlyph;
import com.affymetrix.igb.shared.TrackClickListener;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;

/**
 *
 * @version $Id: TierLabelManager.java 11431 2012-05-04 19:05:44Z hiralv $
 */
public final class TierLabelManager implements PropertyHolder {

    private static final int xoffset_pop = 10;
    private static final int yoffset_pop = 0;

    public static Map<String, Object> getTierProperties(TierGlyph glyph) {
        if (glyph.getAnnotStyle().isGraphTier() && glyph.getChildCount() > 0
                && glyph.getChild(0) instanceof GraphGlyph) {
            return null;
        }

        return getFeatureProperties(glyph.getAnnotStyle().getFeature());
    }

    public static Map<String, Object> getFeatureProperties(GenericFeature feature) {
        if (feature == null) {
            return null;
        }

        Map<String, Object> props = new HashMap<>();
        props.put("File Name", feature.featureName);
        props.put("Description", feature.description());
        if (feature.getFriendlyURL() != null) {
            props.put("url", feature.getFriendlyURL());
        }
        String server = feature.gVersion.gServer.serverName + " (" + feature.gVersion.gServer.serverType.getName() + ")";
        props.put("Server", server);

        return props;
    }

    /**
     * Gets all the GraphGlyph objects inside the given list of
     * TierLabelGlyph's.
     */
    public static List<GraphGlyph> getContainedGraphs(List<TierLabelGlyph> tier_label_glyphs) {
        List<GraphGlyph> result = new ArrayList<>();
        for (TierLabelGlyph tlg : tier_label_glyphs) {
            result.addAll(getContainedGraphs(tlg.getReferenceTier()));
        }
        return result;
    }

    /**
     * Gets all the GraphGlyph objects inside the given TierLabelGlyph.
     */
    private static List<GraphGlyph> getContainedGraphs(TierGlyph tier) {
        List<GraphGlyph> result = new ArrayList<>();
        int child_count = tier.getChildCount();
        if (child_count > 0 && tier.getAnnotStyle().isGraphTier()
                && tier.getChild(0) instanceof GraphGlyph) {
            for (int j = 0; j < child_count; j++) {
                result.add((GraphGlyph) tier.getChild(j));
            }
        }
        return result;
    }

    /**
     * Collapse or expand tier.
     *
     * @param collapsed - boolean indicating whether to collapse or expand
     * tiers.
     */
    static void setTierCollapsed(TierGlyph tg, boolean collapsed) {
        ITrackStyleExtended style = tg.getAnnotStyle();
        if (style.getExpandable()) {
            style.setCollapsed(collapsed);
            tg.setStyle(style);
            // When collapsing, make them all be the same height as the tier.
            // (this is for simplicity in figuring out how to draw things.)
            if (collapsed) {
                List<GraphGlyph> graphs = getContainedGraphs(tg);
                double tier_height = style.getHeight();
                for (GraphGlyph graph : graphs) {
                    Rectangle2D.Double cbox = graph.getCoordBox();
                    graph.setCoords(cbox.x, cbox.y, cbox.width, tier_height);
                }
            }
            tg.getScene().getViews().forEach(tg::pack);
        }
    }

    private final AffyLabelledTierMap tiermap;
    private final AffyTieredMap labelmap;
//	private final GlyphTransformer gs;
    private final JPopupMenu popup;
    private final Set<PopupListener> popup_listeners = new CopyOnWriteArraySet<>();
    private final Set<TrackSelectionListener> track_selection_listeners = new CopyOnWriteArraySet<>();
    private final Comparator<GlyphI> tier_sorter = new GlyphMinYComparator();
    private final NeoGlyphDragListener dragListener = new NeoGlyphDragListener() {
        @Override
        public void heardGlyphDrag(NeoGlyphDragEvent evt) {
            if (evt.getID() == NeoGlyphDragEvent.DRAG_ENDED) {
                List<TierLabelGlyph> label_glyphs = tiermap.getTierLabels();
                Collections.sort(label_glyphs, tier_sorter);

                List<TierGlyph> tiers = tiermap.getTiers();
                tiers.clear();
                for (TierLabelGlyph label : label_glyphs) {
                    TierGlyph tier = (TierGlyph) label.getInfo();
                    tiers.add(tier);
                }

                updatePositions();
                // then repack of course (tiermap repack also redoes labelmap glyph coords...)
                tiermap.packTiers(false, true, true);
                tiermap.updateWidget();
            }
        }
    };

    /**
     * For moving tiers around and adjusting their sizes. Use Swing for the
     * future. Otherwise could have used AWT's MouseAdapter. Actual dragging
     * around is delegated to a GlyphDragger and a GlyphResizer. This is why the
     * mouseDragged method is not implemented.
     */
    private final MouseInputListener ourTierDragger = new MouseInputAdapter() {
//		@Override
//		public void mouseMoved(MouseEvent evt) {
//			if (evt instanceof NeoMouseEvent && evt.getSource() == labelmap) {
//				setCurrentCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//			}
//			else {
//				restoreCursor();
//			}
//		}
//		@Override
//		public void mouseExited(MouseEvent evt) {
//			restoreCursor();
//		}

        @Override
        public void mousePressed(MouseEvent evt) {
            int cursorType = labelmap.getCursor().getType();
            if (Cursor.N_RESIZE_CURSOR == cursorType
                    || Cursor.S_RESIZE_CURSOR == cursorType) {
                // The resizer will handle this one.
                return;
            }
            if (evt instanceof NeoMouseEvent && evt.getSource() == labelmap) {
                NeoMouseEvent nevt = (NeoMouseEvent) evt;
                boolean isPopupTrigger = EventUtils.isOurPopupTrigger(evt);
                List<GlyphI> selected_glyphs = nevt.getItems();
                GlyphI topgl = null;
                if (!selected_glyphs.isEmpty()) {
                    topgl = selected_glyphs.get(selected_glyphs.size() - 1);
                }
				// Dispatch track selection event
                //doTrackSelection(topgl);

                // Normally, clicking will clear previous selections before selecting new things.
                // but we preserve the current selections if:
                //  1. shift or alt key is pressed, or
                //  2. the pop-up key is being pressed
                //     2a. on top of nothing
                //     2b. on top of something previously selected
                boolean preserve_selections = false;
                if (nevt.isAltDown() || nevt.isShiftDown()) {
                    preserve_selections = true;
                    Iterator<GlyphI> iterator = selected_glyphs.iterator();
                    while (iterator.hasNext()) {
                        GlyphI g = iterator.next();
                        if (g.isSelected()) {
                            labelmap.deselect(g);
                            iterator.remove();
                        }
                    }
                } else if (topgl != null && isPopupTrigger) {
                    if (labelmap.getSelected().contains(topgl)) {
                        preserve_selections = true;
                    }
                }
                if (!preserve_selections && (labelmap.getSelected().size() == 1 || (topgl != null && !topgl.isSelected()))) {
                    labelmap.clearSelected();
                    // Deselect graphglyph selected in the tiermap
                    List<GlyphI> deselect = new ArrayList<>();
                    for (GlyphI selected : tiermap.getSelected()) {
                        if (selected instanceof StyledGlyph) {
                            deselect.add(selected);
                        }
                    }
                    tiermap.deselect(deselect);
                }
                labelmap.select(selected_glyphs);

                tiermap.clearSelected();

                doGraphSelections(preserve_selections);
                // make sure selections becomes visible
                if (isPopupTrigger) {
                    doPopup(evt);
                } else if (selected_glyphs.size() > 0) {
                    // take glyph at end of selected, just in case there is more
                    //    than .
                    TierLabelGlyph[] gls = labelmap.getSelected().toArray(new TierLabelGlyph[1]);
                    for (TierLabelGlyph gl : gls) {
                        labelmap.toFront(gl);
                    }
                    dragLabel(gls, nevt);
//					if(selected_glyphs.size() == 1){
//						transformTier(gl);
//					}			
                }
                tiermap.updateWidget();
            }
        }

        /**
         * Finish a drag and drop of a tier label. Also change the map's
         * vertical zoom focus to zoom in on this track.
         */
        @Override
        public void mouseReleased(MouseEvent evt) {
            // Start trying to set the vertical zoom point appropriately.
            // First try, just set it at this place.
            if (evt instanceof NeoMouseEvent) {
                NeoMouseEvent nevt = (NeoMouseEvent) evt;
                double y = nevt.getCoordY();
                Application.getSingleton().getMapView().setZoomSpotY(y);
            }
        }

        private void transformTier(TierLabelGlyph gl) {
            // gs.startscroll(gl);
        }

        private void dragLabel(TierLabelGlyph[] gl, NeoMouseEvent nevt) {
            MultiGlyphDragger dragger = new MultiGlyphDragger((NeoAbstractWidget) nevt.getSource(), gl);
            dragger.addGlyphDragListener(dragListener);
            dragger.setUseCopy(false);
            dragger.startDrag(nevt);
            dragger.setConstraint(NeoConstants.HORIZONTAL, true);
        }

    }; // End of tier dragging MouseInputListener

    /**
     * Determines whether selecting a tier label of a tier that contains only
     * GraphGlyphs should cause the graphs in that tier to become selected.
     */
    private boolean do_graph_selections = false;

    public TierLabelManager(AffyLabelledTierMap map) {
        super();
        tiermap = map;
        popup = new JPopupMenu();

        labelmap = tiermap.getLabelMap();
        labelmap.addMouseListener(this.ourTierDragger);
        labelmap.addMouseMotionListener(this.ourTierDragger);

        labelmap.getScene().setSelectionAppearance(SceneI.SELECT_OUTLINE);
        labelmap.setPixelFuzziness(0); // there are no gaps between tiers, need no fuzziness

//		MouseInputListener resizer;
//		resizer = new MouseInputAdapter() {
        // Stub out resizing to disable it.
//		};
//		resizer = new NewTierResizer(this.tiermap);
//		gs = new GlyphTransformer(map);
//		resizer = new TierResizer(this.tiermap);
//		resizer = new AccordionTierResizer(this.tiermap);
//		labelmap.addMouseListener(resizer);
//		labelmap.addMouseMotionListener(resizer);
    }

    /**
     * Returns a list of TierGlyph items representing the selected tiers.
     */
    public List<TierGlyph> getSelectedTiers() {
        List<TierGlyph> selected_tiers = new ArrayList<>();

        for (TierLabelGlyph tlg : getSelectedTierLabels()) {
            // TierGlyph should be data model for tier label, access via label.getInfo()
            selected_tiers.add(tlg.getReferenceTier());

        }
        return selected_tiers;
    }

    /**
     * Returns a list of selected TierLabelGlyph items.
     */
    public List<TierLabelGlyph> getSelectedTierLabels() {
        // The below loop is unnecessary, but is done to fix generics compiler warnings.
        List<TierLabelGlyph> tlg = new ArrayList<>(labelmap.getSelected().size());
        for (GlyphI g : labelmap.getSelected()) {
            if (g instanceof TierLabelGlyph) {
                tlg.add((TierLabelGlyph) g);
            }
        }
        return tlg;
    }

    public List<Map<String, Object>> getTierProperties() {

        List<Map<String, Object>> propList = new ArrayList<>();

        getSelectedTiers().stream().filter(glyph -> !(glyph.getAnnotStyle().isGraphTier())).forEach(glyph -> {
            Map<String, Object> props = getTierProperties(glyph);

            if (props != null) {
                propList.add(props);
            }
        });

        return propList;
    }

    private Map<String, Object> getTierProperties(ITrackStyleExtended style) {
        for (TierGlyph glyph : getSelectedTiers()) {
            if (glyph.getAnnotStyle().equals(style)) {
                return getFeatureProperties(style.getFeature());
            }
        }

        return null;
    }

    /**
     * Returns a list of all TierLabelGlyph items.
     */
    public List<TierLabelGlyph> getAllTierLabels() {
        return new CopyOnWriteArrayList<>(tiermap.getTierLabels());
    }

    /**
     * Returns a list of all TierGlyph items.
     */
    public List<TierGlyph> getAllTierGlyphs(boolean allTiers) {
        List<TierGlyph> allTierGlyphs = new ArrayList<>();
        for (TierLabelGlyph tierlabel : getAllTierLabels()) {
            if (allTiers) {
                allTierGlyphs.add(tierlabel.getReferenceTier());
            } else if (tierlabel.getReferenceTier().getAnnotStyle().getShow()) {
                allTierGlyphs.add(tierlabel.getReferenceTier());
            }
        }
        return allTierGlyphs;
    }

    /**
     * Returns a list of visible TierGlyph items.
     */
    public List<TierGlyph> getVisibleTierGlyphs() {
        List<TierGlyph> allTierGlyphs = new ArrayList<>();
        for (TierLabelGlyph tierlabel : getAllTierLabels()) {
            if (tierlabel.getReferenceTier().getAnnotStyle().getShow() && tierlabel.getReferenceTier().isVisible()) {
                allTierGlyphs.add(tierlabel.getReferenceTier());
            }
        }
        return allTierGlyphs;
    }

    /**
     * Selects all non-hidden tiers.
     */
    void selectAllTiers() {
        getAllTierLabels().stream().filter(tierlabel -> tierlabel.getReferenceTier().getAnnotStyle().getShow()).forEach(labelmap::select);
        doGraphSelections(false);
        //labelmap.updateWidget();
        tiermap.updateWidget(); // make sure selections becomes visible
    }

    /**
     * Determines whether selecting a tier label of a tier that contains only
     * GraphGlyphs should cause the graphs in that tier to become selected.
     */
    public void setDoGraphSelections(boolean b) {
        do_graph_selections = b;
    }

    /**
     * Handle selection generating a selection event. This was made public to
     * serve the {@link com.affymetrix.igb.action.UnFloatTiersAction}. It needed
     * to restore the selection after acting and to fire the selection changed
     * event so the {@link com.affymetrix.igb.action.FloatTiersAction} could get
     * the news and enable itself. There may be other actions that act on a
     * selection of graph glyphs. Those may well need this too.
     *
     * @param preserve_selection Clear selection if this is false.
     */
    public void doGraphSelections(boolean preserve_selection) {
        if (!do_graph_selections) {
            return;
        }

        GenometryModel gmodel = GenometryModel.getInstance();
        Set<SeqSymmetry> graph_symmetries = new LinkedHashSet<>();
        Set<RootSeqSymmetry> all_symmetries = new HashSet<>();
        graph_symmetries.addAll(gmodel.getSelectedSymmetries(gmodel.getSelectedSeq()));

        if (!preserve_selection) {
            graph_symmetries.clear();
        }

        for (TierLabelGlyph tierlabel : getAllTierLabels()) {
            TierGlyph tg = tierlabel.getReferenceTier();
            int child_count = tg.getChildCount();
            if (child_count > 0) {
                if (tg.getChild(0) instanceof GraphGlyph) {
                    // It would be nice if we could assume that a tier contains only
                    // GraphGlyph's or only non-GraphGlyph's, but that is not true.
                    //
                    // When graph thresholding is turned on, there can be one or
                    // two other EfficientFillRectGlyphs that are a child of the tier glyph
                    // but are not instances of GraphGlyph.  They can be ignored.
                    // (I would like to change them to be children of the GraphGlyph, but
                    // haven't done it yet.)

                    // Assume that if first child is a GraphGlyph, then so are all others
                    for (int i = 0; i < child_count; i++) {
                        GlyphI ob = tg.getChild(i);
                        if (!(ob instanceof GraphGlyph)) {
                            // ignore the glyphs that are not GraphGlyph's
                            continue;
                        }
                        SeqSymmetry sym = (SeqSymmetry) ob.getInfo();
                        // sym will be a GraphSym, but we don't need to cast it
                        if (tierlabel.isSelected()) {
                            graph_symmetries.add(sym);
                            all_symmetries.add((RootSeqSymmetry) sym);
                        } else if (graph_symmetries.contains(sym)) {
                            graph_symmetries.remove(sym);
                        }
                    }
                } else if (tg.getTierType() == TierGlyph.TierType.GRAPH) {
                    SeqSymmetry sym = (SeqSymmetry) tg.getInfo();
                    // sym will be a GraphSym, but we don't need to cast it
                    if (tierlabel.isSelected()) {
                        graph_symmetries.add(sym);
                        all_symmetries.add((RootSeqSymmetry) sym);
                    } else if (graph_symmetries.contains(sym)) {
                        graph_symmetries.remove(sym);
                    }
                } else {
                    RootSeqSymmetry rootSym = (RootSeqSymmetry) tg.getInfo();
                    if (tierlabel.isSelected()) {
                        all_symmetries.add(rootSym);
                    }
                }
            }
        }

        gmodel.setSelectedSymmetries(new ArrayList<>(all_symmetries), new ArrayList<>(graph_symmetries), this);
    }

    /**
     * Restores multiple hidden tiers and then repacks.
     *
     * @param tier_labels a List of GlyphI objects for each of which getInfo()
     * returns a TierGlyph.
     * @param full_repack Whether to do a full repack
     * @param fit_y Whether to change the zoom to fit all the tiers in the view
     * @see #repackTheTiers(boolean, boolean)
     */
    public void showTiers(List<TierLabelGlyph> tier_labels, boolean full_repack, boolean fit_y) {
        tier_labels.stream().filter(g -> g.getInfo() instanceof TierGlyph).forEach(g -> {
            TierGlyph tier = (TierGlyph) g.getInfo();
            tier.getAnnotStyle().setShow(true);
        });

        repackTheTiers(full_repack, fit_y);
    }

    /**
     * Hides multiple tiers and then repacks.
     *
     * @param tier_labels a List of GlyphI objects for each of which getInfo()
     * returns a TierGlyph.
     * @param fit_y Whether to change the zoom to fit all the tiers in the view
     */
    public void hideTiers(List<TierLabelGlyph> tier_labels, boolean full_repack, boolean fit_y) {
        tier_labels.stream().filter(g -> g.getInfo() instanceof TierGlyph).forEach(g -> {
            TierGlyph tier = (TierGlyph) g.getInfo();
            tier.getAnnotStyle().setShow(false);
        });

        repackTheTiers(full_repack, fit_y);
    }

    /**
     * Collapse or expand tiers.
     *
     * @param tier_labels
     * @param collapsed - boolean indicating whether to collapse or expand
     * tiers.
     */
    public void setTiersCollapsed(List<TierLabelGlyph> tier_labels, boolean collapsed) {
        for (TierLabelGlyph tlg : tier_labels) {
            setTierCollapsed(tlg.getReferenceTier(), collapsed);
        }
        tiermap.setTierStyles();
        repackTheTiers(true, true);
        tiermap.updateWidget();
    }

    public void toggleTierCollapsed(List<TierLabelGlyph> tier_glyphs) {
        for (TierLabelGlyph glyph : tier_glyphs) {
            ITrackStyle style = glyph.getReferenceTier().getAnnotStyle();
            setTierCollapsed(glyph.getReferenceTier(), !style.getCollapsed());
        }
        repackTheTiers(true, true);
    }

    /**
     * Rearrange tiers in case mouse is dragged.
     */
    void rearrangeTiers() {

    }

    private void updatePositions() {
        List<TierLabelGlyph> label_glyphs = tiermap.getTierLabels();
        for (int i = 0; i < label_glyphs.size(); i++) {
            label_glyphs.get(i).setPosition(i);
        }
    }

    /**
     * Sorts all tiers and then calls packTiers() and updateWidget().
     */
    public void sortTiers() {
        List<TierLabelGlyph> label_glyphs = tiermap.getTierLabels();
        Collections.sort(label_glyphs, new Comparator<TierLabelGlyph>() {
            @Override
            public int compare(TierLabelGlyph g1, TierLabelGlyph g2) {
                return Double.compare(g1.getPosition(), g2.getPosition());
            }
        });

        // then repack of course (tiermap repack also redoes labelmap glyph coords...)
        tiermap.packTiers(false, true, false);
        tiermap.updateWidget();
    }

    /**
     * Repacks tiers. Should be called after hiding or showing tiers or changing
     * their heights.
     */
    public void repackTheTiers(boolean full_repack, boolean stretch_vertically) {
        tiermap.repackTheTiers(full_repack, stretch_vertically);
    }

    public void addPopupListener(PopupListener p) {
        popup_listeners.add(p);
    }

    public void removePopupListener(PopupListener p) {
        popup_listeners.remove(p);
    }

    /**
     * Removes all elements from the popup, then notifies all
     * {@link TierLabelManager.PopupListener} objects (which may add items to
     * the menu), then displays the popup (if it isn't empty).
     */
    public void doPopup(MouseEvent e) {
        popup.removeAll();

        setPopuptitle();

        for (PopupListener pl : popup_listeners) {
            pl.popupNotify(popup, this);
        }
        List<TierGlyph> selectedGlyphs = getSelectedTiers();
        for (TrackClickListener l : ExtensionPointHandler.getExtensionPoint(TrackClickListener.class).getExtensionPointImpls()) {
            l.trackClickNotify(popup, selectedGlyphs);
        }
        if (popup.getComponentCount() > 0) {
            popup.show(labelmap, e.getX() + xoffset_pop, e.getY() + yoffset_pop);
        }
    }

    /**
     * Sets title for popup. Sets feature name as title if available else shows
     * number of selection.
     */
    private void setPopuptitle() {
        List<TierGlyph> tiers = getSelectedTiers();

        if (tiers.isEmpty()) {
            return;
        }

        String label;
        if (tiers.size() == 1 && tiers.get(0).getAnnotStyle().getTrackName() != null) {
            label = tiers.get(0).getAnnotStyle().getTrackName();
        } else {
            label = tiers.size() + " Selections";
        }

        if (label != null && label.length() > 30) {
            label = label.substring(0, 30) + " ...";
        }

        if (label != null && label.length() > 0) {
            JLabel label_name = new JLabel(label);
            label_name.setEnabled(false); // makes the text look different (usually lighter)
            label_name.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            popup.add(label_name);
        }
    }

    public TierGlyph getTierGlyph(NeoMouseEvent nevt) {
        Rectangle2D.Double coordrect = new Rectangle2D.Double(nevt.getCoordX(), nevt.getCoordY(), 1, 1);
        TierGlyph tglyph = null;
        TierGlyph temp;

        for (TierLabelGlyph tlg : tiermap.getTierLabels()) {
            if (tlg.getInfo() instanceof TierGlyph) {
                temp = (TierGlyph) tlg.getInfo();
                if (temp.intersects(coordrect, tiermap.getView())) {
                    tglyph = temp;
                    break;
                }
            }
        }

        return tglyph;
    }

    public void deselectTierLabels() {
        getAllTierLabels().forEach(labelmap::deselect);
    }

    public void deselect(GlyphI tierGlyph) {
        tiermap.getTierLabels().stream().filter(tlg -> tlg.getReferenceTier() == tierGlyph).forEach(labelmap::deselect);
    }

    public void select(GlyphI tierGlyph) {
        tiermap.getTierLabels().stream().filter(tlg -> tlg.getReferenceTier() == tierGlyph).forEach(labelmap::select);
    }

    public void addTrackSelectionListener(TrackSelectionListener l) {
        track_selection_listeners.add(l);
    }

    public void doTrackSelection(GlyphI topLevelGlyph) {
        for (TrackSelectionListener l : track_selection_listeners) {
            l.trackSelectionNotify(topLevelGlyph, this);
        }
    }

    @Override
    public List<Map<String, Object>> getProperties() {
        return getTierProperties();
    }

    @Override
    public Map<String, Object> determineProps(SeqSymmetry sym) {
        if (sym == null) {
            return Collections.<String, Object>emptyMap();
        }

        Map<String, Object> props = null;
        if (sym instanceof SymWithProps) {
            // using Propertied.cloneProperties() here instead of Propertied.getProperties()
            //   because adding start, end, id, and length as additional key-val pairs to props Map
            //   and don't want these to bloat up sym's properties
            props = ((SymWithProps) sym).cloneProperties();
        }
        if (props == null && sym instanceof DerivedSeqSymmetry) {
            SeqSymmetry original_sym = ((DerivedSeqSymmetry) sym).getOriginalSymmetry();
            if (original_sym instanceof SymWithProps) {
                props = ((SymWithProps) original_sym).cloneProperties();
            }
        }
        if (props == null && sym instanceof CdsSeqSymmetry) {
            SeqSymmetry property_sym = ((CdsSeqSymmetry) sym).getPropertySymmetry();
            if (property_sym instanceof SymWithProps) {
                props = ((SymWithProps) property_sym).cloneProperties();
            }
        }

        if (props == null) {
            // make an empty hashtable if sym has no properties...
            props = new HashMap<>();
        }
        String symid = sym.getID();
        if (symid != null) {
            props.put("id", symid);
        }
        if (sym instanceof GraphSym && !(sym instanceof MisMatchGraphSym)) {
            float[] range = ((GraphSym) sym).getVisibleYRange();
            props.put("min score", range[0]);
            props.put("max score", range[1]);
        }
        if (sym instanceof GraphSym) {
            Map<String, Object> tierProps = getTierProperties(((GraphSym) sym).getGraphState().getTierStyle());
            if (tierProps != null) {
                props.putAll(tierProps);
            }
        }
        return props;
    }

    private void setCurrentCursor(Cursor cursor) {
        Application.getSingleton().getMapView().getSeqMap().setCursor(cursor);
    }

    private void restoreCursor() {
        setCurrentCursor(Application.getSingleton().getMapView().getMapMode().defCursor);
    }

    /**
     * An interface that lets listeners modify the popup menu before it is
     * shown.
     */
    public interface PopupListener {

        /**
         * Called before the {@link TierLabelManager} popup menu is displayed.
         * The listener may add elements to the popup menu before it gets
         * displayed.
         */
        public void popupNotify(JPopupMenu popup, TierLabelManager handler);
    }

    /**
     * An interface that to listener for track selection events.
     */
    public interface TrackSelectionListener {

        public void trackSelectionNotify(GlyphI topLevelGlyph, TierLabelManager handler);
    }
}
