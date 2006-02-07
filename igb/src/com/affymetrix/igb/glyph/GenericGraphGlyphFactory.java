/**
*   Copyright (c) 2001-2005 Affymetrix, Inc.
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

package com.affymetrix.igb.glyph;

import java.awt.*;
import java.util.*;

import com.affymetrix.genoviz.bioviews.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.glyph.*;
import com.affymetrix.igb.tiers.*;
import com.affymetrix.igb.util.GraphGlyphUtils;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.igb.util.GraphSymUtils;
import com.affymetrix.igb.util.UnibrowPrefsUtil;

public class GenericGraphGlyphFactory implements MapViewGlyphFactoryI  {
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  static final boolean DEBUG = false;
  static final boolean use_fixed_pixel_height = false; // TODO
  static final boolean default_show_label = true;
  static final boolean default_show_axis = false;
  static final float default_minvis = Float.NEGATIVE_INFINITY;
  static final float default_maxvis = Float.POSITIVE_INFINITY;
  static final float default_score_thresh = 0;
  static final int default_minrun_thresh = 30;
  static final int default_maxgap_thresh = 100;

  static final boolean default_show_thresh = false;

  static Map seq2yloc = new HashMap();
  static Map seq2facount = new HashMap();

  GraphState state;
  SeqMapView gviewer;

  public static void clear() {
    seq2yloc = new HashMap();
    seq2facount = new HashMap();
  }

  public GenericGraphGlyphFactory(GraphState gs)  {
    super();
    state = gs;
  }

  public GenericGraphGlyphFactory(SeqMapView gv) {
    this(new GraphState());
    gviewer = gv;
    AffyTieredMap map = (AffyTieredMap)gviewer.getSeqMap();
    Rectangle mapbox = map.getView().getPixelBox();
    BioSeq seq = gmodel.getSelectedSeq();
    if (seq == null) { return; }

    double current_yloc;
    if (seq2yloc.get(seq) == null) {
      current_yloc = mapbox.y + 10;
      seq2yloc.put(seq, new Double(current_yloc));
    }
    else {
      current_yloc = ((Double)seq2yloc.get(seq)).doubleValue();
    }
    int facount;
    if (seq2facount.get(seq) == null) {
      facount = 0;
      seq2facount.put(seq, new Integer(facount));
    }
    else {
      facount = ((Integer)seq2facount.get(seq)).intValue();
    }

    boolean use_floating_graphs = GraphGlyphUtils.getGraphPrefsNode().getBoolean(
      GraphGlyphUtils.PREF_USE_FLOATING_GRAPHS, GraphGlyphUtils.default_use_floating_graphs);
      state.setFloatGraph(use_floating_graphs);

    // for now, state's graph height is in coords if graph is attached, and in
    //    pixels if graph is floating
    boolean height_set = false;
    if (state.getFloatGraph()) {
      int pix_height = GraphGlyphUtils.getGraphPrefsNode().getInt(
        GraphGlyphUtils.PREF_FLOATING_PIXEL_HEIGHT, GraphGlyphUtils.default_pix_height);
      state.setGraphHeight(pix_height);
    }
    else {
      double coord_height = GraphGlyphUtils.getGraphPrefsNode().getDouble(
        GraphGlyphUtils.PREF_ATTACHED_COORD_HEIGHT, GraphGlyphUtils.default_coord_height);
      state.setGraphHeight(coord_height);
    }

  if (state.getFloatGraph()) {
      if (current_yloc > (mapbox.y + mapbox.height - state.getGraphHeight())) {
	current_yloc = mapbox.y + 10;
      }
      state.setGraphYPos(current_yloc);
      current_yloc += state.getGraphHeight() + 10;
      seq2yloc.put(seq, new Double(current_yloc));
    }
    //Color col = GraphGlyphUtils.getDefaultGraphColor(facount);

    state.setColor(AnnotStyle.getDefaultInstance().getColor());
    // GAH 8-18-2004 don't have a standard way of figuring out which graphs are probe-based
    //   (in which case need 12/13 thresholded region start/end shift), and which ones aren't.  Since for
    //   now almost all graphs being viewed via IGB and for which thresholding is a useful operation are
    //   probe-based, hardwiring 12/13 shift by default.  If this is not what is desired, users will
    //   need to modify via GraphAdjusterView
    //   Hopefully soon will put in a more intelligent way of assigning threshold region shifts
    //      (or switch to probe-based graphs mapping to center point of probe rather than min of probe span
    state.setThreshStartShift(12);
    state.setThreshEndShift(13);

    seq2facount.put(seq, new Integer(facount + 1));

  }

  public GraphState getGraphState() { return state; }

  public void init (Map options) {

  }

  public void createGlyph(SeqSymmetry sym, SeqMapView smv) {
    if (sym instanceof GraphSym) {
      GraphSym gsym = (GraphSym)sym;

      AnnotStyle annot_style = AnnotStyle.getInstance(gsym.getGraphName(), false);
      state.setColor(annot_style.getColor());

      displayGraph(gsym, smv, state, false);
    }
    else {
      System.err.println("GenericGraphGlyphFactory.createGlyph() called, but symmetry " +
			 "passed in is NOT a GraphGlyph: " + sym);
    }
  }

  /**
   *  Makes a SmartGraphGlyph to represent the input GraphSym,
   *     and either adds it as a floating graph to the SeqMapView or adds it
   *     in a tier, depending on state.getFloatGraph().
   *  Also adds to the SeqMapView's GraphState-to-TierGlyph hash if needed.
   */
  public static GraphGlyph displayGraph(GraphSym graf, SeqMapView smv, GraphState state, boolean update_map)  {
    AnnotatedBioSeq aseq = smv.getAnnotatedSeq();
    BioSeq vseq = smv.getViewSeq();
    BioSeq graph_seq = graf.getGraphSeq();
    AffyTieredMap map = smv.getSeqMap();

    if (graph_seq != aseq) {
      // System.out.println("################## ERROR, graph_seq != aseq #################");
      // may need to modify to handle case where GraphGlyph's seq is one of seqs in aseq's composition...
      return null;
    }

    GraphSym newgraf = graf;
    if (graph_seq != vseq) {
      SeqSymmetry mapping_sym = smv.transformForViewSeq(graf);
      newgraf = GraphSymUtils.transformGraphSym(graf, mapping_sym);
      SeqSpan span_on_vseq = mapping_sym.getSpan(vseq);
      //      Rectangle2D gbox = graph_glyph.getCoordBox();
      //      graph_glyph.setCoords(span_on_vseq.getMin(), gbox.y, span_on_vseq.getLength(), gbox.height);
    }
    if (newgraf.getGraphXCoords() == null || newgraf.getGraphYCoords() == null) {
      return null;
    } else if (newgraf.getGraphXCoords().length == 0 || newgraf.getGraphYCoords().length == 0) {
      return null;
    }

    String graph_name = graf.getGraphName();
    if (graph_name == null) {
      // this probably never actually happens
      graph_name = "Graph #" + System.currentTimeMillis();
      graf.setGraphName(graph_name);
    }

    AnnotStyle annot_style = AnnotStyle.getInstance(graf.getGraphName(), false);

    annot_style.setColor(state.getColor());

    setStateFromProps(graf, state);   // set some GraphState properties based on GraphSym
    SmartGraphGlyph graph_glyph;
    graph_glyph = new SmartGraphGlyph(newgraf.getGraphXCoords(), newgraf.getGraphYCoords(), state);
    graph_glyph.setLabel(graf.getGraphName());
    //    graph_glyph.setGraphState(state);
    //    graph_glyph.setPointCoords(newgraf.getGraphXCoords(), newgraf.getGraphYCoords());

    // moved percentile binning to after transform mapping (use newgraf instead of graf),
    //    since coords to bin may be much smaller if newgraf is set of slices of original graf
    boolean apply_visible_range_filter =
      UnibrowPrefsUtil.getTopNode().getBoolean(GraphGlyphUtils.PREF_APPLY_PERCENTAGE_FILTER,
					       GraphGlyphUtils.default_apply_percentage_filter);
    /*
    if ( apply_visible_range_filter ) {
      // calling calcPercents2Scores() here means that this expensive operation will often
      //    be called twice per graph score array, since it is also called on selected graphs in GraphAdjusterView
      //    need to figure out a way to cache percentiles result so don't have to redo
      float[] percentiles = GraphSymUtils.calcPercents2Scores(newgraf.getGraphYCoords(), 10.0f);
      // percentile 'p' is at index i = p * (percentiles.length - 1) / 100
      // and percentiles.length = 1001 in this case.

      // If the graph data consists mostly of a single value (such as zero) with
      // a small number of outliers, then it is possible for
      // percentiles[10] == percentiles[990].  It is a bad idea to set the
      // visible max and min to the same value, so check for that.
      if (percentiles[10] != percentiles[990]) {
        graph_glyph.setVisibleMinY(percentiles[10]); // 1st percentile
        graph_glyph.setVisibleMaxY(percentiles[990]); // 99th percentile
      }
    }
    */
    Rectangle2D cbox = map.getCoordBounds();
    graph_glyph.setCoords(cbox.x, state.getGraphYPos(), cbox.width, state.getGraphHeight());
    map.setDataModel(graph_glyph, graf); // side-effect of graph_glyph.setInfo(graf)

    // graph_glyph.setFasterDraw(true);
    // graph_glyph.setCalcCache(true);

    if (state.getFloatGraph()) {
      PixelFloaterGlyph floater = new PixelFloaterGlyph();
      map.addItem(floater);
      floater.addChild(graph_glyph);
      floater.setCoords(cbox.x, 0, cbox.width, 0);
      GraphGlyphUtils.checkPixelBounds(graph_glyph, smv);
    }
    else {
      //	System.out.println("*** in GenericGrphaGlyphFactory.displayGraph() ***");
      TierGlyph tglyph = (TierGlyph)smv.getGraphStateTierHash().get(state);
      boolean new_tier = (tglyph == null);
      if (new_tier) {
	//	  System.out.println("*** in GenericGrphaGlyphFactory, making new tier ***");
	if (use_fixed_pixel_height)  {
	  TransformTierGlyph tempgl = new TransformTierGlyph(annot_style);
	  tempgl.setFixedPixelHeight(true);
	  tempgl.setFixedPixHeight(60);
	  tglyph = tempgl;
	}
	else { tglyph = new TierGlyph(annot_style); }
      }

      Color tier_back_col = annot_style.getBackground();

      tglyph.setFillColor(tier_back_col);
      tglyph.setForegroundColor(state.getColor());
      tglyph.addChild(graph_glyph);
      tglyph.setLabel(annot_style.getHumanName());
      //tglyph.setLabel(graf.getGraphName());
      // GAH 11-21-2003  WARNING -- have to add tier to map _after_ it's label has been set,
      //   or the TieredLabelMap won't get assigned labels correctly
      if (new_tier) {
	boolean upper_strand = true;
	Object str = graf.getProperty(GraphSym.PROP_GRAPH_STRAND);
	if ((str instanceof Character) && ((Character) str).charValue()=='-') {
	  upper_strand = false;
	}
	map.addTier(tglyph, upper_strand);
	smv.getGraphStateTierHash().put(state, tglyph);
      }
      tglyph.pack(map.getView());
      if (update_map) {
	map.packTiers(false, true, false);
	map.stretchToFit(false, false);
      }
    }
    report("post packing      ", state, graph_glyph);
    if (update_map) {
      map.updateWidget();
    }
    return graph_glyph;
  }

  public static void report(String str, GraphState state, GraphGlyph graph_glyph) {
    if (DEBUG) {
      System.out.println(str + ": " +
			 "sypos = " + state.getGraphYPos() +
			 ", sheight = " + state.getGraphHeight() +
			 ", gypos = " + graph_glyph.getCoordBox().y +
			 ", gheight = " + graph_glyph.getCoordBox().height +
			 ", vismin = " + graph_glyph.getVisibleMinY() +
			 ", vismax = " + graph_glyph.getVisibleMaxY() +
			 ", pointmin = " + graph_glyph.getGraphMinY() +
			 ", pointmax = " + graph_glyph.getGraphMaxY());
    }
  }

  /**
   *  Displays a graph.
   *   This method relies on
   *       displayGraph(GraphSym graf, SeqMapView smv, GraphState state, boolean update_map), but
   *   in addition to what that method does, it also builds the GraphState based on the input params,
   *   and makes a factory, and adds the factory to the SeqMapView's GraphSym-to-Factory hash.
   */
  public static GraphGlyph displayGraph(GraphSym graf, SeqMapView smv,
					Color col, double graph_yloc, double graph_height,
					boolean use_floater) {
    boolean show_label = GraphGlyphUtils.getGraphPrefsNode().getBoolean("show_graph_label", default_show_label);
    boolean show_axis = GraphGlyphUtils.getGraphPrefsNode().getBoolean("show_graph_axis", default_show_axis);
    return displayGraph(graf, smv, col, graph_yloc, graph_height,
			use_floater, show_label, show_axis,
			default_minvis, default_maxvis,
			default_score_thresh, default_minrun_thresh, default_maxgap_thresh,
			default_show_thresh);
  }

  /**
   *   Display a graph with all properties specified.
   *   This method relies on
   *       displayGraph(GraphSym graf, SeqMapView smv, GraphState state, boolean update_map), but
   *   in addition to what that method does, it also builds the GraphState based on the input params,
   *   and makes a factory, and adds the factory to the SeqMapView's GraphSym-to-Factory hash.
   */
  public static GraphGlyph displayGraph(GraphSym graf, SeqMapView smv,
					Color col, double graph_yloc, double graph_height,
					boolean use_floater, boolean show_label, boolean show_axis,
					float minvis, float maxvis,
					float score_thresh, int minrun_thresh, int maxgap_thresh,
					boolean show_thresh
					) {
    GraphState gstate = new GraphState();

    // All properties were specified, so don't check the AnnotStyle here
    gstate.setColor(col);
    gstate.setGraphYPos(graph_yloc);
    gstate.setGraphHeight(graph_height);
    gstate.setFloatGraph(use_floater);
    gstate.setShowLabel(show_label);
    gstate.setShowAxis(show_axis);
    gstate.setVisibleMinY(minvis);
    gstate.setVisibleMaxY(maxvis);
    gstate.setMinScoreThreshold(score_thresh);
    gstate.setMinRunThreshold(minrun_thresh);
    gstate.setMaxGapThreshold(maxgap_thresh);
    gstate.setShowThreshold(show_thresh);

    //TODO: Should this stuff about is_trans_frag be moved to setStateFromProps()
    // so that it can be used from all the displayGraph() methods ?
    boolean is_trans_frag = ((graf.getProperty("parameter_set_name") != null) &&
			     (((String)graf.getProperty("parameter_set_name")).indexOf("TransFrag") > -1) ) ;
    if (is_trans_frag) {
      gstate.setGraphStyle(GraphGlyph.SPAN_GRAPH);
      // trans frag graphs shouldn't be shifted...
      gstate.setThreshStartShift(0);
      gstate.setThreshEndShift(0);
    }
    else {
      gstate.setGraphStyle(SmartGraphGlyph.MINMAXAVG);
      // hardwiring in shift settings,
      //    assuming these are probe graphs with probe size = 25...
      gstate.setThreshStartShift(12);
      gstate.setThreshEndShift(13);
    }

    setStateFromProps(graf, gstate);

    Map gfactories = smv.getGraphFactoryHash();
    GenericGraphGlyphFactory gfac = new GenericGraphGlyphFactory(gstate);
    //		gfactories.put(fgraf, gstate);
    gfactories.put(graf, gfac);
    GraphGlyph graph_glyph = GenericGraphGlyphFactory.displayGraph(graf, smv, gstate, true);
    return graph_glyph;
  }

  /**
   *  Sets some properties of the given GraphState based on properties in
   *  the given GraphSym's property map.
   *  Currently, sets only the graph "style" based on the value of the property
   *    GraphSym.PROP_INITIAL_GRAPH_STYLE.
   */
  static void setStateFromProps(GraphSym graf, GraphState state) {
    Integer requested_graph_style = (Integer) graf.getProperty(GraphSym.PROP_INITIAL_GRAPH_STYLE);
    if (requested_graph_style != null) {
      state.setGraphStyle(requested_graph_style.intValue());

      // Now set the requested graph style to null so that the user can later
      // change the style in the GUI, if desired
      graf.setProperty(GraphSym.PROP_INITIAL_GRAPH_STYLE, null);
    }
  }
}
