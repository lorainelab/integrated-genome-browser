/**
*   Copyright (c) 2001-2006 Affymetrix, Inc.
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

import com.affymetrix.igb.util.ErrorHandler;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import java.awt.datatransfer.*;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import com.affymetrix.genoviz.awt.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.widget.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.*;
import com.affymetrix.genometry.symmetry.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.util.SeqUtils;

import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.igb.genometry.SimpleSymWithProps;
import com.affymetrix.igb.genometry.TypedSym;
import com.affymetrix.igb.genometry.GraphSym;
import com.affymetrix.igb.genometry.NibbleBioSeq;
import com.affymetrix.igb.genometry.Versioned;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.menuitem.MenuUtil;
import com.affymetrix.igb.tiers.*;
import com.affymetrix.igb.glyph.*;
import com.affymetrix.igb.event.*;
import com.affymetrix.igb.util.CharIterator;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.igb.util.SynonymLookup;
import com.affymetrix.igb.util.WebBrowserControl;
import com.affymetrix.igb.util.UnibrowControlUtils;
import com.affymetrix.igb.util.ObjectUtils;
import com.affymetrix.igb.genometry.SymWithProps;
import com.affymetrix.igb.genometry.SeqSymStartComparator;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.genometry.SmartAnnotBioSeq;
import com.affymetrix.igb.genometry.TypeContainerAnnot;
import com.affymetrix.igb.event.SeqSelectionListener;
import com.affymetrix.igb.event.GroupSelectionListener;
import com.affymetrix.igb.event.SeqModifiedListener;
import com.affymetrix.igb.event.SeqSelectionEvent;
import com.affymetrix.igb.event.GroupSelectionEvent;
import com.affymetrix.igb.event.SeqModifiedEvent;
import com.affymetrix.igb.parsers.XmlPrefsParser;
import com.affymetrix.igb.das2.Das2FeatureRequestSym;

public class SeqMapView extends JPanel
  implements AnnotatedSeqViewer, SymSelectionSource,
	     SymSelectionListener, SeqSelectionListener, GroupSelectionListener, SeqModifiedListener,
	     ActionListener
{

  static final boolean DIAGNOSTICS = false;
  static final boolean DEBUG_TIERS = false;
  public static boolean DEBUG_COMP = false;
  boolean LABEL_TIERMAP = true;
  boolean SPLIT_WINDOWS = false;  // flag for testing transcriptarium split windows strategy
  boolean SUBSELECT_SEQUENCE = true;  // try to visually select range along seq glyph based on rubberbanding
  boolean show_edge_matches = true;
  boolean rev_comp = false;
  boolean coord_shift = false;
  boolean show_slicendice = false;
  boolean slicing_in_effect = false;
  boolean hairline_is_labeled = false;

  SeqSpan viewspan_before_slicing = null;
  java.util.List popup_listeners = new ArrayList();

  MapViewGlyphFactoryI default_glyph_factory = new GenericAnnotGlyphFactory();

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
  boolean SHRINK_WRAP_MAP_BOUNDS = false;

  /**
   *  booleans for zooming performance testing.
   *  strange results -- seeing a dramatic slowdown in zoom responsiveness when
   *     the xzoomer is put in same JPanel as map, with borderlayout and
   *     xzoomer "North" of map.  And it doesn't matter
   *     whether it's a NeoScrollbar or an AdjustableJSlider, still see the slowdown.
   *  BUT, if xzoomer is put "South" of map, don't see slowdown
   *  AND, if internal xscroller is supressed, then don't see the slowdown even
   *           if xzoomer is "North"
   *  AND, if map is put in a JPanel (with BorderLayout, map at "Center"), and that
   *     JPanel is nested within this (with BorderLayout, map_container_panel at
   *     "Center"), and xzoomer is put in this at "North", don't see slowdown
   */
  boolean NEO_XZOOMER = false;
  boolean NEO_YZOOMER = false;
  boolean INTERNAL_XSCROLLER = true;
  boolean INTERNAL_YSCROLLER = true;
  boolean XZOOMER_IN_MAP_CONTAINER = false;

  JFrame frm;
  AffyTieredMap seqmap;
  UnibrowHairline hairline = null;

  AnnotatedBioSeq aseq;

  /**
   *  a virtual sequence that maps the AnnotatedBioSeq aseq to the map coordinates.
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

  Adjustable xzoomer;
  Adjustable yzoomer;

  //static Color almost_black = new Color(20, 20, 20);

  public static final String PREF_AXIS_LABEL_FORMAT = "Axis label format";

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
  public static final String PREF_DEFAULT_ANNOT_COLOR = "Default annotation color";
  public static final String PREF_DEFAULT_BACKGROUND_COLOR = "Default background color";
  public static final String PREF_EDGE_MATCH_COLOR = "Edge match color";
  public static final String PREF_EDGE_MATCH_FUZZY_COLOR = "Edge match fuzzy color";

  /** Name of a boolean preference for whether the hairline lable should be on. */
  public static final String PREF_HAIRLINE_LABELED = "Hairline Label On";

  /** Name of a boolean preference for whether the horizontal zoom slider is above the map. */
  public static final String PREF_X_ZOOMER_ABOVE = "Horizontal Zoomer Above Map";
  
  /** Name of a boolean preference for whether the vertical zoom slider is left of the map. */
  public static final String PREF_Y_ZOOMER_LEFT = "Vertical Zoomer Left of Map";

  public static final Color default_axis_color = Color.BLACK;
  public static final Color default_axis_background = Color.WHITE;
  public static final String default_axis_label_format = VALUE_AXIS_LABEL_FORMAT_COMMA;
  //public static final Color default_default_annot_color = new Color(192, 192, 114);
  //public static final Color default_default_background_color = Color.BLACK;
  public static final Color default_edge_match_color = Color.WHITE;
  public static final Color default_edge_match_fuzzy_color = new Color(200, 200, 200); // light gray
  public static final boolean default_x_zoomer_above = true;
  public static final boolean default_y_zoomer_left = true;

  static NumberFormat nformat = new DecimalFormat();

  //Color default_annot_color = default_default_annot_color;


  /** Hash of method names (lower case) to forward tiers */
  Map method2ftier = new HashMap();
  /** Hash of method names (lower case) to reverse tiers */
  Map method2rtier = new HashMap();
  /** hash of GraphStates to TierGlyphs,
      ( for those GraphStates where state.getFloatGraph() = false))
  */
  Map gstate2tier = new HashMap();
  Map gname2tier = new HashMap();
  Map gid2tier = new HashMap();
  // gid2state to be added later, for sharing of graph states between multiple GraphSyms
  //     on different seqs but with same graph id
  //     (or maybe this should go in GenericGraphGlyphFactory?)
  //  Map gid2state = new HashMap();

  Map meth2factory = (Map)IGB.getIGBPrefs().get(XmlPrefsParser.MATCH_FACTORIES);
  Map regex2factory = (Map)IGB.getIGBPrefs().get(XmlPrefsParser.REGEX_FACTORIES);
  Map graf2factory = new HashMap();   // hash of graph syms to graph factories
  Map grafid2factory = new HashMap();

  //  Color[] tier_colors = { Color.black, almost_black };
  GlyphEdgeMatcher edge_matcher = null;

  JPopupMenu sym_popup;
  JMenu sym_menu;
  JLabel sym_info;

  JTextField bases_per_pixelTF = new JTextField(10);
  JTextField bases_in_viewTF = new JTextField(10);

  // A fake menu item, prevents null pointer exceptions in actionPerformed()
  // for menu items whose real definitions are commented-out in the code
  private static final JMenuItem empty_menu_item = new JMenuItem("");
  //  JMenuItem copyAsCurationMI;
  JMenuItem zoomtoMI = empty_menu_item;
  JMenuItem printMI = empty_menu_item;
  JMenuItem zoomclampMI = empty_menu_item;
  JMenuItem selectParentMI = empty_menu_item;
  JMenuItem renumberMinMI = empty_menu_item;
  JMenuItem renumberMaxMI = empty_menu_item;
  JMenuItem revertCoordsMI = empty_menu_item;
  JMenuItem printSymmetryMI = empty_menu_item;
  JMenuItem slicendiceMI = empty_menu_item;

  // for right-click on background
  JMenuItem renumberMI = empty_menu_item;

  private final ActionListener action_listener;
  private final SeqMapViewMouseListener mouse_listener;

  CharSeqGlyph seq_glyph = null;

  SeqSymmetry seq_selected_sym = null;  // symmetry representing selected region of sequence
  Vector match_glyphs = new Vector();
  Vector selection_listeners = new Vector();
  TierLabelManager tier_manager;
  PixelFloaterGlyph grid_layer = null;
  GridGlyph grid_glyph = null;
  
  Box xzoombox;
  Box yzoombox;

  /** If true, remove empty tiers from map, but not from method2ftier and method2rtier,
   *  when changing sequence.  Thus generally remembers the relative ordering of tiers.
   */
  boolean remember_tiers = true;


  SingletonGenometryModel gmodel = IGB.getGenometryModel();

  /** Constructor. By default, does not add popup menu items. */
  public SeqMapView() {
    this(false);
  }

  public SeqMapView(boolean add_popups) {
    this(add_popups, false);
  }

  /**
   * Constructor.
   * @param add_popups  Whether to add some popup menus to the tier label manager
   *  that control tier hiding and collapsing and so forth.  It is probably best
   *  NOT to set this to true in any view other than the main view; it should
   *  be false in the AltSpliceView, for instance.
   */
  public SeqMapView(boolean add_popups, boolean split_win) {
    SPLIT_WINDOWS = split_win;
    if (SPLIT_WINDOWS) { LABEL_TIERMAP = false; }

    if (SPLIT_WINDOWS) {
      seqmap = new MultiWindowTierMap(false, false);
    }
    else if (LABEL_TIERMAP) {
      seqmap = new AffyLabelledTierMap(INTERNAL_XSCROLLER, INTERNAL_YSCROLLER);
      NeoMap label_map = ((AffyLabelledTierMap)seqmap).getLabelMap();
      label_map.setSelectionAppearance( SceneI.SELECT_OUTLINE );
    }
    else {
      seqmap = new AffyTieredMap(INTERNAL_XSCROLLER, INTERNAL_YSCROLLER);
    }

    //Color bg = default_default_background_color;
    //Color bg = UnibrowPrefsUtil.getColor(UnibrowPrefsUtil.getTopNode(), PREF_DEFAULT_BACKGROUND_COLOR, default_default_background_color);
    //Color bg = AnnotStyle.getDefaultInstance().getBackground();
    Color bg = Color.BLACK; // the map background needs to be a very dark color, or else the
      // hairline won't display very well, because it uses XOR based on this color, and
      // BLACK produces the best contrast for arbitrary tier and annotation colors.
    seqmap.setMapColor(bg);

    edge_matcher = GlyphEdgeMatcher.getSingleton();

    action_listener = new SeqMapViewActionListener();
    mouse_listener = new SeqMapViewMouseListener(this);

    //    map.setScrollingOptimized(true);
    seqmap.getNeoCanvas().setDoubleBuffered(false);
    //    map.getLabelMap().getNeoCanvas().setDoubleBuffered(false);

    seqmap.setReshapeBehavior(seqmap.X, seqmap.NONE);
    seqmap.setScrollIncrementBehavior(seqmap.X, seqmap.AUTO_SCROLL_HALF_PAGE);

    if (NEO_XZOOMER) {
      xzoomer = new NeoScrollbar(NeoScrollbar.HORIZONTAL);
      ((NeoScrollbar) xzoomer).setSendEvents(true);
    }
    else { xzoomer = new AdjustableJSlider(Adjustable.HORIZONTAL); }
    if (NEO_YZOOMER) {
      yzoomer = new NeoScrollbar(NeoScrollbar.VERTICAL);
      ((NeoScrollbar) yzoomer).setSendEvents(true);
    }
    else { yzoomer = new AdjustableJSlider(Adjustable.VERTICAL); }
    seqmap.setZoomer(NeoMap.X, xzoomer);
    seqmap.setZoomer(NeoMap.Y, yzoomer);

    if (LABEL_TIERMAP)  {
      tier_manager = new TierLabelManager((AffyLabelledTierMap)seqmap);
      if (add_popups) {
        tier_manager.addPopupListener(new TierArithmetic(tier_manager, this));
        //TODO: tier_manager.addPopupListener(new CurationPopup(tier_manager, this));
        tier_manager.addPopupListener(new SeqMapViewPopup(tier_manager, this));
      }
    }
    seqmap.setSelectionAppearance( SceneI.SELECT_OUTLINE );
    seqmap.addMouseListener(mouse_listener);
    SmartRubberBand srb = new SmartRubberBand(seqmap);
    seqmap.setRubberBand(srb);
    seqmap.addRubberBandListener(mouse_listener);
    srb.setColor(new Color(100, 100, 255));

    GraphSelectionManager graph_manager = new GraphSelectionManager(this);
    seqmap.addMouseListener(graph_manager);
    this.addPopupListener(graph_manager);

    setupPopups();
    this.setLayout(new BorderLayout());

    xzoombox = Box.createHorizontalBox();
    //    xzoombox.add(new SeqComboBoxView());

    xzoombox.add(new JLabel("bases per pixel:"));
    bases_per_pixelTF.setMaximumSize(new Dimension(10, 20));
    bases_per_pixelTF.addActionListener(this);
    xzoombox.add(bases_per_pixelTF);
    xzoombox.add(new JLabel("bases in view:"));
    bases_in_viewTF.setMaximumSize(new Dimension(10, 20));
    bases_in_viewTF.addActionListener(this);
    seqmap.addViewBoxListener(new NeoViewBoxListener() {
	public void viewBoxChanged(NeoViewBoxChangeEvent evt) {
	  Rectangle2D vbox = evt.getCoordBox();
	  double bases_in_view = vbox.width;
	  //	  System.out.println("map coord width: " + seqmap.getScene().getCoordBox().width + ", view width: " + vbox.width);
	  bases_in_viewTF.setText(nformat.format(bases_in_view));
	  int pixel_width = seqmap.getView().getPixelBox().width;
	  double bases_per_pixel = (double)bases_in_view / (double)pixel_width;
	  bases_per_pixelTF.setText(nformat.format(bases_per_pixel));
	}
      } );
    xzoombox.add(bases_in_viewTF);
    xzoombox.add((Component) xzoomer);
    boolean x_above = UnibrowPrefsUtil.getBooleanParam(PREF_X_ZOOMER_ABOVE, default_x_zoomer_above);
    if (x_above) {
      this.add(BorderLayout.NORTH, xzoombox);
    } else {
      this.add(BorderLayout.SOUTH, xzoombox);      
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
    if (SPLIT_WINDOWS) {
      // don't display map if SPLIT_WINDOWS, display is via multiple separate windows controlled by seqmap
      NeoScrollbar xscroller = new NeoScrollbar(NeoScrollbar.HORIZONTAL);
      NeoScrollbar yscroller = new NeoScrollbar(NeoScrollbar.VERTICAL);
      //      xscroller.setSendEvents(true);  // not sure if this is necessary or desired
      //      yscroller.setSendEvents(true);  // not sure if this is necessary or desired
      seqmap.setRangeScroller(xscroller);
      seqmap.setOffsetScroller(yscroller);
      JPanel scrollP = new JPanel();
      scrollP.setLayout(new BorderLayout());
      scrollP.add("South", xscroller);
      scrollP.add("West", yscroller);
      this.add("Center", scrollP);
    }
    else {
      this.add("Center", seqmap);
    }
    LinkControl link_control = new LinkControl();
    this.addPopupListener(link_control);

    UnibrowPrefsUtil.getTopNode().addPreferenceChangeListener(pref_change_listener);
  }



    // This preference change listener can reset some things, like whether
    // the axis uses comma format or not, in response to changes in the stored
    // preferences.  Changes to axis, and other tier, colors are not so simple,
    // in part because of the need to coordinate with the label glyphs.

  PreferenceChangeListener pref_change_listener = new PreferenceChangeListener() {

      public void preferenceChange(PreferenceChangeEvent pce) {
        if (getAxisTier() == null) { return; }

        if (! pce.getNode().equals(UnibrowPrefsUtil.getTopNode())) {
          return;
        }

        TransformTierGlyph axis_tier = getAxisTier();
        Vector children = axis_tier.getChildren();

        /*  Reseting axis tier color isn't ready for prime time yet.
        if (pce.getKey().equals(PREF_AXIS_BACKGROUND)) {
          Color c = UnibrowPrefsUtil.getColor(UnibrowPrefsUtil.getTopNode(), PREF_AXIS_BACKGROUND, default_axis_background);
          axis_tier.setBackgroundColor(c);
          for (int i=0; i<children.size(); i++) {
            ((Glyph) children.get(i)).setBackgroundColor(c);
          }
          map.updateWidget();
          //System.out.println("Setting axis background: "+c);
        }
        else if (pce.getKey().equals(PREF_AXIS_COLOR)) {
          Color c = UnibrowPrefsUtil.getColor(UnibrowPrefsUtil.getTopNode(), PREF_AXIS_COLOR, default_axis_color);
          axis_tier.setForegroundColor(c);
          for (int i=0; i<children.size(); i++) {
            ((Glyph) children.get(i)).setForegroundColor(c);
          }
          map.updateWidget();
          //System.out.println("Setting axis color: "+c);
        }
        */

        if (pce.getKey().equals(PREF_AXIS_LABEL_FORMAT)) {
          String axis_format = UnibrowPrefsUtil.getTopNode().get(PREF_AXIS_LABEL_FORMAT, default_axis_label_format);
          AxisGlyph ag = null;
          for (int i=0; i<children.size(); i++) {
            if (children.get(i) instanceof AxisGlyph) {ag = (AxisGlyph) children.get(i);}
          }
          if (ag != null) {
            if (VALUE_AXIS_LABEL_FORMAT_COMMA.equalsIgnoreCase(axis_format)) {
              ag.setLabelFormat(AxisGlyph.COMMA);
            } else if (VALUE_AXIS_LABEL_FORMAT_FULL.equalsIgnoreCase(axis_format)) {
              ag.setLabelFormat(AxisGlyph.FULL);
            } else if (VALUE_AXIS_LABEL_FORMAT_NO_LABELS.equalsIgnoreCase(axis_format))  {
	      ag.setLabelFormat(AxisGlyph.NO_LABELS);
	    }
	    else {
              ag.setLabelFormat(AxisGlyph.ABBREV);
            }
          }
          seqmap.updateWidget();
        }

        else if (pce.getKey().equals(PREF_EDGE_MATCH_COLOR) || pce.getKey().equals(PREF_EDGE_MATCH_FUZZY_COLOR)) {
          if (show_edge_matches)  {
            doEdgeMatching(seqmap.getSelected(), true);
          }
        }
        
        else if (pce.getKey().equals(PREF_X_ZOOMER_ABOVE)) {
          boolean b = UnibrowPrefsUtil.getBooleanParam(PREF_X_ZOOMER_ABOVE, default_x_zoomer_above);
          SeqMapView.this.remove(xzoombox);
          if (b) {
            SeqMapView.this.add(BorderLayout.NORTH, xzoombox);
          } else {
            SeqMapView.this.add(BorderLayout.SOUTH, xzoombox);
          }
          SeqMapView.this.invalidate();
        }
        
        else if (pce.getKey().equals(PREF_Y_ZOOMER_LEFT)) {
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

  public JFrame getFrame() { return frm; }

  public void setupPopups() {
    sym_popup = new JPopupMenu();
    sym_info = new JLabel("");
    sym_info.setEnabled(false); // makes the text look different (usually lighter)

    zoomtoMI = setUpMenuItem(sym_popup, "Zoom to selected");
    zoomtoMI.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/Zoom16.gif"));
    selectParentMI = setUpMenuItem(sym_popup, "Select parent");
    printSymmetryMI = setUpMenuItem(sym_popup, "Print symmetry");
    if (show_slicendice) {
      slicendiceMI = setUpMenuItem(sym_popup, "Slice and dice");
    }

    String[] commands = { "ZOOM_OUT_FULLY",
      "ZOOM_OUT_X", "ZOOM_IN_X", "ZOOM_OUT_Y", "ZOOM_IN_Y",
      "SCROLL_UP", "SCROLL_DOWN", "SCROLL_RIGHT", "SCROLL_LEFT"};
    for (int i=0; i<commands.length; i++) {
      MenuUtil.addAccelerator((JComponent) this, action_listener, commands[i]);
    }
  }

  public JPopupMenu getSelectionPopup() { return sym_popup; }

  Map getFactoryHash() { return meth2factory; }
  Map getForwardTierHash() { return method2ftier; }
  Map getReverseTierHash() { return method2rtier; }

  /** A Map of GraphState to TierGlyph */
  public Map getGraphStateTierHash() { return gstate2tier; }
  public Map getGraphFactoryHash() { return graf2factory; }
  public Map getGraphIdFactoryHash() { return grafid2factory; }

  public Map getGraphNameTierHash() { return gname2tier; }
  public Map getGraphIdTierHash() { return gid2tier; }

  TransformTierGlyph axis_tier;

  /** An un-collapsible instance.  It is hideable, though. */
  AnnotStyle axis_annot_style = new AnnotStyle() {
    //public void setShow(boolean b) {}
    public void setSeparate(boolean b) { /* do nothing */ }
    public void setCollapsed(boolean b) { /* do nothing */ }
    public void setColor(Color c) {}
    public void setBackground(Color c) {}
    public void setHumanName(String s) {}
    public void setLabelField(String s) {}
    public void setMaxDepth(int i) {}
  };

  public TransformTierGlyph getAxisTier() { return axis_tier; }

  /** Set up a tier with fixed pixel height and place axis in it. */
  TransformTierGlyph addAxisTier(int tier_index) {

    IAnnotStyle blank_style = axis_annot_style;

    axis_tier = new TransformTierGlyph(blank_style);
    axis_tier.setLabel("Coordinates");
    axis_tier.setFixedPixelHeight(true);
    axis_tier.setFixedPixHeight(45);
    //    axis_tier.setFixedPixelHeight(false);
    AxisGlyph axis = seqmap.addAxis(0);
    axis.setHitable(false);

    Color axis_bg = UnibrowPrefsUtil.getColor(UnibrowPrefsUtil.getTopNode(), PREF_AXIS_BACKGROUND, default_axis_background);
    Color axis_fg = UnibrowPrefsUtil.getColor(UnibrowPrefsUtil.getTopNode(), PREF_AXIS_COLOR, default_axis_color);

    String axis_format = UnibrowPrefsUtil.getTopNode().get(PREF_AXIS_LABEL_FORMAT, VALUE_AXIS_LABEL_FORMAT_COMMA);

    axis.setBackgroundColor(axis_bg);
    axis_tier.setBackgroundColor(axis_bg);
    axis_tier.setFillColor(axis_bg);
    axis.setForegroundColor(axis_fg);
    axis_tier.setForegroundColor(axis_fg);
    if (VALUE_AXIS_LABEL_FORMAT_COMMA.equalsIgnoreCase(axis_format)) {
      axis.setLabelFormat(AxisGlyph.COMMA);
    } else if (VALUE_AXIS_LABEL_FORMAT_FULL.equalsIgnoreCase(axis_format)) {
      axis.setLabelFormat(AxisGlyph.FULL);
    } else if (VALUE_AXIS_LABEL_FORMAT_NO_LABELS.equalsIgnoreCase(axis_format)) {
      axis.setLabelFormat(AxisGlyph.NO_LABELS);
    } else {
      axis.setLabelFormat(AxisGlyph.ABBREV);
    }

    axis_tier.addChild(axis);

    // it is important to set the colors before adding the tier
    // to the map, else the label tier colors won't match
    if (seqmap.getTiers().size() >= tier_index) {
      seqmap.addTier(axis_tier, tier_index);
    }
    else {
      seqmap.addTier(axis_tier);
    }
    seq_glyph = new CharSeqGlyph();
    seq_glyph.setForegroundColor(axis_fg);
    seq_glyph.setShowBackground(false);
    seq_glyph.setHitable(false);
    seq_glyph.setDrawOrder(Glyph.DRAW_CHILDREN_FIRST);
//    seq_glyph.setCoords(viewseq.getMin(), 0, viewseq.getLength(), 10);
    if (viewseq instanceof CompositeNegSeq) {
      CompositeNegSeq compseq = (CompositeNegSeq)viewseq;
      seq_glyph.setCoords(compseq.getMin(), 0, compseq.getLengthDouble(), 10);
    }
    else {
      seq_glyph.setCoords(0, 0, viewseq.getLength(), 10);
    }
    //    System.out.println("seq glyph coords: " + seq_glyph.getCoordBox());

    axis_tier.addChild(seq_glyph);

      // need to change this to get residues from viewseq! (to take account of reverse complement,
      //    coord shift, slice'n'dice, etc.
      // but first, need to fix CompositeBioSeq.isComplete() implementations...
      if (viewseq instanceof CharIterator)  {
	// currently only NibbleBioSeq implements CharacterIterator
	seq_glyph.setResiduesProvider((CharIterator)viewseq, viewseq.getLength());
      }
      else {
	String residues = viewseq.getResidues();
        if (residues != null)  {
          seq_glyph.setResidues(residues);
        }
      }
      if (viewseq instanceof CompositeBioSeq) {
	SeqSymmetry compsym = ((CompositeBioSeq)viewseq).getComposition();
	if (compsym != null) {
	  int compcount = compsym.getChildCount();
	  for (int i=0; i<compcount; i++) {
            // Make glyphs for contigs
	    SeqSymmetry childsym = compsym.getChild(i);
	    SeqSpan childspan = childsym.getSpan(viewseq);
	    SeqSpan ospan = SeqUtils.getOtherSpan(childsym, childspan);

	    GlyphI cgl;
	    if (ospan.getBioSeq().isComplete(ospan.getMin(), ospan.getMax())) {
	      cgl = new FillRectGlyph();
	      cgl.setColor(Color.lightGray);
	    }
	    else {
	      if (viewseq.getID().equals(QuickLoadView2.GENOME_SEQ_ID)) {
		// hide axis numbering
		axis.setLabelFormat(AxisGlyph.NO_LABELS);
		cgl = new com.affymetrix.igb.glyph.LabelledRectGlyph();
		String label = ospan.getBioSeq().getID();
		if (label.startsWith("chr")) { label = label.substring(3); }
		((com.affymetrix.igb.glyph.LabelledRectGlyph)cgl).setLabel(label);
		cgl.setColor(Color.black);
	      }
	      else if (viewseq.getID().equals(QuickLoadView2.ENCODE_REGIONS_ID)) {
                cgl = new com.affymetrix.igb.glyph.LabelledRectGlyph();
                // cgl = new com.affymetrix.genoviz.glyph.LabelledRectGlyph();
		String label = childsym.getID();
		if (label != null) {
		  ((com.affymetrix.igb.glyph.LabelledRectGlyph)cgl).setLabel(label);
		  // ((com.affymetrix.genoviz.glyph.LabelledRectGlyph)cgl).setText(label);
		}
		cgl.setColor(Color.black);
	      }
	      else {
		cgl = new OutlineRectGlyph();
		cgl.setColor(Color.lightGray);
	      }
	    }
	    //	    cgl.setCoords(childspan.getMin(), 0,
	    //			  childspan.getMax()-childspan.getMin(), 10);
	    cgl.setCoords(childspan.getMinDouble(), 0, childspan.getLengthDouble(), 10);
	    // System.out.println("comp coords: " + cgl.getCoordBox());

            // calling cgl.setInfo()
            //   allows info to be seen in selection table
            //   allows easily selecting sequence for contig
            //   allows slicing on contig
            //   Slicing may require lots of memory, so this may be a bad idea.
            //cgl.setInfo(childsym);

            // also note that "Load residues in view" produces additional
            // contig-like glyphs that can partially hide these glyphs.
	    seq_glyph.addChild(cgl);
	  }
	}
      }
    seqmap.repack();
    return axis_tier;
  }


  public void reverseComplement() {
    // still need to deal with residues?
    // also need to deal with graphs
    rev_comp = ! rev_comp;
    setAnnotatedSeq(aseq);
  }


  public void clear() {
    seqmap.clearWidget();
    aseq = null;
    clearSelection();
    method2rtier = new HashMap();
    method2ftier = new HashMap();
    gstate2tier = new HashMap();
    gname2tier = new HashMap();
    gid2tier = new HashMap();
    match_glyphs = new Vector();
    seqmap.updateWidget();
    GenericGraphGlyphFactory.clear();
  }

  /* //TODO
   *  GAH 3-20-2003
   *  WARNING
   *  really need to fix some underlying GenoViz issues for this to be effective in
   *    actually reclaiming memory from graphs:
   *  Specifically, NeoMap.removeItem(GlyphI gl) need to recursively remove child glyphs from
   *     objects such as the Hashtable in NeoMap that maps data models to glyphs
   *  Also, should really be removing not just GraphGlyphs (and their parent
   *     PixelFloaterGlyphs and TierGlyphs), but also should remove GraphSyms from
   *     AnnotatedBioSeq, which currently I'm not doing
   */
  /**
   *  Clears the graphs, and reclaims some memory.
   */
  public void clearGraphs() {
    if (aseq instanceof MutableAnnotatedBioSeq) {
      MutableAnnotatedBioSeq mseq = (MutableAnnotatedBioSeq)aseq;
      int acount = mseq.getAnnotationCount();
      for (int i=acount-1; i>=0; i--) {
	SeqSymmetry annot = mseq.getAnnotation(i);
	if (annot instanceof GraphSym) {
	  mseq.removeAnnotation(annot);
	}
      }
    }
    else {
      System.err.println("Current annotated seq is not mutable, cannot call SeqMapView.clearGraphs()!");
    }

    //Make sure the graph is un-selected in the genometry model, to allow GC
    gmodel.clearSelectedSymmetries(this);
    setAnnotatedSeq(aseq, false, true);
  }

  /** Sets the sequence; if null, has the same effect as calling clear(). */
  public void setAnnotatedSeq(AnnotatedBioSeq seq) {
    setAnnotatedSeq(seq, false, false);
  }

  /**
   *   Sets the sequence.  If null, has the same effect as calling clear().
   *<pre>
   *   want to optimize for several situations:
   *       a) merging newly loaded data with existing data (adding more annotations to
   *           existing AnnotatedBioSeq) -- would like to avoid recreation and repacking
   *           of already glyphified annotations
   *       b) reverse complementing existing AnnotatedBioSeq
   *       c) coord shifting existing AnnotatedBioSeq
   *   in all these cases:
   *       "new" AnnotatedBioSeq == old AnnotatedBioSeq
   *       existing glyphs could be reused (in (b) they'd have to be "flipped")
   *       should preserve selection
   *       should preserve view (x/y scale/offset) (in (b) would preserve "flipped" view)
   *   only some of the above optimization/preservation are implemented yet
   *   WARNING: currently graphs are not properly displayed when reverse complementing,
   *               need to "genometrize" them
   *            currently sequence is not properly displayed when reverse complementing
   *
   *</pre>
   *   @param preserve_selection  if true, then try and keep same selection
   *   @param preserve_view  if true, then try and keep same scroll and zoom / scale and offset...
   */
  public void setAnnotatedSeq(AnnotatedBioSeq seq, boolean preserve_selection, boolean preserve_view) {
    RepaintManager rm = RepaintManager.currentManager(this);
    Image bufimg = rm.getOffscreenBuffer(this,
					 this.getSize().width,
					 this.getSize().height);
    VolatileImage vbufimg = (VolatileImage)rm.getVolatileOffscreenBuffer(this,
						  this.getSize().width,
						  this.getSize().height);

    if (DIAGNOSTICS) {
      System.out.println("RepaintManager offscreen buffer: " + bufimg);
      System.out.println("RepaintManager volatile offscreen buffer: " + vbufimg);
      System.out.println("offscreen buffer is VolatileImage: " +
			 (bufimg instanceof VolatileImage));
      System.out.println("volatile offscreen buffer is VolatileImage: " +
			 (vbufimg instanceof VolatileImage));
    }

    if (frm != null) {
      String title = null;
      if (seq == null) {
        title = IGB.APP_NAME;
      } else {
        String version_info = getVersionInfo(seq);
        if (version_info == null) {
          title = IGB.APP_NAME + ":      " + seq.getID();
        }
        else {
          title = IGB.APP_NAME + ":      " + seq.getID() + "  (" + version_info + ")";
        }
      }
      frm.setTitle(title);
    }

    if (seq == null) {
      clear();
      return;
    }

    com.affymetrix.genoviz.util.Timer tim = new com.affymetrix.genoviz.util.Timer();
    tim.start();

    boolean same_seq = ((seq == this.aseq) && (seq != null));

    ArrayList temp_tiers = null;
    int axis_index = 0;
    boolean axis_was_hidden = false;
    match_glyphs = new Vector();
    java.util.List old_selections = Collections.EMPTY_LIST;
    double old_zoom_spot_x = seqmap.getZoomCoord(seqmap.X);
    double old_zoom_spot_y = seqmap.getZoomCoord(seqmap.Y);

    if (same_seq) {
      // Gather information about what is currently selected, so can restore it later
      if (preserve_selection) {
        old_selections = getSelectedSyms();
      } else {
        old_selections = Collections.EMPTY_LIST;
      }
    }

    if (same_seq || remember_tiers) {

      // stash annotation tiers for proper state restoration after resetting for same seq
      //    (but presumably added / deleted / modified annotations...)

      temp_tiers = new ArrayList();
      // copying map tiers to separate list to avoid problems when removing tiers
      //   (and thus modifying map.getTiers() list -- could probably deal with this
      //    via iterators, but feels safer this way...)
      ArrayList cur_tiers = new ArrayList(seqmap.getTiers());
      for (int i=0; i<cur_tiers.size(); i++) {
        TierGlyph tg = (TierGlyph)cur_tiers.get(i);
        if (tg == axis_tier) {
          if (DEBUG_TIERS)  { System.out.println("removing axis tier from temp_tiers"); }
          axis_index = i;
          axis_was_hidden = (axis_tier.getState() == TierGlyph.HIDDEN);
        }
        else {
          tg.removeAllChildren();
          temp_tiers.add(tg);
          if (DEBUG_TIERS)  { System.out.println("removing tier from map: " + tg.getLabel()); }
          seqmap.removeTier(tg);
        }
      }
    } else {
      method2rtier = new HashMap();
      method2ftier = new HashMap();
      gstate2tier = new HashMap();
      gname2tier = new HashMap();
      gid2tier = new HashMap();
    }

    seqmap.clearWidget();
    seqmap.clearSelected(); // may already be done by map.clearWidget()

    aseq = seq;

    // if shifting coords, then seq2viewSym and viewseq are already taken care of,
    //   but reset coord_shift to false...
    if (coord_shift) {
      // map range will probably change after this if SHRINK_WRAP_MAP_BOUNDS is set to true...
      //      map.setMapRange(viewseq.getMin(), viewseq.getMax());
      //      seqmap.setMapRange(0, viewseq.getLength());
      coord_shift = false;
    }
    else {
      if (rev_comp) {
	// setting up genometry for non-identity mapping of seq annotation to view coords...
	seq2viewSym = new SimpleMutableSeqSymmetry();
	seq2viewSym.addSpan(new SimpleSeqSpan(0, aseq.getLength(), aseq));
	if (aseq instanceof NibbleBioSeq && aseq.isComplete()) {
	  // test for aseq.isComplete() --> if true, then has nibble array...
	  viewseq = ((NibbleBioSeq)aseq).getReverseComplement();
	}
	else {
	  viewseq = new CompositeNegSeq("view_seq", 0, aseq.getLength());
	}
	seq2viewSym.addSpan(new SimpleSeqSpan(aseq.getLength(), 0, viewseq));
	((SimpleCompositeBioSeq)viewseq).setComposition(seq2viewSym);
  	transform_path = new SeqSymmetry[1];
	transform_path[0] = seq2viewSym;
      }
      else {
        viewseq = aseq;
        seq2viewSym = null;
        transform_path = null;
      }
    }
    int seq_min;
    int seq_max;
    if (viewseq instanceof CompositeNegSeq) {
      seq_min = ((CompositeNegSeq)viewseq).getMin();
      seq_max = ((CompositeNegSeq)viewseq).getMax();
    }
    else {
      seq_min = 0;
      seq_max = viewseq.getLength();
    }

    seqmap.setMapRange(seq_min, seq_max);

    // The hairline needs to be among the first glyphs added,
    // to keep it from interfering with selection of other glyphs.
    if (hairline != null) { hairline.destroy(); }
    hairline = new UnibrowHairline(seqmap);
    hairline.getShadow().setLabeled(hairline_is_labeled);

    // add back in previous annotation tiers (with all children removed)
    if (temp_tiers != null) {
      if (DEBUG_TIERS)  {
	System.out.println("same seq, trying to add back old tiers (after removing children)");
      }
      for (int i=0; i<temp_tiers.size(); i++) {
	TierGlyph tg = (TierGlyph)temp_tiers.get(i);
	if (DEBUG_TIERS)  {
	  System.out.println("adding back tier: " + tg.getLabel() + ", scene = " + tg.getScene());
	}
        // Reset tier properties: this is mainly needed to reset the background color
        if (tg.getAnnotStyle() != null) {tg.setStyle(tg.getAnnotStyle());}

        seqmap.addTier(tg);
      }
    }
    temp_tiers.clear(); // redundant hint to garbage collection

    TransformTierGlyph at = addAxisTier(axis_index);
    if (axis_was_hidden) {at.setState(TierGlyph.HIDDEN);}
    addAnnotationTiers();
    removeEmptyTiers();
    //map.sortTiers();

    seqmap.repack();

    if (same_seq && preserve_selection) {
      // reselect glyph(s) based on selected sym(s);
      // Unfortunately, some previously selected syms will not be directly
      // associatable with new glyphs, so not all selections can be preserved
      Iterator iter = old_selections.iterator();
      while (iter.hasNext()) {
        SeqSymmetry old_selected_sym = (SeqSymmetry) iter.next();

	GlyphI gl = (GlyphI)seqmap.getItem(old_selected_sym);
	if (gl != null) {
	  seqmap.select(gl);
	}
      }
      setZoomSpotX(old_zoom_spot_x);
      setZoomSpotY(old_zoom_spot_y);

      doEdgeMatching(seqmap.getSelected(), false);

    } else {
      // do selection based on what the genometry model thinks is selected
      java.util.List symlist = gmodel.getSelectedSymmetries(seq);
      select(symlist,false,false,false);

      String title = getSelectionTitle(seqmap.getSelected());
      IGB.getSingletonIGB().setStatus(title, false);

      doEdgeMatching(seqmap.getSelected(), false);
    }

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
	System.out.println("annot bounds: " + annot_bounds.getMin() +
			   ", " + annot_bounds.getMax());
	// transform to view
	MutableSeqSymmetry sym = new SimpleMutableSeqSymmetry();
	sym.addSpan(annot_bounds);
	if (aseq != viewseq) {
	  //	  SeqUtils.transformSymmetry(sym, seq2viewSym);
	  SeqUtils.transformSymmetry(sym, transform_path);
	}
	SeqSpan view_bounds = sym.getSpan(viewseq);
	System.out.println("annot view bounds: " + view_bounds.getMin() +
			   ", " + view_bounds.getMax());
	seqmap.setMapRange(view_bounds.getMin(), view_bounds.getMax());
      }
    }

    seqmap.toFront(axis_tier);
    java.util.List floating_layers = getFloatingLayers();

    // restore floating layers to front of map
    for (int i=0; i<floating_layers.size(); i++) {
      GlyphI layer_glyph = (GlyphI)floating_layers.get(i);
      seqmap.toFront(layer_glyph);
    }
    // notifyPlugins();
    if (same_seq && preserve_view) {
      seqmap.stretchToFit(false, true);
    }
    else {
      seqmap.stretchToFit(true, true);
      zoomToSelections();
      int[] range = seqmap.getVisibleRange();
      setZoomSpotX(0.5*(range[0] + range[1]));
    }
    seqmap.updateWidget();
    if (DIAGNOSTICS) {
      System.out.println("Time to convert models to display: " + tim.read()/1000f);
    }
  }


  protected String getVersionInfo(BioSeq seq) {
    String version_info = null;
    if (seq instanceof SmartAnnotBioSeq && (((SmartAnnotBioSeq)seq).getSeqGroup() != null) )  {
      AnnotatedSeqGroup group = ((SmartAnnotBioSeq)seq).getSeqGroup();
      if (group.getDescription() != null)  { version_info = group.getDescription(); }
      else { version_info = group.getID(); }
    }
    if ((version_info == null)  && (seq instanceof Versioned)) {
      version_info = ((Versioned)seq).getVersion();
    }
    return version_info;
  }


  /**
   *  Returns all floating layers _except_ grid layer (which is supposed to stay
   *  behind everything else).
   */
  public java.util.List getFloatingLayers() {
    java.util.List layers = new ArrayList();
    GlyphI root_glyph = seqmap.getScene().getGlyph();
    int gcount = root_glyph.getChildCount();
    for (int i=0; i<gcount; i++) {
      GlyphI cgl = (GlyphI)root_glyph.getChild(i);
      if ((cgl instanceof PixelFloaterGlyph) && (cgl != grid_layer)) {
	layers.add(cgl);
      }
    }
    return layers;
  }

  void removeEmptyTiers() {
    // synchronizing on method2ftier to ensure (hopefully) that entries Set of Map.Entries
    //   will not change out from under the iterator
    ArrayList keys_to_remove = new ArrayList();
    synchronized (method2ftier) {
      Set entries = method2ftier.entrySet();
      Iterator iter = entries.iterator();
      while (iter.hasNext()) {
	Map.Entry ent = (Map.Entry)iter.next();
	String key = (String)ent.getKey();
	TierGlyph tg = (TierGlyph)ent.getValue();
	if (tg.getChildCount() <= 0) {
	  if (DEBUG_TIERS)  {
	    System.out.println("in removeEmptyTiers(), removing tier: " + tg.getLabel());
	  }

          if (remember_tiers) {
            tg.setState(TierGlyph.HIDDEN);
          } else {
            seqmap.removeTier(tg);
            keys_to_remove.add(key);
          }
	} else {
          //doesn't work: tg.restoreState(); // doesn't take into account AnnotStyle.getShow()
          if (tg.getAnnotStyle() != null) {tg.setStyle(tg.getAnnotStyle());}
        }
      }
    }

    for (int i=0; i<keys_to_remove.size(); i++) {
      method2ftier.remove(keys_to_remove.get(i));
    }

    keys_to_remove = new ArrayList();
    // synchronizing on method2rtier to ensure (hopefully) that entries Set of Map.Entries
    //   will not change out from under the iterator
    synchronized (method2rtier) {
      Set entries = method2rtier.entrySet();
      Iterator iter = entries.iterator();
      while (iter.hasNext()) {
	Map.Entry ent = (Map.Entry)iter.next();
	String key = (String)ent.getKey();
	TierGlyph tg = (TierGlyph)ent.getValue();
	if (tg.getChildCount() <= 0) {
	  if (DEBUG_TIERS)  {
	    System.out.println("in removeEmptyTiers(), removing tier: " + tg.getLabel());
	  }
          if (remember_tiers) {
            tg.setState(TierGlyph.HIDDEN);
          } else {
            seqmap.removeTier(tg);
            keys_to_remove.add(key);
          }
	} else {
          //doesn't work: tg.restoreState(); // doesn't take into account AnnotStyle.getShow()
          if (tg.getAnnotStyle() != null) {tg.setStyle(tg.getAnnotStyle());}
	}
      }
    }
    for (int i=0; i<keys_to_remove.size(); i++) {
      method2rtier.remove(keys_to_remove.get(i));
    }

    // now make sure getting rid of any empty tiers that weren't present in
    // method2ftier or method2rtier (graph tiers, for instance)
    // really should replace above with just this, and something within this loop to
    //   ensure that ftier/rtier hashes get entries removed as well...
    java.util.List tiers = seqmap.getTiers();
    int tiercount = tiers.size();
    for (int i=tiercount-1; i>=0; i--) {
      TierGlyph tg = (TierGlyph)tiers.get(i);
      if (tg.getChildCount() <= 0) {
        if (DEBUG_TIERS)  {
          System.out.println("in removeEmptyTiers() part 2, removing tier: " + tg.getLabel());
        }
        if (remember_tiers) {
          tg.setState(TierGlyph.HIDDEN);
        } else {
          seqmap.removeTier(tg);
        }
      }
    }
  }


  /**
   *  find min and max of annotations along AnnotatedBioSeq aseq.
   *<p>
   *  takes a boolean argument for whether to excludes GraphSym bounds
   *    (actual bounds of GraphSyms are currently problematic, but if (!exclude_graphs) then
   *      this method uses the first and last point in graph to determine graph bounds, and
   *      assumes that graph x coords are in order)
   *<p>
   *    This method is currently somewhat problematic, since it does not descend into BioSeqs
   *      that aseq might be composed of to factor in bounds of annotations on those sequences
   */
  public SeqSpan getAnnotationBounds(boolean exclude_graphs) {
  //  public SeqSpan getAnnotationBounds(Vector graphs) {
    int annotCount = aseq.getAnnotationCount();
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;
    for (int i=0; i<annotCount; i++) {
      // all_gene_searches, all_repeat_searches, etc.
      SeqSymmetry annotSym = aseq.getAnnotation(i);
      if (annotSym instanceof GraphSym) {
	if (! exclude_graphs) {
	  GraphSym graf = (GraphSym)annotSym;
	  int[] xcoords = graf.getGraphXCoords();
	  min = (int)Math.min(xcoords[0], min);
	  max = (int)Math.max(xcoords[xcoords.length-1], max);
	}
      }
      else {
	SeqSpan span = annotSym.getSpan(aseq);
	if (span != null) {
	  min = Math.min(span.getMin(), min);
	  max = Math.max(span.getMax(), max);
	}
      }
    }
    if (min != Integer.MAX_VALUE && max != Integer.MIN_VALUE) {
      min = Math.max(0, min-100);
      max = Math.min(aseq.getLength(), max+100);
      return new SimpleSeqSpan(min, max, aseq);
    }
    else {
      return null;
    }
  }

  void addAnnotationTiers() {
    if (DEBUG_COMP)  {
      System.out.println("$$$$$$$ called SeqMapView.addAnnotationTiers(), aseq: " + aseq.getID() + " $$$$$$$");
    }
    int annotCount = aseq.getAnnotationCount();
    for (int i=0; i<annotCount; i++) {
      SeqSymmetry annotSym = aseq.getAnnotation(i);
      if (annotSym instanceof SymWithProps) {
	addAnnotationGlyphs(annotSym);
      }
    }

    if (aseq instanceof CompositeBioSeq &&
	((CompositeBioSeq)aseq).getComposition() != null) {
      // muck with aseq, seq2viewsym, transform_path to trick addAnnotationTiers(),
      //   addLeafsToTier(), addToTier(), etc. into mapping from compositon sequences
      AnnotatedBioSeq cached_aseq = aseq;
      MutableSeqSymmetry cached_seq2viewSym = seq2viewSym;
      SeqSymmetry[] cached_path = transform_path;

      SeqSymmetry comp = ((CompositeBioSeq)aseq).getComposition();
      // assuming a two-level deep compositioin hierarchy for now...
      //   need to make more recursive at some point...
      //   (or does recursive call to addAnnotationTiers already give us full recursion?!!)
      int scount = comp.getChildCount();
      for (int i=0; i<scount; i++) {
      //      for (int i=0; i<1; i++) {
	SeqSymmetry csym = comp.getChild(i);
	// return seq in a symmetry span that _doesn't_ match aseq
	BioSeq cseq = SeqUtils.getOtherSeq(csym, cached_aseq);
	if (DEBUG_COMP)  { System.out.println(" other seq: " + cseq.getID() + ",  " + cseq); }
	if (cseq instanceof AnnotatedBioSeq) {
	  aseq = (AnnotatedBioSeq)cseq;
	  if (cached_seq2viewSym == null) {
	    transform_path = new SeqSymmetry[1];
	    transform_path[0] = csym;
	  }
	  else {
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

  /**
   *  Finds the "method" for a SeqSymmetry.
   *  Looks for the "method" in these places, in order:
   *   (1) the property "method", (2) the property "type",
   *   (3) TypedSym.getType().
   *  If no method is found, returns null.
   */
  public static String determineMethod(SeqSymmetry sym) {
    String meth = null;
    if (sym instanceof SymWithProps)  {
      SymWithProps psym = (SymWithProps)sym;
      meth = (String)psym.getProperty("method");
      if (meth == null) { meth = (String) psym.getProperty("type"); }
    }
    if (meth == null) {
      if (sym instanceof TypedSym) {
        meth = ((TypedSym)sym).getType();
      }
    }
    return meth;
  }

  public void addAnnotationGlyphs(SeqSymmetry annotSym) {
    // Map symmetry subclass or method type to a factory, and call factory to make glyphs
    MapViewGlyphFactoryI factory = null;
    String meth = null;
    if (annotSym instanceof GraphSym) {
      //      factory =	(MapViewGlyphFactoryI)getGraphFactoryHash().get(annotSym);
      factory = (MapViewGlyphFactoryI)getGraphIdFactoryHash().get(annotSym.getID());
      if (factory == null) {
	factory = new GenericGraphGlyphFactory(this);
        getGraphFactoryHash().put(annotSym, factory);
        getGraphIdFactoryHash().put(annotSym.getID(), factory);
      }
    }
    else {
      meth = determineMethod(annotSym);
      if (meth != null) {
        factory = (MapViewGlyphFactoryI)meth2factory.get(meth);
        if (factory == null) {
          java.util.List keyset = new ArrayList(regex2factory.keySet());

          // Look for a matching pattern, going backwards, so that the
          // patterns from the last preferences read take precedence over the
          // first ones read (such as the default prefs).  Within a single
          // file, the last matching pattern will trump any earlier ones.
          for (int j=keyset.size()-1 ; j >= 0 && factory == null; j--) {
            java.util.regex.Pattern regex = (java.util.regex.Pattern) keyset.get(j);
            if (regex.matcher(meth).find()) {
              factory = (MapViewGlyphFactoryI) regex2factory.get(regex);
              // Put (a clone of?) the factory in meth2factory to speed things up next time through.
              // (A clone would let us later modify the color, etc. of that copy)
              meth2factory.put(meth, factory);
            }
          }

        }
        if (factory == null) {
          factory = default_glyph_factory;
          // Again, a clone might be better.
          meth2factory.put(meth, default_glyph_factory);
        }
      }
    }
    if (factory == null) { factory = default_glyph_factory; }

    if (DEBUG_COMP && transform_path != null)  {
      System.out.println("transform path length: " + transform_path.length);
      for (int i=0; i<transform_path.length; i++) {
	SeqUtils.printSymmetry(transform_path[i]);
      }
    }
    factory.createGlyph(annotSym, this);

    // do "middleground" shading for tracks loaded via DAS/2
    if ((meth != null) &&
	(annotSym instanceof TypeContainerAnnot) &&
	(annotSym.getChildCount() > 0)  &&
	(annotSym.getChild(0) instanceof Das2FeatureRequestSym) ) {
      int child_count = annotSym.getChildCount();
      TierGlyph fortier = (TierGlyph) getForwardTierHash().get(meth.toLowerCase());
      TierGlyph revtier = (TierGlyph) getReverseTierHash().get(meth.toLowerCase());
      for (int i=0; i<child_count; i++) {
	SeqSymmetry csym = annotSym.getChild(i);
	if (csym instanceof Das2FeatureRequestSym) {
	  Das2FeatureRequestSym dsym = (Das2FeatureRequestSym)csym;
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


  public AnnotatedBioSeq getAnnotatedSeq() {
    return aseq;
  }


  /**
   *  Gets the view seq.
   *  Note: {@link #getViewSeq()} and {@link #getAnnotatedSeq()} may return
   *  different BioSeq's !
   *  This allows for reverse complement, coord shifting, seq slicing, etc.
   *  Returns BioSeq that is the SeqMapView's _view_ onto the
   *     AnnotatedBioSeq returned by getAnnotatedSeq()
   *  @see #getTransformPath()
   */
  public BioSeq getViewSeq() {
    return viewseq;
  }

  /**
   *  Returns the series of transformations that can be used to map
   *  a SeqSymmetry from {@link #getAnnotatedSeq()} to
   *  {@link #getViewSeq()}.
   */
  public SeqSymmetry[] getTransformPath() { return transform_path; }

  /** Returns a transformed copy of the given symmetry based on
   *  {@link #getTransformPath()}.  If no transform is necessary, simply
   *  returns the original symmetry.
   */
  public SeqSymmetry transformForViewSeq(SeqSymmetry insym) {
    return transformForViewSeq(insym, getAnnotatedSeq());
  }

  public SeqSymmetry transformForViewSeq(SeqSymmetry insym, BioSeq seq_to_compare) {
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
    java.util.List glyphlist = new ArrayList();
    GlyphI rootglyph = seqmap.getScene().getGlyph();
    collectGraphs(rootglyph, glyphlist);
    // convert graph glyphs to GraphSyms via glyphsToSyms
    java.util.List symlist = glyphsToSyms(glyphlist);
    //    System.out.println("called SeqMapView.selectAllGraphs(), select count: " + symlist.size());
    // call select(list) on list of graph syms
    select(symlist, false, true, true);
  }

  protected static void collectGraphs(GlyphI parent, Collection graphlist) {
    if (parent instanceof GraphGlyph) {
      graphlist.add(parent);
    }
    int childcount = parent.getChildCount();
    if (childcount > 0) {
      for (int i=0; i<childcount; i++) {
	GlyphI child = parent.getChild(i);
	collectGraphs(child, graphlist);
      }
    }
  }

  void select(java.util.List sym_list) {
    select(sym_list, false, false, true);
  }

  void select(java.util.List sym_list, boolean add_to_previous,
		     boolean call_listeners, boolean update_widget) {
    if (! add_to_previous)  {
      clearSelection();
    }

    int symcount = sym_list.size();
    for (int i=0; i<symcount; i++) {
      SeqSymmetry sym = (SeqSymmetry)sym_list.get(i);
      // currently assuming 1-to-1 mapping of sym to glyph
      GlyphI gl = (GlyphI)seqmap.getItem(sym);
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

  // This version of select() is not currently used
  void select(SeqSymmetry sym, boolean add_to_previous,
		     boolean call_listeners, boolean update_widget) {
    if (sym == null) {
      select(Collections.EMPTY_LIST, add_to_previous, call_listeners, update_widget);
    } else {
      ArrayList list = new ArrayList(1);
      list.add(sym);
      select(list, add_to_previous, call_listeners, update_widget);
    }
  }

  protected void clearSelection() {
    seqmap.clearSelected();
    setSelectedRegion(null, false);
    //  clear match_glyphs?
  }

  protected SeqSymmetry glyphToSym(GlyphI gl) {
    if (gl.getInfo() instanceof SeqSymmetry) {
      return (SeqSymmetry) gl.getInfo();
    } else {
      return null;
      /*
      // Create a fake symmetry for things that don't have any glyph info.
      // This allows the genomic coordinates of the selected item to be visible in the SymTableView,
      // and allows slicing to be done based on the item.
      Rectangle2D cb = gl.getCoordBox();
      SeqSymmetry fake_sym = new SingletonSeqSymmetry((int) cb.x, (int) (cb.x + cb.width-1), aseq);
      return fake_sym;
      */
    }
  }

  /**
   * Given a list of glyphs, returns a list of syms that those
   *  glyphs represent.
   */
  protected java.util.List glyphsToSyms(java.util.List glyphs) {
    java.util.List syms = new ArrayList(glyphs.size());
    if (glyphs.size() > 0) {
      // if some syms are represented by multiple glyphs, then want to make sure glyphsToSyms()
      //    doesn't return the same sym multiple times, so have to do a bit more work
      if (seqmap.hasMultiGlyphsPerModel()) {
	//	System.out.println("#########    in SeqMapView.glyphsToSyms(), possible multiple glyphs per sym");
	HashMap symhash = new HashMap();
	for (int i=0; i<glyphs.size(); i++) {
	  GlyphI gl = (GlyphI)glyphs.get(i);
	  SeqSymmetry sym = glyphToSym(gl);
	  if (sym != null) { symhash.put(sym, sym); }
	}
	Iterator iter = symhash.values().iterator();
	while (iter.hasNext()) {
	  syms.add(iter.next());
	}
      }
      else {
	//  no syms represented by multiple glyphs, so can use more efficient way of collecting syms
	for (int i=0; i<glyphs.size(); i++) {
	  GlyphI gl = (GlyphI)glyphs.get(i);
	  SeqSymmetry sym = glyphToSym(gl);
	  if (sym != null) { syms.add(sym); }
	}
      }
    }
    return syms;
  }


  /**
   *  Figures out which symmetries are currently selected and then calls
   *  {@link SingletonGenometryModel#setSelectedSymmetries(java.util.List, Object)}.
   */
  void postSelections() {
    Vector selected_glyphs = seqmap.getSelected();
    java.util.List selected_syms = glyphsToSyms(selected_glyphs);
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
      }
      else {
        SeqSpan seq_region = seq_selected_sym.getSpan(aseq);
        seq_glyph.select(seq_region.getMin(), seq_region.getMax());
        IGB.getSingletonIGB().setStatus(SeqUtils.spanToString(seq_region), false);
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
  public SeqSymmetry getSelectedRegion() {
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
    }
    else {
      java.util.List syms = getSelectedSyms();
      if (syms.size() == 1) {
        residues_sym = (SeqSymmetry) syms.get(0);
        from = " from selected item";
      }
    }

    if (residues_sym == null) {
      IGB.errorPanel("Can't copy to clipboard",
      "No selection or multiple selections.  Select a single item before copying its residues to clipboard.");
    }
    else  {
      SeqSpan span = residues_sym.getSpan(aseq);
      if (aseq == null) {
        // This is a fishy test.  How could aseq possibly be null?
	IGB.errorPanel("Don't have residues, can't copy to clipboard");
      }
      else { // 2
	int child_count = residues_sym.getChildCount();
	if (child_count > 0) {
	  // make new resorted sym to fix any problems with orientation
	  //   within the original sym...
	  //
	  // GAH 12-15-2003  should really do some sort of recursive sort, but for
	  //   now assuming depth = 2...  actually, should _really_ fix this when building SeqSymmetries,
	  //   so order of children reflects the order they should be spliced in, rather
	  //   than their order relative to a particular seq
	  java.util.List sorted_children = new ArrayList(child_count);
	  for (int i=0; i<child_count; i++) {
	    sorted_children.add(residues_sym.getChild(i));
	  }
	  boolean forward = span.isForward();

	  Comparator symcompare = new SeqSymStartComparator(aseq, forward);
	  Collections.sort(sorted_children, symcompare);
	  MutableSeqSymmetry sorted_sym = new SimpleMutableSeqSymmetry();
	  for (int i=0; i<child_count; i++) {
	    sorted_sym.addChild((SeqSymmetry)sorted_children.get(i));
	  }
	  residues_sym = sorted_sym;
	}

	String residues = SeqUtils.getResidues(residues_sym, aseq);
	if (residues != null) {
	  int rescount = residues.length();
	  boolean complete = true;
	  for (int i=0; i<rescount; i++) {
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
            String message = "Copied "+hackstr.length()+" residues" + from + " to clipboard";
            IGB.getSingletonIGB().setStatus(message);
	    success = true;
	  }
	  else {
	    IGB.errorPanel("Missing Sequence Residues",
            "Don't have all the needed residues, can't copy to clipboard.\n" +
            "Please load sequence residues for this region.");
	  }
	}
      }
    }
    if (! success) {
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
   *  Returns the most recently selected glyph.
   */
  GlyphI getSelectedGlyph() {
    if (seqmap.getSelected().isEmpty()) {
      return null;
    } else {
      return (GlyphI) seqmap.getSelected().lastElement();
    }
  }

  /**
   *  Determines which SeqSymmetry's are selected by looking at which Glyph's
   *  are currently selected.  The list will not include the selected sequence
   *  region, if any.  Use getSelectedRegion() for that.
   *  @return a List of SeqSymmetry objects, possibly empty.
   */
  public java.util.List getSelectedSyms() {
    Vector glyphs = seqmap.getSelected();
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

  public void sliceBySelection()  {
    sliceAndDice(getSelectedSyms());
  }

  public void testUnion() {
    testUnion(getSelectedSyms());
  }

  public void testUnion(java.util.List syms) {
    SimpleSymWithProps unionSym = new SimpleSymWithProps();
    unionSym.setProperty("method", "union_test");
    SeqUtils.union(syms, unionSym, aseq);
    ((MutableAnnotatedBioSeq)aseq).addAnnotation(unionSym);
    System.out.println("*** unionSym: " + unionSym + ", childcount = " + unionSym.getChildCount());
    setAnnotatedSeq(aseq, true, true);
  }

  public void sliceAndDice(java.util.List syms) {
    SimpleSymWithProps unionSym = new SimpleSymWithProps();
    SeqUtils.union(syms, unionSym, aseq);
    sliceAndDice(unionSym);
  }

  public SeqSymmetry getSliceSymmetry() {
    return slice_symmetry;
  }

  /**
   *  Performs a genometry-based slice-and-dice.
   *  Assumes that symmetry children are ordered by child.getSpan(aseq).getMin().
   */
  public void sliceAndDice(SeqSymmetry sym) {
    //    System.out.println("%%%%%% called SeqMapView.sliceAndDice() %%%%%%");
    if (! slicing_in_effect) {
      //   only redo viewspan_before_slicing if slicing is not already in effect, because
      //   if (slicing_in_effect) and slicing again, probably just adjusting slice buffer
      viewspan_before_slicing = getVisibleSpan();
    }
    int childCount = (sym==null) ? 0 : sym.getChildCount();

    if (childCount <= 0) {
      return;
    }
    coord_shift = true;
    if (seq2viewSym == null) {
      seq2viewSym = new SimpleMutableSeqSymmetry();
    }
    else  {
      seq2viewSym.clear();
    }

    slice_symmetry = sym;
    viewseq = new CompositeNegSeq("view_seq", 0, aseq.getLength());
    // rebuild seq2viewSym as a symmetry mapping slices of aseq to abut next to each other
    //    mapped to viewseq
    int prev_max = 0;
    int slice_offset = 0;
    for (int i=0; i<childCount; i++) {
      SeqSymmetry child = sym.getChild(i);
      SeqSpan exact_span = child.getSpan(aseq);
      if (exact_span == null) { continue; }  // skip any children that don't have a span in aseq
      int next_min;
      if (i == (childCount-1)) {
	next_min = aseq.getLength();
      }
      else {
	next_min = sym.getChild(i+1).getSpan(aseq).getMin();
      }
      int slice_min = (int)Math.max(prev_max, (exact_span.getMin() - slice_buffer));
      int slice_max = (int)Math.min(next_min, (exact_span.getMax() + slice_buffer));
      SeqSpan seq_slice_span = new SimpleSeqSpan(slice_min, slice_max, aseq);

      int slice_length = seq_slice_span.getLength();
      SeqSpan view_slice_span = new SimpleSeqSpan(slice_offset, slice_offset + slice_length, viewseq);
      MutableSeqSymmetry slice_sym = new SimpleMutableSeqSymmetry();
      slice_sym.addSpan(seq_slice_span);
      slice_sym.addSpan(view_slice_span);
      seq2viewSym.addChild(slice_sym);
      slice_offset += slice_length;
      prev_max = slice_max;
    }
    SeqSpan seq_span = SeqUtils.getChildBounds(seq2viewSym, aseq);
    SeqSpan view_span = SeqUtils.getChildBounds(seq2viewSym, viewseq);
    seq2viewSym.addSpan(seq_span);
    seq2viewSym.addSpan(view_span);

    ((CompositeNegSeq)viewseq).setComposition(seq2viewSym);
    ((CompositeNegSeq)viewseq).setBounds(view_span.getMin(), view_span.getMax());
    transform_path = new SeqSymmetry[1];
    transform_path[0] = seq2viewSym;
    slicing_in_effect = true;
    setAnnotatedSeq(aseq);
  }

  /** Currently has no effect: the grid is not currently available. */
  public void setGridColor() {
    if (grid_glyph != null) {
      Color col = JColorChooser.showDialog(frm,
					   "Grid Color Chooser", grid_glyph.getColor());
      if (col != null) {
	grid_glyph.setColor(col);
	grid_glyph.setVisibility(true);  // making sure grid visibility is on
	seqmap.updateWidget();
      }
    }
  }

  /** Currently has no effect: the grid is not currently available. */
  public void setGridSpacing() {
    if (grid_glyph != null) {
      String str = JOptionPane.showInputDialog(frm,
        "Number of bases between grid lines: ", Double.toString(grid_glyph.getGridSpacing()));
      if (str != null) {
	try {
	  double bases = Double.parseDouble(str);
	  grid_glyph.setGridSpacing(bases);
	  grid_glyph.setVisibility(true);  // making sure grid visibility is on
	}
	catch (Exception ex) {
          ErrorHandler.errorPanel("Error setting grid spacing to '"+str+"'", ex);
        }
      }
      seqmap.updateWidget();
    }
  }

  /** Currently has no effect: the grid is not currently available. */
  public void toggleGrid() {
    if (grid_glyph != null) {
      boolean grid_on =  grid_glyph.isVisible();
      grid_on = ! grid_on;
      grid_glyph.setVisibility(grid_on);
      seqmap.updateWidget();
    }
  }


  //  public void autoScroll(int timer_interval, int bases_to_scroll) {
  /*
   *  units to scroll are either in pixels or bases
   */
  ActionListener map_auto_scroller = null;
  javax.swing.Timer swing_timer = null;
  int as_bases_per_pix = 75;
  int as_pix_to_scroll = 3;
  int as_time_interval = 20;
  int modcount = 0;

  public void toggleAutoScroll() {
    if (map_auto_scroller == null) {
      //      toggleAutoScroll(
      JPanel pan = new JPanel();

      int bases_in_view = (int) seqmap.getView().getCoordBox().width;
      int pixel_width = seqmap.getView().getPixelBox().width;
      as_bases_per_pix = bases_in_view / pixel_width;

      // as_bases_per_pix *should* be a float, or else should simply
      // use the current resoltion without asking the user,
      // but since it is an integer, we have to set the minimum value as 1
      if (as_bases_per_pix < 1) {as_bases_per_pix = 1;}

      final JTextField bases_per_pixTF = new JTextField("" + as_bases_per_pix);
      final JTextField pix_to_scrollTF = new JTextField("" + as_pix_to_scroll);
      final JTextField time_intervalTF = new JTextField("" + as_time_interval);
      float bases_per_minute = (float)
	// 1000 ==> ms/s , 60 ==> s/minute, as_time_interval ==> ms/scroll
	(1.0 * as_bases_per_pix * as_pix_to_scroll * 1000 * 60 / as_time_interval);
      bases_per_minute = Math.abs(bases_per_minute);
      float minutes_per_seq = viewseq.getLength() / bases_per_minute;
      final JLabel bases_per_minuteL = new JLabel("" + (bases_per_minute/1000000));
      final JLabel minutes_per_seqL = new JLabel("" + (minutes_per_seq));

      pan.setLayout(new GridLayout(5,2));
      pan.add(new JLabel("Resolution (bases per pixel)"));
      pan.add(bases_per_pixTF);
      pan.add(new JLabel("Scroll increment (pixels)"));
      pan.add(pix_to_scrollTF);
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

          float bases_per_minute = (float)
            // 1000 ==> ms/s , 60 ==> s/minute, as_time_interval ==> ms/scroll
            (1.0 * as_bases_per_pix * as_pix_to_scroll * 1000 * 60 / as_time_interval);
          bases_per_minute = Math.abs(bases_per_minute);
          float minutes_per_seq = viewseq.getLength() / bases_per_minute;
          bases_per_minuteL.setText("" + (bases_per_minute/1000000));
          minutes_per_seqL.setText("" + (minutes_per_seq));
        }
      };

      bases_per_pixTF.addActionListener(al);
      pix_to_scrollTF.addActionListener(al);
      time_intervalTF.addActionListener(al);

      int val = JOptionPane.showOptionDialog(this, pan, "AutoScroll Parameters",
					     JOptionPane.OK_CANCEL_OPTION,
					     JOptionPane.PLAIN_MESSAGE,
					     null, null, null);
      if (val == JOptionPane.OK_OPTION) {
        as_bases_per_pix = normalizeTF(bases_per_pixTF, as_bases_per_pix, 1, Integer.MAX_VALUE);
        as_pix_to_scroll = normalizeTF(pix_to_scrollTF, as_pix_to_scroll, -1000, 1000);
        as_time_interval = normalizeTF(time_intervalTF, as_time_interval, 1, 1000);
        toggleAutoScroll(as_bases_per_pix, as_pix_to_scroll, as_time_interval);
      }
    }
    else {
      swing_timer.stop();
      swing_timer = null;
      map_auto_scroller = null;
    }
  }

  // Normalize a text field so that it holds an integer, with a fallback value
  // if there is a problem, and a minimum and maximum
  int normalizeTF(JTextField tf, int fallback, int min, int max) {
    int result = fallback;
    try {
      result = Integer.parseInt(tf.getText());
    } catch (NumberFormatException nfe) {
      Toolkit.getDefaultToolkit().beep();
      result = fallback;
    }
    if (result < min) { result = min; }
    else if (result > max) { result = max; }
    tf.setText(Integer.toString(result));
    return result;
  }

  public void toggleAutoScroll(int bases_per_pixel, int pix_to_scroll,
			       int timer_interval) {
    double pix_per_coord = 1.0 / (double)bases_per_pixel;
    final double coords_to_scroll = (double)pix_to_scroll / pix_per_coord;

    Rectangle2D cbox = seqmap.getViewBounds();
    //Rectangle pbox = seqmap.getView().getPixelBox();
    double start = (int)cbox.x;

    seqmap.zoom(NeoWidgetI.X, pix_per_coord);
    seqmap.scroll(NeoWidgetI.X, start);

    if (map_auto_scroller == null) {
      map_auto_scroller = new ActionListener() {
	  public void actionPerformed(ActionEvent evt) {
	    Rectangle2D vbox = seqmap.getViewBounds();
	    Rectangle2D mbox = seqmap.getCoordBounds();
	    int scrollpos = (int)(vbox.x + coords_to_scroll);
	    if ((scrollpos + vbox.width) > (mbox.x + mbox.width)) {
	      // end of sequence reached, so stop scrolling
	      swing_timer.stop();
	      swing_timer = null;
	      map_auto_scroller = null;
	    }
	    else {
	      seqmap.scroll(NeoWidgetI.X, scrollpos);
	      seqmap.updateWidget();
	    }
	  }
	};

      swing_timer = new javax.swing.Timer(timer_interval, map_auto_scroller);
      swing_timer.start();
      // Other options:
      //    java.util.Timer ??
      //    com.affymetrix.genoviz.util.NeoTimerEventClock ??
    }
    else {
      swing_timer.stop();
      swing_timer = null;
      map_auto_scroller = null;
    }
  }


  public void zoomTo(SeqSpan span) {
    int smin = span.getMin();
    int smax = span.getMax();
    float coord_width = smax - smin;
    float pixel_width = seqmap.getView().getPixelBox().width;
    double pixels_per_coord = pixel_width / coord_width; // can be Infinity, but the Math.min() takes care of that
    pixels_per_coord = Math.min(pixels_per_coord, seqmap.getMaxZoom(NeoWidgetI.X));
    seqmap.zoom(NeoWidgetI.X, pixels_per_coord);
    seqmap.scroll(NeoWidgetI.X, smin);
    seqmap.setZoomBehavior(seqmap.X, seqmap.CONSTRAIN_COORD, (smin + smax)/2);
    seqmap.updateWidget();
  }

  public void zoomToGlyph(GlyphI gl) {
    if (gl != null ) {
      zoomToRectangle(gl.getCoordBox());
    }
  }

  /** Zoom to a region including all the currently selected Glyphs. */
  public void zoomToSelections() {
    Vector selections = seqmap.getSelected();
    if (selections.size()>0) {
      zoomToRectangle(getRegionForGlyphs(selections));
    }
  }

  /** Returns a rectangle containing all the current selections.
   *  @return null if the vector of glyphs is empty
   */
  public Rectangle2D getRegionForGlyphs(java.util.List glyphs) {
    int size = glyphs.size();
    if (size>0) {
      Rectangle2D rect = new Rectangle2D();
      GlyphI g0 = (GlyphI) glyphs.get(0);
      rect.copyRect(g0.getCoordBox());
      for (int i=1; i<size; i++) {
        GlyphI g = (GlyphI) glyphs.get(i);
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
  public void zoomToRectangle(Rectangle2D rect) {
    if (rect != null ) {
      seqmap.zoom ( NeoWidgetI.X, Math.min(
        seqmap.getView().getPixelBox().width / (rect.width * 1.1f),
        seqmap.getMaxZoom(NeoWidgetI.X)
        ));
      seqmap.scroll ( NeoWidgetI.X,  - ( seqmap.getVisibleRange()[0] ) );
      seqmap.scroll ( NeoWidgetI.X, (rect.x - rect.width * 0.05 ) );
      seqmap.setZoomBehavior(seqmap.X, seqmap.CONSTRAIN_COORD, (rect.x + rect.width/2));
      seqmap.setZoomBehavior(seqmap.Y, seqmap.CONSTRAIN_COORD, (rect.y + rect.height/2));
      seqmap.updateWidget();
    }
  }


  public void unclamp() {
    if (viewseq instanceof CompositeNegSeq) {
      int min = ((CompositeNegSeq)viewseq).getMin();
      int max = ((CompositeNegSeq)viewseq).getMax();
      System.out.println("unclamping, xmin = " + min + ", xmax = " + max);
      seqmap.setMapRange(min, max);
    }
    else {
      System.out.println("unclamping, xmin = " + 0 + ", xmax = " + viewseq.getLength());
      seqmap.setMapRange(0, viewseq.getLength());
    }
    seqmap.stretchToFit(false, false);
    seqmap.updateWidget();
  }


  public void clampToView() {
    Rectangle2D vbox = seqmap.getView().getCoordBox();
    System.out.println("clamping, xmin = " + (int)vbox.x + ", xmax = " + (int)(vbox.x + vbox.width));
    seqmap.setMapRange((int)(vbox.x), (int)(vbox.x+vbox.width));
    seqmap.stretchToFit(false, false);
    seqmap.updateWidget();
  }

  public void clampToGlyph(GlyphI gl) {
    zoomToGlyph(gl);
    Rectangle2D vbox = seqmap.getViewBounds();
    //    map.setMapRange(vbox.x, vbox.x+vbox.width);
    seqmap.setMapRange((int)(vbox.x), (int)(vbox.x+vbox.width));
    seqmap.stretchToFit(false, false); // to adjust scrollers and zoomers
    seqmap.updateWidget();
  }

  public void pushView(String remote_address) {
    Rectangle2D vbox = seqmap.getView().getCoordBox();
    int start = (int)vbox.x;
    int end = (int)(vbox.x + vbox.width);
    SeqSpan span = new SimpleSeqSpan(start, end, aseq);
    UnibrowControlUtils.sendLocationCommand(remote_address, span);
    System.out.println("sent span to: " + remote_address);
  }

  public void invokeUcscView() {
    // links to UCSC look like this:
    //  http://genome.ucsc.edu/cgi-bin/hgTracks?db=hg11&position=chr22:15916196-31832390
    String ucsc_url = null;
    if (! (aseq instanceof NibbleBioSeq)) {
      IGB.errorPanel("Can't call UCSC", "Sequence has no version info");
    }
    else if (slicing_in_effect) {
      IGB.errorPanel("Can't call UCSC", "Currently looking at sliced view, can't call UCSC");
    }
    else {
      Rectangle2D vbox = seqmap.getView().getCoordBox();
      int start = (int)vbox.x;
      int end = (int)(vbox.x + vbox.width);
      String ucsc_root = "http://genome.ucsc.edu/cgi-bin/hgTracks?";
      SynonymLookup lookup = SynonymLookup.getDefaultLookup();
      // hardwiring to try and find synonym with "hg" prefix -- will only work for
      //  human genome versions !
      String seqid = aseq.getID();
      String version = ((NibbleBioSeq)aseq).getVersion();
      java.util.List syns = lookup.getSynonyms(version);
      //      System.out.println("syns: " + syns);
      String ucsc_version = null;
      if (syns == null) {
	syns = new ArrayList();
	syns.add(version);
      }
      for (int i=0; i<syns.size(); i++) {
	String syn = (String)syns.get(i);
	// Having to hardwire this check to figure out which synonym to use to match
	//  with UCSC.  Really need to have some way when loading synonyms to specify
	//  which ones should be used when communicating with which external resource!
	//	System.out.println("testing syn: " + syn);
	if (syn.startsWith("hg") || syn.startsWith("mm") ||
	    syn.startsWith("rn") || syn.startsWith("ce") || syn.startsWith("dm"))  {
	  ucsc_version = syn;
	  break;
	}
      }
      if (ucsc_version != null) {
	String postfix = "db=" + ucsc_version + "&position=" +
	  seqid + ":" + start + "-" + end;
	ucsc_url = ucsc_root + postfix;
	WebBrowserControl.displayURLEventually(ucsc_url);
	System.out.println("UCSC URL: " + ucsc_url);
      }
      //    ?db=hg11&position=chr22:15916196-31832390
      //    ucsc_url = "http://
      else {
	System.out.println("Can't call UCSC, couldn't figure out how to access genome version");
	IGB.errorPanel("Can't call UCSC, couldn't figure out how to access genome version");
      }
    }
  }


  public void doEdgeMatching(java.util.List query_glyphs, boolean update_map) {

    if (match_glyphs != null && match_glyphs.size() > 0) {
      seqmap.removeItem(match_glyphs);  // remove all match glyphs in match_glyphs vector
    }

    int qcount = query_glyphs.size();
    int match_query_count = query_glyphs.size();
    for (int i=0; i<qcount && match_query_count <= max_for_matching; i++) {
      match_query_count += ((GlyphI)query_glyphs.get(i)).getChildCount();
    }

    if (match_query_count <= max_for_matching) {
      match_glyphs = new Vector();
      ArrayList target_glyphs = new ArrayList();
      target_glyphs.add(seqmap.getScene().getGlyph());
      double fuzz = getEdgeMatcher().getFuzziness();
      if (fuzz==0.0) {
        Color edge_match_color = UnibrowPrefsUtil.getColor(UnibrowPrefsUtil.getTopNode(), PREF_EDGE_MATCH_COLOR, default_edge_match_color);
	getEdgeMatcher().setColor(edge_match_color);
      } else {
        Color edge_match_fuzzy_color = UnibrowPrefsUtil.getColor(UnibrowPrefsUtil.getTopNode(), PREF_EDGE_MATCH_FUZZY_COLOR, default_edge_match_fuzzy_color);
	getEdgeMatcher().setColor(edge_match_fuzzy_color);
      }
      getEdgeMatcher().matchEdges(seqmap, query_glyphs, target_glyphs, match_glyphs);
    }
    else {
      IGB.getSingletonIGB().setStatus("Skipping edge matching; too many items selected.");
    }

    if (update_map)  { seqmap.updateWidget(); }
  }

  public boolean getEdgeMatching() { return show_edge_matches; }
  public void setEdgeMatching(boolean b) {
    show_edge_matches = b;
    if (show_edge_matches) {
      doEdgeMatching(seqmap.getSelected(), true);
    } else {
      doEdgeMatching(new Vector(0), true);
    }
  }

  public void adjustEdgeMatching(int bases) {
    getEdgeMatcher().setFuzziness(bases);
    if (show_edge_matches)  {
      doEdgeMatching(seqmap.getSelected(), true);
    }
  }

  /**
   *  return a SeqSpan representing the visible bounds of the view seq
   */
  public SeqSpan getVisibleSpan() {
    Rectangle2D vbox = seqmap.getView().getCoordBox();
    SeqSpan vspan = new SimpleSeqSpan((int)vbox.x,
				      (int)(vbox.x+vbox.width),
				      viewseq);
    return vspan;
  }

  public GlyphEdgeMatcher getEdgeMatcher() { return edge_matcher; }

  public void setShrinkWrap(boolean b) {
    SHRINK_WRAP_MAP_BOUNDS = b;
    setAnnotatedSeq(aseq);
  }

  public boolean getShrinkWrap() { return SHRINK_WRAP_MAP_BOUNDS; }

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
    String src_id = ObjectUtils.objString(src);
    // ignore self-generated xym selection -- already handled internally
    if (src == this) {
      if (IGB.DEBUG_EVENTS) {System.out.println("SeqMapView received selection event originating from itself: " + src_id);}
      String title = getSelectionTitle(seqmap.getSelected());
      IGB.getSingletonIGB().setStatus(title, false);
    }
    // ignore sym selection originating from AltSpliceView, don't want to change internal selection based on this
    else if ((src instanceof AltSpliceView) || (src instanceof SeqMapView))  {
      // catching SeqMapView as source of event because currently sym selection events actually originating
      //    from AltSpliceView have their source set to the AltSpliceView's internal SeqMapView...
      if (IGB.DEBUG_EVENTS) {System.out.println("SeqMapView received selection event from another SeqMapView: " + src_id);}
    }
    else {
      if (IGB.DEBUG_EVENTS) {System.out.println("SeqMapView received selection event originating from: " + src_id);}
      java.util.List symlist = evt.getSelectedSyms();
      // select:
      //   add_to_previous ==> false
      //   call_listeners ==> false
      //   update_widget ==>  false   (zoomToSelections() will make an updateWidget() call...)
      select(symlist, false, false, false);
      zoomToSelections();
      String title = getSelectionTitle(seqmap.getSelected());
      IGB.getSingletonIGB().setStatus(title, false);
    }
  }


  /** Sets the hairline position and zoom center to the given spot. Does not call map.updateWidget() */
  public final void setZoomSpotX(double x) {
    if (hairline != null) {hairline.setSpot(x);}
    seqmap.setZoomBehavior(seqmap.X, seqmap.CONSTRAIN_COORD, x);
  }

  /** Sets the hairline position to the given spot. Does not call map.updateWidget() */
  public final void setZoomSpotY(double y) {
    seqmap.setZoomBehavior(seqmap.Y, seqmap.CONSTRAIN_COORD, y);
  }

  /** Toggles the hairline between labeled/unlabled and returns true
   *  if it ends-up labeled.
   */
  public boolean toggleHairlineLabel() {
    hairline_is_labeled = ! hairline_is_labeled;
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
    if (menu != null) {menu.add(mi);}
    return mi;
  }

  /** For each current selection, deselect it and select its parent instead.
   *  @param top_level if true, will select only top-level parents
   */
  void selectParents(boolean top_level) {
    // copy selections to a new list before starting, because list of selections will be modified
    java.util.List all_selections = new ArrayList(seqmap.getSelected());
    Iterator iter = all_selections.iterator();
    while (iter.hasNext()) {
      GlyphI child = (GlyphI) iter.next();
      GlyphI pglyph = getParent(child, top_level);
      if ( pglyph != child) {
        seqmap.deselect(child);
        seqmap.select(pglyph);
      }
    }

    if (show_edge_matches)  {
      doEdgeMatching(seqmap.getSelected(), false);
    }
    seqmap.updateWidget();
    postSelections();
  }

  /** Get the parent, or top-level parent, of a glyph, with certain restictions.
   *  Will not return a TierGlyph or RootGlyph or a glyph that isn't hitable, but
   *  will return the original GlyphI instead.
   *  @param top_level if true, will recurse up to the top-level parent, with
   *  certain restrictions: recursion will stop before reaching a TierGlyph
   *  or RootGlyph or a glyph that isn't hitable.
   */
  public GlyphI getParent(GlyphI g, boolean top_level) {
    GlyphI result = g;
    GlyphI pglyph = g.getParent();
    // the test for isHitable will automatically exclude seq_glyph
    if ( pglyph != null && pglyph.isHitable() && ! (pglyph instanceof TierGlyph) && !(pglyph instanceof RootGlyph)) {
      if (top_level) {
        GlyphI t = pglyph;
        while (t != null && t.isHitable() && ! (t instanceof TierGlyph) && ! ( t instanceof RootGlyph)) {
          pglyph = t;
          t = t.getParent();
        }
      }
      result = pglyph;
    }
    return result;
  }

  private class SeqMapViewActionListener implements ActionListener {

    public SeqMapViewActionListener() {
      //super(true);
    }

    public void actionPerformed(ActionEvent evt) {
      String command = evt.getActionCommand();
      //System.out.println("SeqMapView received action event "+command);

      if (command.equals(zoomtoMI.getText())) {
        zoomToSelections();
      }
      else if (command.equals(zoomclampMI.getText())) {
        Vector selected_glyphs = seqmap.getSelected();
        if (selected_glyphs.isEmpty()) {
          IGB.errorPanel("Nothing selected");
        } else {
          clampToGlyph((GlyphI) selected_glyphs.lastElement());
        }
      }
      else if (command.equals(selectParentMI.getText())) {
        if (seqmap.getSelected().isEmpty()) {
          IGB.errorPanel("Nothing selected");
        } else if (seqmap.getSelected().size() == 1) {
          // one selection: select its parent, not recursively
          selectParents(false);
        } else {
          // multiple selections: select parents recursively
          selectParents(true);
        }
      }
      else if (command.equals(printSymmetryMI.getText())) {
        SeqSymmetry sym = getSelectedSymmetry();
        //TODO: Why not print ALL selections?
        if (sym == null) {
          IGB.errorPanel("No symmetry selected");
        } else {
          SeqUtils.printSymmetry(sym);
        }
      }
      else if (command.equals(slicendiceMI)) {
        sliceBySelection();
      }
      else if (command.equals("ZOOM_OUT_FULLY")) {
        Adjustable adj = seqmap.getZoomer(NeoMap.X);
        adj.setValue(adj.getMinimum());
        adj = seqmap.getZoomer(NeoMap.Y);
        adj.setValue(adj.getMinimum());
        //map.updateWidget();
      }
      else if (command.equals("ZOOM_OUT_X")) {
        Adjustable adj = seqmap.getZoomer(NeoMap.X);
        adj.setValue(adj.getValue()- (adj.getMaximum()-adj.getMinimum())/20);
        //map.updateWidget();
      }
      else if (command.equals("ZOOM_IN_X")) {
        Adjustable adj = seqmap.getZoomer(NeoMap.X);
        adj.setValue(adj.getValue()+ (adj.getMaximum()-adj.getMinimum())/20);
        //map.updateWidget();
      }
      else if (command.equals("ZOOM_OUT_Y")) {
        Adjustable adj = seqmap.getZoomer(NeoMap.Y);
        adj.setValue(adj.getValue()- (adj.getMaximum()-adj.getMinimum())/20);
        //map.updateWidget();
      }
      else if (command.equals("ZOOM_IN_Y")) {
        Adjustable adj = seqmap.getZoomer(NeoMap.Y);
        adj.setValue(adj.getValue()+ (adj.getMaximum()-adj.getMinimum())/20);
        //map.updateWidget();
      }
      else if (command.equals("SCROLL_LEFT")) {
        int[] visible =  seqmap.getVisibleRange();
        seqmap.scroll(NeoWidgetI.X, visible[0]+ (visible[1]-visible[0])/10 );
        seqmap.updateWidget();
      }
      else if (command.equals("SCROLL_RIGHT")) {
        int[] visible =  seqmap.getVisibleRange();
        seqmap.scroll(NeoWidgetI.X, visible[0]- (visible[1]-visible[0])/10 );
        seqmap.updateWidget();
      }
      else if (command.equals("SCROLL_UP")) {
        int[] visible =  seqmap.getVisibleOffset();
        seqmap.scroll(NeoWidgetI.Y, visible[0]+ (visible[1]-visible[0])/10 );
        seqmap.updateWidget();
      }
      else if (command.equals("SCROLL_DOWN")) {
        int[] visible =  seqmap.getVisibleOffset();
        seqmap.scroll(NeoWidgetI.Y, visible[0]- (visible[1]-visible[0])/10 );
        seqmap.updateWidget();
      }
    }
  }

  // sets the text on the JLabel based on the current selection
  private void setPopupMenuTitle(JLabel label, java.util.List selected_glyphs) {
    String title = getSelectionTitle(selected_glyphs);
    // limit the popup title to 30 characters because big popup-menus don't work well
    if (title != null && title.length() > 30) {
      title = title.substring(0, 30) + " ...";
    }
    label.setText(title);
  }

  // Compare the code here with SymTableView.selectionChanged()
  // The logic about finding the ID from instances of DerivedSeqSymmetry
  // should be similar in both places, or else users could get confused.
  private String getSelectionTitle(java.util.List selected_glyphs) {
    String id = null;
    if (selected_glyphs.isEmpty()) {
      id = "No selection";
    }
    else {
      if (selected_glyphs.size() == 1) {
        GlyphI topgl = (GlyphI) selected_glyphs.get(0);
        Object info = topgl.getInfo();
        SeqSymmetry sym = null;
        if (info instanceof SeqSymmetry) {
          sym = (SeqSymmetry) info;
        }
        if (sym instanceof SymWithProps) {
          id = (String) ((SymWithProps) sym).getProperty("id");
        }
        if (id == null && sym instanceof DerivedSeqSymmetry) {
          SeqSymmetry original = ((DerivedSeqSymmetry) sym).getOriginalSymmetry();
          if (original instanceof Propertied) {
            id = (String) ((Propertied) original).getProperty("id");
          }
        }
        if (id == null && topgl instanceof GraphGlyph) {
          GraphGlyph gg = (GraphGlyph) topgl;
          if (gg.getLabel() != null) {
            id = "Graph: "+ gg.getLabel();
          } else {
            id = "Graph Selected";
          }
        }
        if (id == null) {
          // If ID of item is null, check recursively for parent ID, or parent of that...
          GlyphI pglyph = topgl.getParent();
          if (pglyph != null && ! (pglyph instanceof TierGlyph) && !(pglyph instanceof RootGlyph)) {
            ArrayList v = new ArrayList(1);
            v.add(pglyph);
            // Add one ">" symbol for each level of getParent()
            id = "> "+getSelectionTitle(v);
          }
          else {
            id = "Unknown Selection";
          }
        }
      } else {
        id = "" + selected_glyphs.size() + " Selections";
      }
    }
    if (id == null) { id = ""; }
    return id;
  }


  private final int xoffset_pop = 10;
  private final int yoffset_pop = 0;

  void showPopup(NeoMouseEvent nevt) {
    sym_popup.setVisible(false); // in case already showing
    sym_popup.removeAll();

    java.util.List selected_glyphs = seqmap.getSelected();

    setPopupMenuTitle(sym_info, selected_glyphs);
    sym_popup.add(sym_info);
    sym_popup.add(printMI);
    if (! selected_glyphs.isEmpty()) {
      sym_popup.add(zoomtoMI);
    }
    java.util.List selected_syms = getSelectedSyms();
    if (selected_syms.size() > 0) {
      sym_popup.add(selectParentMI);
      sym_popup.add(printSymmetryMI);
    }

    for (int i=0; i<popup_listeners.size(); i++) {
      ContextualPopupListener listener = (ContextualPopupListener)popup_listeners.get(i);
      listener.popupNotify(sym_popup, selected_syms);
    }
    if (sym_popup.getComponentCount() > 0) {
      //      sym_popup.show(seqmap, nevt.getX()+xoffset_pop, nevt.getY()+yoffset_pop);
      // if seqmap is a MultiWindowTierMap, then using seqmap as Component target arg to popup.show()
      //  won't work, since it's component is never actually rendered -- so checking here
      /// to use appropriate target Component and pixel position
      EventObject oevt = nevt.getOriginalEvent();
      //      System.out.println("original event: " + oevt);
      if ((oevt != null) && (oevt.getSource() instanceof Component)) {
	Component target = (Component)oevt.getSource();
	if (oevt instanceof MouseEvent) {
	  //	  System.out.println("using original event target and coords");
	  MouseEvent mevt = (MouseEvent)oevt;
	  sym_popup.show(target, mevt.getX()+xoffset_pop, mevt.getY()+yoffset_pop);
	}
	else {
	  //	  System.out.println("using original event target");
	  sym_popup.show(target, nevt.getX()+xoffset_pop, nevt.getY()+yoffset_pop);
	}
      }
      else {
	sym_popup.show(seqmap, nevt.getX()+xoffset_pop, nevt.getY()+yoffset_pop);
      }
    }
    // For garbage collection, it would be nice to add a listener that
    // could call sym_popup.removeAll() when the popup is removed from view.
  }


  public void addPopupListener(ContextualPopupListener listener) {
    popup_listeners.add(listener);
  }

  public void removePopupListener(ContextualPopupListener listener) {
    popup_listeners.remove(listener);
  }


  /** Recurse through glyphs and collect those that are instanceof GraphGlyph. */
  public java.util.List collectGraphs() {
    ArrayList graphs = new ArrayList();
    GlyphI root = seqmap.getScene().getGlyph();
    collectGraphs(root, graphs);
    return graphs;
  }

  /** Recurse through glyph hierarchy and collect graphs. */
  public static void collectGraphs(GlyphI gl, java.util.List graphs) {
    int max = gl.getChildCount();
    for (int i=0; i<max; i++) {
      GlyphI child = gl.getChild(i);
      if (child instanceof GraphGlyph) {
        graphs.add(child);
      }
      if (child.getChildCount() > 0) {
        collectGraphs(child, graphs);
      }
    }
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
   *  @return an array of two Tiers, one forward (or mixed-direction), one reverse;
   *    If you want to treat the first one as mixed-direction, then place all
   *    the glyphs in it; the second tier will not be displayed if it remains empty.
   */
  public TierGlyph[] getTiers(String meth, boolean next_to_axis, IAnnotStyle style) {
      if (style == null) {
        throw new NullPointerException();
      }

      // Always returns two tiers.  Could change to return only one tier if
      // that is what the style suggests.

      AffyTieredMap map = this.getSeqMap();

      // try to match up method with tier...
      // have meth2forward, meth2reverse hashtables to map
      //    method name to forward and reverse tier hashtables
      Map method2ftier = this.getForwardTierHash();
      Map method2rtier = this.getReverseTierHash();

      TierGlyph fortier = (TierGlyph)method2ftier.get(meth.toLowerCase());
      TierGlyph revtier = (TierGlyph)method2rtier.get(meth.toLowerCase());

      TierGlyph axis_tier = this.getAxisTier();
      if (fortier == null) {
        fortier = new TierGlyph(style);
        setUpTierPacker(fortier, true);
        method2ftier.put(meth.toLowerCase(), fortier);
      }
      if (fortier != null) {
        String label;
        if (style instanceof AnnotStyle) {
          if (((AnnotStyle) style).getSeparate()) {
            //fortier.setDirection(TierGlyph.DIRECTION_FORWARD);
            label = style.getHumanName() + " (+)";
          } else {
            //fortier.setDirection(TierGlyph.DIRECTION_NONE);
            label = style.getHumanName() + " (+/-)";
          }
        } else { // may be an instance of graph annot style
          //fortier.setDirection(TierGlyph.DIRECTION_FORWARD);
          //label = meth + " (+)";
          label = meth;
        }
        fortier.setLabel(label);
      }
      if (map.getTierIndex(fortier) == -1) {
        if (next_to_axis)  {
          int axis_index = map.getTierIndex(axis_tier);
          map.addTier(fortier, axis_index);
        }
        else { map.addTier(fortier, true); }
      }

      if (revtier == null)  {
        revtier = new TierGlyph(style);
        //revtier.setDirection(TierGlyph.DIRECTION_REVERSE);
        setUpTierPacker(revtier, false);
        method2rtier.put(meth.toLowerCase(), revtier);
      }
      if (revtier != null) {
        if (style instanceof AnnotStyle) {
          revtier.setLabel(style.getHumanName() + " (-)");
        } else { // style is a graph style or is null (this may not even be possible)
          revtier.setLabel(meth + " (-)");
        }
      }
      if (map.getTierIndex(revtier) == -1) {
        if (next_to_axis)  {
          int axis_index = map.getTierIndex(axis_tier);
          map.addTier(revtier, axis_index+1);
        }
        else { map.addTier(revtier, false); }
      }

      TierGlyph[] tiers = {fortier, revtier};
      return tiers;
  }

  void setUpTierPacker(TierGlyph tg, boolean above_axis) {
    ExpandPacker ep = new FasterExpandPacker();
    if (above_axis) {
      ep.setMoveType(ExpandPacker.UP);
    } else {
      ep.setMoveType(ExpandPacker.DOWN);
    }
    tg.setExpandedPacker(ep);
    tg.setMaxExpandDepth(tg.getAnnotStyle().getMaxDepth());
  }

  public void groupSelectionChanged(GroupSelectionEvent evt)  {
    AnnotatedSeqGroup group = evt.getSelectedGroup();
    if (IGB.DEBUG_EVENTS)  {
      System.out.println("SeqMapView received seqGroupSelected() call: " + group.getID() + ",  " + group);
    }
    else {
      if (IGB.DEBUG_EVENTS)  { System.out.println("SeqMapView received seqGroupSelected() call, but group = null"); }
    }

    if ((aseq != null) && (aseq instanceof SmartAnnotBioSeq) &&
        (((SmartAnnotBioSeq)aseq).getSeqGroup() == group) ) {
      // don't clear if seq belongs to seq group...
    }
    else  {
      clear();
    }
  }

  public void seqSelectionChanged(SeqSelectionEvent evt)  {
    if (IGB.DEBUG_EVENTS)  {
      System.out.println("SeqMapView received SeqSelectionEvent, selected seq: " + evt.getSelectedSeq());
    }
    final AnnotatedBioSeq newseq = evt.getSelectedSeq();
    // Don't worry if newseq is null, setAnnotatedSeq can handle that
    // (It can also handle the case where newseq is same as old seq.)
    setAnnotatedSeq(newseq);
  }

  public void seqModified(SeqModifiedEvent evt) {
    SmartAnnotBioSeq modseq = evt.getModifiedSeq();
    if (modseq == this.getAnnotatedSeq()) {
      System.out.println("SeqMapView received a seqModified event, re-rendering via setAnnotatedSeq() call");
      setAnnotatedSeq(modseq, true, true);
    }
    else {
      System.out.println("ERROR: SeqMapView received a seqModified event for a sequence that is " +
			 "not the sequence it is currently viewing");
    }
  }

  public void actionPerformed(ActionEvent evt)  {
    Object src = evt.getSource();
    if (src == bases_per_pixelTF)  {
      System.out.println("action received on bases_per_pixel text field");
      try {
	float bases_per_pixel = Float.parseFloat(bases_per_pixelTF.getText());
	float pixels_per_base = 1.0f/bases_per_pixel;
	seqmap.zoom(NeoWidgetI.X, pixels_per_base);
	seqmap.updateWidget();
      }
      catch (Exception ex) {
	bases_per_pixelTF.setText("");
      }
    }
    else if (src == bases_in_viewTF)  {

    }
  }


}

