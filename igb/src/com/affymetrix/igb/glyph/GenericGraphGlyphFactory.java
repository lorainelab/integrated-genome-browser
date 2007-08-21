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

package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.GraphIntervalSym;
import java.util.*;

import com.affymetrix.genoviz.bioviews.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometryImpl.*;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphStateI;
import com.affymetrix.genometryImpl.style.IAnnotStyle;

import com.affymetrix.igb.util.*;
import com.affymetrix.igb.tiers.*;
import com.affymetrix.igb.view.SeqMapView;

public class GenericGraphGlyphFactory implements MapViewGlyphFactoryI  {
  static final boolean DEBUG = false;
  boolean check_same_seq = true;
  
  /** Name of a parameter for the init() method.  Set to Boolean.TRUE or Boolean.FALSE. 
   *  Determines whether the glyph factory will try to determine whether the GraphSym
   *  that it is drawing is defined on the currently-displayed bioseq.
   *  In some cases, you may want to intentionally display a graph on a seq that 
   *  has a different ID without checking to see if the ID's match.
   */
  static final public String CHECK_SAME_SEQ_OPTION = "Check Same Seq";

  public GenericGraphGlyphFactory() {
  }

  /** Allows you to set the parameter CHECK_SAME_SEQ_OPTION. */
  public void init (Map options) {
    Boolean ccs = (Boolean) options.get(CHECK_SAME_SEQ_OPTION);
    if (ccs != null) {
      check_same_seq = ccs.booleanValue();
    }
  }

  public void createGlyph(SeqSymmetry sym, SeqMapView smv) {
    boolean wrong_type = false;
    if (sym instanceof GraphSym) {
      GraphSym gsym = (GraphSym) sym;
      displayGraph(gsym, smv, false);
    }
    else {
      System.err.println("GenericGraphGlyphFactory.createGlyph() called, but symmetry " +
			 "passed in is NOT a GraphSym: " + sym);
    }
  }

  /**
   *  Makes a SmartGraphGlyph to represent the input GraphSym,
   *     and either adds it as a floating graph to the SeqMapView or adds it
   *     in a tier, depending on getGraphState().getGraphStyle().getFloatGraph() 
   *     and getGraphState().getComboStyle().
   *  All graphs that share the same tier style or the same combo tier style,
   *     will go in the same tier.  Graphs with a non-null combo tier style
   *     will go into an attached tier, never a floating glyph.
   *  Also adds to the SeqMapView's GraphState-to-TierGlyph hash if needed.
   */
  public GraphGlyph displayGraph(GraphSym graf, SeqMapView smv, boolean update_map)  {
    GraphState state = (GraphState) graf.getGraphState();
    
    AnnotatedBioSeq aseq = smv.getAnnotatedSeq();
    BioSeq vseq = smv.getViewSeq();
    BioSeq graph_seq = graf.getGraphSeq();
    AffyTieredMap map = smv.getSeqMap();
    
    if (check_same_seq && graph_seq != aseq) {
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
    if (check_same_seq && graph_seq != vseq) {
      // The new graph doesn't need a new GraphState or a new ID.  
      // Changing any graph properties will thus apply to the original graph.
      
      SeqSymmetry mapping_sym = smv.transformForViewSeq(graf, graph_seq);
      newgraf = GraphSymUtils.transformGraphSym(graf, mapping_sym, false);
    }
    if (newgraf == null || newgraf.getGraphXCoords() == null) {
      return null;
    } else if (newgraf.getGraphXCoords().length == 0 || newgraf.getPointCount() == 0) {
      return null;
    }

    String graph_name = newgraf.getGraphName();
    if (graph_name == null) {
      // this probably never actually happens
      graph_name = "Graph #" + System.currentTimeMillis();
      newgraf.setGraphName(graph_name);
    }

    GraphGlyph graph_glyph;
    if (newgraf instanceof GraphIntervalSym) {
      GraphIntervalSym gis = (GraphIntervalSym) newgraf;
      graph_glyph = new SmartGraphGlyph(gis.getGraphXCoords(), gis.getGraphWidthCoords(), gis, state);
    } else {
      graph_glyph = new SmartGraphGlyph(newgraf.getGraphXCoords(), newgraf, state);
    }
    graph_glyph.getGraphState().getTierStyle().setHumanName(newgraf.getGraphName());
    
    GraphStateI gstate = graf.getGraphState();
    IAnnotStyle tier_style = gstate.getTierStyle();
    
    Rectangle2D cbox = map.getCoordBounds();
    graph_glyph.setCoords(cbox.x, tier_style.getY(), cbox.width, tier_style.getHeight());
    map.setDataModelFromOriginalSym(graph_glyph, graf); // has side-effect of graph_glyph.setInfo(graf)

    // graph_glyph.setFasterDraw(true);
    // graph_glyph.setCalcCache(true);
    
    // Allow floating glyphs ONLY when combo style is null.
    // (Combo graphs cannot yet float.)
    if (gstate.getComboStyle() == null && gstate.getFloatGraph()) {
      graph_glyph.setCoords(cbox.x, tier_style.getY(), cbox.width, tier_style.getHeight());
      GraphGlyphUtils.checkPixelBounds(graph_glyph, smv);
      smv.getPixelFloaterGlyph().addChild(graph_glyph);
    }
    else {
      if (gstate.getComboStyle() != null) {
        tier_style = gstate.getComboStyle();
      }

      int direction = TierGlyph.DIRECTION_NONE;
      if (GraphSym.GRAPH_STRAND_MINUS.equals(graf.getProperty(GraphSym.PROP_GRAPH_STRAND))) {
        direction = TierGlyph.DIRECTION_REVERSE;
      }
      
      TierGlyph tglyph = smv.getGraphTier(tier_style, direction);
      tglyph.addChild(graph_glyph);

      tglyph.pack(map.getView());
      if (update_map) {
	map.packTiers(false, true, false);
	map.stretchToFit(false, false);
      }
    }
    if (update_map) {
      map.updateWidget();
    }

    return graph_glyph;
  }

// /**
//  *  Displays a graph.
//  */
//  public static GraphGlyph displayGraph(GraphSym graf, SeqMapView smv,
//    java.awt.Color col, double yloc, double height, boolean use_floater) {
//    
//    GraphStateI gstate = graf.getGraphState();
//    IAnnotStyle gstyle = gstate.getTierStyle();
//    gstyle.setColor(col);
//    gstyle.setY(yloc);
//    gstyle.setHeight(height);
//    gstate.setFloatGraph(use_floater);
//    
//    return displayGraph(graf, smv, true);
//  }

  public static void report(String str, GraphState state, GraphGlyph graph_glyph) {
    if (DEBUG) {
      System.out.println(str + ": " +
			 ", sheight = " + state.getTierStyle().getHeight() +
			 ", gypos = " + graph_glyph.getCoordBox().y +
			 ", gheight = " + graph_glyph.getCoordBox().height +
			 ", vismin = " + graph_glyph.getVisibleMinY() +
			 ", vismax = " + graph_glyph.getVisibleMaxY() +
			 ", pointmin = " + graph_glyph.getGraphMinY() +
			 ", pointmax = " + graph_glyph.getGraphMaxY());
    }
  }
}
