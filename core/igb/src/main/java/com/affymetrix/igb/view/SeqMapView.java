package com.affymetrix.igb.view;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SupportsCdsSpan;
import com.affymetrix.genometry.event.AxisPopupListener;
import com.affymetrix.genometry.event.ContextualPopupListener;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GroupSelectionEvent;
import com.affymetrix.genometry.event.GroupSelectionListener;
import com.affymetrix.genometry.event.PropertyHandler;
import com.affymetrix.genometry.event.PropertyHolder;
import com.affymetrix.genometry.event.SeqMapRefreshed;
import com.affymetrix.genometry.event.SeqSelectionEvent;
import com.affymetrix.genometry.event.SeqSelectionListener;
import com.affymetrix.genometry.event.SymSelectionListener;
import com.affymetrix.genometry.general.GenericFeature;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.style.GraphState;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symmetry.DerivedSeqSymmetry;
import com.affymetrix.genometry.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometry.symmetry.RootSeqSymmetry;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.CdsSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.symmetry.impl.MutableSingletonSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleMutableSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SingletonSymWithProps;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.CDS_END;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.CDS_START;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.CHROMOSOME;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.DIRECTION;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.END;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.FEATURE_TYPE;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.GENE_NAME;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.ID;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.LENGTH;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.MATCH;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.METHOD;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.REVERSE_DIRECTION;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.SEQ_ID;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.START;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.STRAND;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.TYPE;
import com.affymetrix.genometry.util.BioSeqUtils;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genometry.util.ThreadUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.RubberBand;
import com.affymetrix.genoviz.bioviews.SceneI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.glyph.AxisGlyph;
import com.affymetrix.genoviz.glyph.CoordFloaterGlyph;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.genoviz.glyph.FloaterGlyph;
import com.affymetrix.genoviz.glyph.RootGlyph;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.widget.AutoScroll;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBConstants;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.action.ClampViewAction;
import com.affymetrix.igb.action.CopyResiduesAction;
import com.affymetrix.igb.action.LoadPartialSequenceAction;
import com.affymetrix.igb.action.MapModeScrollAction;
import com.affymetrix.igb.action.MapModeSelectAction;
import com.affymetrix.igb.action.RefreshDataAction;
import com.affymetrix.igb.action.SelectParentAction;
import com.affymetrix.igb.action.SelectionRuleAction;
import com.affymetrix.igb.action.ZoomInXAction;
import com.affymetrix.igb.action.ZoomInYAction;
import com.affymetrix.igb.action.ZoomOnSelectedSymsAction;
import com.affymetrix.igb.action.ZoomOutXAction;
import com.affymetrix.igb.action.ZoomOutYAction;
import com.affymetrix.igb.glyph.CharSeqGlyph;
import com.affymetrix.igb.glyph.GlyphEdgeMatcher;
import com.affymetrix.igb.glyph.GraphSelectionManager;
import com.affymetrix.igb.services.registry.MapTierTypeHolder;
import com.affymetrix.igb.swing.JRPPopupMenu;
import com.affymetrix.igb.swing.JRPWidget;
import com.affymetrix.igb.swing.MenuUtil;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.CoordinateStyle;
import com.affymetrix.igb.tiers.MouseShortCut;
import com.affymetrix.igb.tiers.SeqMapViewPopup;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TierLabelManager;
import com.affymetrix.igb.tiers.TierResizer;
import com.affymetrix.igb.tiers.TrackStylePropertyListener;
import com.affymetrix.igb.tiers.TrackstylePropertyMonitor;
import static com.affymetrix.igb.view.SeqMapViewConstants.PREF_EDGE_MATCH_COLOR;
import static com.affymetrix.igb.view.SeqMapViewConstants.PREF_EDGE_MATCH_FUZZY_COLOR;
import static com.affymetrix.igb.view.SeqMapViewConstants.PREF_SHOW_TOOLTIP;
import static com.affymetrix.igb.view.SeqMapViewConstants.PREF_TRACK_RESIZING_BEHAVIOR;
import static com.affymetrix.igb.view.SeqMapViewConstants.PREF_X_ZOOMER_ABOVE;
import static com.affymetrix.igb.view.SeqMapViewConstants.PREF_Y_ZOOMER_LEFT;
import static com.affymetrix.igb.view.SeqMapViewConstants.SEQ_MODE;
import com.affymetrix.igb.view.factories.GraphGlyphFactory;
import com.affymetrix.igb.view.factories.MapTierGlyphFactoryI;
import com.affymetrix.igb.view.load.AutoLoadThresholdHandler;
import com.lorainelab.igb.genoviz.extensions.GraphGlyph;
import com.lorainelab.igb.genoviz.extensions.SeqMapViewExtendedI;
import com.lorainelab.igb.genoviz.extensions.StyledGlyph;
import com.lorainelab.igb.genoviz.extensions.TierGlyph;
import java.awt.AWTEvent;
import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;
import java.util.prefs.PreferenceChangeListener;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A panel hosting a labeled tier map. Despite it's name this is actually a
 * panel and not a {@link ViewI}.
 */
public class SeqMapView extends JPanel
        implements SeqMapViewExtendedI, SeqSelectionListener, GroupSelectionListener, TrackStylePropertyListener, PropertyHolder, JRPWidget {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(SeqMapView.class);
    public static final boolean default_auto_change_view = false;
    public static final boolean default_show_prop_tooltip = true;
    private static final boolean DEBUG_TIERS = false;
    static final Cursor defaultCursor, openHandCursor, closedHandCursor;
    private static final int max_for_matching = 500; //maximum number of query glyphs for edge matcher.
    public static final Color default_edge_match_color = new Color(204, 0, 255);
    public static final Color default_edge_match_fuzzy_color = new Color(200, 200, 200); // light gray
    public static final boolean defaultXZoomerAbove = true;
    public static final boolean defaultYZoomerLeft = true;
    private static final Font max_zoom_font = NeoConstants.default_bold_font.deriveFont(30.0f);
    private static final JMenuItem empty_menu_item = new JMenuItem("");
    public static final Font axisFont = NeoConstants.default_bold_font;
    private static final int xoffset_pop = 10;
    private static final int yoffset_pop = 0;
    private static final int[] default_range = new int[]{0, 100};
    private static final int[] default_offset = new int[]{0, 100};
    private static final GenometryModel gmodel = GenometryModel.getInstance();

    static {
        defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
        openHandCursor = new Cursor(Cursor.HAND_CURSOR);
        closedHandCursor = new Cursor(Cursor.HAND_CURSOR);
    }

    /**
     * Creates an instance to be used as the SeqMap. Set-up of listeners and
     * such will be done in init()
     */
    private static AffyTieredMap createAffyTieredMap() {
        AffyTieredMap resultSeqMap = new AffyLabelledTierMap(true, true);
        resultSeqMap.enableDragScrolling(true);
        ((AffyLabelledTierMap) resultSeqMap).getLabelMap().enableMouseWheelAction(false);
        resultSeqMap.setMaxZoomToFont(max_zoom_font);
        NeoMap label_map = ((AffyLabelledTierMap) resultSeqMap).getLabelMap();
        label_map.setSelectionAppearance(SceneI.SELECT_OUTLINE);
        label_map.setReshapeBehavior(NeoAbstractWidget.Y, NeoConstants.NONE);
        label_map.setMaxZoomToFont(max_zoom_font);
        return resultSeqMap;
    }

    /**
     * Sets the axis label format from the value in the persistent preferences.
     */
    public static void setAxisFormatFromPrefs(AxisGlyph axis) {
        // It might be good to move this to AffyTieredMap
        String axis_format = PreferenceUtils.getTopNode().get(CoordinateStyle.PREF_COORDINATE_LABEL_FORMAT, CoordinateStyle.VALUE_COORDINATE_LABEL_FORMAT_COMMA);
        if (CoordinateStyle.VALUE_COORDINATE_LABEL_FORMAT_COMMA.equalsIgnoreCase(axis_format)) {
            axis.setLabelFormat(AxisGlyph.COMMA);
        } else if (CoordinateStyle.VALUE_COORDINATE_LABEL_FORMAT_FULL.equalsIgnoreCase(axis_format)) {
            axis.setLabelFormat(AxisGlyph.FULL);
        } else if (CoordinateStyle.VALUE_COORDINATE_LABEL_FORMAT_NO_LABELS.equalsIgnoreCase(axis_format)) {
            axis.setLabelFormat(AxisGlyph.NO_LABELS);
        } else {
            axis.setLabelFormat(AxisGlyph.ABBREV);
        }
    }

    private static void addPreviousTierGlyphs(AffyTieredMap seqmap, List<TierGlyph> temp_tiers) {
        // add back in previous annotation tiers (with all children removed)
        if (temp_tiers != null) {
            for (TierGlyph tg : temp_tiers) {
                if (DEBUG_TIERS) {
                }
                if (tg.getAnnotStyle() != null) {
                    tg.setStyle(tg.getAnnotStyle());
                }
                seqmap.addTier(tg, false);
            }
            temp_tiers.clear(); // redundant hint to garbage collection
        }
    }

    /**
     * Returns all floating layers _except_ grid layer (which is supposed to
     * stay behind everything else).
     */
    private static List<GlyphI> getFloatingLayers(GlyphI root_glyph) {
        List<GlyphI> layers = new ArrayList<>();
        int gcount = root_glyph.getChildCount();
        for (int i = 0; i < gcount; i++) {
            GlyphI cgl = root_glyph.getChild(i);
            if (cgl instanceof FloaterGlyph) {
                layers.add(cgl);
            }
        }
        return layers;
    }

    /**
     * Given a list of glyphs, returns a list of syms that those glyphs
     * represent.
     */
    public static List<SeqSymmetry> glyphsToSyms(List<? extends GlyphI> glyphs) {
        Set<SeqSymmetry> symSet = new LinkedHashSet<>(glyphs.size());	// use LinkedHashSet to preserve order
        for (GlyphI gl : glyphs) {
            if (gl.getInfo() instanceof SeqSymmetry) {
                symSet.add((SeqSymmetry) gl.getInfo());
            }
        }
        return new ArrayList<>(symSet);
    }

    /**
     * Given a list of glyphs, returns a list of root syms that those glyphs
     * represent.
     */
    public static List<RootSeqSymmetry> glyphsToRootSyms(List<? extends GlyphI> glyphs) {
        Set<RootSeqSymmetry> symSet = new LinkedHashSet<>(glyphs.size());	// use LinkedHashSet to preserve order
        for (GlyphI gl : glyphs) {
            if (gl.getInfo() instanceof RootSeqSymmetry) {
                symSet.add((RootSeqSymmetry) gl.getInfo());
            }
        }
        return new ArrayList<>(symSet);
    }

    /**
     * Returns a rectangle containing all the current selections.
     *
     * @return null if the vector of glyphs is empty
     */
    private static Rectangle2D.Double getRegionForGlyphs(List<GlyphI> glyphs) {
        if (glyphs.isEmpty()) {
            return null;
        }
        Rectangle2D.Double rect = new Rectangle2D.Double();
        GlyphI g0 = glyphs.get(0);
        rect.setRect(g0.getCoordBox());
        for (GlyphI g : glyphs) {
            rect.add(g.getCoordBox());
        }
        return rect;
    }

    /**
     * Find the top-most parent glyphs of the given glyphs.
     *
     * @param childGlyphs a list of GlyphI objects, typically the selected
     * glyphs
     * @return a list where each child is replaced by its top-most parent, if it
     * has a parent, or else the child itself is included in the list
     */
    static List<GlyphI> getParents(List<GlyphI> childGlyphs) {
        Set<GlyphI> results = new LinkedHashSet<>(childGlyphs.size());
        for (GlyphI child : childGlyphs) {
            GlyphI pglyph = getParent(child, true);
            results.add(pglyph);
        }
        return new ArrayList<>(results);
    }

    /**
     * Get the parent, or top-level parent, of a glyph, with certain
     * restrictions. Will not return a TierGlyph or RootGlyph or a glyph that
     * isn't hitable, but will return the original GlyphI instead.
     *
     * @param top_level if true, will recurse up to the top-level parent, with
     * certain restrictions: recursion will stop before reaching a TierGlyph
     */
    private static GlyphI getParent(GlyphI g, boolean top_level) {
        GlyphI pglyph = g.getParent();
        // the test for isHitable will automatically exclude seq_glyph
        if (pglyph != null && pglyph.isHitable() && !(pglyph instanceof TierGlyph) && !(pglyph instanceof RootGlyph)) {
            if (top_level) {
                GlyphI t = pglyph;
                while (t != null && t.isHitable() && !(t instanceof TierGlyph) && !(t instanceof RootGlyph)) {
                    pglyph = t;
                    t = t.getParent();
                }
            }
            return pglyph;
        }
        return g;
    }

    final static Predicate<? super GlyphI> isTierGlyph = child -> child instanceof TierGlyph;
    final static Predicate<? super GlyphI> hasChildren = glyph -> glyph.getChildCount() > 0;
    final static Predicate<? super GlyphI> isGraphTierGlyph = isTierGlyph.and(tierGlyph -> ((TierGlyph) tierGlyph).getTierType() == TierGlyph.TierType.GRAPH);
    final static Predicate<? super GlyphI> isGraphGlyph = child -> child instanceof GraphGlyph;

    /**
     * Recurse through glyph hierarchy and collect graphs.
     */
    private static void collectGraphs(GlyphI gl, List<GraphGlyph> graphs) {
        Optional.ofNullable(gl.getChildren()).ifPresent(children -> {
            children.stream()
                    .filter(isGraphTierGlyph)
                    .filter(hasChildren)
                    .forEach(agg -> {
                        agg.getChildren().stream()
                        .filter(isGraphGlyph)
                        .forEach(graphGlyph -> graphs.add((GraphGlyph) graphGlyph));
                    });
        });
    }

    private static void splitGraph(GraphGlyph glyph) {
        GraphSym gsym = (GraphSym) glyph.getInfo();
        GraphState gstate = gsym.getGraphState();
        if (gstate.getComboStyle() != null) {
            gstate.getTierStyle().setY(gstate.getComboStyle().getY());
        }
        gstate.setComboStyle(null, 0);
        gstate.getTierStyle().setJoin(false);
        gstate.getTierStyle().setFloatTier(false);
    }
    private final SymSelectionListener symSelectionListener;
    protected boolean subselectSequence = true;  // try to visually select range along seq glyph based on rubberbanding
    protected boolean coord_shift = false;
    boolean showEdgeMatches = PreferenceUtils.getTopNode().getBoolean(PreferenceUtils.SHOW_EDGEMATCH_OPTION, PreferenceUtils.default_show_edge_match);
    private boolean show_prop_tooltip = PreferenceUtils.getTopNode().getBoolean(PREF_SHOW_TOOLTIP, default_show_prop_tooltip);
    private MapMode mapMode;
    private com.affymetrix.igb.swing.JRPToggleButton select_mode_button;
    private com.affymetrix.igb.swing.JRPToggleButton scroll_mode_button;
    private final Set<ContextualPopupListener> popup_listeners = new CopyOnWriteArraySet<>();
    private final Set<AxisPopupListener> axisPopupListeners = new CopyOnWriteArraySet<>();
    private String id;
    private boolean shrinkWrapMapBounds = false;
    protected AffyTieredMap seqmap;
    private UnibrowHairline hairline = null;
    protected BioSeq aseq;
    protected BioSeq viewseq; //a virtual sequence that maps the BioSeq aseq to the map coordinates.
    protected MutableSeqSymmetry seq2viewSym;
    protected SeqSymmetry[] transform_path;
    private final FloaterGlyph pixel_floater_glyph = new CoordFloaterGlyph();
    private final AutoScroll autoScroll = new AutoScroll();
    private final GlyphEdgeMatcher edge_matcher;
    private JRPPopupMenu sym_popup = null;
    private SeqSymmetry toolTipSym;
    JMenuItem slicendiceMI = empty_menu_item;
    JMenuItem seqViewerOptions = empty_menu_item;
    private final SeqMapViewMouseListener mouse_listener;
    private SeqSymmetry seq_selected_sym = null;  // symmetry representing selected region of sequence
    private SeqSpan horizontalClampedRegion = null; //Span representing clamped region
    protected TierLabelManager tierLabelManager;
    protected JComponent xzoombox;
    protected JComponent yzoombox;
    protected com.affymetrix.igb.swing.JRPButton zoomInXB;
    protected com.affymetrix.igb.swing.JRPButton zoomInYB;
    protected com.affymetrix.igb.swing.JRPButton zoomOutXB;
    protected com.affymetrix.igb.swing.JRPButton zoomOutYB;
    protected MapRangeBox map_range_box;
    protected com.affymetrix.igb.swing.JRPButton partial_residuesB;
    boolean report_hairline_position_in_status_bar = false;
    boolean report_status_in_status_bar = true;
    private SeqSymmetry sym_used_for_title = null;
    private TierGlyph tier_used_in_selection_info = null;
    private PropertyHandler propertyHandler;
    private final GenericAction refreshDataAction;
    private SeqMapViewPopup popup;
    private final Set<SeqMapRefreshed> seqmap_refresh_list = new CopyOnWriteArraySet<>();
    private TierGlyph axis_tier;
    private final SeqMapToolTips seqMapToolTips;
    private AutoLoadThresholdHandler autoload;
    private final PreferenceChangeListener pref_change_listener;

    private MouseListener continuousActionListener = new MouseAdapter() {
        private javax.swing.Timer timer;

        @Override
        public void mousePressed(MouseEvent e) {
            if (!(e.getSource() instanceof JButton)
                    || ((JButton) e.getSource()).getAction() == null) {
                return;
            }

            timer = new javax.swing.Timer(200, ((JButton) e.getSource()).getAction());
            timer.start();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (timer != null) {
                timer.stop();
            }
        }
    };

    AWTEventListener modeController = new AWTEventListener() {
        @Override
        public void eventDispatched(AWTEvent event) {
            if (seqmap.isShowing()) {
                Rectangle rect = new Rectangle();
                rect.setBounds(seqmap.getLocationOnScreen().x, seqmap.getLocationOnScreen().y,
                        seqmap.getBounds().width, seqmap.getBounds().height);
                if (rect.contains(MouseInfo.getPointerInfo().getLocation())) {
                    toggleMode((KeyEvent) event);
                }
            }
        }

        private void toggleMode(KeyEvent event) {
            MapMode currMode = getMapMode();
            if (event.getKeyCode() == KeyEvent.VK_ALT
                    && (event.getID() == KeyEvent.KEY_PRESSED
                    || event.getID() == KeyEvent.KEY_RELEASED)) {
                if (currMode == MapMode.MapSelectMode) {
                    scroll_mode_button.doClick();
                } else {
                    select_mode_button.doClick();
                }
            }
        }
    };

    public SeqMapView(boolean add_popups, String theId, JFrame frame) {
        super();

        this.id = theId;
        com.affymetrix.igb.swing.script.ScriptManager.getInstance().addWidget(this);
        seqmap = createAffyTieredMap();
        seqmap.setReshapeBehavior(NeoAbstractWidget.X, NeoConstants.NONE);
        seqmap.setReshapeBehavior(NeoAbstractWidget.Y, NeoConstants.NONE);
        seqmap.addComponentListener(new SeqMapViewComponentListener());
        seqmap.setMapColor(Color.WHITE);
        edge_matcher = GlyphEdgeMatcher.getSingleton();
        mouse_listener = new SeqMapViewMouseListener(this);
        seqMapToolTips = new SeqMapToolTips(frame);
        seqmap.getNeoCanvas().setDoubleBuffered(false);
        seqmap.setScrollIncrementBehavior(AffyTieredMap.X, AffyTieredMap.AUTO_SCROLL_HALF_PAGE);
        Adjustable xzoomer = getXZoomer(this.id);
        ((JSlider) xzoomer).setToolTipText(BUNDLE.getString("horizontalZoomToolTip"));
        Adjustable yzoomer = new com.affymetrix.igb.swing.RPAdjustableJSlider(this.id + "_yzoomer", Adjustable.VERTICAL);
        ((JSlider) yzoomer).setToolTipText(BUNDLE.getString("verticalZoomToolTip"));
        seqmap.setZoomer(NeoMap.X, xzoomer);
        seqmap.setZoomer(NeoMap.Y, yzoomer);
        tierLabelManager = new TierLabelManager((AffyLabelledTierMap) seqmap);
        popup = new SeqMapViewPopup(tierLabelManager, this);
        MouseShortCut msc = new MouseShortCut(popup);
        tierLabelManager.setDoGraphSelections(true);
        GraphSelectionManager gsm = new GraphSelectionManager(this);
        seqmap.addMouseListener(gsm);
        if (add_popups) {
            tierLabelManager.addPopupListener(popup);
            autoload = new AutoLoadThresholdHandler(this);
        }

        TierLabelManager.TrackSelectionListener track_selection_listener = (topLevelGlyph, handler) -> {
            // TODO:  Find properties of selected track and show in 'Selection Info' tab.
        };
        tierLabelManager.addTrackSelectionListener(track_selection_listener);

        seqmap.setSelectionAppearance(SceneI.SELECT_OUTLINE);
        seqmap.addMouseListener(mouse_listener);
        seqmap.addMouseListener(msc);
        seqmap.addMouseMotionListener(mouse_listener);
        ((AffyLabelledTierMap) seqmap).getLabelMap().addMouseMotionListener(mouse_listener);

        tierLabelManager.setDoGraphSelections(true);
        RubberBand srb = new SeqMapViewRubberBand(seqmap);
        seqmap.setRubberBand(srb);
        seqmap.addRubberBandListener(mouse_listener);
        srb.setColor(new Color(100, 100, 255));

        SmartDragScrollMonitor sdsm = new SmartDragScrollMonitor(this);
        seqmap.setDragScrollMonitor(sdsm);

        this.setLayout(new BorderLayout());

        xzoombox = Box.createHorizontalBox();
        map_range_box = new MapRangeBox(this);
        addSearchButton(this.id);

        xzoombox.add(map_range_box.range_box);

        select_mode_button = new com.affymetrix.igb.swing.JRPToggleButton(this.id + "_select_mode_button",
                new MapModeSelectAction(this.id));
        select_mode_button.setText("");
        select_mode_button.setToolTipText(BUNDLE.getString("selectModeToolTip"));
        select_mode_button.setMargin(new Insets(2, 4, 2, 4));
        xzoombox.add(select_mode_button);

        scroll_mode_button = new com.affymetrix.igb.swing.JRPToggleButton(this.id + "_scroll_mode_button",
                new MapModeScrollAction(this.id));
        scroll_mode_button.setText("");
        scroll_mode_button.setToolTipText(BUNDLE.getString("scrollModeToolTip"));
        scroll_mode_button.setMargin(new Insets(2, 4, 2, 4));
        xzoombox.add(scroll_mode_button);
        ButtonGroup group = new ButtonGroup();
        group.add(select_mode_button);
        group.add(scroll_mode_button);
        select_mode_button.doClick(); // default

        xzoombox.add(Box.createRigidArea(new Dimension(6, 0)));
        addZoomOutXButton(this.id);
        xzoombox.add((Component) xzoomer);
        addZoomInXButton(this.id);
        xzoombox.add(Box.createRigidArea(new Dimension(6, 0)));

        refreshDataAction = new RefreshDataAction(this);
        addRefreshButton(this.id);
        addLoadResidueButton(this.id);

        boolean x_above = PreferenceUtils.getBooleanParam(PREF_X_ZOOMER_ABOVE, defaultXZoomerAbove);
        JPanel pan = new JPanel(new BorderLayout(0, 0));
        pan.add("Center", xzoombox);
        if (x_above) {
            this.add(BorderLayout.NORTH, pan);
        } else {
            this.add(BorderLayout.SOUTH, pan);
        }

        yzoombox = Box.createVerticalBox();
        yzoombox.add(Box.createRigidArea(new Dimension(6, 0)));
        addZoomOutYButton(this.id);
        yzoombox.add((Component) yzoomer, BorderLayout.CENTER);
        addZoomInYButton(this.id);

        yzoombox.add(Box.createRigidArea(new Dimension(6, 0)));

        boolean y_left = PreferenceUtils.getBooleanParam(PREF_Y_ZOOMER_LEFT, defaultYZoomerLeft);
        if (y_left) {
            this.add(BorderLayout.WEST, yzoombox);
        } else {
            this.add(BorderLayout.EAST, yzoombox);
        }

        this.add(BorderLayout.CENTER, seqmap);

        String behavior = PreferenceUtils.getStringParam(PREF_TRACK_RESIZING_BEHAVIOR, TierResizer.class.getSimpleName());
        PreferenceUtils.getTopNode().put(PREF_TRACK_RESIZING_BEHAVIOR, behavior);

        TrackstylePropertyMonitor.getPropertyTracker().addPropertyListener(this);
        Toolkit.getDefaultToolkit().addAWTEventListener(modeController, AWTEvent.KEY_EVENT_MASK);

        pref_change_listener = new SeqMapViewPrefChangeListenerImpl(this);
        symSelectionListener = new SeqMapViewSymSelectionListenerImpl(this);

        PreferenceUtils.getTopNode().addPreferenceChangeListener(pref_change_listener);
    }

    protected void addZoomInXButton(String id) {
        zoomInXB = new com.affymetrix.igb.swing.JRPButton(id + "_zoomInX_button", ZoomInXAction.getIconOnlyAction());
        zoomInXB.setToolTipText(BUNDLE.getString("horizontalZoomerPlusButtonTooltip"));
        zoomInXB.setMargin(new Insets(0, 0, 0, 0));
        zoomInXB.addMouseListener(continuousActionListener);
        xzoombox.add(zoomInXB);
    }

    protected void addZoomOutXButton(String id) {
        zoomOutXB = new com.affymetrix.igb.swing.JRPButton(id + "_zoomOutX_button", ZoomOutXAction.getIconOnlyAction());
        zoomOutXB.setToolTipText(BUNDLE.getString("horizontalZoomerMinusButtonTooltip"));
        zoomOutXB.setMargin(new Insets(0, 0, 0, 0));
        zoomOutXB.addMouseListener(continuousActionListener);
        xzoombox.add(zoomOutXB);
    }

    protected void addZoomInYButton(String id) {
        zoomInYB = new com.affymetrix.igb.swing.JRPButton(id + "_zoomInY_button", ZoomInYAction.getIconOnlyAction());
        zoomInYB.setToolTipText(BUNDLE.getString("verticalZoomerPlusButtonTooltip"));
        zoomInYB.setAlignmentX(CENTER_ALIGNMENT);
        zoomInYB.setMargin(new Insets(0, 0, 0, 0));
        zoomInYB.addMouseListener(continuousActionListener);
        yzoombox.add(zoomInYB, BorderLayout.SOUTH);
    }

    protected void addZoomOutYButton(String id) {
        zoomOutYB = new com.affymetrix.igb.swing.JRPButton(id + "_zoomOutYX_button", ZoomOutYAction.getIconOnlyAction());
        zoomOutYB.setToolTipText(BUNDLE.getString("verticalZoomerMinusButtonTooltip"));
        zoomOutYB.setAlignmentX(CENTER_ALIGNMENT);
        zoomOutYB.setMargin(new Insets(0, 0, 0, 0));
        zoomOutYB.addMouseListener(continuousActionListener);
        yzoombox.add(zoomOutYB, BorderLayout.NORTH);
    }

    protected void addRefreshButton(String id) {
        com.affymetrix.igb.swing.JRPButton refresh_button = new com.affymetrix.igb.swing.JRPButton(id + "_refresh_button", refreshDataAction);
//		refresh_button.setText("");
        refresh_button.setIcon(MenuUtil.getIcon("16x16/actions/refresh.png"));
        refresh_button.setMargin(new Insets(2, 4, 2, 4));
        xzoombox.add(refresh_button);
    }

    protected void addLoadResidueButton(String id) {
        partial_residuesB = new com.affymetrix.igb.swing.JRPButton("DataAccess_sequenceInView", LoadPartialSequenceAction.getAction());
        partial_residuesB.setToolTipText(MessageFormat.format(IGBConstants.BUNDLE.getString("load"), IGBConstants.BUNDLE.getString("partialNucleotideSequence")));
        partial_residuesB.setIcon(CommonUtils.getInstance().getIcon("16x16/actions/dna.gif"));
        partial_residuesB.setText("Load Sequence");
        partial_residuesB.setMargin(new Insets(2, 4, 2, 4));
        xzoombox.add(partial_residuesB);
    }

    protected void addSearchButton(String id) {
        com.affymetrix.igb.swing.JRPButton searchButton = new com.affymetrix.igb.swing.JRPButton(this.id + "_search_button",
                new GenericAction(null, BUNDLE.getString("magnifyGlassTolltip"),
                        "16x16/actions/system-search.png",
                        null, //"22x22/actions/system-search.png",
                        KeyEvent.VK_UNDEFINED) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        super.actionPerformed(e);
                        map_range_box.actionPerformed(e);
                    }
                }
        );
        searchButton.setMargin(new Insets(2, 4, 2, 4));
        xzoombox.add(searchButton);
    }

    protected Adjustable getXZoomer(String id) {
        return new ThresholdXZoomer(id, this);
    }

    public TierLabelManager getTierManager() {
        return tierLabelManager;
    }

    public TierGlyph getAxisTier() {
        return axis_tier;
    }

    protected void clear() {
        seqmap.clearWidget();
        aseq = null;
        this.viewseq = null;
        clearSelection();
        TrackView.getInstance().clear();
        seqmap.setMapRange(default_range[0], default_range[1]);
        seqmap.setMapOffset(default_offset[0], default_offset[1]);
        seqmap.stretchToFit(true, true);
        seqmap.updateWidget();
    }

    public void dataRemoved() {
        setAnnotatedSeq(aseq);
        AltSpliceView slice_view = (AltSpliceView) ((IGB) IGB.getSingleton()).getView(AltSpliceView.class.getName());
        if (slice_view != null) {
            slice_view.getSplicedView().dataRemoved();
        }
    }

    /**
     * Sets the sequence; if null, has the same effect as calling clear().
     */
    public void setAnnotatedSeq(BioSeq seq) {
        setAnnotatedSeq(seq, false, (seq == this.aseq) && (seq != null));
    }

    /**
     * Sets the sequence. If null, has the same effect as calling clear().
     *
     * @param preserve_selection if true, then try and keep same selections
     * @param preserve_view if true, then try and keep same scroll and zoom /
     * scale and offset in // both x and y direction. [GAH: temporarily changed
     * to preserve scale in only the x direction]
     */
    @Override
    public void setAnnotatedSeq(BioSeq seq, boolean preserve_selection, boolean preserve_view) {
        setAnnotatedSeq(seq, preserve_selection, preserve_view, false);
    }

    @Override
    public void setAnnotatedSeq(BioSeq seq, boolean preserve_selection, boolean preserve_view_x, boolean preserve_view_y) {
        if (seq == null) {
            //clear();
            return;
        }

        boolean same_seq = (seq == this.aseq);

        List<SeqSymmetry> old_selections = Collections.<SeqSymmetry>emptyList();
        double old_zoom_spot_x = seqmap.getZoomCoord(AffyTieredMap.X);
        double old_zoom_spot_y = seqmap.getZoomCoord(AffyTieredMap.Y);

        if (same_seq) {
            // Gather information about what is currently selected, so can restore it later
            if (preserve_selection) {
                old_selections = getSelectedSyms();
            } else {
                old_selections = Collections.<SeqSymmetry>emptyList();
            }
        }

        // Save selected tiers
        List<TierGlyph> old_tier_selections = getTierManager().getSelectedTiers();
        List<TierGlyph> cur_tiers = new ArrayList<>(seqmap.getTiers());
        TierGlyph axisTierGlyph = (axis_tier == null) ? null : axis_tier;
        int axis_index = Math.max(0, cur_tiers.indexOf(axisTierGlyph));	// if not found, set to 0
        List<TierGlyph> temp_tiers = copyMapTierGlyphs(cur_tiers, axis_index);

        seqmap.clearWidget();
        seqmap.clearSelected(); // may already be done by map.clearWidget()

        pixel_floater_glyph.removeAllChildren();
        pixel_floater_glyph.setParent(null);

        seqmap.addItem(pixel_floater_glyph);

        // Synchronized to keep aseq from getting set to null
        synchronized (this) {
            aseq = seq;

            // if shifting coords, then seq2viewSym and viewseq are already taken care of,
            //   but reset coord_shift to false...
            if (coord_shift) {
                // map range will probably change after this if SHRINK_WRAP_MAP_BOUNDS is set to true...
                coord_shift = false;
            } else {
                this.viewseq = seq;
                seq2viewSym = null;
                transform_path = null;
            }

            seqmap.setMapRange(viewseq.getMin(), viewseq.getMax());
            addGlyphs(temp_tiers, axis_index);
        }

        seqmap.repack();

        if (same_seq && preserve_selection) {
            // reselect glyph(s) based on selected sym(s);
            // Unfortunately, some previously selected syms will not be directly
            // associatable with new glyphs, so not all selections can be preserved
            for (SeqSymmetry old_selected_sym : old_selections) {
                GlyphI gl = seqmap.getItemFromTier(old_selected_sym);
                if (gl != null) {
                    seqmap.select(gl);
                }
            }
            setZoomSpotX(old_zoom_spot_x);
            setZoomSpotY(old_zoom_spot_y);
        } else {
            // do selection based on what the genometry model thinks is selected
            List<SeqSymmetry> symlist = gmodel.getSelectedSymmetries(seq);
            select(symlist, false, false, false);

            setSelectionStatus(getSelectionTitle(seqmap.getSelected()));
        }

        // Restore selected tiers
        if (old_tier_selections != null) {
            getTierManager().getAllTierLabels().stream().filter(tierLabelGlyph -> tierLabelGlyph.getReferenceTier().isVisible()
                    && old_tier_selections.contains(tierLabelGlyph.getReferenceTier())).forEach(tierLabelGlyph -> {
                        ((AffyLabelledTierMap) getSeqMap()).getLabelMap().select(tierLabelGlyph);
                    });
        }

        if (showEdgeMatches) {
            doEdgeMatching(seqmap.getSelected(), false);
        }

        if (shrinkWrapMapBounds) {
            shrinkWrap();
        }

        seqmap.toFront(axis_tier);

        // restore floating layers to front of map
        getFloatingLayers(seqmap.getScene().getGlyph()).forEach(seqmap::toFront);

        // Ignore preserve_view if seq has changed
        if ((preserve_view_x || preserve_view_y) && same_seq) {
            if (horizontalClampedRegion != null) {
                seqmap.setMapRange(horizontalClampedRegion.getStart(), horizontalClampedRegion.getEnd());
            }
            seqmap.stretchToFit(!preserve_view_x, !preserve_view_y);

            /**
             * Possible bug : When all strands are hidden. tier label and tiers
             * do appear at same position.
             *
             */
            // NOTE: Below call to stretchToFit is not redundancy. It is there
            //       to solve above mentioned bug.
            // Probably not necessary after a fix in r9248 - HV
            //seqmap.stretchToFit(!preserve_view_x, !preserve_view_y);
        } else {
            seqmap.stretchToFit(true, true);

            zoomToSelections();
            postSelections();
            int[] range = seqmap.getVisibleRange();
            setZoomSpotX(0.5 * (range[0] + range[1]));
            if (horizontalClampedRegion != null) {
                horizontalClamp(false);
            }
        }

        seqMapRefresh();

        seqmap.updateWidget();

        //A Temporary hack to solve problem when a 'genome' is selected
        if (IGBConstants.GENOME_SEQ_ID.equals((seq.getID()))) {
            seqmap.scroll(NeoMap.X, seqmap.getScroller(NeoMap.X).getMinimum());
        }

        if (seq.getLength() > 0) {
            autoScroll.configure(seqmap, 0, seq.getLength());
        }
    }

    public void seqMapRefresh() {
        seqmap_refresh_list.forEach(com.affymetrix.genometry.event.SeqMapRefreshed::mapRefresh);
    }

    /**
     * Returns a tier for the given style and direction, creating them if they
     * don't already exist. Generally called by the Glyph Factory. Note that
     * this can create empty tiers. But if the tiers are not filled with
     * something, they will later be removed automatically.
     *
     * @param sym The SeqSymmetry (data model) for the track
     * @param style a non-null instance of IAnnotStyle; tier label and other
     * properties are determined by the IAnnotStyle.
     * @param tier_direction the direction of the track (FORWARD, REVERSE, or
     * BOTH)
     */
    @Override
    public TierGlyph getTrack(ITrackStyleExtended style, StyledGlyph.Direction tier_direction) {
        return TrackView.getInstance().getTrack(this, style, tier_direction);
    }

    public void preserveSelectionAndPerformAction(AbstractAction action) {
        // If action is null then there is no point of this method.
        if (action == null) {
            return;
        }

        List<SeqSymmetry> old_sym_selections = getSelectedSyms();
        seqmap.clearSelected();
        if (showEdgeMatches) {
            doEdgeMatching(seqmap.getSelected(), false);
        }

        action.actionPerformed(null);

        // reselect glyph(s) based on selected sym(s);
        // Unfortunately, some previously selected syms will not be directly
        // associatable with new glyphs, so not all selections can be preserved
        for (SeqSymmetry old_selected_sym : old_sym_selections) {
            GlyphI gl = seqmap.getItemFromTier(old_selected_sym);
            if (gl != null) {
                seqmap.select(gl);
            }
        }
        if (showEdgeMatches) {
            doEdgeMatching(seqmap.getSelected(), true);
        }
    }

    // copying map tiers to separate list to avoid problems when removing tiers
    //   (and thus modifying map.getTiers() list -- could probably deal with this
    //    via iterators, but feels safer this way...)
    private List<TierGlyph> copyMapTierGlyphs(List<TierGlyph> cur_tiers, int axis_index) {
        List<TierGlyph> temp_tiers = new ArrayList<>();
        for (TierGlyph tg : cur_tiers) {
            temp_tiers.add(tg);
            if (DEBUG_TIERS) {
            }
            tg.removeAllChildren();
            tg.setScene(null);
        }
        return temp_tiers;
    }

    private void addGlyphs(List<TierGlyph> temp_tiers, int axis_index) {
        // The hairline needs to be among the first glyphs added,
        // to keep it from interfering with selection of other glyphs.
        if (hairline != null) {
            hairline.destroy();
        }
        hairline = new UnibrowHairline(seqmap);
        addPreviousTierGlyphs(seqmap, temp_tiers);

        // Since axis has to added only one, add it here instead of annotation track.
        axis_tier = this.getTrack(CoordinateStyle.coordinate_annot_style, StyledGlyph.Direction.AXIS);
        MapTierGlyphFactoryI factory = MapTierTypeHolder.getDefaultFactoryFor(FileTypeCategory.Axis);
        factory.createGlyphs(null, CoordinateStyle.coordinate_annot_style, this, aseq);

        addAnnotationTracks();
        removeEmptyTierGlyphs(new ArrayList<>(seqmap.getTiers()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <G extends GlyphI> G getItemFromTier(Object datamodel) {
        return (G) seqmap.getItemFromTier(datamodel);
    }

    @Override
    public boolean shouldAddCytobandGlyph() {
        return true;
    }

    private void shrinkWrap() {
        /*
         * Shrink wrapping is a little more complicated than one might expect,
         * but it needs to take into account the mapping of the annotated
         * sequence to the view (although currently assumes this mapping doesn't
         * do any rearrangements, etc.) (alternative, to ensure that _arbitrary_
         * genometry mapping can be accounted for, is to base annotation bounds
         * on map glyphs, but then have to go into tiers to get children bounds,
         * and filter out stuff like axis and DNA glyphs, etc...)
         */
        SeqSpan annot_bounds = SeqUtils.getAnnotationBounds(aseq);
        if (annot_bounds != null) {
            // transform to view
            MutableSeqSymmetry sym = new SimpleMutableSeqSymmetry();
            sym.addSpan(annot_bounds);
            if (aseq != viewseq) {
                SeqUtils.transformSymmetry(sym, transform_path);
            }
            SeqSpan view_bounds = sym.getSpan(viewseq);
            seqmap.setMapRange(view_bounds.getMin(), view_bounds.getMax());
        }
    }

    /**
     * hide TierGlyphs with no children (that is how IGB indicates that a glyph
     * is garbage)
     *
     * @param tiers the list of TierGlyphs
     */
    private void removeEmptyTierGlyphs(List<TierGlyph> tiers) {
        tiers.stream().filter(tg -> tg.getChildCount() == 0).forEach(seqmap::removeTier);
    }

    private void addAnnotationTracks() {
        TrackView.getInstance().addTracks(this, aseq);
        if (aseq.getComposition() != null) {
            handleCompositionSequence();
        }
        addDependentAndEmptyTrack();
    }

    protected void addDependentAndEmptyTrack() {
        TrackView.getInstance().addDependentAndEmptyTrack(this, aseq);
    }

    // muck with aseq, seq2viewsym, transform_path to trick addAnnotationTiers(),
    // addLeafsToTier(), addToTier(), etc. into mapping from composition sequences
    private void handleCompositionSequence() {
        BioSeq cached_aseq = aseq;
        MutableSeqSymmetry cached_seq2viewSym = seq2viewSym;
        SeqSymmetry[] cached_path = transform_path;
        SeqSymmetry comp = aseq.getComposition();
        // assuming a two-level deep composition hierarchy for now...
        // need to make more recursive at some point...
        // (or does recursive call to addAnnotationTiers already give us full recursion?!!)
        int scount = comp.getChildCount();
        for (int i = 0; i < scount; i++) {
            SeqSymmetry csym = comp.getChild(i);
            // return seq in a symmetry span that _doesn't_ match aseq
            BioSeq cseq = SeqUtils.getOtherSeq(csym, cached_aseq);
            if (cseq != null) {
                aseq = cseq;
                if (cached_seq2viewSym == null) {
                    transform_path = new SeqSymmetry[1];
                    transform_path[0] = csym;
                } else {
                    transform_path = new SeqSymmetry[2];
                    transform_path[0] = csym;
                    transform_path[1] = cached_seq2viewSym;
                }
                addAnnotationTracks();
            }
        }
        // restore aseq and seq2viewsym afterwards...
        aseq = cached_aseq;
        seq2viewSym = cached_seq2viewSym;
        transform_path = cached_path;
    }

    @Override
    public boolean isGenomeSequenceSupported() {
        return true;
    }

    @Override
    public BioSeq getAnnotatedSeq() {
        return aseq;
    }

    /**
     * Gets the view seq. Note: {@link #getViewSeq()} and
     * {@link #getAnnotatedSeq()} may return different BioSeq's ! This allows
     * for reverse complement, coord shifting, seq slicing, etc. Returns BioSeq
     * that is the SeqMapView's _view_ onto the BioSeq returned by
     * getAnnotatedSeq()
     *
     * @see #getTransformPath()
     */
    @Override
    public BioSeq getViewSeq() {
        return viewseq;
    }

    /**
     * Returns the series of transformations that can be used to map a
     * SeqSymmetry from {@link #getAnnotatedSeq()} to {@link #getViewSeq()}.
     */
    @Override
    public SeqSymmetry[] getTransformPath() {
        return transform_path;
    }

    /**
     * Returns a transformed copy of the given symmetry based on
     * {@link #getTransformPath()}. If no transform is necessary, simply returns
     * the original symmetry.
     */
    @Override
    public SeqSymmetry transformForViewSeq(SeqSymmetry insym, BioSeq seq_to_compare) {
        if (seq_to_compare != getViewSeq()) {
            MutableSeqSymmetry tempsym = SeqUtils.copyToDerived(insym);
            SeqUtils.transformSymmetry(tempsym, getTransformPath());
            return tempsym;
        }
        return insym;
    }

    @Override
    public SeqSymmetry transformForViewSeq(SeqSymmetry insym, MutableSeqSymmetry result, BioSeq seq_to_compare) {
        if (seq_to_compare != getViewSeq()) {
            SeqUtils.transformSymmetry(result, getTransformPath());
            return result;
        }
        return insym;
    }

    @Override
    public AffyTieredMap getSeqMap() {
        return seqmap;
    }

    @Override
    public void selectAllGraphs() {
        List<GraphGlyph> glyphlist = collectGraphs();
        List<GraphGlyph> visibleList = new ArrayList<>(glyphlist.size());

        //Remove hidden Graphs
        for (GraphGlyph g : glyphlist) {
            if (g.getGraphState().getTierStyle().getShow()) {
                visibleList.add(g);
            }
        }
        glyphlist.clear();

        // convert graph glyphs to GraphSyms via glyphsToSyms
        // Bring them all into the visual area
        visibleList.stream().filter(gl -> gl.getAnnotStyle().isFloatTier()).forEach(gl -> {
            getFloaterGlyph().checkBounds(gl, getSeqMap().getView());
        });

        select(glyphsToSyms(visibleList), false, true, true);
    }

    @Override
    public void select(List<SeqSymmetry> sym_list, boolean normal_selection) {
        select(sym_list, false, normal_selection, true);
        if (normal_selection) {
            zoomToSelections();
            List<GlyphI> glyphs = seqmap.getSelected();
            setSelectionStatus(getSelectionTitle(glyphs));
            if (showEdgeMatches) {
                doEdgeMatching(glyphs, false);
            }
            if (autoload != null) {
                autoload.loadData();
            }
        }
    }

    @Override
    public void select(GlyphI glyph) {
        List<GlyphI> glyphs = Collections.singletonList(glyph);
        setSelectionStatus(getSelectionTitle(glyphs));
    }

    public void selectTrack(TierGlyph tier, boolean selected) {
        if (tier.getAnnotStyle().isFloatTier()) {
            tier.setSelected(selected);
        } else if (selected) {
            tierLabelManager.select(tier);
        } else {
            tierLabelManager.deselect(tier);
        }
        seqmap.updateWidget();
        postSelections();
    }

    public void select(List<SeqSymmetry> sym_list, boolean add_to_previous, boolean call_listeners, boolean update_widget) {
        if (!add_to_previous) {
            clearSelection();
        }

        for (SeqSymmetry sym : sym_list) {
            // currently assuming 1-to-1 mapping of sym to glyph
            GlyphI gl = seqmap.getItemFromTier(sym);
            if (gl != null) {
                seqmap.select(gl);
            }
        }
        if (update_widget) {
            seqmap.updateWidget();
        }
        if (call_listeners) {
            postSelections();
        }
    }

    protected void clearSelection() {
        sym_used_for_title = null;
        tier_used_in_selection_info = null;
        seqmap.clearSelected();
        setSelectedRegion(null, null, false);
        //  clear match_glyphs?
    }

    /**
     * Figures out which symmetries are currently selected and then calls
     * {@link GenometryModel#setSelectedSymmetries(List, List, Object)}.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void postSelections() {
        // Note that seq_selected_sym (the selected residues) is not included in selected_syms
        gmodel.setSelectedSymmetries(glyphsToRootSyms(tierLabelManager.getSelectedTiers()), getSelectedSyms(), this);
    }

    @Override
    public void trackstylePropertyChanged(EventObject eo) {
        //postSelections();
    }

    // assumes that region_sym contains a span with span.getBioSeq() ==  current seq (aseq)
    public void setSelectedRegion(SeqSymmetry region_sym, GlyphI seq_glyph, boolean update_widget) {
        seq_selected_sym = region_sym;
        // Note: SUBSELECT_SEQUENCE might possibly be set to false in the AltSpliceView
        if (subselectSequence && seq_glyph != null) {
            if (region_sym == null) {
                seq_glyph.setSelected(false);
            } else {
                SeqSpan seq_region = seq_selected_sym.getSpan(aseq);
                // corrected for interbase coords
                seqmap.select(seq_glyph, seq_region.getMin(), seq_region.getMax() - 1);
                setSelectionStatus(SeqUtils.spanToString(seq_region));
            }
            if (update_widget) {
                seqmap.updateWidget();
            }
        }
    }

    public List<GlyphI> getAllSelectedTiers() {
        List<GlyphI> allSelectedTiers = new ArrayList<>();
        // this adds all tracks selected on the label, including join tracks (not join children)
        for (TierGlyph tierGlyph : tierLabelManager.getSelectedTiers()) {
            allSelectedTiers.add(tierGlyph);
        }
        // this adds all tracks selected on the track itself (arrow on left edge), including join tracks and join children
        tierLabelManager.getVisibleTierGlyphs().stream().filter(tierGlyph -> !allSelectedTiers.contains(tierGlyph)).forEach(tierGlyph -> {
            if (tierGlyph.getTierType() == TierGlyph.TierType.GRAPH && tierGlyph.getChildCount() > 0) {
                for (GlyphI child : tierGlyph.getChildren()) {
                    if (child.isSelected()) {
                        allSelectedTiers.add(child);
                    }
                }
            } else if (tierGlyph.isSelected()) {
                allSelectedTiers.add(tierGlyph);
            }
        });
        return allSelectedTiers;
    }

    @Override
    public List<GraphGlyph> getSelectedFloatingGraphGlyphs() {
        List<GraphGlyph> graphGlyphs = new ArrayList<>();
        if (pixel_floater_glyph.getChildren() != null) {
            for (GlyphI floatGlyph : pixel_floater_glyph.getChildren()) {
                if (floatGlyph.isSelected()) {
                    graphGlyphs.add((GraphGlyph) floatGlyph);
                }
            }
        }
        return graphGlyphs;
    }

    public List<GraphGlyph> getFloatingGraphGlyphs() {
        List<GraphGlyph> graphGlyphs = new ArrayList<>();
        if (pixel_floater_glyph.getChildren() != null) {
            for (GlyphI floatGlyph : pixel_floater_glyph.getChildren()) {
                graphGlyphs.add((GraphGlyph) floatGlyph);
            }
        }
        return graphGlyphs;
    }

    /**
     * Determines which SeqSymmetry's are selected by looking at which Glyph's
     * are currently selected. The list will not include the selected sequence
     * region, if any. Use getSelectedRegion() for that.
     *
     * @return a List of SeqSymmetry objects, possibly empty.
     */
    @Override
    public List<SeqSymmetry> getSelectedSyms() {
        return glyphsToSyms(seqmap.getSelected());
    }

    @Override
    public void zoomTo(SeqSpan span) {
        BioSeq zseq = span.getBioSeq();
        if (zseq != null && zseq != this.getAnnotatedSeq()) {
            gmodel.setSelectedSeq(zseq);
        }
        zoomTo(span.getMin(), span.getMax());
    }

    public double getPixelsToCoord(double smin, double smax) {
        if (getAnnotatedSeq() == null) {
            return 0;
        }
        double coord_width = Math.min(getAnnotatedSeq().getLengthDouble(), smax) - Math.max(getAnnotatedSeq().getMin(), smin);
        double pixel_width = seqmap.getView().getPixelBox().width;
        double pixels_per_coord = pixel_width / coord_width; // can be Infinity, but the Math.min() takes care of that
        pixels_per_coord = Math.min(pixels_per_coord, seqmap.getMaxZoom(NeoAbstractWidget.X));
        return pixels_per_coord;
    }

    public void zoomTo(double smin, double smax) {
        double pixels_per_coord = getPixelsToCoord(smin, smax);
        seqmap.zoom(NeoAbstractWidget.X, pixels_per_coord);
        seqmap.scroll(NeoAbstractWidget.X, smin);
        seqmap.setZoomBehavior(AffyTieredMap.X, AffyTieredMap.CONSTRAIN_COORD, (smin + smax) / 2);
        seqmap.updateWidget();
    }

    /**
     * Zoom to a region including all the currently selected Glyphs.
     */
    public void zoomToSelections() {
        List<GlyphI> selections = seqmap.getSelected();
        if (selections.size() > 0) {
            zoomToGlyphs(selections);
        } else if (seq_selected_sym != null) {
            SeqSpan span = getViewSeqSpan(seq_selected_sym);
            zoomTo(span);
        }
    }

    /**
     * Center at the hairline.
     */
    @Override
    public void centerAtHairline() {
        if (this.hairline == null) {
            return;
        }
        double pos = this.hairline.getSpot();
        Rectangle2D.Double vbox = this.getSeqMap().getViewBounds();
        double map_start = pos - vbox.width / 2;

        this.getSeqMap().scroll(NeoMap.X, map_start);
        this.setZoomSpotX(pos);
        this.getSeqMap().updateWidget();
    }

    public void zoomToGlyphs(List<GlyphI> glyphs) {
        zoomToRectangle(getRegionForGlyphs(glyphs));
    }

    /**
     * Zoom to include (and slightly exceed) a given rectangular region in
     * coordbox coords.
     */
    private void zoomToRectangle(Rectangle2D.Double rect) {
        if (rect != null && aseq != null) {
            double desired_width = Math.min(rect.width * 1.1f, aseq.getLength() * 1.0f);
            seqmap.zoom(NeoAbstractWidget.X, Math.min(
                    seqmap.getView().getPixelBox().width / desired_width,
                    seqmap.getMaxZoom(NeoAbstractWidget.X)));
            seqmap.scroll(NeoAbstractWidget.X, -(seqmap.getVisibleRange()[0]));
            seqmap.scroll(NeoAbstractWidget.X, (rect.x - rect.width * 0.05));
            double map_center = rect.x + rect.width / 2 - seqmap.getViewBounds().width / 2;
            seqmap.scroll(NeoAbstractWidget.X, map_center);	// Center at hairline
            seqmap.setZoomBehavior(AffyTieredMap.X, AffyTieredMap.CONSTRAIN_COORD, (rect.x + rect.width / 2));
            seqmap.setZoomBehavior(AffyTieredMap.Y, AffyTieredMap.CONSTRAIN_COORD, (rect.y + rect.height / 2));
            seqmap.updateWidget();
        }
    }

    public void toggleHorizontalClamp() {
        preserveSelectionAndPerformAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                horizontalClamp(horizontalClampedRegion == null);
                seqmap.repackTheTiers(true, false);
            }
        });
    }

    public void horizontalClamp(boolean clamp) {
        if (clamp) {
            Rectangle2D.Double vbox = seqmap.getViewBounds();
            horizontalClamp((int) (vbox.x), (int) (vbox.x + vbox.width));
        } else {
            if (viewseq != null) {
                horizontalClamp(viewseq.getMin(), viewseq.getMax());
            }
        }
        ClampViewAction.getAction().putValue(Action.SELECTED_KEY, horizontalClampedRegion != null);
    }

    protected void horizontalClamp(int start, int end) {
        if (viewseq != null && viewseq.getMin() == start && viewseq.getMax() == end) {
            horizontalClampedRegion = null;
        } else {
            horizontalClampedRegion = new SimpleSeqSpan(start, end, viewseq);
        }
        seqmap.setMapRange(start, end);
    }

    /**
     * Do edge matching. If query_glyphs is empty, clear all edges.
     *
     * @param query_glyphs
     * @param update_map
     */
    public void doEdgeMatching(List<GlyphI> query_glyphs, boolean update_map) {
        // Clear previous edges
        seqmap.clearEdgeMatches();

        int qcount = query_glyphs.size();
        if (qcount <= 0) {
            return;
        }

        int match_query_count = query_glyphs.size();
        for (int i = 0; i < qcount && match_query_count <= max_for_matching; i++) {
            match_query_count += query_glyphs.get(i).getChildCount();
        }

        if (match_query_count <= max_for_matching) {
            Rectangle2D.Double coordrect = new Rectangle2D.Double(query_glyphs.get(0).getCoordBox().x, 0, 0, seqmap.getScene().getCoordBox().height);
            for (GlyphI glyph : query_glyphs) {
                Rectangle2D.Double.union(coordrect, glyph.getCoordBox(), coordrect);
            }

            List<GlyphI> target_glyphs = doTheSelection(coordrect);

            if (target_glyphs.isEmpty()) {
                return;
            }

            double fuzz = getEdgeMatcher().getFuzziness();
            if (fuzz == 0.0) {
                Color edge_match_color = PreferenceUtils.getColor(PREF_EDGE_MATCH_COLOR, default_edge_match_color);
                getEdgeMatcher().setColor(edge_match_color);
            } else {
                Color edge_match_fuzzy_color = PreferenceUtils.getColor(PREF_EDGE_MATCH_FUZZY_COLOR, default_edge_match_fuzzy_color);
                getEdgeMatcher().setColor(edge_match_fuzzy_color);
            }
            seqmap.addEdgeMatches(getEdgeMatcher().matchEdges(seqmap, query_glyphs, target_glyphs));
            setStatus(null);
        }

        if (update_map) {
            seqmap.updateWidget();
        }
    }

    protected void setEdgeMatching(boolean b) {
        showEdgeMatches = b;
        if (showEdgeMatches) {
            doEdgeMatching(seqmap.getSelected(), true);
        } else {
            doEdgeMatching(new ArrayList<>(0), true);
        }
    }

    public List<GlyphI> doTheSelection(Rectangle2D.Double coordrect) {
        List<GlyphI> glyphs = new ArrayList<>();

        for (TierGlyph tg : seqmap.getTiers()) {
            // Do not perform selection on axis tier childrens
            if (tg == getAxisTier()) {
                continue;
            }
            //First check of tier glyph intersects
            if (tg.isVisible() && tg.intersects(coordrect, seqmap.getView())) {
                glyphs.addAll(tg.pickTraversal(coordrect, seqmap.getView()));
            }
        }

        return glyphs;
    }

    public void adjustEdgeMatching(int bases) {
        getEdgeMatcher().setFuzziness(bases);
        if (showEdgeMatches) {
            doEdgeMatching(seqmap.getSelected(), true);
        }
    }

    public void redoEdgeMatching() {
        if (showEdgeMatches) {
            doEdgeMatching(seqmap.getSelected(), true);
        }
    }

    /**
     * return a SeqSpan representing the visible bounds of the view seq
     */
    @Override
    public SeqSpan getVisibleSpan() {
        Rectangle2D.Double vbox = seqmap.getView().getCoordBox();
        SeqSpan vspan = new SimpleSeqSpan((int) vbox.x,
                (int) (vbox.x + vbox.width),
                viewseq);
        return vspan;
    }

    public GlyphEdgeMatcher getEdgeMatcher() {
        return edge_matcher;
    }

    public void setShrinkWrap(boolean b) {
        shrinkWrapMapBounds = b;
        setAnnotatedSeq(aseq);
    }

    public boolean getShrinkWrap() {
        return shrinkWrapMapBounds;
    }

    /**
     * Sets the hairline position and zoom center to the given spot. Does not
     * call map.updateWidget()
     */
    @Override
    public final void setZoomSpotX(double x) {
        int intx = (int) x;
        if (hairline != null) {
            hairline.setSpot(intx);
        }
        seqmap.setZoomBehavior(AffyTieredMap.X, AffyTieredMap.CONSTRAIN_COORD, intx);
    }

    /**
     * Sets the hairline position to the given spot. Does not call
     * map.updateWidget()
     */
    @Override
    public final void setZoomSpotY(double y) {
        seqmap.setZoomBehavior(AffyTieredMap.Y, AffyTieredMap.CONSTRAIN_COORD, y);
    }

    @Override
    public SeqMapViewMouseListener getMouseListener() {
        return mouse_listener;
    }

    /**
     * Select the parents of the current selections
     */
    public void selectParents() {
        if (seqmap.getSelected().isEmpty()) {
            ErrorHandler.errorPanel("Nothing selected");
            return;
        }

        boolean top_level = seqmap.getSelected().size() > 1;
        // copy selections to a new list before starting, because list of selections will be modified
        List<GlyphI> all_selections = new ArrayList<>(seqmap.getSelected());
        for (GlyphI child : all_selections) {
            GlyphI pglyph = getParent(child, top_level);
            if (pglyph != child) {
                seqmap.deselect(child);
                seqmap.select(pglyph);
            }
        }

        if (showEdgeMatches) {
            doEdgeMatching(seqmap.getSelected(), false);
        }
        seqmap.updateWidget();
        postSelections();
    }

    public void setSelectionStatus(String title) {
        Map<String, Object> props = null;
        if (tier_used_in_selection_info != null) {
            props = TierLabelManager.getTierProperties(tier_used_in_selection_info);
        } else {
            props = determineProps(sym_used_for_title);
        }
        Application.getSingleton().setSelField(props, title, sym_used_for_title);
    }

    private void setStatus(String title) {
        if (!report_status_in_status_bar) {
            return;
        }
        Application.getSingleton().setStatus(title, false);
    }

    // Compare the code here with SymTableView.selectionChanged()
    // The logic about finding the ID from instances of DerivedSeqSymmetry
    // should be similar in both places, or else users could get confused.
    public String getSelectionTitle(List<GlyphI> selected_glyphs) {
        String id = null;
        tier_used_in_selection_info = null;
        if (selected_glyphs.isEmpty()) {
            id = "";
            sym_used_for_title = null;
            List<TierGlyph> tierglyphs = getTierManager().getSelectedTiers();
            if (!tierglyphs.isEmpty()) {
                if (tierglyphs.size() > 1) {
                    id = "" + tierglyphs.size() + " Selections";
                    sym_used_for_title = null;
                } else {
                    tier_used_in_selection_info = tierglyphs.get(0);
                    Map<String, Object> props = TierLabelManager.getTierProperties(tier_used_in_selection_info);
                    if (props != null && !props.isEmpty() && props.containsKey("File Name")) {
                        id = props.get("File Name").toString();
                        sym_used_for_title = (SeqSymmetry) tierglyphs.get(0).getInfo();
                    }
                }
            }
        } else {
            if (selected_glyphs.size() == 1) {
                GlyphI topgl = selected_glyphs.get(0);
                Object info = topgl.getInfo();
                SeqSymmetry sym = null;
                // IGBF-323 Really bad logic. Need to come up with something better.
                if (info instanceof SeqSymmetry) {
                    sym = (SeqSymmetry) info;
                }
                if (sym instanceof MutableSingletonSeqSymmetry) {
                    id = sym.getID();
                    sym_used_for_title = sym;
                }
                if (id == null && sym instanceof GraphSym) {
                    id = ((GraphSym) sym).getGraphName();
                    sym_used_for_title = sym;
                }
                if (id == null && sym instanceof SymWithProps) {
                    id = (String) ((SymWithProps) sym).getProperty(GENE_NAME);
                    sym_used_for_title = sym;
                }
                if (id == null && sym instanceof SymWithProps) {
                    id = (String) ((SymWithProps) sym).getProperty(ID);
                    sym_used_for_title = sym;
                }
                if (id == null && sym instanceof DerivedSeqSymmetry) {
                    SeqSymmetry original = ((DerivedSeqSymmetry) sym).getOriginalSymmetry();
                    if (original instanceof MutableSingletonSeqSymmetry) {
                        id = original.getID();
                        sym_used_for_title = original;
                    } else if (original instanceof SymWithProps) {
                        id = (String) ((SymWithProps) original).getProperty(ID);
                        sym_used_for_title = original;
                    }
                }
                if (id == null && sym instanceof CdsSeqSymmetry) {
                    SeqSymmetry property_sym = ((CdsSeqSymmetry) sym).getPropertySymmetry();
                    if (property_sym instanceof SymWithProps) {
                        id = (String) ((SymWithProps) property_sym).getProperty(GENE_NAME);
                        sym_used_for_title = sym;
                    }
                    if (id == null && property_sym instanceof SymWithProps) {
                        id = (String) ((SymWithProps) property_sym).getProperty(ID);
                        sym_used_for_title = sym;
                    }
                }
                if (id == null && topgl instanceof CharSeqGlyph && seq_selected_sym != null) {
                    SeqSpan seq_region = seq_selected_sym.getSpan(aseq);
                    id = SeqUtils.spanToString(seq_region);
                    sym_used_for_title = seq_selected_sym;
                }
                if (id == null && topgl instanceof GraphGlyph) {
                    GraphGlyph gg = (GraphGlyph) topgl;
                    if (gg.getLabel() != null) {
                        id = "Graph: " + gg.getLabel();
                    } else {
                        id = "Graph Selected";
                    }
                    sym_used_for_title = null;
                }
                if (id == null && sym instanceof SymWithProps) {
                    id = (String) ((SymWithProps) sym).getProperty(MATCH);
                    sym_used_for_title = sym;
                }
                if (id == null && sym instanceof SymWithProps) {
                    id = (String) ((SymWithProps) sym).getProperty(FEATURE_TYPE);
                    sym_used_for_title = sym;
                }
                if (id == null) {
                    id = "Unknown Selection";
                    sym_used_for_title = sym;
                }
            } else {
                sym_used_for_title = null;
                id = "" + selected_glyphs.size() + " Selections";
            }
        }
        return id;
    }

    private JRPPopupMenu getOrganizedPopups(JRPPopupMenu sym_popup) {
        TreeMap<Integer, Component> popups = sym_popup.getPopupsMap();
        for (Map.Entry<Integer, Component> mapEntry : popups.entrySet()) {
            sym_popup.add(mapEntry.getValue());
        }
        return sym_popup;
    }

    void showPopup(NeoMouseEvent nevt) {
        if (sym_popup == null) {
            sym_popup = new JRPPopupMenu();
        }
        sym_popup.setVisible(false); // in case already showing
        sym_popup.removeAll();

        if (seqmap.getSelected().isEmpty()) {
            Rectangle2D.Double cbox = new Rectangle2D.Double(nevt.getCoordX(), nevt.getCoordY(), 1, 1);
            for (TierGlyph tg : seqmap.getTiers()) {
                if (tg == getAxisTier()) {
                    continue;
                }
                if (tg.isVisible() && tg.intersects(cbox, seqmap.getView())) {
                    for (TierLabelGlyph tglyph : tierLabelManager.getAllTierLabels()) {
                        if (tglyph.getInfo() == tg) {
                            tierLabelManager.deselectTierLabels();
                            tierLabelManager.select(tglyph.getReferenceTier());
                            seqmap.updateWidget();
                            postSelections();
                            tierLabelManager.doPopup(nevt);
                            return;
                        }

                    }

                }
            }
            return;
        }

        preparePopup(sym_popup, nevt);
        sym_popup = getOrganizedPopups(sym_popup);

        if (sym_popup.getComponentCount()
                == 0) {
            return;
        }

        if (sym_popup.getComponentCount()
                > 0) {

            if (nevt == null) {
                // this might happen from pressing the Windows context menu key
                sym_popup.show(seqmap, 15, 15);
                return;
            }

            // if resultSeqMap is a MultiWindowTierMap, then using resultSeqMap as Component target arg to popup.show()
            //  won't work, since its component is never actually rendered -- so checking here
            /// to use appropriate target Component and pixel position
            EventObject oevt = nevt.getOriginalEvent();
            if ((oevt != null) && (oevt.getSource() instanceof Component)) {
                Component target = (Component) oevt.getSource();
                if (oevt instanceof MouseEvent) {
                    MouseEvent mevt = (MouseEvent) oevt;
                    sym_popup.show(target, mevt.getX() + xoffset_pop, mevt.getY() + yoffset_pop);
                } else {
                    sym_popup.show(target, nevt.getX() + xoffset_pop, nevt.getY() + yoffset_pop);
                }
            } else {
                sym_popup.show(seqmap, nevt.getX() + xoffset_pop, nevt.getY() + yoffset_pop);
            }
        }
        sym_popup.repaint();
    }

    public boolean isPopupActive() {
        if (sym_popup != null) {
            return sym_popup.isVisible();
        } else {
            return false;
        }
    }

    /**
     * Prepares the given popup menu to be shown. The popup menu should have
     * items added to it by this method. Display of the popup menu will be
     * handled by showPopup(), which calls this method.
     */
    protected void preparePopup(JRPPopupMenu popup, NeoMouseEvent nevt) {
        final List<GlyphI> selected_glyphs = seqmap.getSelected();

        Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        if (selected_glyphs.size() == 1) {
            Border colorBorder = BorderFactory.createLineBorder(selected_glyphs.get(0).getColor());
            popup.setBorder(BorderFactory.createCompoundBorder(colorBorder, emptyBorder));
        } else {
            Border colorBorder = BorderFactory.createLineBorder(Color.BLACK);
            popup.setBorder(BorderFactory.createCompoundBorder(colorBorder, emptyBorder));
        }

        JMenuItem select_parent_action = new JMenuItem(SelectParentAction.getAction());
        select_parent_action.setIcon(null);
        JMenuItem zoom_on_selected = new JMenuItem(ZoomOnSelectedSymsAction.getAction());
        JMenuItem load_partial_sequence = new JMenuItem(LoadPartialSequenceAction.getAction());
        load_partial_sequence.setIcon(null);
        JMenuItem copy_residues_action = new JMenuItem(CopyResiduesAction.getAction());
        copy_residues_action.setIcon(null);

        JMenuItem select_rule_action = new JMenuItem(SelectionRuleAction.getAction());

        final List<SeqSymmetry> selected_syms = getSelectedSyms();

        if (!selected_syms.isEmpty() && !(selected_syms.get(0) instanceof GraphSym)) {
            for (ContextualPopupListener listener : popup_listeners) {
                listener.popupNotify(popup, selected_syms, sym_used_for_title);
            }
            JSeparator afterGetInfoSep = new JSeparator();
            JSeparator afterViewReadSep = new JSeparator();
            JSeparator afterPrimerBalstSep = new JSeparator();
            popup.add(afterGetInfoSep, 6);
            popup.add(afterViewReadSep, 12);
            popup.add(afterPrimerBalstSep, 20);
            popup.add(select_rule_action, 4);
            popup.add(select_parent_action, 22);
            popup.add(zoom_on_selected, 24);

        }

        TierGlyph tglyph = tierLabelManager.getTierGlyph(nevt);

        if (tglyph != null) {
            GenericFeature feature = tglyph.getAnnotStyle().getFeature();
            if (feature == null) {
                //Check if clicked on axis.
                if (tglyph == axis_tier) {
                    SeqSpan visible = getVisibleSpan();
                    if (selected_syms.isEmpty() && !gmodel.getSelectedSeq().isAvailable(visible.getMin(), visible.getMax())) {
                        popup.add(load_partial_sequence);
                    }

                    if (seq_selected_sym != null && aseq.isAvailable(seq_selected_sym.getSpan(aseq))) {
                        popup.add(copy_residues_action);
                        for (AxisPopupListener listener : axisPopupListeners) {
                            listener.addPopup(popup);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void addAxisPopupListener(AxisPopupListener listener) {
        axisPopupListeners.add(listener);
    }

    @Override
    public void removeAxisPopupListener(AxisPopupListener listener) {
        axisPopupListeners.remove(listener);
    }

    @Override
    public void addPopupListener(ContextualPopupListener listener) {
        popup_listeners.add(listener);
    }

    @Override
    public void removePopupListener(ContextualPopupListener listener) {
        popup_listeners.remove(listener);
    }

    private boolean matchesCategory(RootSeqSymmetry rootSeqSymmetry, FileTypeCategory... categories) {
        if (rootSeqSymmetry == null) {
            return false;
        }

        if (categories == null || categories.length == 0 || (categories.length == 1 && categories[0] == null)) {
            return true;
        }

        for (FileTypeCategory category : categories) {
            if (rootSeqSymmetry.getCategory() == category) {
                return true;
            }
        }

        return false;
    }

    public void selectAll(FileTypeCategory... category) {
        clearAllSelections();
        // this selects all regular tracks on the label
        AffyTieredMap labelmap = ((AffyLabelledTierMap) seqmap).getLabelMap();
        for (TierLabelGlyph labelGlyph : tierLabelManager.getAllTierLabels()) {
            TierGlyph tierGlyph = (TierGlyph) labelGlyph.getInfo();
            if (labelGlyph.isVisible()
                    && tierGlyph.getInfo() != null) {
                boolean matches = matchesCategory((RootSeqSymmetry) tierGlyph.getInfo(), category);
                if (matches) {
                    labelmap.select(labelGlyph);
                }
            }
        }
        // this selects all floating tracks
        if (pixel_floater_glyph.getChildren() != null) {
            for (GlyphI floatGlyph : pixel_floater_glyph.getChildren()) {
                boolean matches = matchesCategory((RootSeqSymmetry) floatGlyph.getInfo(), category);
                if (matches) {
                    seqmap.select(floatGlyph);
                }
            }
        }
        // this selects all join subtracks on the track itself (arrow on left edge)
        tierLabelManager.getVisibleTierGlyphs().stream().filter(tierGlyph -> tierGlyph.getTierType() == TierGlyph.TierType.GRAPH && tierGlyph.getChildCount() > 0).forEach(tierGlyph -> {
            for (GlyphI child : tierGlyph.getChildren()) {
                boolean matches = matchesCategory((RootSeqSymmetry) child.getInfo(), category);
                if (matches) {
                    seqmap.select(child);
                }
            }
        });
        gmodel.setSelectedSymmetries(glyphsToRootSyms(tierLabelManager.getSelectedTiers()), getSelectedSyms(), this);
        seqmap.updateWidget();
    }

    private void clearAllSelections() {
        AffyTieredMap labelmap = ((AffyLabelledTierMap) seqmap).getLabelMap();
        labelmap.clearSelected();
        if (pixel_floater_glyph.getChildren() != null) {
            for (GlyphI floatGlyph : pixel_floater_glyph.getChildren()) {
                floatGlyph.setSelected(false);
            }
        }
        tierLabelManager.getVisibleTierGlyphs().stream().filter(tierGlyph -> tierGlyph.getTierType() == TierGlyph.TierType.GRAPH && tierGlyph.getChildCount() > 0).forEach(tierGlyph -> {
            for (GlyphI child : tierGlyph.getChildren()) {
                seqmap.deselect(child);
            }
        });
    }

    public void deselectAll() {
        clearAllSelections();
        gmodel.setSelectedSymmetries(Collections.<RootSeqSymmetry>emptyList(), Collections.<SeqSymmetry>emptyList(), this);
        seqmap.updateWidget();
    }

    /**
     * Recurse through glyphs and collect those that are instances of
     * GraphGlyph.
     */
    List<GraphGlyph> collectGraphs() {
        List<GraphGlyph> graphs = new ArrayList<>();
        GlyphI root = seqmap.getScene().getGlyph();
        collectGraphs(root, graphs);
        return graphs;
    }

    @Override
    public void addToPixelFloaterGlyph(GlyphI glyph) {
        FloaterGlyph floater = pixel_floater_glyph;
        Rectangle2D.Double cbox = getSeqMap().getCoordBounds();
        floater.setCoords(cbox.x, 0, cbox.width, 0);
        floater.addChild(glyph);
    }

    @Override
    public FloaterGlyph getFloaterGlyph() {
        return pixel_floater_glyph;
    }

    @Override
    public void groupSelectionChanged(GroupSelectionEvent evt) {
        AnnotatedSeqGroup current_group = null;
        AnnotatedSeqGroup new_group = evt.getSelectedGroup();
        if (aseq != null) {
            current_group = aseq.getSeqGroup();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("SeqMapView received seqGroupSelected() call: " + ((new_group != null) ? new_group.getID() : "null"));
        }

        if ((new_group != current_group) && (current_group != null)) {
            clear();
        }
    }

    @Override
    public void seqSelectionChanged(SeqSelectionEvent evt) {
        if (logger.isDebugEnabled()) {
            logger.debug("SeqMapView received SeqSelectionEvent, selected seq: " + evt.getSelectedSeq());
        }

        final BioSeq newseq = evt.getSelectedSeq();
        setAnnotatedSeq(newseq);
    }

    /**
     * Get the span of the symmetry that is on the seq being viewed.
     */
    @Override
    public SeqSpan getViewSeqSpan(SeqSymmetry sym) {
        return sym.getSpan(viewseq);
    }

    /**
     * Sets tool tip from given glyphs.
     *
     * @param glyphs
     */
    public void setToolTip(MouseEvent evt, GlyphI glyph) {
        if (!show_prop_tooltip) {
            return;
        }

        if (glyph != null && glyph.getInfo() instanceof SeqSymmetry) {
            setToolTip(evt, (SeqSymmetry) glyph.getInfo(), -1);
        } else {
            setToolTip(evt, null, -1);
        }
    }

    /**
     * Sets tool tip from graph glyph.
     *
     * @param glyph
     */
    public void setToolTip(MouseEvent evt, int x, GraphGlyph glyph) {
        if (!show_prop_tooltip) {
            return;
        }

        if (glyph != null && glyph.getInfo() instanceof SeqSymmetry
                && seqmap.getView().getTransform().getScaleX() > 0.2) {
            setToolTip(evt, (SeqSymmetry) glyph.getInfo(), x);
        } else {
            setToolTip(evt, null, x);
        }
    }

    private void setToolTip(MouseEvent evt, SeqSymmetry sym, int x) {

        // Nothing has changed return
        if (toolTipSym == null && sym == null) {
            return;
        }

        // Check if tooltip has changed from current tooltip.
        if ((toolTipSym == null && sym != null)
                || (toolTipSym != null && sym == null)
                || toolTipSym != sym
                || x != -1) {
            toolTipSym = sym;
            Map<String, Object> properties = null;
            if (toolTipSym != null && propertyHandler != null) {
                if (toolTipSym instanceof GraphSym) {
                    properties = propertyHandler.getGraphPropertiesRowColumn((GraphSym) toolTipSym, x, this);
                } else {
                    properties = propertyHandler.getPropertiesRow(toolTipSym, this);
                }
            }
            if (evt != null && properties != null) {
                Point point = new Point(evt.getXOnScreen() + ((AffyLabelledTierMap) seqmap).getLabelMap().getWidth(), evt.getYOnScreen());
                seqMapToolTips.setToolTip(point, properties, sym);
            } else {
                seqMapToolTips.setToolTip(null, properties, sym);
            }
        }
    }

    public void disableToolTip() {
        seqMapToolTips.setVisible(false);
    }

    public void showProperties(int x, GraphGlyph glyph) {
        List<GraphGlyph> glyphs = new ArrayList<>();
        glyphs.add(glyph);
        List<SeqSymmetry> sym = SeqMapView.glyphsToSyms(glyphs);

        if (!sym.isEmpty()) {
            if (propertyHandler != null) {
                propertyHandler.showGraphProperties((GraphSym) sym.get(0), x, this);
            }
        }
    }

    public void setShowPropertiesTooltip(boolean b) {
        show_prop_tooltip = b;
        if (!b) {
            setToolTip(null, null, -1);
        }
    }

    public boolean getShowPropertiesTooltip() {
        return show_prop_tooltip;
    }

    @Override
    public void addToRefreshList(SeqMapRefreshed smr) {
        seqmap_refresh_list.add(smr);
    }

    @Override
    public SeqSymmetry getSeqSymmetry() {
        return seq_selected_sym;
    }

    public GenericAction getRefreshDataAction() {
        return refreshDataAction;
    }

    @Override
    public void setPropertyHandler(PropertyHandler propertyHandler) {
        this.propertyHandler = propertyHandler;
    }

    public MapMode getMapMode() {
        return mapMode;
    }

    public void setMapMode(MapMode mapMode) {
        this.mapMode = mapMode;

        seqmap.setRubberBandBehavior(mapMode.rubber_band);
        seqmap.enableCanvasDragging(mapMode.drag_scroll);
        seqmap.enableDragScrolling(!mapMode.drag_scroll);
        seqmap.setCursor(mapMode.defCursor);
    }

    public void saveSession() {
        PreferenceUtils.getSessionPrefsNode().put(SEQ_MODE, getMapMode().name());
    }

    public void loadSession() {
        String mapMode = PreferenceUtils.getSessionPrefsNode().get(SEQ_MODE, SeqMapView.MapMode.MapSelectMode.name());
        if (MapMode.MapScrollMode.name().equals(mapMode)) {
            scroll_mode_button.doClick();
        }
        if (MapMode.MapSelectMode.name().equals(mapMode)) {
            select_mode_button.doClick();
        }
    }

    @Override
    public void setRegion(int start, int end, BioSeq seq) {
        if (start >= 0 && end > 0 && end != Integer.MAX_VALUE) {
            final SeqSpan view_span = new SimpleSeqSpan(start, end, seq);
            zoomTo(view_span);
            final double middle = (start + end) / 2.0;
            setZoomSpotX(middle);
            if (autoload != null) {
                autoload.loadData();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, Object>> getProperties() {
        List<Map<String, Object>> propList = new ArrayList<>();
        List<SeqSymmetry> selected_syms = getSelectedSyms();
        for (GlyphI glyph : getSeqMap().getSelected()) {

            if (glyph.getInfo() instanceof SeqSymmetry
                    && selected_syms.contains(glyph.getInfo())) {
                continue;
            }

            Map<String, Object> props = null;
            if (glyph.getInfo() instanceof Map) {
                props = (Map<String, Object>) glyph.getInfo();
            } else {
                props = new HashMap<>();
            }

            boolean direction = true;
            if (props.containsKey(DIRECTION)) {
                if (props.get(DIRECTION).equals(REVERSE_DIRECTION)) {
                    direction = false;
                }
            }

            Rectangle2D.Double boundary = glyph.getSelectedRegion();
            int start = (int) boundary.getX();
            int length = (int) boundary.getWidth();
            int end = start + length;
            if (!direction) {
                int temp = start;
                start = end;
                end = temp;
            }
            props.put(START, start);
            props.put(END, end);
            props.put(LENGTH, length);

            propList.add(props);
        }
        propList.addAll(getTierManager().getProperties());
        return propList;
    }

    @Override
    public Map<String, Object> determineProps(SeqSymmetry sym) {
        Map<String, Object> props = new HashMap<>();
        if (sym == null) {
            return props;
        }
        Map<String, Object> tierprops = getTierManager().determineProps(sym);
        if (tierprops != null) {
            props.putAll(tierprops);
        }
        SeqSpan span = getViewSeqSpan(sym);
        if (span != null) {
            String chromID = span.getBioSeq().getID();
            props.put(CHROMOSOME, chromID);
            props.put(START,
                    NumberFormat.getIntegerInstance().format(span.getStart()));
            props.put(END,
                    NumberFormat.getIntegerInstance().format(span.getEnd()));
            props.put(LENGTH,
                    NumberFormat.getIntegerInstance().format(span.getLength()));
            props.put(STRAND,
                    span.isForward() ? "+" : "-");
            props.remove(SEQ_ID); // this is redundant if "chromosome" property is set
            if (props.containsKey(METHOD)) {
                props.remove(METHOD);
            }
            if (props.containsKey(TYPE)) {
                props.remove(TYPE);
            }
        }
        if (sym instanceof CdsSeqSymmetry) {
            sym = ((CdsSeqSymmetry) sym).getPropertySymmetry();
        }
        if (sym instanceof SupportsCdsSpan) {
            span = ((SupportsCdsSpan) sym).getCdsSpan();
            if (span != null) {
                props.put(CDS_START,
                        NumberFormat.getIntegerInstance().format(span.getStart()));
                props.put(CDS_END,
                        NumberFormat.getIntegerInstance().format(span.getEnd()));

            }
        }
        return props;
    }

    public MapRangeBox getMapRangeBox() {
        return map_range_box;
    }

    public com.affymetrix.igb.swing.JRPButton getPartial_residuesButton() {
        return partial_residuesB;
    }

    public SeqMapViewPopup getPopup() {
        return popup;
    }

    public AutoScroll getAutoScroll() {
        return autoScroll;
    }

    public AutoLoadThresholdHandler getAutoLoadAction() {
        return autoload;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean consecutiveOK() {
        return true;
    }

    /**
     * Update the widget in this panel. Putting this awkward idiom here to try
     * to contain its spread. It is here for backward compatability.
     */
    @Override
    public void updatePanel(boolean preserveViewX, boolean preserveViewY) {
        this.setAnnotatedSeq(this.getAnnotatedSeq(), true, preserveViewX, preserveViewY);
    }

    /**
     * Update the widget in this panel.
     */
    @Override
    public void updatePanel() {
        this.updatePanel(true, true);
    }

    @Override
    public void repackTheTiers(boolean full_repack, boolean stretch_vertically) {
        seqmap.repackTheTiers(full_repack, stretch_vertically);
    }

    @Override
    public void split(GlyphI glyph) {
        if (!(glyph instanceof GraphGlyph)) {
            return;
        }
        GlyphI joinedParent = glyph.getParent();
        if (glyph.getParent() != null && glyph.getParent().getChildCount() == 2) {
            List<GraphGlyph> graphGlyphs = new ArrayList<>(2);
            for (int i = 0; i < glyph.getParent().getChildCount(); i++) {
                if (glyph.getParent().getChild(i) instanceof GraphGlyph) {
                    splitGraph((GraphGlyph) glyph.getParent().getChild(i));
                    graphGlyphs.add((GraphGlyph) glyph.getParent().getChild(i));
                }
            }
            for (GraphGlyph graphGlyph : graphGlyphs) {
                joinedParent.removeChild(graphGlyph);
                TierGlyph tier = GraphGlyphFactory.addGraphGlyphToTier(graphGlyph, graphGlyph.getAnnotStyle(), this, aseq);
                tier.pack(getSeqMap().getView());
            }
        } else {
            splitGraph((GraphGlyph) glyph);
            joinedParent.removeChild(glyph);
        }
        joinedParent.pack(getSeqMap().getView());
    }

    public SymSelectionListener getSymSelectionListener() {
        return symSelectionListener;
    }

    public boolean isShowEdgeMatches() {
        return showEdgeMatches;
    }

    public JComponent getXzoombox() {
        return xzoombox;
    }

    public JComponent getYzoombox() {
        return yzoombox;
    }

    @Override
    public List<GlyphI> searchForRegexInResidues(boolean forward, Pattern regex, String residues, int residue_offset, Color hitColor) {
        final List<GlyphI> resultGlyphs = new ArrayList<>();
        List<SingletonSymWithProps> results = BioSeqUtils.searchForRegexInResidues(forward, regex, residues, residue_offset, getAnnotatedSeq());
        for (SingletonSymWithProps result : results) {
            GlyphI gl = new FillRectGlyph() {
                @Override
                public void moveAbsolute(double x, double y) {
                }

                @Override
                public void moveRelative(double diffx, double diffy) {
                }
            };
            gl.setInfo(result);
            gl.setColor(hitColor);
            double pos = forward ? 27 : 32;
            gl.setCoords(result.getMin(), pos, result.getLength(), 10);
            resultGlyphs.add(gl);
        }
        ThreadUtils.runOnEventQueue(() -> {
            resultGlyphs.forEach(axis_tier::addChild);
        });
        return resultGlyphs;
    }

    @Override
    public void setBackGroundProvider(ViewI.BackGroundProvider bgp, ViewI.BackGroundProvider labelbgp) {
        seqmap.getView().setBackGroundProvider(bgp);
        ((AffyLabelledTierMap) seqmap).getLabelMap().getView().setBackGroundProvider(labelbgp);
        seqmap.updateWidget();
    }

    public static enum MapMode {

        MapSelectMode(true, false, defaultCursor, defaultCursor),
        MapScrollMode(false, true, openHandCursor, closedHandCursor),
        MapZoomMode(false, false, defaultCursor, defaultCursor);
        public boolean rubber_band;
        public boolean drag_scroll;
        public Cursor defCursor;
        public Cursor pressedCursor;

        private MapMode(boolean rubber_band, boolean drag_scroll, Cursor defaultCursor, Cursor pressedCursor) {
            this.rubber_band = rubber_band;
            this.drag_scroll = drag_scroll;
            this.defCursor = defaultCursor;
            this.pressedCursor = pressedCursor;
        }
    }

    public class SeqMapViewComponentListener extends ComponentAdapter {
        // update graphs and annotations when the map is resized.

        @Override
        public void componentResized(ComponentEvent e) {
            SwingUtilities.invokeLater(() -> {
                List<GraphGlyph> graphs = collectGraphs();
                graphs.stream().filter(graph -> graph.getAnnotStyle().isFloatTier()).forEach(graph -> {
                    getFloaterGlyph().checkBounds(graph, getSeqMap().getView());
                });
                getSeqMap().stretchToFit(false, false);
                getSeqMap().updateWidget();

            });
        }

    }

    private class SeqMapViewRubberBand extends RubberBand {

        SeqMapViewRubberBand(Component c) {
            super(c);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (axis_tier != null && !axis_tier.inside(e.getX(), e.getY())) {
                heardEvent(e);
            }
        }
    }

}
