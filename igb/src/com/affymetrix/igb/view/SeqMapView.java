/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.DerivedSeqSymmetry;
import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.MutableSeqSpan;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.glyph.AxisGlyph;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.genoviz.glyph.OutlineRectGlyph;
import com.affymetrix.genoviz.glyph.RootGlyph;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.genoviz.widget.Shadow;
import com.affymetrix.genoviz.awt.AdjustableJSlider;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.SceneI;
import com.affymetrix.genoviz.util.Timer;
import com.affymetrix.genoviz.bioviews.PackerI;

import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.LeafSingletonSymmetry;
import com.affymetrix.genometryImpl.symmetry.MutableSingletonSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimplePairSeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;

import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.comparator.SeqSymStartComparator;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.ScoredContainerSym;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.TypeContainerAnnot;
import com.affymetrix.genometryImpl.parsers.CytobandParser;
import com.affymetrix.genometryImpl.event.*;
import com.affymetrix.genometryImpl.style.IAnnotStyle;
import com.affymetrix.genometryImpl.style.IAnnotStyleExtended;
import com.affymetrix.genometryImpl.util.SearchableCharIterator;
import com.affymetrix.genometryImpl.util.SynonymLookup;


import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.das2.Das2FeatureRequestSym;
import com.affymetrix.igb.tiers.*;
import com.affymetrix.igb.glyph.*;
import com.affymetrix.igb.menuitem.MenuUtil;
import com.affymetrix.igb.stylesheet.InvisibleBoxGlyph;
import com.affymetrix.igb.stylesheet.XmlStylesheetGlyphFactory;
import com.affymetrix.igb.stylesheet.XmlStylesheetParser;
import com.affymetrix.igb.util.GraphGlyphUtils;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.igb.util.WebBrowserControl;
import com.affymetrix.igb.view.load.GeneralLoadUtils;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.prefs.*;
import java.util.regex.Pattern;
import javax.swing.*;

/**
 *
 * @version $Id$
 */
public class SeqMapView extends JPanel
				implements AnnotatedSeqViewer, SymSelectionSource,
				SymSelectionListener, SeqSelectionListener, GroupSelectionListener {

	private static final boolean DIAGNOSTICS = false;
	private static final boolean DEBUG_TIERS = false;
	public static boolean DEBUG_COMP = false;
	private static final boolean DEBUG_STYLESHEETS = false;
	/** Add spans to the transformation sym that will cause all
	 * "intron" spans in the regions of aseq between exons chosen for slicing
	 * to be transformed into zero-length spans.
	 * This allows glyph factories to find "deleted" exons
	 * and draw them (if desired) without requiring messy calculations in
	 * the glyph factories.
	 */
	private static final boolean ADD_INTRON_TRANSFORMS = true;
	/**
	 * Extends the action of ADD_INTRON_TRANSFORMS to add an extra transform for
	 * the "intron" that extends from the start of the sequence to the first
	 * selected exon and from the end of the last selected exon to the end of
	 * the sequence.  It is probably NOT very efficient to do this.  It seems
	 * to work better to let the glyph factories figure out this information in
	 * other ways.
	 */
	private static final boolean ADD_EDGE_INTRON_TRANSFORMS = false;
	protected boolean view_cytobands_in_axis = true;
	public static final Pattern CYTOBAND_TIER_REGEX = Pattern.compile(".*" + CytobandParser.CYTOBAND_TIER_NAME);
	//  public boolean LABEL_TIERMAP = true;
	//  boolean SPLIT_WINDOWS = false;  // flag for testing transcriptarium split windows strategy
	boolean SUBSELECT_SEQUENCE = true;  // try to visually select range along seq glyph based on rubberbanding
	boolean show_edge_matches = true;
	//boolean rev_comp = false;
	boolean coord_shift = false;
	boolean show_slicendice = false;
	boolean slicing_in_effect = false;
	boolean hairline_is_labeled = true;
	SeqSpan viewspan_before_slicing = null;
	List<SymSelectionListener> selection_listeners = new ArrayList<SymSelectionListener>();
	List<ContextualPopupListener> popup_listeners = new ArrayList<ContextualPopupListener>();
	protected XmlStylesheetGlyphFactory default_glyph_factory = new XmlStylesheetGlyphFactory();
	/**
	 *  number of bases that slicer tries to buffer on each side of every span it is using to guide slicing
	 */
	int slice_buffer = 100;
	/**
	 *  maximum number of query glyphs for edge matcher.
	 *  any more than this and won't attempt to edge match
	 *  (edge matching is currently very inefficient with large numbers of glyphs --
	 *   something like O(N * M), where N is number of query glyphs and
	 *   M is total number of glyphs to try and match against query glyphs
	 *   [or possibly O(N^2 * M) ???] )
	 */
	int max_for_matching = 500;
	/**
	 *  current symmetry used to determine slicing
	 */
	SeqSymmetry slice_symmetry;
	/** boolean for setting map range to min and max bounds of
	AnnotatedBioSeq's annotations */
	private boolean SHRINK_WRAP_MAP_BOUNDS = false;
	
	protected boolean INTERNAL_XSCROLLER = true;
	protected boolean INTERNAL_YSCROLLER = true;

	JFrame frm;
	protected AffyTieredMap seqmap;
	UnibrowHairline hairline = null;
	BioSeq aseq;
	/**
	 *  a virtual sequence that maps the MutableAnnotatedBioSeq aseq to the map coordinates.
	 *  if the mapping is identity, then:
	 *     vseq == aseq OR
	 *     vseq.getComposition().getSpan(aseq) = SeqSpan(0, aseq.getLength(), aseq)
	 *  if the mapping is reverse complement, then:
	 *     vseq.getComposition().getSpan(aseq) = SeqSpan(aseq.getLength(), 0, aseq);
	 *
	 */
	BioSeq viewseq;
	// mapping of annotated seq to virtual "view" seq
	MutableSeqSymmetry seq2viewSym;
	SeqSymmetry[] transform_path;
	private static final String PREF_AXIS_LABEL_FORMAT = "Axis label format";
	/** One of the acceptable values of {@link #PREF_AXIS_LABEL_FORMAT}. */
	public static final String VALUE_AXIS_LABEL_FORMAT_COMMA = "COMMA";
	/** One of the acceptable values of {@link #PREF_AXIS_LABEL_FORMAT}. */
	public static final String VALUE_AXIS_LABEL_FORMAT_FULL = "FULL";
	/** One of the acceptable values of {@link #PREF_AXIS_LABEL_FORMAT}. */
	public static final String VALUE_AXIS_LABEL_FORMAT_ABBREV = "ABBREV";
	/** One of the acceptable values of {@link #PREF_AXIS_LABEL_FORMAT}. */
	public static final String VALUE_AXIS_LABEL_FORMAT_NO_LABELS = "NO_LABELS";
	public static final String PREF_AXIS_COLOR = "Axis color";
	public static final String PREF_AXIS_BACKGROUND = "Axis background";
	public static final String PREF_AXIS_NAME = "Axis name";
	public static final String PREF_DEFAULT_ANNOT_COLOR = "Default annotation color";
	public static final String PREF_DEFAULT_BACKGROUND_COLOR = "Default background color";
	public static final String PREF_EDGE_MATCH_COLOR = "Edge match color";
	public static final String PREF_EDGE_MATCH_FUZZY_COLOR = "Edge match fuzzy color";
	/** Name of a boolean preference for whether the hairline lable should be on. */
	public static final String PREF_HAIRLINE_LABELED = "Hairline Label On";
	/** Name of a boolean preference for whether the horizontal zoom slider is above the map. */
	private static final String PREF_X_ZOOMER_ABOVE = "Horizontal Zoomer Above Map";
	/** Name of a boolean preference for whether the vertical zoom slider is left of the map. */
	private static final String PREF_Y_ZOOMER_LEFT = "Vertical Zoomer Left of Map";
	public static final Color default_axis_color = Color.BLACK;
	public static final Color default_axis_background = Color.WHITE;
	public static final String default_axis_label_format = VALUE_AXIS_LABEL_FORMAT_COMMA;
	//public static final Color default_default_annot_color = new Color(192, 192, 114);
	//public static final Color default_default_background_color = Color.BLACK;
	public static final Color default_edge_match_color = Color.WHITE;
	public static final Color default_edge_match_fuzzy_color = new Color(200, 200, 200); // light gray
	public static final boolean default_x_zoomer_above = true;
	public static final boolean default_y_zoomer_left = true;
	static NumberFormat nformat = NumberFormat.getIntegerInstance();
	/** Hash of method names (lower case) to forward tiers */
	Map<String, TierGlyph> method2ftier = new HashMap<String, TierGlyph>();
	/** Hash of method names (lower case) to reverse tiers */
	Map<String, TierGlyph> method2rtier = new HashMap<String, TierGlyph>();
	/** Hash of GraphStates to TierGlyphs. */
	Map<IAnnotStyle, TierGlyph> gstyle2tier = new HashMap<IAnnotStyle, TierGlyph>();
	//Map gstyle2floatTier = new HashMap();
	PixelFloaterGlyph pixel_floater_glyph = new PixelFloaterGlyph();
	GlyphEdgeMatcher edge_matcher = null;
	JPopupMenu sym_popup;
	JMenu sym_menu;
	JLabel sym_info;
	// A fake menu item, prevents null pointer exceptions in actionPerformed()
	// for menu items whose real definitions are commented-out in the code
	private static final JMenuItem empty_menu_item = new JMenuItem("");
	JMenuItem zoomtoMI = empty_menu_item;
	JMenuItem centerMI = empty_menu_item;
	JMenuItem selectParentMI = empty_menu_item;
	JMenuItem slicendiceMI = empty_menu_item;
	// for right-click on background
	protected SeqMapViewActionListener action_listener;
	protected SeqMapViewMouseListener mouse_listener;
	CharSeqGlyph seq_glyph = null;
	SeqSymmetry seq_selected_sym = null;  // symmetry representing selected region of sequence
	Vector<GlyphI> match_glyphs = new Vector<GlyphI>();
	protected TierLabelManager tier_manager;
	PixelFloaterGlyph grid_layer = null;
	GridGlyph grid_glyph = null;
	protected JComponent xzoombox;
	protected JComponent yzoombox;
	protected MapRangeBox map_range_box;
	//JButton refreshB = new JButton("Refresh Data");
	SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
	final static Font SMALL_FONT = new Font("SansSerif", Font.PLAIN, 10);
	public static Font axisFont = new Font("Courier", Font.BOLD, 12);
	boolean report_hairline_position_in_status_bar = false;
	boolean report_status_in_status_bar = true;
	protected SeqSymmetry sym_used_for_title = null;

	/*
	 *  units to scroll are either in pixels or bases
	 */
	ActionListener map_auto_scroller = null;
	javax.swing.Timer swing_timer = null;
	int as_bases_per_pix = 75;
	int as_pix_to_scroll = 4;
	int as_time_interval = 20;
	int as_start_pos = 0;
	int as_end_pos;
	int modcount = 0;
	private final int xoffset_pop = 10;
	private final int yoffset_pop = 0;
	JMenu navigation_menu = null;
	Thread slice_thread = null;
	/** Whether the Application name goes first in the title bar.
	 */
	protected boolean appNameFirstInTitle = false;

	/** Constructor provided for subclasses.
	 *  In other cases, use {@link #makeSeqMapView}.
	 */
	protected SeqMapView() {
		super();
	}

	public final class SeqMapViewComponentListener extends ComponentAdapter {
		// update graphs and annotations when the map is resized.

		@Override
		public void componentResized(ComponentEvent e) {
			SwingUtilities.invokeLater(new Runnable() {

				public void run() {
					List graphs = collectGraphs();
					for (int i = 0; i < graphs.size(); i++) {
						GraphGlyphUtils.checkPixelBounds((GraphGlyph) graphs.get(i), getSeqMap());
					}
					getSeqMap().stretchToFit(false, true);
					getSeqMap().updateWidget();

				}
			});
		}
	};

	/**
	 * Creates an instance.
	 * @param add_popups  Whether to add some popup menus to the tier label manager
	 *  that control tier hiding and collapsing and so forth.  It is probably best
	 *  NOT to set this to true in any view other than the main view; it should
	 *  be false in the AltSpliceView, for instance.
	 */
	public static SeqMapView makeSeqMapView(boolean add_popups, boolean split_win) {
		SeqMapView smv = new SeqMapView();
		smv.init(add_popups, split_win, true);
		return smv;
	}

	/** Creates an instance to be used as the SeqMap.  Set-up of listeners and such
	 *  will be done in init()
	 */
	protected AffyTieredMap createSeqMap(boolean splitWindows, boolean labelTiermap,
					boolean internalXScroller, boolean internalYScroller) {
		AffyTieredMap resultSeqMap;
		if (splitWindows) {
			resultSeqMap = new MultiWindowTierMap(false, false);
		} else if (labelTiermap) {
			resultSeqMap = new AffyLabelledTierMap(internalXScroller, internalYScroller);
			NeoMap label_map = ((AffyLabelledTierMap) resultSeqMap).getLabelMap();
			label_map.setSelectionAppearance(SceneI.SELECT_OUTLINE);
			label_map.setReshapeBehavior(NeoAbstractWidget.Y, NeoConstants.NONE);
		} else {
			resultSeqMap = new AffyTieredMap(internalXScroller, internalYScroller);
		}
		return resultSeqMap;
	}

	protected void init(boolean add_popups, boolean split_win, boolean label_tiermap) {

		if (split_win) {
			label_tiermap = false;
		}

		seqmap = createSeqMap(split_win, label_tiermap, INTERNAL_XSCROLLER, INTERNAL_YSCROLLER);

		seqmap.setReshapeBehavior(NeoAbstractWidget.X, NeoConstants.NONE);
		seqmap.setReshapeBehavior(NeoAbstractWidget.Y, NeoConstants.NONE);

		seqmap.addComponentListener(new SeqMapViewComponentListener());

		// the MapColor MUST be a very dark color or else the hairline (which is
		// drawn with XOR) will not be visible!
		seqmap.setMapColor(Color.BLACK);

		edge_matcher = GlyphEdgeMatcher.getSingleton();


		action_listener = new SeqMapViewActionListener(this);
		mouse_listener = new SeqMapViewMouseListener(this);

		//    map.setScrollingOptimized(true);
		seqmap.getNeoCanvas().setDoubleBuffered(false);
		//    map.getLabelMap().getNeoCanvas().setDoubleBuffered(false);

		seqmap.setScrollIncrementBehavior(AffyTieredMap.X, AffyTieredMap.AUTO_SCROLL_HALF_PAGE);

		Adjustable xzoomer;
		Adjustable yzoomer;

		xzoomer = new AdjustableJSlider(Adjustable.HORIZONTAL);
		((JSlider)xzoomer).setToolTipText("Horizontal zoom");
		yzoomer = new AdjustableJSlider(Adjustable.VERTICAL);
		((JSlider)yzoomer).setToolTipText("Vertical zoom");

		seqmap.setZoomer(NeoMap.X, xzoomer);
		seqmap.setZoomer(NeoMap.Y, yzoomer);

		if (label_tiermap) {
			tier_manager = new TierLabelManager((AffyLabelledTierMap) seqmap);
			tier_manager.setDoGraphSelections(true);
			if (add_popups) {
				//NOTE: popup listeners are called in reverse of the order that they are added
				// Must use separate instances of GraphSelectioManager if we want to use
				// one as a ContextualPopupListener AND one as a TierLabelHandler.PopupListener
				//tier_manager.addPopupListener(new GraphSelectionManager(this));
				tier_manager.addPopupListener(new TierArithmetic(tier_manager, this));
				//TODO: tier_manager.addPopupListener(new CurationPopup(tier_manager, this));
				tier_manager.addPopupListener(new SeqMapViewPopup(tier_manager, this));
			}
		}

		seqmap.setSelectionAppearance(SceneI.SELECT_OUTLINE);
		seqmap.addMouseListener(mouse_listener);

		if (label_tiermap) {
			tier_manager.setDoGraphSelections(true);
		}
		// A "Smart" rubber band is necessary becaus we don't want our attempts
		// to drag the graph handles to also cause rubber-banding
		SmartRubberBand srb = new SmartRubberBand(seqmap);
		seqmap.setRubberBand(srb);
		seqmap.addRubberBandListener(mouse_listener);
		srb.setColor(new Color(100, 100, 255));

		GraphSelectionManager graph_manager = new GraphSelectionManager(this);
		seqmap.addMouseListener(graph_manager);
		this.addPopupListener(graph_manager);

		setupPopups();
		this.setLayout(new BorderLayout());

		map_range_box = new MapRangeBox(this);
		xzoombox = Box.createHorizontalBox();
		xzoombox.add(map_range_box.range_box);

		xzoombox.add(Box.createRigidArea(new Dimension(6, 0)));
		xzoombox.add((Component) xzoomer);

		boolean x_above = UnibrowPrefsUtil.getBooleanParam(PREF_X_ZOOMER_ABOVE, default_x_zoomer_above);
		if (x_above) {
			JPanel pan = new JPanel(new BorderLayout());
			pan.add("Center", xzoombox);
			this.add(BorderLayout.NORTH, pan);
		} else {
			JPanel pan = new JPanel(new BorderLayout());
			pan.add("Center", xzoombox);
			this.add(BorderLayout.SOUTH, pan);
		}

		yzoombox = Box.createVerticalBox();
		yzoombox.add((Component) yzoomer);
		boolean y_left = UnibrowPrefsUtil.getBooleanParam(PREF_Y_ZOOMER_LEFT, default_y_zoomer_left);
		if (y_left) {
			this.add(BorderLayout.WEST, yzoombox);
		} else {
			this.add(BorderLayout.EAST, yzoombox);
		}

		// experimenting with transcriptarium split windows
		if (split_win) {
			// don't display map if split_win, display is via multiple separate windows controlled by resultSeqMap

			//  switched to using JScrollBar,
			//  previous problems with Swing JScrollBar (and I think AWT Scrollbar as well)
			//      seem to have been resolved as of JDK 1.5
			JScrollBar xscroller = new JScrollBar(JScrollBar.HORIZONTAL);
			JScrollBar yscroller = new JScrollBar(JScrollBar.VERTICAL);

			seqmap.setRangeScroller(xscroller);
			seqmap.setOffsetScroller(yscroller);
			JPanel scrollP = new JPanel();
			scrollP.setLayout(new BorderLayout());
			scrollP.add(BorderLayout.SOUTH, xscroller);
			scrollP.add(BorderLayout.WEST, yscroller);
			this.add(BorderLayout.CENTER, scrollP);
		} else {
			this.add(BorderLayout.CENTER, seqmap);
		}
		LinkControl link_control = new LinkControl();
		this.addPopupListener(link_control);

		default_glyph_factory.setStylesheet(XmlStylesheetParser.getUserStylesheet());

		UnibrowPrefsUtil.getTopNode().addPreferenceChangeListener(pref_change_listener);
	}
	// This preference change listener can reset some things, like whether
	// the axis uses comma format or not, in response to changes in the stored
	// preferences.  Changes to axis, and other tier, colors are not so simple,
	// in part because of the need to coordinate with the label glyphs.
	PreferenceChangeListener pref_change_listener = new PreferenceChangeListener() {

		public void preferenceChange(PreferenceChangeEvent pce) {
			if (getAxisTier() == null) {
				return;
			}

			if (!pce.getNode().equals(UnibrowPrefsUtil.getTopNode())) {
				return;
			}

			TransformTierGlyph axis_tier = getAxisTier();
			
			if (pce.getKey().equals(PREF_AXIS_LABEL_FORMAT)) {
				AxisGlyph ag = null;
				for (GlyphI child : axis_tier.getChildren()) {
					if (child instanceof AxisGlyph) {
						ag = (AxisGlyph) child;
					}
				}
				if (ag != null) {
					setAxisFormatFromPrefs(ag);
				}
				seqmap.updateWidget();
			} else if (pce.getKey().equals(PREF_EDGE_MATCH_COLOR) || pce.getKey().equals(PREF_EDGE_MATCH_FUZZY_COLOR)) {
				if (show_edge_matches) {
					doEdgeMatching(seqmap.getSelected(), true);
				}
			} else if (pce.getKey().equals(PREF_X_ZOOMER_ABOVE)) {
				boolean b = UnibrowPrefsUtil.getBooleanParam(PREF_X_ZOOMER_ABOVE, default_x_zoomer_above);
				SeqMapView.this.remove(xzoombox);
				if (b) {
					SeqMapView.this.add(BorderLayout.NORTH, xzoombox);
				} else {
					SeqMapView.this.add(BorderLayout.SOUTH, xzoombox);
				}
				SeqMapView.this.invalidate();
			} else if (pce.getKey().equals(PREF_Y_ZOOMER_LEFT)) {
				boolean b = UnibrowPrefsUtil.getBooleanParam(PREF_Y_ZOOMER_LEFT, default_y_zoomer_left);
				SeqMapView.this.remove(yzoombox);
				if (b) {
					SeqMapView.this.add(BorderLayout.WEST, yzoombox);
				} else {
					SeqMapView.this.add(BorderLayout.EAST, yzoombox);
				}
				SeqMapView.this.invalidate();
			}
		}
	};

	public void setFrame(JFrame frm) {
		this.frm = frm;
	}

	public TierLabelManager getTierManager() {
		return tier_manager;
	}

	public JFrame getFrame() {
		return frm;
	}

	public void setupPopups() {
		sym_popup = new JPopupMenu();
		sym_info = new JLabel("");
		sym_info.setEnabled(false); // makes the text look different (usually lighter)

		centerMI = setUpMenuItem(sym_popup, "Center at selected");

		zoomtoMI = setUpMenuItem(sym_popup, "Zoom to selected");
		zoomtoMI.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/Zoom16.gif"));

		selectParentMI = setUpMenuItem(sym_popup, "Select parent");
		//printSymmetryMI = setUpMenuItem(sym_popup, "Print symmetry");
		
		if (show_slicendice) {
			slicendiceMI = setUpMenuItem(sym_popup, "Slice and dice");
		}
	}

	public JPopupMenu getSelectionPopup() {
		return sym_popup;
	}
	TransformTierGlyph axis_tier;

	private IAnnotStyleExtended getAxisAnnotStyle() {
		return axis_annot_style;
	}
	/** An un-collapsible instance.  It is hideable, though. */
	static AnnotStyle axis_annot_style = new AnnotStyle() {

		{ // a non-static initializer block
			setHumanName("Coordinates");
		}

		@Override
		public boolean getSeparate() {
			return false;
		}

		@Override
		public boolean getCollapsed() {
			return false;
		}

		@Override
		public boolean getExpandable() {
			return false;
		}

		@Override
		public void setColor(Color c) {
			UnibrowPrefsUtil.putColor(UnibrowPrefsUtil.getTopNode(), PREF_AXIS_COLOR, c);
		}

		@Override
		public Color getColor() {
			return UnibrowPrefsUtil.getColor(UnibrowPrefsUtil.getTopNode(), PREF_AXIS_COLOR, default_axis_color);
		}

		@Override
		public void setBackground(Color c) {
			UnibrowPrefsUtil.putColor(UnibrowPrefsUtil.getTopNode(), PREF_AXIS_BACKGROUND, c);
		}

		@Override
		public Color getBackground() {
			return UnibrowPrefsUtil.getColor(UnibrowPrefsUtil.getTopNode(), PREF_AXIS_BACKGROUND, default_axis_background);
		}

		@Override
		public void setHumanName(String s) {
			//UnibrowPrefsUtil.getTopNode().put(PREF_AXIS_NAME, s);
			super.setHumanName(s);
		}

		@Override
		public String getHumanName() {
			//return UnibrowPrefsUtil.getTopNode().get(PREF_AXIS_NAME, "Coordinates");
			return super.getHumanName();
		}

		@Override
		public void setShow(boolean b) {
			super.setShow(b);
		}
	};

	public TransformTierGlyph getAxisTier() {
		return axis_tier;
	}

	/** Set up a tier with fixed pixel height and place axis in it. */
	private TransformTierGlyph addAxisTier(int tier_index) {

		axis_tier = new TransformTierGlyph(getAxisAnnotStyle());
		axis_tier.setFixedPixelHeight(true);
		axis_tier.setFixedPixHeight(45);
		axis_tier.setDirection(TierGlyph.DIRECTION_AXIS);
		//    axis_tier.setFixedPixelHeight(false);
		AxisGlyph axis = seqmap.addAxis(0);
		axis.setHitable(false);
		axis.setFont(axisFont);

		Color axis_bg = getAxisAnnotStyle().getBackground();
		Color axis_fg = getAxisAnnotStyle().getColor();

		axis.setBackgroundColor(axis_bg);
		axis_tier.setBackgroundColor(axis_bg);
		axis_tier.setFillColor(axis_bg);
		axis.setForegroundColor(axis_fg);
		axis_tier.setForegroundColor(axis_fg);
		setAxisFormatFromPrefs(axis);

		if (view_cytobands_in_axis) {
			GlyphI cytoband_glyph = makeCytobandGlyph();
			if (cytoband_glyph != null) {
				axis_tier.addChild(cytoband_glyph);
				axis_tier.setFixedPixHeight(axis_tier.getFixedPixHeight() + (int) cytoband_glyph.getCoordBox().height);
			}
		}

		axis_tier.addChild(axis);

		// it is important to set the colors before adding the tier
		// to the map, else the label tier colors won't match
		if (seqmap.getTiers().size() >= tier_index) {
			seqmap.addTier(axis_tier, tier_index);
		} else {
			seqmap.addTier(axis_tier);
		}
		seq_glyph = new CharSeqGlyph();
		seq_glyph.setForegroundColor(axis_fg);
		seq_glyph.setShowBackground(false);
		seq_glyph.setHitable(false);
		seq_glyph.setDrawOrder(Glyph.DRAW_CHILDREN_FIRST);

		BioSeq compseq = viewseq;
		seq_glyph.setCoords(compseq.getMin(), 0, compseq.getLengthDouble(), 10);

		axis_tier.addChild(seq_glyph);

		// need to change this to get residues from viewseq! (to take account of reverse complement,
		//    coord shift, slice'n'dice, etc.
		// but first, need to fix BioSeq.isComplete() implementations...
		// currently only GeneralBioSeq implements CharacterIterator
		seq_glyph.setResiduesProvider(viewseq, viewseq.getLength());

		SeqSymmetry compsym = viewseq.getComposition();
		if (compsym != null) {
			int compcount = compsym.getChildCount();
			// create a color, c3, in between the foreground and background colors
			Color c1 = axis.getForegroundColor();
			Color c2 = axis.getBackgroundColor();
			Color c3 = new Color(
							(c1.getRed() + 2 * c2.getRed()) / 3,
							(c1.getGreen() + 2 * c2.getGreen()) / 3,
							(c1.getBlue() + 2 * c2.getBlue()) / 3);

			for (int i = 0; i < compcount; i++) {
				// Make glyphs for contigs
				SeqSymmetry childsym = compsym.getChild(i);
				SeqSpan childspan = childsym.getSpan(viewseq);
				SeqSpan ospan = SeqUtils.getOtherSpan(childsym, childspan);

				GlyphI cgl;
				if (ospan.getBioSeq().isComplete(ospan.getMin(), ospan.getMax())) {
					cgl = new FillRectGlyph();
					cgl.setColor(c3);
				} else {
					if (viewseq.getID().equals(IGBConstants.GENOME_SEQ_ID)) {
						// hide axis numbering
						axis.setLabelFormat(AxisGlyph.NO_LABELS);
						cgl = new com.affymetrix.igb.glyph.LabelledRectGlyph();
						String label = ospan.getBioSeq().getID();
						if (label.toLowerCase().startsWith("chr")) {
							label = label.substring(3);
						}
						((com.affymetrix.igb.glyph.LabelledRectGlyph) cgl).setLabel(label);
						cgl.setColor(axis.getForegroundColor());
					} else if (viewseq.getID().equals(IGBConstants.ENCODE_REGIONS_ID)) {
						cgl = new com.affymetrix.igb.glyph.LabelledRectGlyph();
						String label = childsym.getID();
						if (label != null) {
							((com.affymetrix.igb.glyph.LabelledRectGlyph) cgl).setLabel(label);
						}
						cgl.setColor(axis.getForegroundColor());
					} else {
						cgl = new OutlineRectGlyph();
						cgl.setColor(axis.getForegroundColor());
					}
				}

				cgl.setCoords(childspan.getMinDouble(), 0, childspan.getLengthDouble(), 10);

				// also note that "Load residues in view" produces additional
				// contig-like glyphs that can partially hide these glyphs.
				seq_glyph.addChild(cgl);
			}
		}

		return axis_tier;
	}

	public EfficientSolidGlyph makeCytobandGlyph() {
		BioSeq sma = (BioSeq) getAnnotatedSeq();
		//      SymWithProps cyto_annots = sma.getAnnotation(CytobandParser.CYTOBAND_TIER_NAME);
		SymWithProps cyto_annots = null;
		List cyto_tiers = sma.getAnnotations(CYTOBAND_TIER_REGEX);
		if (cyto_tiers.size() > 0) {
			cyto_annots = (SymWithProps) cyto_tiers.get(0);
			//	SeqUtils.printSymmetry(cyto_annots);
			}

		if (cyto_annots instanceof TypeContainerAnnot) {
			TypeContainerAnnot cyto_container = (TypeContainerAnnot) cyto_annots;
			return makeCytobandGlyph(cyto_container);
		}

		return null;
	}

	/**
	 *  Creates a cytoband glyph.  Handling two cases:
	 * 1. cytoband syms are children of TypeContainerAnnot;
	 * 2. cytoband syms are grandchildren of TypeContainerAnnot
	 *        (when cytobands are loaded via DAS/2, child of TypeContainerAnnot
	 *         will be a Das2FeatureRequestSym, which will have cytoband children).
	 */
	public EfficientSolidGlyph makeCytobandGlyph(TypeContainerAnnot cyto_container) {
		int cyto_height = 11; // the pointed glyphs look better if this is an odd number

		RoundRectMaskGlyph cytoband_glyph_A = null;
		RoundRectMaskGlyph cytoband_glyph_B = null;

		List<CytobandParser.CytobandSym> bands = new ArrayList<CytobandParser.CytobandSym>();
		for (int q = 0; q < cyto_container.getChildCount(); q++) {
			SeqSymmetry child = cyto_container.getChild(q);
			if (child instanceof CytobandParser.CytobandSym) {
				bands.add((CytobandParser.CytobandSym) child);
			} else if (child != null) {
				for (int subindex = 0; subindex < child.getChildCount(); subindex++) {
					SeqSymmetry grandchild = child.getChild(subindex);
					if (grandchild instanceof CytobandParser.CytobandSym) {
						bands.add((CytobandParser.CytobandSym) grandchild);
					}
				}
			}
		}
		//System.out.println("   band count: " + bands.size());

		int centromerePoint = -1;
		for (int i = 0; i < bands.size() - 1; i++) {
			if (bands.get(i).getArm() != bands.get(i + 1).getArm()) {
				centromerePoint = i;
				break;
			}
		}

		for (int q = 0; q < bands.size(); q++) {
			//          SeqSymmetry sym  = cyto_container.getChild(q);
			SeqSymmetry sym = (SeqSymmetry) bands.get(q);
			SeqSymmetry sym2 = transformForViewSeq(sym, getAnnotatedSeq());

			SeqSpan cyto_span = getViewSeqSpan(sym2);

			if (cyto_span != null && sym instanceof CytobandParser.CytobandSym) {
				CytobandParser.CytobandSym cyto_sym = (CytobandParser.CytobandSym) sym;

				//float score = ((Scored) cyto_sym).getScore();
				GlyphI efg;
				if (CytobandParser.BAND_ACEN.equals(cyto_sym.getBand())) {
					//efg = new PointedGlyph();
					//efg.setCoords(cyto_span.getStartDouble(), 2.0+2, cyto_span.getLengthDouble(), cyto_height-4);
					//((PointedGlyph) efg).setForward(! cyto_sym.getID().startsWith("q"));

					efg = new EfficientPaintRectGlyph();
					efg.setCoords(cyto_span.getStartDouble(), 2.0, cyto_span.getLengthDouble(), cyto_height);
					((EfficientPaintRectGlyph) efg).setPaint(CytobandParser.acen_paint);

				} else if (CytobandParser.BAND_STALK.equals(cyto_sym.getBand())) {
					//efg = new DoublePointedGlyph();
					//efg.setCoords(cyto_span.getStartDouble(), 2.0+2, cyto_span.getLengthDouble(), cyto_height-4);

					efg = new EfficientPaintRectGlyph();
					efg.setCoords(cyto_span.getStartDouble(), 2.0, cyto_span.getLengthDouble(), cyto_height);
					((EfficientPaintRectGlyph) efg).setPaint(CytobandParser.stalk_paint);

				} else if ("".equals(cyto_sym.getBand())) {
					efg = new EfficientOutlinedRectGlyph();
					efg.setCoords(cyto_span.getStartDouble(), 2.0, cyto_span.getLengthDouble(), cyto_height);
				} else {
					efg = new com.affymetrix.genoviz.glyph.LabelledRectGlyph();
					efg.setCoords(cyto_span.getStartDouble(), 2.0, cyto_span.getLengthDouble(), cyto_height);
					((com.affymetrix.genoviz.glyph.LabelledRectGlyph) efg).setForegroundColor(cyto_sym.getTextColor());
					((com.affymetrix.genoviz.glyph.LabelledRectGlyph) efg).setText(cyto_sym.getID());
					((com.affymetrix.genoviz.glyph.LabelledRectGlyph) efg).setFont(SMALL_FONT);
				}
				efg.setColor(cyto_sym.getColor());
				getSeqMap().setDataModelFromOriginalSym(efg, cyto_sym);


				if (q <= centromerePoint) {
					if (cytoband_glyph_A == null) {
						cytoband_glyph_A = new RoundRectMaskGlyph(axis_tier.getBackgroundColor());
						cytoband_glyph_A.setColor(Color.GRAY);
						//cytoband_glyph_A.setHitable(false);
						cytoband_glyph_A.setCoordBox(efg.getCoordBox());
					}
					cytoband_glyph_A.addChild(efg);
					cytoband_glyph_A.getCoordBox().add(efg.getCoordBox());
				} else {
					if (cytoband_glyph_B == null) {
						cytoband_glyph_B = new RoundRectMaskGlyph(axis_tier.getBackgroundColor());
						cytoband_glyph_B.setColor(Color.GRAY);
						//cytoband_glyph_B.setHitable(false);
						cytoband_glyph_B.setCoordBox(efg.getCoordBox());
					}
					cytoband_glyph_B.addChild(efg);
					cytoband_glyph_B.getCoordBox().add(efg.getCoordBox());
				}

			}
		}

		InvisibleBoxGlyph cytoband_glyph = new InvisibleBoxGlyph();
		cytoband_glyph.setMoveChildren(false);
		if (cytoband_glyph_A != null && cytoband_glyph_B != null) {
			cytoband_glyph.setCoordBox(cytoband_glyph_A.getCoordBox());
			cytoband_glyph.getCoordBox().add(cytoband_glyph_B.getCoordBox());
			cytoband_glyph.addChild(cytoband_glyph_A);
			cytoband_glyph.addChild(cytoband_glyph_B);
		}

		return cytoband_glyph;
	}

	/** By default, this does nothing.  Subclasses can implement. */
	public void addCytobandAsTier(TypeContainerAnnot tca) {
		return;
	}

	/** Sets the axis label format from the value in the persistent preferences. */
	public static void setAxisFormatFromPrefs(AxisGlyph axis) {
		// It might be good to move this to AffyTieredMap
		String axis_format = UnibrowPrefsUtil.getTopNode().get(PREF_AXIS_LABEL_FORMAT, VALUE_AXIS_LABEL_FORMAT_COMMA);
		if (VALUE_AXIS_LABEL_FORMAT_COMMA.equalsIgnoreCase(axis_format)) {
			axis.setLabelFormat(AxisGlyph.COMMA);
		} else if (VALUE_AXIS_LABEL_FORMAT_FULL.equalsIgnoreCase(axis_format)) {
			axis.setLabelFormat(AxisGlyph.FULL);
		} else if (VALUE_AXIS_LABEL_FORMAT_NO_LABELS.equalsIgnoreCase(axis_format)) {
			axis.setLabelFormat(AxisGlyph.NO_LABELS);
		} else {
			axis.setLabelFormat(AxisGlyph.ABBREV);
		}
	}

	public void clear() {
		stopSlicingThread();
		seqmap.clearWidget();
		aseq = null;
		viewseq = null;
		clearSelection();
		method2rtier = new HashMap<String, TierGlyph>();
		method2ftier = new HashMap<String, TierGlyph>();
		gstyle2tier = new HashMap<IAnnotStyle, TierGlyph>();
		//gstyle2floatTier = new HashMap();
		match_glyphs = new Vector<GlyphI>();
		seqmap.updateWidget();
	}

	/* //TODO
	 *  GAH 3-20-2003
	 *  WARNING
	 *  really need to fix some underlying GenoViz issues for this to be effective in
	 *    actually reclaiming memory from graphs:
	 *  Specifically, NeoMap.removeItem(GlyphI gl) need to recursively remove child glyphs from
	 *     objects such as the Hashtable in NeoMap that maps data models to glyphs
	 *  Also, should really be removing not just GraphGlyphs (and their parent
	 *     PixelFloaterGlyphs and TierGlyphs)

	 */
	/**
	 *  Clears the graphs, and reclaims some memory.
	 */
	public void clearGraphs() {
		if (aseq != null) {
			BioSeq mseq = aseq;
			int acount = mseq.getAnnotationCount();
			for (int i = acount - 1; i >= 0; i--) {
				SeqSymmetry annot = mseq.getAnnotation(i);
				if (annot instanceof GraphSym) {
					mseq.removeAnnotation(annot); // This also removes from the AnnotatedSeqGroup.
				}
			}
		} else {
			System.err.println("Current annotated seq is not mutable, cannot call SeqMapView.clearGraphs()!");
		}

		//Make sure the graph is un-selected in the genometry model, to allow GC
		gmodel.clearSelectedSymmetries(this);
		setAnnotatedSeq(aseq, false, true);
	}

	/** Sets the sequence; if null, has the same effect as calling clear(). */
	public void setAnnotatedSeq(MutableAnnotatedBioSeq seq) {
		if ((seq == this.aseq) && (seq != null)) {
			// if the seq is not changing, try to preserve current view
			setAnnotatedSeq(seq, false, true);
		} else {
			setAnnotatedSeq(seq, false, false);
		}
	}

	/**
	 *   Sets the sequence.  If null, has the same effect as calling clear().
	 *   @param preserve_selection  if true, then try and keep same selections
	 *   @param preserve_view  if true, then try and keep same scroll and zoom / scale and offset in
	 *       // both x and y direction.
	 *       [GAH: temporarily changed to preserve scale in only the x direction]
	 */
	public void setAnnotatedSeq(MutableAnnotatedBioSeq seq, boolean preserve_selection, boolean preserve_view) {
		//    setAnnotatedSeq(seq, preserve_selection, preserve_view, preserve_view);
		setAnnotatedSeq(seq, preserve_selection, preserve_view, false);
	}

	public void setAnnotatedSeq(MutableAnnotatedBioSeq seq, boolean preserve_selection, boolean preserve_view_x, boolean preserve_view_y) {
		//   want to optimize for several situations:
		//       a) merging newly loaded data with existing data (adding more annotations to
		//           existing AnnotatedBioSeq) -- would like to avoid recreation and repacking
		//           of already glyphified annotations
		//       b) reverse complementing existing AnnotatedBioSeq
		//       c) coord shifting existing AnnotatedBioSeq
		//   in all these cases:
		//       "new" MutableAnnotatedBioSeq == old AnnotatedBioSeq
		//       existing glyphs could be reused (in (b) they'd have to be "flipped")
		//       should preserve selection
		//       should preserve view (x/y scale/offset) (in (b) would preserve "flipped" view)
		//   only some of the above optimization/preservation are implemented yet
		//   WARNING: currently graphs are not properly displayed when reverse complementing,
		//               need to "genometrize" them
		//            currently sequence is not properly displayed when reverse complementing
		//

		stopSlicingThread();

		setTitleBar(seq);

		if (seq == null) {
			clear();
			return;
		}

		Timer tim = new Timer();
		tim.start();

		boolean same_seq = ((seq == this.aseq) && (seq != null));


		match_glyphs = new Vector<GlyphI>();
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

		// stash annotation tiers for proper state restoration after resetting for same seq
		//    (but presumably added / deleted / modified annotations...)

		ArrayList<TierGlyph> temp_tiers = new ArrayList<TierGlyph>();
		int axis_index = 0;
		// copying map tiers to separate list to avoid problems when removing tiers
		//   (and thus modifying map.getTiers() list -- could probably deal with this
		//    via iterators, but feels safer this way...)
		ArrayList<TierGlyph> cur_tiers = new ArrayList<TierGlyph>(seqmap.getTiers());
		for (int i = 0; i < cur_tiers.size(); i++) {
			TierGlyph tg = cur_tiers.get(i);
			if (tg == axis_tier) {
				axis_index = i;
			} else {
				tg.removeAllChildren();
				temp_tiers.add(tg);
				if (DEBUG_TIERS) {
					System.out.println("removing tier from map: " + tg.getLabel());
				}
				seqmap.removeTier(tg);
			}
		}

		seqmap.clearWidget();
		seqmap.clearSelected(); // may already be done by map.clearWidget()

		pixel_floater_glyph.removeAllChildren();
		pixel_floater_glyph.setParent(null);
		seqmap.addItem(pixel_floater_glyph);

		aseq = (BioSeq)seq;

		// if shifting coords, then seq2viewSym and viewseq are already taken care of,
		//   but reset coord_shift to false...
		if (coord_shift) {
			// map range will probably change after this if SHRINK_WRAP_MAP_BOUNDS is set to true...
			//      map.setMapRange(viewseq.getMin(), viewseq.getMax());
			//      resultSeqMap.setMapRange(0, viewseq.getLength());
			coord_shift = false;
		} else {

			viewseq = aseq;
			seq2viewSym = null;
			transform_path = null;
			//}
		}

		BioSeq compnegseq = viewseq;
		seqmap.setMapRange(compnegseq.getMin(), compnegseq.getMax());


		// The hairline needs to be among the first glyphs added,
		// to keep it from interfering with selection of other glyphs.
		if (hairline != null) {
			hairline.destroy();
		}
		hairline = new UnibrowHairline(seqmap);
		hairline.getShadow().setLabeled(hairline_is_labeled);

		// add back in previous annotation tiers (with all children removed)
		if (temp_tiers != null) {
			for (int i = 0; i < temp_tiers.size(); i++) {
				TierGlyph tg = temp_tiers.get(i);
				if (DEBUG_TIERS) {
					System.out.println("adding back tier: " + tg.getLabel() + ", scene = " + tg.getScene());
				}
				// Reset tier properties: this is mainly needed to reset the background color
				if (tg.getAnnotStyle() != null) {
					tg.setStyle(tg.getAnnotStyle());
				}

				seqmap.addTier(tg);
			}
			temp_tiers.clear(); // redundant hint to garbage collection
		}

		TransformTierGlyph at = addAxisTier(axis_index);
		addAnnotationTiers();
		removeEmptyTiers();

		seqmap.repack();

		if (same_seq && preserve_selection) {
			// reselect glyph(s) based on selected sym(s);
			// Unfortunately, some previously selected syms will not be directly
			// associatable with new glyphs, so not all selections can be preserved
			Iterator iter = old_selections.iterator();
			while (iter.hasNext()) {
				SeqSymmetry old_selected_sym = (SeqSymmetry) iter.next();

				GlyphI gl = seqmap.getItem(old_selected_sym);
				if (gl != null) {
					seqmap.select(gl);
				}
			}
			setZoomSpotX(old_zoom_spot_x);
			setZoomSpotY(old_zoom_spot_y);

			if (show_edge_matches) {
				doEdgeMatching(seqmap.getSelected(), false);
			}

		} else {
			// do selection based on what the genometry model thinks is selected
			List<SeqSymmetry> symlist = gmodel.getSelectedSymmetries(seq);
			select(symlist, false, false, false);

			String title = getSelectionTitle(seqmap.getSelected());
			setStatus(title);

			if (show_edge_matches) {
				doEdgeMatching(seqmap.getSelected(), false);
			}
		}

		shrinkWrap();

		seqmap.toFront(axis_tier);

		// restore floating layers to front of map
		for (GlyphI layer_glyph : this.getFloatingLayers()) {
			seqmap.toFront(layer_glyph);
		}
		// notifyPlugins();

		// Ignore preserve_view if seq has changed
		if ((preserve_view_x || preserve_view_y) && same_seq) {
			seqmap.stretchToFit(!preserve_view_x, !preserve_view_y);
		} else {
			seqmap.stretchToFit(true, true);
			zoomToSelections();
			int[] range = seqmap.getVisibleRange();
			setZoomSpotX(0.5 * (range[0] + range[1]));
		}
		seqmap.updateWidget();
		if (DIAGNOSTICS) {
			System.out.println("Time to convert models to display: " + tim.read() / 1000f);
		}
	}

	private void shrinkWrap() {
		if (SHRINK_WRAP_MAP_BOUNDS) {
			/*
			 *  Shrink wrapping is a little more complicated than one might expect, but it
			 *   needs to take into account the mapping of the annotated sequence to the
			 *   view (although currently assumes this mapping doesn't do any rearrangements, etc.)
			 *   (alternative, to ensure that _arbitrary_ genometry mapping can be accounted for,
			 *    is to base annotation bounds on map glyphs, but then have to go into tiers to
			 *    get children bounds, and filter out stuff like axis and DNA glyphs, etc...)
			 */
			SeqSpan annot_bounds = getAnnotationBounds(true);
			if (annot_bounds != null) {
				System.out.println("annot bounds: " + annot_bounds.getMin() + ", " + annot_bounds.getMax());
				// transform to view
				MutableSeqSymmetry sym = new SimpleMutableSeqSymmetry();
				sym.addSpan(annot_bounds);
				if (aseq != viewseq) {
					//	  SeqUtils.transformSymmetry(sym, seq2viewSym);
					SeqUtils.transformSymmetry(sym, transform_path);
				}
				SeqSpan view_bounds = sym.getSpan(viewseq);
				System.out.println("annot view bounds: " + view_bounds.getMin() + ", " + view_bounds.getMax());
				seqmap.setMapRange(view_bounds.getMin(), view_bounds.getMax());
			}
		}
	}

	protected String getVersionInfo(MutableAnnotatedBioSeq seq) {
		if (seq == null) {
			return null;
		}
		String version_info = null;
		if (((BioSeq) seq).getSeqGroup() != null) {
			AnnotatedSeqGroup group = ((BioSeq) seq).getSeqGroup();
			if (group.getDescription() != null) {
				version_info = group.getDescription();
			} else {
				version_info = group.getID();
			}
		}
		if (version_info == null) {
			version_info = ((BioSeq) seq).getVersion();
		}
		if ("hg17".equals(version_info)) {
			version_info = "hg17 = NCBI35";
		} else if ("hg18".equals(version_info)) {
			version_info = "hg18 = NCBI36";
		}
		return version_info;
	}

	protected void setTitleBar(MutableAnnotatedBioSeq seq) {
		Pattern pattern = Pattern.compile("chr([0-9XYM]*)");
		if (frm != null) {
			StringBuffer title = new StringBuffer(128);
			if (appNameFirstInTitle) {
				title.append(IGBConstants.APP_NAME + " " + IGBConstants.IGB_FRIENDLY_VERSION);
			}
			if (seq != null) {
				if (title.length() > 0) {
					title.append(" - ");
				}
				String seqid = seq.getID().trim();
				if (pattern.matcher(seqid).matches()) {
					seqid = seqid.replace("chr", "Chromosome ");
				}

				title.append(seqid);
				String version_info = getVersionInfo(seq);
				if (version_info != null) {
					title.append("  (").append(version_info).append(')');
				}
			}
			if (!appNameFirstInTitle) {
				if (title.length() > 0) {
					title.append(" - ");
				}
				title.append(IGBConstants.APP_NAME + " " + IGBConstants.IGB_FRIENDLY_VERSION);
			}
			frm.setTitle(title.toString());
		}
	}

	/**
	 *  Returns all floating layers _except_ grid layer (which is supposed to stay
	 *  behind everything else).
	 */
	private List<GlyphI> getFloatingLayers() {
		List<GlyphI> layers = new ArrayList<GlyphI>();
		GlyphI root_glyph = seqmap.getScene().getGlyph();
		int gcount = root_glyph.getChildCount();
		for (int i = 0; i < gcount; i++) {
			GlyphI cgl = root_glyph.getChild(i);
			if ((cgl instanceof PixelFloaterGlyph) && (cgl != grid_layer)) {
				layers.add(cgl);
			}
		}
		return layers;
	}

	void removeEmptyTiers() {
		// Hides all empty tiers.  Doesn't really remove them.
		for (TierGlyph tg : seqmap.getTiers()) {
			if (tg.getChildCount() <= 0) {
				tg.setState(TierGlyph.HIDDEN);
			}
		}
	}

	/**
	 *  Find min and max of annotations along MutableAnnotatedBioSeq aseq.
	 *<p>
	 *  takes a boolean argument for whether to excludes GraphSym bounds
	 *    (actual bounds of GraphSyms are currently problematic, but if (!exclude_graphs) then
	 *      this method uses the first and last point in graph to determine graph bounds, and
	 *      assumes that graph x coords are in order)
	 *<p>
	 *    This method is currently somewhat problematic, since it does not descend into BioSeqs
	 *      that aseq might be composed of to factor in bounds of annotations on those sequences
	 */
	private final SeqSpan getAnnotationBounds(boolean exclude_graphs) {
		int annotCount = aseq.getAnnotationCount();
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for (int i = 0; i < annotCount; i++) {
			// all_gene_searches, all_repeat_searches, etc.
			SeqSymmetry annotSym = aseq.getAnnotation(i);
			if (annotSym instanceof GraphSym) {
				if (!exclude_graphs) {
					GraphSym graf = (GraphSym) annotSym;
					int[] xcoords = graf.getGraphXCoords();
					min = Math.min(xcoords[0], min);
					max = Math.max(xcoords[xcoords.length - 1], max);
				}
			} else if (annotSym instanceof TypeContainerAnnot) {
				TypeContainerAnnot tca = (TypeContainerAnnot) annotSym;
				int[] sub_bounds = getAnnotationBounds(aseq, tca, exclude_graphs, min, max);
				min = sub_bounds[0];
				max = sub_bounds[1];
			} else { // this shouldn't happen: should only be TypeContainerAnnots
				SeqSpan span = annotSym.getSpan(aseq);
				if (span != null) {
					min = Math.min(span.getMin(), min);
					max = Math.max(span.getMax(), max);
				}
			}
		}
		if (min != Integer.MAX_VALUE && max != Integer.MIN_VALUE) {
			min = Math.max(0, min - 100);
			max = Math.min(aseq.getLength(), max + 100);
			return new SimpleSeqSpan(min, max, aseq);
		} else {
			return null;
		}
	}

	/** Returns the minimum and maximum positions of all included annotations.
	 *  Necessary because getMin() and getMax() do not give this information
	 *  for this type of SeqSymmetry.
	 *
	 *  @param seq  consider annotations only on this seq
	 *  @param exclude_graphs if true, ignore graph annotations
	 *  @param min  an initial minimum value.
	 *  @param max  an initial maximum value.
	 */
	private static final int[] getAnnotationBounds(MutableAnnotatedBioSeq seq, TypeContainerAnnot tca, boolean exclude_graphs, int min, int max) {
		int[] min_max = new int[2];
		min_max[0] = min;
		min_max[1] = max;

		int child_count = tca.getChildCount();
		for (int j = 0; j < child_count; j++) {
			SeqSymmetry next_sym = tca.getChild(j);

			int annotCount = next_sym.getChildCount();
			for (int i = 0; i < annotCount; i++) {
				// all_gene_searches, all_repeat_searches, etc.
				SeqSymmetry annotSym = next_sym.getChild(i);
				if (annotSym instanceof GraphSym) {
					if (!exclude_graphs) {
						GraphSym graf = (GraphSym) annotSym;
						int[] xcoords = graf.getGraphXCoords();
						min_max[0] = Math.min(xcoords[0], min_max[0]);
						min_max[1] = Math.max(xcoords[xcoords.length - 1], min_max[1]);   // JN - was using min_max[0]; fixed
						// TODO: This needs to take into account GraphIntervalSyms width coords also !!
						// The easiest way would be to re-write the GraphSym and GraphIntervalSym
						// method getSpan() so that it returned the correct values.
					}
				} else {
					SeqSpan span = annotSym.getSpan(seq);
					if (span != null) {
						min_max[0] = Math.min(span.getMin(), min_max[0]);
						min_max[1] = Math.max(span.getMax(), min_max[1]);
					}
				}
			}
		}
		return min_max;
	}

	private void addAnnotationTiers() {
		if (aseq == null) {
			// This shouldn't happen, but I've seen it during startup: eee july 2007
			return;
		}
		if (DEBUG_COMP) {
			System.out.println("$$$$$$$ called SeqMapView.addAnnotationTiers(), aseq: " + aseq.getID() + " $$$$$$$");
		}
		// WARNING: use aseq.getAnnotationCount() in loop, because some annotations end up lazily instantiating
		//   other annotations and adding them to the annotation list
		// For example, accessing methods for the first time on a LazyChpSym can cause it to dynamically add
		//      probeset annotation tracks
		for (int i = 0; i < aseq.getAnnotationCount(); i++) {
			SeqSymmetry annotSym = aseq.getAnnotation(i);

			// skip over any cytoband data.  It is shown in a different way
			if (annotSym instanceof TypeContainerAnnot) {
				TypeContainerAnnot tca = (TypeContainerAnnot) annotSym;

				if (CYTOBAND_TIER_REGEX.matcher(tca.getType()).matches()) {
					addCytobandAsTier(tca);
					continue;
				}
			}

			if (annotSym instanceof SymWithProps) {
				addAnnotationGlyphs(annotSym);
			}
		}

		if (aseq != null &&
						aseq.getComposition() != null) {
			// muck with aseq, seq2viewsym, transform_path to trick addAnnotationTiers(),
			//   addLeafsToTier(), addToTier(), etc. into mapping from compositon sequences
			BioSeq cached_aseq = aseq;
			MutableSeqSymmetry cached_seq2viewSym = seq2viewSym;
			SeqSymmetry[] cached_path = transform_path;

			SeqSymmetry comp = aseq.getComposition();
			// assuming a two-level deep composition hierarchy for now...
			//   need to make more recursive at some point...
			//   (or does recursive call to addAnnotationTiers already give us full recursion?!!)
			int scount = comp.getChildCount();
			for (int i = 0; i < scount; i++) {
				//      for (int i=0; i<1; i++) {
				SeqSymmetry csym = comp.getChild(i);
				// return seq in a symmetry span that _doesn't_ match aseq
				BioSeq cseq = (BioSeq)SeqUtils.getOtherSeq(csym, cached_aseq);
				if (DEBUG_COMP) {
					System.out.println(" other seq: " + cseq.getID() + ",  " + cseq);
				}
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
					if (DEBUG_COMP) {
						System.out.println("  calling addAnnotationTiers with transform path length: " + transform_path.length);
					}
					addAnnotationTiers();
				}
			}

			// restore aseq and seq2viewsym afterwards...
			aseq = cached_aseq;
			seq2viewSym = cached_seq2viewSym;
			transform_path = cached_path;
		}
	}
	// We only need a single GraphGlyphFactory because all graph properties
	// are in the GraphState object.
	GenericGraphGlyphFactory graph_factory = null;

	public GenericGraphGlyphFactory getGenericGraphGlyphFactory() {
		if (graph_factory == null) {
			graph_factory = new GenericGraphGlyphFactory();
		}
		return graph_factory;
	}
	// We only need a single ScoredContainerGlyphFactory because all graph properties
	// are in the GraphState object.
	ScoredContainerGlyphFactory container_factory = null;

	public ScoredContainerGlyphFactory getScoredContainerGlyphFactory() {
		if (container_factory == null) {
			container_factory = new ScoredContainerGlyphFactory();
		}
		return container_factory;
	}

	// The parameter "method" is now ignored because the default glyph factory
	// is an XmlStylesheetGlyphFactory, and it will take the method and type
	// into account when determining how to draw a sym.
	public MapViewGlyphFactoryI getAnnotationGlyphFactory(String method) {
		return default_glyph_factory;
	}

	private void addAnnotationGlyphs(SeqSymmetry annotSym) {
		// Map symmetry subclass or method type to a factory, and call factory to make glyphs
		MapViewGlyphFactoryI factory = null;
		String meth = BioSeq.determineMethod(annotSym);
		//    System.out.println("adding annotation glyphs for method: " + meth);

		if (annotSym instanceof ScoredContainerSym) {
			factory = getScoredContainerGlyphFactory();
		} else if (annotSym instanceof GraphSym) {
			factory = getGenericGraphGlyphFactory();
		} else {
			factory = getAnnotationGlyphFactory(meth);
		}

		if (DEBUG_COMP && transform_path != null) {
			System.out.println("transform path length: " + transform_path.length);
			for (int i = 0; i < transform_path.length; i++) {
				SeqUtils.printSymmetry(transform_path[i]);
			}
		}

		factory.createGlyph(annotSym, this);
		doMiddlegroundShading(annotSym, meth);
	}

	private void doMiddlegroundShading(SeqSymmetry annotSym, String meth) {

		// do "middleground" shading for tracks loaded via DAS/2
		if ((meth != null) &&
						(annotSym instanceof TypeContainerAnnot) &&
						(annotSym.getChildCount() > 0) &&
						(annotSym.getChild(0) instanceof Das2FeatureRequestSym)) {
			int child_count = annotSym.getChildCount();
			TierGlyph fortier = method2ftier.get(meth.toLowerCase());
			TierGlyph revtier = method2rtier.get(meth.toLowerCase());
			for (int i = 0; i < child_count; i++) {
				SeqSymmetry csym = annotSym.getChild(i);
				if (csym instanceof Das2FeatureRequestSym) {
					Das2FeatureRequestSym dsym = (Das2FeatureRequestSym) csym;
					SeqSpan ospan = dsym.getOverlapSpan();
					// System.out.println("DAS FEATURE SYM: " + SeqUtils.spanToString(csym.getSpan(0)));
					if (fortier != null) {
						GlyphI mglyph = new EfficientFillRectGlyph();
						//	    mglyph.setColor(Color.lightGray);  this is done in TierGlyph for now...
						mglyph.setCoords(ospan.getMin(), 0, ospan.getMax() - ospan.getMin(), 0);
						fortier.addMiddleGlyph(mglyph);
					}
					if (revtier != null) {
						GlyphI mglyph = new EfficientFillRectGlyph();
						//	    mglyph.setColor(Color.lightGray);  this is done in TierGlyph for now...
						mglyph.setCoords(ospan.getMin(), 0, ospan.getMax() - ospan.getMin(), 0);
						revtier.addMiddleGlyph(mglyph);
					}
				}
			}
		}

	}

	public MutableAnnotatedBioSeq getAnnotatedSeq() {
		return aseq;
	}

	/**
	 *  Gets the view seq.
	 *  Note: {@link #getViewSeq()} and {@link #getAnnotatedSeq()} may return
	 *  different BioSeq's !
	 *  This allows for reverse complement, coord shifting, seq slicing, etc.
	 *  Returns MutableAnnotatedBioSeq that is the SeqMapView's _view_ onto the
	 *     MutableAnnotatedBioSeq returned by getAnnotatedSeq()
	 *  @see #getTransformPath()
	 */
	public MutableAnnotatedBioSeq getViewSeq() {
		return viewseq;
	}

	/**
	 *  Returns the series of transformations that can be used to map
	 *  a SeqSymmetry from {@link #getAnnotatedSeq()} to
	 *  {@link #getViewSeq()}.
	 */
	public SeqSymmetry[] getTransformPath() {
		return transform_path;
	}

	/** Returns a transformed copy of the given symmetry based on
	 *  {@link #getTransformPath()}.  If no transform is necessary, simply
	 *  returns the original symmetry.
	 */
	public SeqSymmetry transformForViewSeq(SeqSymmetry insym) {
		return transformForViewSeq(insym, getAnnotatedSeq());
	}

	public SeqSymmetry transformForViewSeq(SeqSymmetry insym, MutableAnnotatedBioSeq seq_to_compare) {
		SeqSymmetry result_sym = insym;
		//    if (getAnnotatedSeq() != getViewSeq()) {
		if (seq_to_compare != getViewSeq()) {
			MutableSeqSymmetry tempsym = SeqUtils.copyToDerived(insym);
			//      System.out.println("^^^^^^^ calling SeqUtils.transformSymmetry()");
			SeqUtils.transformSymmetry(tempsym, getTransformPath());
			result_sym = tempsym;
		}
		return result_sym;
	}

	public AffyTieredMap getSeqMap() {
		return seqmap;
	}

	public void selectAllGraphs() {
		List<GlyphI> glyphlist = new ArrayList<GlyphI>();
		GlyphI rootglyph = seqmap.getScene().getGlyph();
		collectGraphs(rootglyph, glyphlist);
		// convert graph glyphs to GraphSyms via glyphsToSyms

		// Bring them all into the visual area
		for (int i = 0; i < glyphlist.size(); i++) {
			GraphGlyphUtils.checkPixelBounds((GraphGlyph) glyphlist.get(i), getSeqMap());
		}

		List<SeqSymmetry> symlist = glyphsToSyms(glyphlist);
		//    System.out.println("called SeqMapView.selectAllGraphs(), select count: " + symlist.size());
		// call select(list) on list of graph syms
		select(symlist, false, true, true);
	}

	void select(List<SeqSymmetry> sym_list) {
		select(sym_list, false, false, true);
	}

	private void select(List<SeqSymmetry> sym_list, boolean add_to_previous,
					boolean call_listeners, boolean update_widget) {
		if (!add_to_previous) {
			clearSelection();
		}

		for (SeqSymmetry sym : sym_list) {
			// currently assuming 1-to-1 mapping of sym to glyph
			GlyphI gl = seqmap.getItem(sym);
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
		seqmap.clearSelected();
		setSelectedRegion(null, false);
		//  clear match_glyphs?
	}

	private SeqSymmetry glyphToSym(GlyphI gl) {
		if (gl.getInfo() instanceof SeqSymmetry) {
			return (SeqSymmetry) gl.getInfo();
		} else {
			return null;
		}
	}

	/**
	 * Given a list of glyphs, returns a list of syms that those
	 *  glyphs represent.
	 */
	private List<SeqSymmetry> glyphsToSyms(List<GlyphI> glyphs) {
		List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>(glyphs.size());
		if (glyphs.size() > 0) {
			// if some syms are represented by multiple glyphs, then want to make sure glyphsToSyms()
			//    doesn't return the same sym multiple times, so have to do a bit more work
			if (seqmap.hasMultiGlyphsPerModel()) {
				//	System.out.println("#########    in SeqMapView.glyphsToSyms(), possible multiple glyphs per sym");
				HashMap<SeqSymmetry, SeqSymmetry> symhash = new HashMap<SeqSymmetry, SeqSymmetry>();
				for (int i = 0; i < glyphs.size(); i++) {
					GlyphI gl = glyphs.get(i);
					SeqSymmetry sym = glyphToSym(gl);
					if (sym != null) {
						symhash.put(sym, sym);
					}
				}
				Iterator<SeqSymmetry> iter = symhash.values().iterator();
				while (iter.hasNext()) {
					syms.add(iter.next());
				}
			} else {
				//  no syms represented by multiple glyphs, so can use more efficient way of collecting syms
				for (int i = 0; i < glyphs.size(); i++) {
					GlyphI gl = glyphs.get(i);
					SeqSymmetry sym = glyphToSym(gl);
					if (sym != null) {
						syms.add(sym);
					}
				}
			}
		}
		return syms;
	}

	/**
	 *  Figures out which symmetries are currently selected and then calls
	 *  {@link SingletonGenometryModel#setSelectedSymmetries(List, Object)}.
	 */
	void postSelections() {
		Vector<GlyphI> selected_glyphs = seqmap.getSelected();

		List<SeqSymmetry> selected_syms = glyphsToSyms(selected_glyphs);
		// Note that seq_selected_sym (the selected residues) is not included in selected_syms
		gmodel.setSelectedSymmetries(selected_syms, this);
	}

	// assumes that region_sym contains a span with span.getBioSeq() ==  current seq (aseq)
	public void setSelectedRegion(SeqSymmetry region_sym, boolean update_widget) {
		seq_selected_sym = region_sym;
		// Note: SUBSELECT_SEQUENCE might possibly be set to false in the AltSpliceView
		if (SUBSELECT_SEQUENCE && seq_glyph != null) {
			if (region_sym == null) {
				seq_glyph.setSelected(false);
			} else {
				SeqSpan seq_region = seq_selected_sym.getSpan(aseq);
				seq_glyph.select(seq_region.getMin(), seq_region.getMax());
				setStatus(SeqUtils.spanToString(seq_region));
			}
			if (update_widget) {
				seqmap.updateWidget();
			}
		}
	}

	/** Returns the region of sequence residues that is selected, or null.
	 *  Note that this SeqSymmetry is not included in the return value of
	 *  getSelectedSyms().
	 */
	private SeqSymmetry getSelectedRegion() {
		return seq_selected_sym;
	}

	/**
	 * Copies residues of selection to clipboard
	 * If a region of sequence is selected, should copy genomic residues
	 * If an annotation is selected, should the residues of the leaf nodes of the annotation, spliced together
	 */
	public boolean copySelectedResidues() {
		boolean success = false;
		SeqSymmetry residues_sym = null;
		Clipboard clipboard = this.getToolkit().getSystemClipboard();
		String from = "";

		if (seq_selected_sym != null) {
			residues_sym = getSelectedRegion();
			from = " from selected region";
		} else {
			List<SeqSymmetry> syms = getSelectedSyms();
			if (syms.size() == 1) {
				residues_sym = syms.get(0);
				from = " from selected item";
			}
		}

		if (residues_sym == null) {
			Application.errorPanel("Can't copy to clipboard",
							"No selection or multiple selections.  Select a single item before copying its residues to clipboard.");
		} else {
			SeqSpan span = residues_sym.getSpan(aseq);
			if (aseq == null) {
				// This is a fishy test.  How could aseq possibly be null?
				Application.errorPanel("Don't have residues, can't copy to clipboard");
			} else { // 2
				int child_count = residues_sym.getChildCount();
				if (child_count > 0) {
					// make new resorted sym to fix any problems with orientation
					//   within the original sym...
					//
					// GAH 12-15-2003  should really do some sort of recursive sort, but for
					//   now assuming depth = 2...  actually, should _really_ fix this when building SeqSymmetries,
					//   so order of children reflects the order they should be spliced in, rather
					//   than their order relative to a particular seq
					List<SeqSymmetry> sorted_children = new ArrayList<SeqSymmetry>(child_count);
					for (int i = 0; i < child_count; i++) {
						sorted_children.add(residues_sym.getChild(i));
					}
					boolean forward = span.isForward();

					Comparator<SeqSymmetry> symcompare = new SeqSymStartComparator(aseq, forward);
					Collections.sort(sorted_children, symcompare);
					MutableSeqSymmetry sorted_sym = new SimpleMutableSeqSymmetry();
					for (int i = 0; i < child_count; i++) {
						sorted_sym.addChild(sorted_children.get(i));
					}
					residues_sym = sorted_sym;
				}

				String residues = SeqUtils.getResidues(residues_sym, aseq);
				if (residues != null) {
					int rescount = residues.length();
					boolean complete = true;
					for (int i = 0; i < rescount; i++) {
						char res = residues.charAt(i);
						if (res == '-' || res == ' ' || res == '.') {
							complete = false;
							break;
						}
					}
					if (complete) {
						/*
						 *  WARNING
						 *  This bit of code *looks* unnecessary, but is needed because
						 *    StringSelection is buggy (at least with jdk1.3):
						 *    making a StringSelection with a String that has been derived from another
						 *    String via substring() ends up starting from the beginning of the _original_
						 *    String (maybe because of the way derived and original Strings do char-array sharing)
						 * THEREFORE, need to make a String with its _own_ internal char array that starts with
						 *   the 0th character...
						 */
						StringBuffer hackbuf = new StringBuffer(residues);
						String hackstr = new String(hackbuf);
						StringSelection data = new StringSelection(hackstr);
						clipboard.setContents(data, null);
						String message = "Copied " + hackstr.length() + " residues" + from + " to clipboard";
						setStatus(message);
						success = true;
					} else {
						Application.errorPanel("Missing Sequence Residues",
										"Don't have all the needed residues, can't copy to clipboard.\n" +
										"Please load sequence residues for this region.");
					}
				}
			}
		}
		if (!success) {
			// null out clipboard if unsuccessful (otherwise might get fooled into thinking
			//   the copy operation worked...)
			// GAH 12-16-2003
			// for some reason, can't null out clipboard with [null] or [new StringSelection("")],
			//   have to put in at least one character -- just putting in a space for now
			clipboard.setContents(new StringSelection(" "), null);
		}
		return success;
	}

	/**
	 *  Determines which SeqSymmetry's are selected by looking at which Glyph's
	 *  are currently selected.  The list will not include the selected sequence
	 *  region, if any.  Use getSelectedRegion() for that.
	 *  @return a List of SeqSymmetry objects, possibly empty.
	 */
	List<SeqSymmetry> getSelectedSyms() {
		Vector<GlyphI> glyphs = seqmap.getSelected();
		return glyphsToSyms(glyphs);
	}

	/**
	 *  Returns a selected symmetry, based on getSelectedGlyph().
	 *  It is probably better to use getSelectedSyms() in most cases.
	 *  @return a SeqSymmetry or null
	 */
	public SeqSymmetry getSelectedSymmetry() {
		Vector glyphs = seqmap.getSelected();
		if (glyphs.isEmpty()) {
			return null;
		} else {
			return glyphToSym((GlyphI) glyphs.lastElement());
		}
	}

	public void setSliceBuffer(int bases, boolean refresh) {
		slice_buffer = bases;
		if (refresh && slicing_in_effect) {
			sliceAndDice(slice_symmetry);
		}
	}

	public int getSliceBuffer() {
		return slice_buffer;
	}

	public void sliceBySelection() {
		sliceAndDice(getSelectedSyms());
	}

	public void sliceAndDice(final List<SeqSymmetry> syms) {
		stopSlicingThread();

		slice_thread = new Thread() {

			@Override
			public void run() {
				enableSeqMap(false);
				sliceAndDiceNow(syms);
				enableSeqMap(true);
			}
		};

		slice_thread.start();
	}

	void sliceAndDiceNow(List<SeqSymmetry> syms) {
		SimpleSymWithProps unionSym = new SimpleSymWithProps();
		SeqUtils.union(syms, unionSym, aseq);
		sliceAndDiceNow(unionSym);
	}

	public SeqSymmetry getSliceSymmetry() {
		return slice_symmetry;
	}

	// disables the sliced view while the slicing thread works
	void enableSeqMap(boolean b) {
		seqmap.setVisible(b);
		if (map_range_box != null) {
			if (!b) {
				map_range_box.range_box.setText("Working...");
			}
		}
		Component[] comps = xzoombox.getComponents();
		for (int i = 0; i < comps.length; i++) {
			comps[i].setEnabled(b);
		}
		comps = yzoombox.getComponents();
		for (int i = 0; i < comps.length; i++) {
			comps[i].setEnabled(b);
		}
	}

	public void sliceAndDice(final SeqSymmetry sym) {
		stopSlicingThread();

		slice_thread = new Thread() {

			public void run() {
				enableSeqMap(false);
				sliceAndDiceNow(sym);
				enableSeqMap(true);
			}
		};

		slice_thread.start();
	}

	void stopSlicingThread() {
		if (slice_thread == Thread.currentThread()) {
			//System.out.println("Current thread is the slicer!");
		} else if (slice_thread != null && slice_thread.isAlive()) {
			//System.out.println("Stopping a thread!");
			slice_thread.stop(); // TODO: Deprecated, but seems OK here.  Maybe fix later.
			slice_thread = null;
			enableSeqMap(true);
		}
	}

	/**
	 *  Performs a genometry-based slice-and-dice.
	 *  Assumes that symmetry children are ordered by child.getSpan(aseq).getMin().
	 */
	private void sliceAndDiceNow(SeqSymmetry sym) {
		//    System.out.println("%%%%%% called SeqMapView.sliceAndDice() %%%%%%");
		if (!slicing_in_effect) {
			//   only redo viewspan_before_slicing if slicing is not already in effect, because
			//   if (slicing_in_effect) and slicing again, probably just adjusting slice buffer
			viewspan_before_slicing = getVisibleSpan();
		}
		int childCount = (sym == null) ? 0 : sym.getChildCount();

		if (childCount <= 0) {
			return;
		}
		coord_shift = true;
		if (seq2viewSym == null) {
			seq2viewSym = new SimpleMutableSeqSymmetry();
		} else {
			seq2viewSym.clear();
		}

		slice_symmetry = sym;
		viewseq = new BioSeq("view_seq", "", aseq.getLength());
		//viewseq = new com.affymetrix.genometry.seq.SimpleCompAnnotBioSeq("view_seq", aseq.getLength());

		// rebuild seq2viewSym as a symmetry mapping slices of aseq to abut next to each other
		//    mapped to viewseq
		int prev_max = 0;
		int slice_offset = 0;
		MutableSeqSpan prev_seq_slice = null;
		MutableSeqSpan prev_view_slice = null;
		for (int i = 0; i < childCount; i++) {
			SeqSymmetry child = sym.getChild(i);
			SeqSpan exact_span = child.getSpan(aseq);
			if (exact_span == null) {
				continue;
			}  // skip any children that don't have a span in aseq
			int next_min;
			if (i == (childCount - 1)) {
				next_min = aseq.getLength();
			} else {
				next_min = sym.getChild(i + 1).getSpan(aseq).getMin();
			}

			int slice_min = Math.max(prev_max, (exact_span.getMin() - slice_buffer));
			int slice_max = Math.min(next_min, (exact_span.getMax() + slice_buffer));
			MutableSeqSpan seq_slice_span = new SimpleMutableSeqSpan(slice_min, slice_max, aseq);

			int slice_length = seq_slice_span.getLength();
			MutableSeqSpan view_slice_span =
							new SimpleMutableSeqSpan(slice_offset, slice_offset + slice_length, viewseq);

			if (prev_seq_slice != null && SeqUtils.looseOverlap(prev_seq_slice, seq_slice_span)) {
				// if new seq slice span abuts the old one, then just
				// lengthen existing spans (seq and view) rather than adding new ones
				SeqUtils.encompass(prev_seq_slice, seq_slice_span, prev_seq_slice);
				SeqUtils.encompass(prev_view_slice, view_slice_span, prev_view_slice);
			} else {
				if (ADD_INTRON_TRANSFORMS) {
					if (prev_seq_slice != null) {
						SeqSpan intron_region_span = new SimpleSeqSpan(prev_seq_slice.getMax(), seq_slice_span.getMin(), aseq);
						SeqSpan zero_length_span = new SimpleSeqSpan(view_slice_span.getMin(), view_slice_span.getMin(), viewseq);
						// SimplePairSeqSymmetry is better than EfficientPairSeqSymmetry here,
						// since there will be frequent calls to getSpan(BioSeq)
						seq2viewSym.addChild(new SimplePairSeqSymmetry(intron_region_span, zero_length_span));
					} else if (ADD_EDGE_INTRON_TRANSFORMS && i == 0) {
						// Add an extra transform for the "intron" that extends from the start of the sequence to the first selected exon
						SeqSpan intron_region_span = new SimpleSeqSpan(0, seq_slice_span.getMin(), aseq);
						SeqSpan zero_length_span = new SimpleSeqSpan(view_slice_span.getMin(), view_slice_span.getMin(), viewseq);
						seq2viewSym.addChild(new SimplePairSeqSymmetry(intron_region_span, zero_length_span));
					}
				}

				SeqSymmetry slice_sym = new SimplePairSeqSymmetry(seq_slice_span, view_slice_span);
				seq2viewSym.addChild(slice_sym);

				prev_seq_slice = seq_slice_span;
				prev_view_slice = view_slice_span;
			}
			slice_offset += slice_length;
			prev_max = slice_max;
		}

		if (ADD_EDGE_INTRON_TRANSFORMS && ADD_INTRON_TRANSFORMS) {
			// Add an extra transform for the "intron" that extends from the last selection to the end of the sequence
			SeqSpan intron_region_span = new SimpleSeqSpan(prev_seq_slice.getMax(), aseq.getLength(), aseq);
			SeqSpan zero_length_span = new SimpleSeqSpan(prev_view_slice.getMax(), prev_view_slice.getMax(), viewseq);
			seq2viewSym.addChild(new SimplePairSeqSymmetry(intron_region_span, zero_length_span));
		}

		SeqSpan seq_span = SeqUtils.getChildBounds(seq2viewSym, aseq);
		SeqSpan view_span = SeqUtils.getChildBounds(seq2viewSym, viewseq);
		seq2viewSym.addSpan(seq_span);
		seq2viewSym.addSpan(view_span);

		viewseq.setComposition(seq2viewSym);
		viewseq.setBounds(view_span.getMin(), view_span.getMax());
		transform_path = new SeqSymmetry[1];
		transform_path[0] = seq2viewSym;
		slicing_in_effect = true;

		setAnnotatedSeq(aseq);
	}

	public void toggleAutoScroll() {
		if (map_auto_scroller == null) {
			//      toggleAutoScroll(
			JPanel pan = new JPanel();

			Rectangle2D.Double cbox = seqmap.getViewBounds();
			//      int bases_in_view = (int) resultSeqMap.getView().getCoordBox().width;
			int bases_in_view = (int) cbox.width;
			as_start_pos = (int) cbox.x;
			as_end_pos = this.getViewSeq().getLength();
			int pixel_width = seqmap.getView().getPixelBox().width;
			as_bases_per_pix = bases_in_view / pixel_width;

			// as_bases_per_pix *should* be a float, or else should simply
			// use the current resoltion without asking the user,
			// but since it is an integer, we have to set the minimum value as 1
			if (as_bases_per_pix < 1) {
				as_bases_per_pix = 1;
			}

			final JTextField bases_per_pixTF = new JTextField("" + as_bases_per_pix);
			final JTextField pix_to_scrollTF = new JTextField("" + as_pix_to_scroll);
			final JTextField time_intervalTF = new JTextField("" + as_time_interval);
			final JTextField start_posTF = new JTextField("" + as_start_pos);
			final JTextField end_posTF = new JTextField("" + as_end_pos);

			float bases_per_minute = (float) // 1000 ==> ms/s , 60 ==> s/minute, as_time_interval ==> ms/scroll
							(1.0 * as_bases_per_pix * as_pix_to_scroll * 1000 * 60 / as_time_interval);
			bases_per_minute = Math.abs(bases_per_minute);
			float minutes_per_seq = viewseq.getLength() / bases_per_minute;
			final JLabel bases_per_minuteL = new JLabel("" + (bases_per_minute / 1000000));
			final JLabel minutes_per_seqL = new JLabel("" + (minutes_per_seq));

			pan.setLayout(new GridLayout(7, 2));
			pan.add(new JLabel("Resolution (bases per pixel)"));
			pan.add(bases_per_pixTF);
			pan.add(new JLabel("Scroll increment (pixels)"));
			pan.add(pix_to_scrollTF);
			pan.add(new JLabel("Starting base position"));
			pan.add(start_posTF);
			pan.add(new JLabel("Ending base position"));
			pan.add(end_posTF);
			pan.add(new JLabel("Time interval (milliseconds)"));
			pan.add(time_intervalTF);
			pan.add(new JLabel("Megabases per minute:  "));
			pan.add(bases_per_minuteL);
			pan.add(new JLabel("Total minutes for seq:  "));
			pan.add(minutes_per_seqL);

			ActionListener al = new ActionListener() {

				public void actionPerformed(ActionEvent evt) {
					as_bases_per_pix = normalizeTF(bases_per_pixTF, as_bases_per_pix, 1, Integer.MAX_VALUE);
					as_pix_to_scroll = normalizeTF(pix_to_scrollTF, as_pix_to_scroll, -1000, 1000);
					as_time_interval = normalizeTF(time_intervalTF, as_time_interval, 1, 1000);
					as_end_pos = normalizeTF(end_posTF, as_end_pos, 1, getViewSeq().getLength());
					as_start_pos = normalizeTF(start_posTF, as_start_pos, 0, as_end_pos);

					float bases_per_minute = (float) // 1000 ==> ms/s , 60 ==> s/minute, as_time_interval ==> ms/scroll
									(1.0 * as_bases_per_pix * as_pix_to_scroll * 1000 * 60 / as_time_interval);
					bases_per_minute = Math.abs(bases_per_minute);
					//          float minutes_per_seq = viewseq.getLength() / bases_per_minute;
					float minutes_per_seq = (as_end_pos - as_start_pos) / bases_per_minute;
					bases_per_minuteL.setText("" + (bases_per_minute / 1000000));
					minutes_per_seqL.setText("" + (minutes_per_seq));
				}
			};

			bases_per_pixTF.addActionListener(al);
			pix_to_scrollTF.addActionListener(al);
			time_intervalTF.addActionListener(al);
			start_posTF.addActionListener(al);
			end_posTF.addActionListener(al);

			int val = JOptionPane.showOptionDialog(this, pan, "AutoScroll Parameters",
							JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.PLAIN_MESSAGE,
							null, null, null);
			if (val == JOptionPane.OK_OPTION) {
				as_bases_per_pix = normalizeTF(bases_per_pixTF, as_bases_per_pix, 1, Integer.MAX_VALUE);
				as_pix_to_scroll = normalizeTF(pix_to_scrollTF, as_pix_to_scroll, -1000, 1000);
				as_time_interval = normalizeTF(time_intervalTF, as_time_interval, 1, 1000);
				toggleAutoScroll(as_bases_per_pix, as_pix_to_scroll, as_time_interval, as_start_pos, as_end_pos, true);
			}
		} else {
			swing_timer.stop();
			swing_timer = null;
			map_auto_scroller = null;
		}
	}

	// Normalize a text field so that it holds an integer, with a fallback value
	// if there is a problem, and a minimum and maximum
	private static int normalizeTF(JTextField tf, int fallback, int min, int max) {
		int result = fallback;
		try {
			result = Integer.parseInt(tf.getText());
		} catch (NumberFormatException nfe) {
			Toolkit.getDefaultToolkit().beep();
			result = fallback;
		}
		if (result < min) {
			result = min;
		} else if (result > max) {
			result = max;
		}
		tf.setText(Integer.toString(result));
		return result;
	}

	private void toggleAutoScroll(int bases_per_pixel, int pix_to_scroll,
					int timer_interval, final int start_coord, final int end_coord, final boolean cycle) {
		double pix_per_coord = 1.0 / (double) bases_per_pixel;
		final double coords_to_scroll = (double) pix_to_scroll / pix_per_coord;

		seqmap.zoom(NeoAbstractWidget.X, pix_per_coord);
		seqmap.scroll(NeoAbstractWidget.X, start_coord);

		if (map_auto_scroller == null) {
			map_auto_scroller = new ActionListener() {

				public void actionPerformed(ActionEvent evt) {
					Rectangle2D.Double vbox = seqmap.getViewBounds();
					//	    Rectangle2D.Double mbox = resultSeqMap.getCoordBounds();
					int scrollpos = (int) (vbox.x + coords_to_scroll);
					//	    if ((scrollpos + vbox.width) > (mbox.x + mbox.width))  {
					if ((scrollpos + vbox.width) > end_coord) {
						if (cycle) {
							seqmap.scroll(NeoAbstractWidget.X, start_coord);
							seqmap.updateWidget();
						} else {
							// end of sequence reached, so stop scrolling
							swing_timer.stop();
							swing_timer = null;
							map_auto_scroller = null;
						}
					} else {
						seqmap.scroll(NeoAbstractWidget.X, scrollpos);
						seqmap.updateWidget();
					}
				}
			};

			swing_timer = new javax.swing.Timer(timer_interval, map_auto_scroller);
			swing_timer.start();
			// Other options:
			//    java.util.Timer ??
			//    com.affymetrix.genoviz.util.NeoTimerEventClock ??
		} else {
			swing_timer.stop();
			swing_timer = null;
			map_auto_scroller = null;
		}
	}

	public void zoomTo(SeqSpan span) {
		MutableAnnotatedBioSeq zseq = span.getBioSeq();
		if ((zseq instanceof MutableAnnotatedBioSeq) &&
						(zseq != this.getAnnotatedSeq())) {
			gmodel.setSelectedSeq(zseq);
		}
		zoomTo(span.getMin(), span.getMax());
	}

	public void zoomTo(double smin, double smax) {
		double coord_width = smax - smin;
		double pixel_width = seqmap.getView().getPixelBox().width;
		double pixels_per_coord = pixel_width / coord_width; // can be Infinity, but the Math.min() takes care of that
		pixels_per_coord = Math.min(pixels_per_coord, seqmap.getMaxZoom(NeoAbstractWidget.X));
		seqmap.zoom(NeoAbstractWidget.X, pixels_per_coord);
		seqmap.scroll(NeoAbstractWidget.X, smin);
		seqmap.setZoomBehavior(AffyTieredMap.X, AffyTieredMap.CONSTRAIN_COORD, (smin + smax) / 2);
		seqmap.updateWidget();
	}

	/*private void zoomToGlyph(GlyphI gl) {
		if (gl != null) {
			zoomToRectangle(gl.getCoordBox());
		}
	}*/

	/** Zoom to a region including all the currently selected Glyphs. */
	public void zoomToSelections() {
		Vector<GlyphI> selections = seqmap.getSelected();
		if (selections.size() > 0) {
			zoomToRectangle(getRegionForGlyphs(selections));
		} else if (getSelectedRegion() != null) {
			SeqSpan span = getViewSeqSpan(getSelectedRegion());
			zoomTo(span);
		}
	}

	/**
	 * Center at the hairline.
	 */
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

	/** Returns a rectangle containing all the current selections.
	 *  @return null if the vector of glyphs is empty
	 */
	private static Rectangle2D.Double getRegionForGlyphs(List<GlyphI> glyphs) {
		int size = glyphs.size();
		if (size > 0) {
			Rectangle2D.Double rect = new Rectangle2D.Double();
			GlyphI g0 = glyphs.get(0);
			rect.setRect(g0.getCoordBox());
			for (int i = 1; i < size; i++) {
				GlyphI g = glyphs.get(i);
				rect.add(g.getCoordBox());
			}
			return rect;
		} else {
			return null;
		}
	}

	/**
	 *  Zoom to include (and slightly exceed) a given rectangular region in coordbox coords.
	 */
	private void zoomToRectangle(Rectangle2D.Double rect) {
		if (rect != null) {
			double desired_width = Math.min(rect.width * 1.1f, aseq.getLength() * 1.0f);
			seqmap.zoom(NeoAbstractWidget.X, Math.min(
							seqmap.getView().getPixelBox().width / desired_width,
							seqmap.getMaxZoom(NeoAbstractWidget.X)));
			seqmap.scroll(NeoAbstractWidget.X, -(seqmap.getVisibleRange()[0]));
			seqmap.scroll(NeoAbstractWidget.X, (rect.x - rect.width * 0.05));
			seqmap.setZoomBehavior(AffyTieredMap.X, AffyTieredMap.CONSTRAIN_COORD, (rect.x + rect.width / 2));
			seqmap.setZoomBehavior(AffyTieredMap.Y, AffyTieredMap.CONSTRAIN_COORD, (rect.y + rect.height / 2));
			seqmap.updateWidget();
		}
	}

	public void unclamp() {
		if (viewseq instanceof BioSeq) {
			int min = viewseq.getMin();
			int max = viewseq.getMax();
			seqmap.setMapRange(min, max);
		} else {
			seqmap.setMapRange(0, viewseq.getLength());
		}
		seqmap.stretchToFit(false, false);
		seqmap.updateWidget();
	}

	public void clampToView() {
		Rectangle2D.Double vbox = seqmap.getViewBounds();
		seqmap.setMapRange((int) (vbox.x), (int) (vbox.x + vbox.width));
		seqmap.stretchToFit(false, false); // to adjust scrollers and zoomers
		seqmap.updateWidget();
	}

	/** Returns the genome UcscVersion in UCSC two-letter plus number format, like "hg17". */
	private String getUcscGenomeVersion() {
		String ucsc_version = null;
		if (aseq instanceof BioSeq && !slicing_in_effect) {
			SynonymLookup lookup = SynonymLookup.getDefaultLookup();

			String version = aseq.getVersion();
			Collection<String> syns = lookup.getSynonyms(version);

			if (syns == null) {
				syns = new ArrayList<String>();
				syns.add(version);
			}
			for (String syn : syns) {
				// Having to hardwire this check to figure out which synonym to use to match
				//  with UCSC.  Really need to have some way when loading synonyms to specify
				//  which ones should be used when communicating with which external resource!
				//	System.out.println("testing syn: " + syn);
				if (syn.startsWith("hg") || syn.startsWith("mm") ||
								syn.startsWith("rn") || syn.startsWith("ce") || syn.startsWith("dm")) {
					ucsc_version = syn;
				}
			}
		}
		return ucsc_version;
	}

	/**
	 *  Returns the current position in the format used by the UCSC browser.
	 *  This format is also understood by GBrowse and the MapRangeBox of IGB.
	 *  @return a String such as "chr22:15916196-31832390", or null.
	 */
	private String getRegionString() {
		String region = null;
		if (!slicing_in_effect) {
			Rectangle2D.Double vbox = seqmap.getView().getCoordBox();
			int start = (int) vbox.x;
			int end = (int) (vbox.x + vbox.width);
			String seqid = aseq.getID();

			region = seqid + ":" + start + "-" + end;
		}
		return region;
	}

	public void invokeUcscView() {
		// links to UCSC look like this:
		//  http://genome.ucsc.edu/cgi-bin/hgTracks?db=hg11&position=chr22:15916196-31832390
		String UcscVersion = getUcscGenomeVersion();
		String region = getRegionString();

		if (UcscVersion != null && region != null) {
			String ucsc_url = "http://genome.ucsc.edu/cgi-bin/hgTracks?" + "db=" + UcscVersion + "&position=" + region;
			WebBrowserControl.displayURLEventually(ucsc_url);
		} else {
			String genomeVersion = aseq.getID();
			if (aseq instanceof BioSeq) {
				genomeVersion = aseq.getVersion();
			}
			Application.errorPanel("Don't have UCSC information for genome " + genomeVersion);
		}
	}

	/**
	 * Do edge matching.  If query_glyphs is empty, clear all edges.
	 * @param query_glyphs
	 * @param update_map
	 */
	public void doEdgeMatching(List<GlyphI> query_glyphs, boolean update_map) {
		// Clear previous edges
		if (match_glyphs != null && match_glyphs.size() > 0) {
			seqmap.removeItem(match_glyphs);  // remove all match glyphs in match_glyphs vector
		}

		int qcount = query_glyphs.size();
		int match_query_count = query_glyphs.size();
		for (int i = 0; i < qcount && match_query_count <= max_for_matching; i++) {
			match_query_count += query_glyphs.get(i).getChildCount();
		}

		if (match_query_count <= max_for_matching) {
			match_glyphs = new Vector<GlyphI>();
			ArrayList<GlyphI> target_glyphs = new ArrayList<GlyphI>();
			target_glyphs.add(seqmap.getScene().getGlyph());
			double fuzz = getEdgeMatcher().getFuzziness();
			if (fuzz == 0.0) {
				Color edge_match_color = UnibrowPrefsUtil.getColor(UnibrowPrefsUtil.getTopNode(), PREF_EDGE_MATCH_COLOR, default_edge_match_color);
				getEdgeMatcher().setColor(edge_match_color);
			} else {
				Color edge_match_fuzzy_color = UnibrowPrefsUtil.getColor(UnibrowPrefsUtil.getTopNode(), PREF_EDGE_MATCH_FUZZY_COLOR, default_edge_match_fuzzy_color);
				getEdgeMatcher().setColor(edge_match_fuzzy_color);
			}
			getEdgeMatcher().matchEdges(seqmap, query_glyphs, target_glyphs, match_glyphs);
		} else {
			setStatus("Skipping edge matching; too many items selected.");
		}

		if (update_map) {
			seqmap.updateWidget();
		}
	}

	public boolean getEdgeMatching() {
		return show_edge_matches;
	}

	public void setEdgeMatching(boolean b) {
		show_edge_matches = b;
		if (show_edge_matches) {
			doEdgeMatching(seqmap.getSelected(), true);
		} else {
			doEdgeMatching(new Vector<GlyphI>(0), true);
		}
	}

	public void adjustEdgeMatching(int bases) {
		getEdgeMatcher().setFuzziness(bases);
		if (show_edge_matches) {
			doEdgeMatching(seqmap.getSelected(), true);
		}
	}

	/**
	 *  return a SeqSpan representing the visible bounds of the view seq
	 */
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
		SHRINK_WRAP_MAP_BOUNDS = b;
		setAnnotatedSeq(aseq);
	}

	public boolean getShrinkWrap() {
		return SHRINK_WRAP_MAP_BOUNDS;
	}

	/**
	 *  SymSelectionSource interface
	 */
	public void addSymSelectionListener(SymSelectionListener listener) {
		selection_listeners.add(listener);
	}

	public void removeSymSelectionListener(SymSelectionListener listener) {
		selection_listeners.remove(listener);
	}

	/**
	 *  SymSelectionListener interface
	 */
	public void symSelectionChanged(SymSelectionEvent evt) {
		Object src = evt.getSource();
		String src_id = src.getClass().getName() + "@" + Integer.toHexString(src.hashCode());

		// ignore self-generated xym selection -- already handled internally
		if (src == this) {
			if (Application.DEBUG_EVENTS) {
				System.out.println("SeqMapView received selection event originating from itself: " + src_id);
			}
			String title = getSelectionTitle(seqmap.getSelected());
			setStatus(title);
		} // ignore sym selection originating from AltSpliceView, don't want to change internal selection based on this
		else if ((src instanceof AltSpliceView) || (src instanceof SeqMapView)) {
			// catching SeqMapView as source of event because currently sym selection events actually originating
			//    from AltSpliceView have their source set to the AltSpliceView's internal SeqMapView...
			if (Application.DEBUG_EVENTS) {
				System.out.println("SeqMapView received selection event from another SeqMapView: " + src_id);
			}
		} else {
			if (Application.DEBUG_EVENTS) {
				System.out.println("SeqMapView received selection event originating from: " + src_id);
			}
			List<SeqSymmetry> symlist = evt.getSelectedSyms();
			// select:
			//   add_to_previous ==> false
			//   call_listeners ==> false
			//   update_widget ==>  false   (zoomToSelections() will make an updateWidget() call...)
			select(symlist, false, true, false);
			// Zoom to selections, unless the selection was caused by the TierLabelManager
			// (which sets the selection source as the AffyTieredMap, i.e. getSeqMap())
			if (src != getSeqMap()) {
				zoomToSelections();
			}
			String title = getSelectionTitle(seqmap.getSelected());
			setStatus(title);
		}
	}

	/** Sets the hairline position and zoom center to the given spot. Does not call map.updateWidget() */
	public final void setZoomSpotX(double x) {
		int intx = (int) x;
		if (hairline != null) {
			hairline.setSpot(intx);
			showHairlinePositionInStatusBar();
		}
		seqmap.setZoomBehavior(AffyTieredMap.X, AffyTieredMap.CONSTRAIN_COORD, intx);
	}

	/** Sets the hairline position to the given spot. Does not call map.updateWidget() */
	public final void setZoomSpotY(double y) {
		seqmap.setZoomBehavior(AffyTieredMap.Y, AffyTieredMap.CONSTRAIN_COORD, y);
	}

	/** Toggles the hairline between labeled/unlabeled and returns true
	 *  if it ends sup labeled.
	 */
	public boolean toggleHairlineLabel() {
		hairline_is_labeled = !hairline_is_labeled;
		if (hairline != null) {
			Shadow s = hairline.getShadow();
			s.setLabeled(hairline_is_labeled);
			seqmap.updateWidget();
		}
		return hairline_is_labeled;
	}

	public boolean isHairlineLabeled() {
		return hairline_is_labeled;
	}

	private final JMenuItem setUpMenuItem(JPopupMenu menu, String action_command) {
		return setUpMenuItem((Container) menu, action_command, action_listener);
	}

	/**
	 *  Adds a new menu item and sets-up an accelerator key based
	 *  on user prefs.  The accelerator key is registered directly
	 *  to the SeqMapView *and* on the JMenuItem itself: this does
	 *  not seem to cause a conflict.
	 *  @param menu if not null, the new JMenuItem will be added
	 *  to the given Container (perhaps a JMenu or JPopupMenu).
	 *  Use null if you don't want that to happen.
	 */
	public final JMenuItem setUpMenuItem(Container menu, String action_command,
					ActionListener al) {
		JMenuItem mi = new JMenuItem(action_command);
		// Setting accelerator via the MenuUtil.addAccelerator makes it also
		// work when the pop-up menu isn't visible.
		KeyStroke ks = MenuUtil.addAccelerator((JComponent) this,
						al, action_command);
		if (ks != null) {
			// Make the accelerator be visible in the menu item.
			mi.setAccelerator(ks);
		}
		mi.addActionListener(al);
		if (menu != null) {
			menu.add(mi);
		}
		return mi;
	}

	/** Select the parents of the current selections */
	public void selectParents() {
		if (seqmap.getSelected().isEmpty()) {
			Application.errorPanel("Nothing selected");
		} else if (seqmap.getSelected().size() == 1) {
			// one selection: select its parent, not recursively
			selectParents(false);
		} else {
			// multiple selections: select parents recursively
			selectParents(true);
		}
	}

	/** For each current selection, deselect it and select its parent instead.
	 *  @param top_level if true, will select only top-level parents
	 */
	void selectParents(boolean top_level) {
		// copy selections to a new list before starting, because list of selections will be modified
		List<GlyphI> all_selections = new ArrayList<GlyphI>(seqmap.getSelected());
		Iterator<GlyphI> iter = all_selections.iterator();
		while (iter.hasNext()) {
			GlyphI child = iter.next();
			GlyphI pglyph = getParent(child, top_level);
			if (pglyph != child) {
				seqmap.deselect(child);
				seqmap.select(pglyph);
			}
		}

		if (show_edge_matches) {
			doEdgeMatching(seqmap.getSelected(), false);
		}
		seqmap.updateWidget();
		postSelections();
	}

	/** Find the top-most parent glyphs of the given glyphs.
	 *  @param childGlyphs a list of GlyphI objects, typically the selected glyphs
	 *  @return a list where each child is replaced by its top-most parent, if it
	 *  has a parent, or else the child itself is included in the list
	 */
	public static List<GlyphI> getParents(List<GlyphI> childGlyphs) {
		boolean top_level = true;
		// linked hash set keeps parents in same order as child list so that comparison
		// like childList.equals(parentList) can be used.
		java.util.Set<GlyphI> results = new LinkedHashSet<GlyphI>(childGlyphs.size());
		for (GlyphI child : childGlyphs) {
			GlyphI pglyph = getParent(child, top_level);
			results.add(pglyph);
		}
		return new ArrayList<GlyphI>(results);
	}

	/** Get the parent, or top-level parent, of a glyph, with certain restictions.
	 *  Will not return a TierGlyph or RootGlyph or a glyph that isn't hitable, but
	 *  will return the original GlyphI instead.
	 *  @param top_level if true, will recurse up to the top-level parent, with
	 *  certain restrictions: recursion will stop before reaching a TierGlyph
	 */
	public static GlyphI getParent(GlyphI g, boolean top_level) {
		GlyphI result = g;
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
			result = pglyph;
		}
		return result;
	}

	// sets the text on the JLabel based on the current selection
	private void setPopupMenuTitle(JLabel label, List<GlyphI> selected_glyphs) {
		String title = "";
		if (selected_glyphs.size() == 1 && selected_glyphs.get(0) instanceof GraphGlyph) {
			GraphGlyph gg = (GraphGlyph) selected_glyphs.get(0);
			title = gg.getLabel();
		} else {
			title = getSelectionTitle(selected_glyphs);
		}
		// limit the popup title to 30 characters because big popup-menus don't work well
		if (title != null && title.length() > 30) {
			title = title.substring(0, 30) + " ...";
		}
		label.setText(title);
	}

	void showHairlinePositionInStatusBar() {
		if (!report_hairline_position_in_status_bar) {
			return;
		}
		/*if (hairline == null || Application.getSingleton() == null) {
		return;
		}
		String pos = "  " + nformat.format((int) hairline.getSpot()) + "  ";
		Application.getSingleton().setStatusBarHairlinePosition(pos);*/
	}

	void setStatus(String title) {
		if (!report_status_in_status_bar) {
			return;
		}
		showHairlinePositionInStatusBar();
		Application.getSingleton().setStatus(title, false);
	}

	// Compare the code here with SymTableView.selectionChanged()
	// The logic about finding the ID from instances of DerivedSeqSymmetry
	// should be similar in both places, or else users could get confused.
	private String getSelectionTitle(List<GlyphI> selected_glyphs) {
		String id = null;
		if (selected_glyphs.isEmpty()) {
			//id = "No selection";
			id = "";
			sym_used_for_title = null;
		} else {
			if (selected_glyphs.size() == 1) {
				GlyphI topgl = selected_glyphs.get(0);
				Object info = topgl.getInfo();
				SeqSymmetry sym = null;
				if (info instanceof SeqSymmetry) {
					sym = (SeqSymmetry) info;
				}
				if (sym instanceof MutableSingletonSeqSymmetry) {
					id = ((LeafSingletonSymmetry) sym).getID();
					sym_used_for_title = sym;
				}
				if (id == null && sym instanceof SymWithProps) {
					id = (String) ((SymWithProps) sym).getProperty("id");
					sym_used_for_title = sym;
				}
				if (id == null && sym instanceof DerivedSeqSymmetry) {
					SeqSymmetry original = ((DerivedSeqSymmetry) sym).getOriginalSymmetry();
					if (original instanceof MutableSingletonSeqSymmetry) {
						id = ((LeafSingletonSymmetry) original).getID();
						sym_used_for_title = original;
					} else if (original instanceof SymWithProps) {
						id = (String) ((SymWithProps) original).getProperty("id");
						sym_used_for_title = original;
					}
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
				if (id == null) {
					// If ID of item is null, check recursively for parent ID, or parent of that...
					GlyphI pglyph = topgl.getParent();
					if (pglyph != null && !(pglyph instanceof TierGlyph) && !(pglyph instanceof RootGlyph)) {
						// Add one ">" symbol for each level of getParent()
						sym_used_for_title = null; // may be re-set in the recursive call
						id = "> " + getSelectionTitle(Arrays.asList(pglyph));
					} else {
						id = "Unknown Selection";
						sym_used_for_title = null;
					}
				}
			} else {
				sym_used_for_title = null;
				id = "" + selected_glyphs.size() + " Selections";
			}
		}
		if (id == null) {
			id = "";
			sym_used_for_title = null;
		}
		return id;
	}

	/** Prepares the given popup menu to be shown.  The popup menu should have
	 *  items added to it by this method.  Display of the popup menu will be
	 *  handled by showPopup(), which calls this method.
	 */
	protected void preparePopup(JPopupMenu popup) {
		List<GlyphI> selected_glyphs = seqmap.getSelected();

		setPopupMenuTitle(sym_info, selected_glyphs);

		popup.add(sym_info);
		//popup.add(printMI);
		if (!selected_glyphs.isEmpty()) {
			popup.add(zoomtoMI);
		}
		popup.add(centerMI);
		List<SeqSymmetry> selected_syms = getSelectedSyms();
		if (!selected_syms.isEmpty()) {
			popup.add(selectParentMI);
		}
		/*if (selected_syms.size() == 1) {
		popup.add(printSymmetryMI);
		}*/
		if (DEBUG_STYLESHEETS) {
			Action reload_stylesheet = new AbstractAction("Re-load user stylesheet") {

				public void actionPerformed(ActionEvent evt) {
					XmlStylesheetParser.refreshUserStylesheet();
					XmlStylesheetParser.refreshSystemStylesheet();
					default_glyph_factory.setStylesheet(XmlStylesheetParser.getUserStylesheet());
					setAnnotatedSeq(getAnnotatedSeq());
				}
			};

			popup.add(reload_stylesheet);
		}

		for (ContextualPopupListener listener : popup_listeners) {
			listener.popupNotify(popup, selected_syms, sym_used_for_title);
		}
	}

	void showPopup(NeoMouseEvent nevt) {
		sym_popup.setVisible(false); // in case already showing
		sym_popup.removeAll();

		preparePopup(sym_popup);

		if (sym_popup.getComponentCount() > 0) {

			if (nevt == null) {
				// this might happen from pressing the Windows context menu key
				sym_popup.show(seqmap, 15, 15);
				return;
			}

			//      sym_popup.show(resultSeqMap, nevt.getX()+xoffset_pop, nevt.getY()+yoffset_pop);
			// if resultSeqMap is a MultiWindowTierMap, then using resultSeqMap as Component target arg to popup.show()
			//  won't work, since its component is never actually rendered -- so checking here
			/// to use appropriate target Component and pixel position
			EventObject oevt = nevt.getOriginalEvent();
			//      System.out.println("original event: " + oevt);
			if ((oevt != null) && (oevt.getSource() instanceof Component)) {
				Component target = (Component) oevt.getSource();
				if (oevt instanceof MouseEvent) {
					//	  System.out.println("using original event target and coords");
					MouseEvent mevt = (MouseEvent) oevt;
					sym_popup.show(target, mevt.getX() + xoffset_pop, mevt.getY() + yoffset_pop);
				} else {
					//	  System.out.println("using original event target");
					sym_popup.show(target, nevt.getX() + xoffset_pop, nevt.getY() + yoffset_pop);
				}
			} else {
				sym_popup.show(seqmap, nevt.getX() + xoffset_pop, nevt.getY() + yoffset_pop);
			}
		}
		// For garbage collection, it would be nice to add a listener that
		// could call sym_popup.removeAll() when the popup is removed from view.
	}

	public void addPopupListener(ContextualPopupListener listener) {
		popup_listeners.add(listener);
	}

	/*public void removePopupListener(ContextualPopupListener listener) {
	popup_listeners.remove(listener);
	}*/
	public List<ContextualPopupListener> getPopupListeners() {
		return Collections.<ContextualPopupListener>unmodifiableList(popup_listeners);
	}

	/** Recurse through glyphs and collect those that are instanceof GraphGlyph. */
	public List<GlyphI> collectGraphs() {
		ArrayList<GlyphI> graphs = new ArrayList<GlyphI>();
		GlyphI root = seqmap.getScene().getGlyph();
		collectGraphs(root, graphs);
		return graphs;
	}

	/** Recurse through glyph hierarchy and collect graphs. */
	public static void collectGraphs(GlyphI gl, List<GlyphI> graphs) {
		int max = gl.getChildCount();
		for (int i = 0; i < max; i++) {
			GlyphI child = gl.getChild(i);
			if (child instanceof GraphGlyph) {
				graphs.add((GraphGlyph) child);
			}
			if (child.getChildCount() > 0) {
				collectGraphs(child, graphs);
			}
		}
	}

	public GlyphI getPixelFloaterGlyph() {
		PixelFloaterGlyph floater = pixel_floater_glyph;
		Rectangle2D.Double cbox = getSeqMap().getCoordBounds();
		floater.setCoords(cbox.x, 0, cbox.width, 0);

		return floater;
	}

	/**
	 *  Returns a tier for the given IAnnotStyle, creating the tier if necessary.
	 *  Generally called by a Graph Glyph Factory.
	 *  @param tier_direction use {@link TierGlyph#DIRECTION_REVERSE} if you want
	 *  the tier to go below the axis. Other values have no effect.
	 */
	public TierGlyph getGraphTier(IAnnotStyle style, int tier_direction) {
		if (style == null) {
			throw new NullPointerException();
		}

		TierGlyph tier = gstyle2tier.get(style);
		if (tier == null) {
			tier = new TierGlyph(style);
			tier.setDirection(tier_direction);
			setUpTierPacker(tier, true, false);
			gstyle2tier.put(style, tier);
		}

		PackerI pack = tier.getPacker();
		if (pack instanceof CollapsePacker) {
			CollapsePacker cp = (CollapsePacker) pack;
			cp.setParentSpacer(0); // fill tier to the top and bottom edges
			cp.setAlignment(CollapsePacker.ALIGN_CENTER);
		}

		tier.setDirection(tier_direction);
		tier.setLabel(style.getHumanName());
		tier.setFillColor(style.getBackground());
		tier.setForegroundColor(style.getColor());

		if (getSeqMap().getTierIndex(tier) == -1) {
			boolean above_axis = true;
			if (tier_direction == TierGlyph.DIRECTION_REVERSE) {
				above_axis = false;
			} else {
				above_axis = true;
			}
			getSeqMap().addTier(tier, above_axis);
		}
		return tier;
	}

	/**
	 *  Returns a forward and reverse tier for the given method, creating them if they don't
	 *  already exist.
	 *  Generally called by the Glyph Factory.
	 *  Note that this can create empty tiers.  But if the tiers are not filled with
	 *  something, they will later be removed automatically by {@link SeqMapView#setAnnotatedSeq(AnnotatedBioSeq)}.
	 *  @param meth  The tier name; it will be treated as case-insensitive.
	 *  @param next_to_axis Do you want the Tier as close to the axis as possible?
	 *  @param style  a non-null instance of IAnnotStyle; tier label and other properties
	 *   are determined by the IAnnotStyle.
	 *  @return an array of two (not necessarily distinct) tiers, one forward and one reverse.
	 *    The array may instead contain two copies of one mixed-direction tier;
	 *    in this case place glyphs for both forward and revers items into it.
	 */
	public TierGlyph[] getTiers(String meth, boolean next_to_axis, IAnnotStyleExtended style) {
		return getTiers(meth, next_to_axis, style, true);
	}

	/**
	 *  This UcscVersion of getTiers() allows you to specify whether the tier will hold
	 *  glyphs that are all of the same height.  If so, a more efficient packer can
	 *  be used.  Note: if style.isGraphTier() is true, then the given value of
	 *  constant_height will be ignored and re-set to false.
	 */
	public TierGlyph[] getTiers(String meth, boolean next_to_axis, IAnnotStyleExtended style, boolean constant_heights) {
		if (style == null) {
			throw new NullPointerException();
		}
		AffyTieredMap map = this.getSeqMap();

		// try to match up method with tiers...
		TierGlyph fortier = method2ftier.get(meth.toLowerCase());
		TierGlyph revtier = method2rtier.get(meth.toLowerCase());

		if (style.isGraphTier()) {
			constant_heights = false;
		}

		TierGlyph axis_tier = this.getAxisTier();
		if (fortier == null) {
			fortier = makeTierGlyph(style);
			setUpTierPacker(fortier, true, constant_heights);
			method2ftier.put(meth.toLowerCase(), fortier);
		}

		if (style.getSeparate()) {
			fortier.setDirection(TierGlyph.DIRECTION_FORWARD);
		} else {
			fortier.setDirection(TierGlyph.DIRECTION_BOTH);
		}
		fortier.setLabel(style.getHumanName());

		if (map.getTierIndex(fortier) == -1) {
			if (next_to_axis) {
				int axis_index = map.getTierIndex(axis_tier);
				map.addTier(fortier, axis_index);
			} else {
				map.addTier(fortier, true);
			}
		}

		if (revtier == null) {
			revtier = makeTierGlyph(style);
			revtier.setDirection(TierGlyph.DIRECTION_REVERSE);
			setUpTierPacker(revtier, false, constant_heights);
			method2rtier.put(meth.toLowerCase(), revtier);
		}
		revtier.setLabel(style.getHumanName());

		if (map.getTierIndex(revtier) == -1) {
			if (next_to_axis) {
				int axis_index = map.getTierIndex(axis_tier);
				map.addTier(revtier, axis_index + 1);
			} else {
				map.addTier(revtier, false);
			}
		}

		if (style.getSeparate()) {
			return new TierGlyph[]{fortier, revtier};
		} else {
			// put everything in a single tier
			return new TierGlyph[]{fortier, fortier};
		}
	}

	public TierGlyph makeTierGlyph(IAnnotStyle style) {
		return new TierGlyph(style);
	}

	void setUpTierPacker(TierGlyph tg, boolean above_axis, boolean constantHeights) {
		FasterExpandPacker ep = new FasterExpandPacker();
		ep.setConstantHeights(constantHeights);
		if (above_axis) {
			ep.setMoveType(ExpandPacker.UP);
		} else {
			ep.setMoveType(ExpandPacker.DOWN);
		}
		tg.setExpandedPacker(ep);
		tg.setMaxExpandDepth(tg.getAnnotStyle().getMaxDepth());
	}

	public void groupSelectionChanged(GroupSelectionEvent evt) {
		AnnotatedSeqGroup current_group = null;
		AnnotatedSeqGroup new_group = evt.getSelectedGroup();
		if (aseq instanceof BioSeq) {
			current_group = aseq.getSeqGroup();
		}

		if (Application.DEBUG_EVENTS) {
			System.out.println("SeqMapView received seqGroupSelected() call: " + ((new_group != null) ? new_group.getID() : "null"));
		}

		if ((new_group != current_group) && (current_group != null)) {
			//      ViewPersistenceUtils.saveGroupView(this);
			//      ViewPersistenceUtils.saveSeqView(this);
			clear();
		}
	}

	public void seqSelectionChanged(SeqSelectionEvent evt) {
		if (Application.DEBUG_EVENTS) {
			System.out.println("SeqMapView received SeqSelectionEvent, selected seq: " + evt.getSelectedSeq());
		}
		final MutableAnnotatedBioSeq newseq = evt.getSelectedSeq();
		// Don't worry if newseq is null, setAnnotatedSeq can handle that
		// (It can also handle the case where newseq is same as old seq.)

		// trying out not calling setAnnotatedSeq() unless seq that is selected is actually different than previous seq being viewed
		// Maybe should change SingletonGenometryModel.setSelectedSeq() to only fire if seq changes...
		//    if (aseq != newseq) {
		//      setAnnotatedSeq(newseq);
		//    }

		// reverted to calling setAnnotatedSeq regardless of whether newly selected seq is same as previously selected seq,
		//    because often need to trigger repacking / rendering anyway
		setAnnotatedSeq(newseq);
	}

	/** Get the span of the symmetry that is on the seq being viewed. */
	public SeqSpan getViewSeqSpan(SeqSymmetry sym) {
		return sym.getSpan(viewseq);
	}
}

