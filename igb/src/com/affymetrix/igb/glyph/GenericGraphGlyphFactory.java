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
import com.affymetrix.genoviz.widget.NeoMap;

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
  static final int default_thresh_direction = GraphState.THRESHOLD_DIRECTION_GREATER;

  static Map seq2yloc = new HashMap();

  //  SeqMapView gviewer;

  public static void clear() {
    seq2yloc = new HashMap();
  }

  public GenericGraphGlyphFactory(SeqMapView gv) {
    this(gv.getSeqMap());
  }

  public GenericGraphGlyphFactory(NeoMap map) {
    //this(new GraphState());
    //    gviewer = gv;
    //    AffyTieredMap map = (AffyTieredMap)gviewer.getSeqMap();
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

    /*
    if (state.getFloatGraph()) {
      if (current_yloc > (mapbox.y + mapbox.height - state.getGraphHeight())) {
	current_yloc = mapbox.y + 10;
      }
      state.setGraphYPos(current_yloc);
      current_yloc += state.getGraphHeight() + 10;
      seq2yloc.put(seq, new Double(current_yloc));
    }
     */
  }


  public void init (Map options) {

  }

  public void createGlyph(SeqSymmetry sym, SeqMapView smv) {
    if (sym instanceof GraphSym) {
      GraphSym gsym = (GraphSym) sym;
      displayGraph(gsym, smv, gsym.getGraphState(), false);
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

    // GAH 2006-03-26
    //    want to add code here to handle situation where a "virtual" seq is being display on SeqMapView,
    //       and it is composed of GraphSym's from multiple annotated seqs, but they're really from the
    //       same data source (or they're the "same" data on different chromosomes for example)
    //       In this case want these displayed as a single graph

    //   match these up based on identical graph names / ids, then:
    //    Approach 1)
    //       build a CompositeGraphSym on the virtual seq
    //       make a single GraphGlyph
    //    Approach 2)
    //       create a new CompositeGraphGlyph subclass (or do I already have this?)
    //       make multiple GraphGlyphs
    //    Approach 3)
    //       ???

    GraphSym newgraf = graf;
    if (graph_seq != vseq) {
      //      System.out.println("need to transform graph sym: ");
      //      SeqUtils.printSymmetry(graf);
      SeqSymmetry[] transform_path = smv.getTransformPath();
      if (transform_path != null && transform_path.length > 0) {
	//	System.out.println("transform path, length: " + transform_path.length);
	//	SeqUtils.printSymmetry(transform_path[0]);
      }
      else {
	//	System.out.println("no transform path");
      }
      //      SeqUtils.debug_step3 = true;
      SeqSymmetry mapping_sym = smv.transformForViewSeq(graf, graph_seq);
      //      SeqUtils.debug_step3 = false;
      newgraf = GraphSymUtils.transformGraphSym(graf, mapping_sym, false);
      SeqSpan span_on_vseq = mapping_sym.getSpan(vseq);
      //      Rectangle2D gbox = graph_glyph.getCoordBox();
      //      graph_glyph.setCoords(span_on_vseq.getMin(), gbox.y, span_on_vseq.getLength(), gbox.height);
    }
    if (newgraf == null || newgraf.getGraphXCoords() == null || newgraf.getGraphYCoords() == null) {
      return null;
    } else if (newgraf.getGraphXCoords().length == 0 || newgraf.getGraphYCoords().length == 0) {
      return null;
    }

    String graph_name = newgraf.getGraphName();
    if (graph_name == null) {
      // this probably never actually happens
      graph_name = "Graph #" + System.currentTimeMillis();
      newgraf.setGraphName(graph_name);
    }

    SmartGraphGlyph graph_glyph;
    graph_glyph = new SmartGraphGlyph(newgraf.getGraphXCoords(), newgraf.getGraphYCoords(), state);
    graph_glyph.setLabel(newgraf.getGraphName());
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
      //      TierGlyph tglyph = (TierGlyph)smv.getGraphStateTierHash().get(state);
      //      TierGlyph tglyph = (TierGlyph)smv.getGraphNameTierHash().get(newgraf.getGraphName());
      TierGlyph tglyph = (TierGlyph)smv.getGraphIdTierHash().get(newgraf.getID());

      boolean new_tier = (tglyph == null);
      if (new_tier) {
	//        System.out.println("*** in GenericGraphGlyphFactory, making new tier ***");
	if (use_fixed_pixel_height)  {
	  TransformTierGlyph tempgl = new TransformTierGlyph(state);
	  tempgl.setFixedPixelHeight(true);
	  tempgl.setFixedPixHeight(60);
	  tglyph = tempgl;
	}
	else { tglyph = new TierGlyph(state); }
	tglyph.setState(TierGlyph.COLLAPSED);
	PackerI pack = tglyph.getPacker();
	if (pack instanceof CollapsePacker) {
	  ((CollapsePacker)pack).setParentSpacer(0);
	}
      }

      // want to allow for multiple graph glyphs overlaid in same tier
      tglyph.setState(TierGlyph.COLLAPSED);

      Color tier_back_col = state.getBackground();

      tglyph.setFillColor(tier_back_col);
      tglyph.setForegroundColor(state.getColor());
      tglyph.addChild(graph_glyph);
      tglyph.setLabel(state.getLabel());
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
      }
      smv.getGraphStateTierHash().put(state, tglyph);
      smv.getGraphNameTierHash().put(newgraf.getGraphName(), tglyph);
      smv.getGraphIdTierHash().put(newgraf.getID(), tglyph);

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
			default_show_thresh, default_thresh_direction);
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
					boolean show_thresh, int thresh_direction
					) {
    GraphState gstate = graf.getGraphState();

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
    gstate.setThresholdDirection(thresh_direction);

    //GenericGraphGlyphFactory gfac = new GenericGraphGlyphFactory(gstate);
    GenericGraphGlyphFactory gfac = new GenericGraphGlyphFactory(smv);
    //		gfactories.put(fgraf, gstate);
    smv.getGraphFactoryHash().put(graf, gfac);
    smv.getGraphIdFactoryHash().put(graf.getID(), gfac);
    GraphGlyph graph_glyph = GenericGraphGlyphFactory.displayGraph(graf, smv, gstate, true);
    return graph_glyph;
  }
}
