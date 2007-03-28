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

import java.util.*;

import com.affymetrix.genoviz.bioviews.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.parsers.ScoredIntervalParser;
import com.affymetrix.igb.tiers.*;
import com.affymetrix.igb.util.FloatList;
import com.affymetrix.igb.util.GraphGlyphUtils;
import com.affymetrix.igb.util.IntList;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.igb.view.SeqMapView;

public class ScoredContainerGlyphFactory implements MapViewGlyphFactoryI  {
  static final boolean DEBUG = false;
  
  static final boolean separate_by_strand = true;
  
  public ScoredContainerGlyphFactory() {
  }
  
  /** Does nothing. */
  public void init(Map options) {
  }
  
  public void createGlyph(SeqSymmetry sym, SeqMapView smv) {
    boolean attach_graphs = UnibrowPrefsUtil.getBooleanParam(ScoredIntervalParser.PREF_ATTACH_GRAPHS,
        ScoredIntervalParser.default_attach_graphs);
    if (sym instanceof ScoredContainerSym) {
      ScoredContainerSym container = (ScoredContainerSym) sym;
      if (DEBUG)  {System.out.println("&&&&& in ScoredContainerGlyphFactory, attach graphs: " + attach_graphs); }
      // first draw the little rectangle that will go in an annotation tier
      // and be used to select regions for the pivot view
      MapViewGlyphFactoryI annotation_factory = smv.getAnnotationGlyphFactory(smv.determineMethod(sym));
      if (annotation_factory != null) {
        annotation_factory.createGlyph(sym, smv);
      }
      
      // then draw the graphs
      if (attach_graphs) {
        displayGraphs(container, smv, false);
      }
    } else {
      System.err.println("ScoredContainerGlyphFactory.createGlyph() called, but symmetry " +
          "passed in is NOT a ScoredContainerSym: " + sym);
    }
    if (DEBUG)  { System.out.println("&&&&& exiting ScoredContainerGlyphFactory"); }
  }
  
  public void displayGraphs(ScoredContainerSym original_container, SeqMapView smv, boolean update_map)  {
    
    AnnotatedBioSeq aseq = smv.getAnnotatedSeq();
    if (DEBUG)  { System.out.println("   creating graphs on seq: " + aseq.getID()); }
    BioSeq vseq = smv.getViewSeq();
    AffyTieredMap map = smv.getSeqMap();
    if (original_container.getSpan(aseq) == null) {
      return;
    }
    GraphIntervalSym[] the_graph_syms = null;
    DerivedSeqSymmetry derived_sym = null;
    if (aseq != vseq) {
      derived_sym = SeqUtils.copyToDerived(original_container);
      SeqUtils.transformSymmetry(derived_sym, smv.getTransformPath());
      the_graph_syms = makeGraphsFromDerived(derived_sym, vseq);
    } else { // aseq == vseq, so no transformation needed
      the_graph_syms = makeGraphs(original_container, SingletonGenometryModel.getGenometryModel().getSelectedSeqGroup());
    }
    Rectangle2D cbox = map.getCoordBounds();
    for (int q=0; q<the_graph_syms.length; q++) {
      GraphIntervalSym gis = the_graph_syms[q];
      
      SmartGraphGlyph graph_glyph = new SmartGraphGlyph(gis.getGraphXCoords(), gis.getGraphWidthCoords(), gis.getGraphYCoords(), gis.getGraphState());
      graph_glyph.getGraphState().getTierStyle().setHumanName(gis.getGraphName());
      GraphState gstate = graph_glyph.getGraphState();
      
      IAnnotStyle tier_style = gstate.getTierStyle(); // individual style: combo comes later
      graph_glyph.setCoords(cbox.x, tier_style.getY(), cbox.width, tier_style.getHeight());
      
      map.setDataModelFromOriginalSym(graph_glyph, gis); // has side-effect of graph_glyph.setInfo(graf)
      
      // Allow floating glyphs ONLY when combo style is null.
      // (Combo graphs cannot yet float.)
      if (gstate.getComboStyle() == null && gstate.getFloatGraph()) {
        graph_glyph.setCoords(cbox.x, tier_style.getY(), cbox.width, tier_style.getHeight());
        GraphGlyphUtils.checkPixelBounds(graph_glyph, smv);
        smv.getPixelFloaterGlyph().addChild(graph_glyph);
      } else {
        if (gstate.getComboStyle() != null) {
          tier_style = gstate.getComboStyle();
        }
        int tier_direction = TierGlyph.DIRECTION_FORWARD;
        if (GraphSym.GRAPH_STRAND_MINUS.equals(gis.getProperty(GraphSym.PROP_GRAPH_STRAND))) {
          tier_direction = TierGlyph.DIRECTION_REVERSE;
        }
        TierGlyph tglyph = smv.getGraphTier(tier_style, tier_direction);
        tglyph.addChild(graph_glyph);
        tglyph.pack(map.getView());
      }
    }
    
    if (update_map) {
      map.packTiers(false, true, false);
      map.stretchToFit(false, false);
    }
    if (update_map) {
      map.updateWidget();
    }
    return;
  }
  
  static GraphIntervalSym[] makeGraphs(ScoredContainerSym container, AnnotatedSeqGroup seq_group) {
    int score_count = container.getScoreCount();
    ArrayList results = null;
    if (separate_by_strand) {
      results = new ArrayList(score_count * 2);
    } else {
      results = new ArrayList(score_count);
    }
    
    for (int i=0; i<score_count; i++) {
      String score_name = container.getScoreName(i);
      if (separate_by_strand)  {
        GraphSym forward_gsym = container.makeGraphSym(score_name, true, seq_group);
        if (forward_gsym != null) {
          results.add(forward_gsym);
        }
        GraphSym reverse_gsym = container.makeGraphSym(score_name, false, seq_group);
        if (reverse_gsym != null) {
          results.add(reverse_gsym);
        }
      } else {
        GraphSym gsym = container.makeGraphSym(score_name, seq_group);
        if (gsym != null) {
          results.add(gsym);
        }
      }
    }
    return (GraphIntervalSym[]) results.toArray(new GraphIntervalSym[results.size()]);
  }
  
  
  public static GraphIntervalSym[] makeGraphsFromDerived(DerivedSeqSymmetry derived_parent_sym, BioSeq seq) {
    ScoredContainerSym original_container = (ScoredContainerSym) derived_parent_sym.getOriginalSymmetry();
    
    int score_count = original_container.getScoreCount();
    ArrayList results = null;
    if (separate_by_strand) {
      results = new ArrayList(score_count * 2);
    } else {
      results = new ArrayList(score_count);
    }
    
    for (int i=0; i<score_count; i++) {
      String score_name = original_container.getScoreName(i);
      if (separate_by_strand)  {
        GraphSym forward_gsym = makeGraphSymFromDerived(derived_parent_sym, score_name, seq, '+');
        if (forward_gsym != null) {
          results.add(forward_gsym);
        }
        GraphSym reverse_gsym = makeGraphSymFromDerived(derived_parent_sym, score_name, seq, '-');
        if (reverse_gsym != null) {
          results.add(reverse_gsym);
        }
      } else {
        GraphSym gsym = makeGraphSymFromDerived(derived_parent_sym, score_name, seq, '.');
        if (gsym != null) {
          results.add(gsym);
        }
      }
    }
    
    return (GraphIntervalSym[]) results.toArray(new GraphIntervalSym[results.size()]);
  }
  
  // strands should be one of '+', '-' or '.'
  // name -- should be a score name in the original ScoredContainerSym
  static GraphIntervalSym makeGraphSymFromDerived(DerivedSeqSymmetry derived_parent, String name, BioSeq seq, final char strands) {
    ScoredContainerSym original_container = (ScoredContainerSym) derived_parent.getOriginalSymmetry();
    
    float[] original_scores = original_container.getScores(name);
    
    // Simply knowing the correct graph ID is the key to getting the correct
    // graph state, with the accompanying tier style and tier combo style.
    String id = original_container.getGraphID(name, strands);
    
    if (original_scores == null) {
      System.err.println("ScoreContainerSym.makeGraphSym() called, but no scores found for: " + name);
      return null;
    }
    
    //int score_count = original_scores.length;
    int derived_child_count = derived_parent.getChildCount();
    IntList xcoords = new IntList(derived_child_count);
    IntList wcoords = new IntList(derived_child_count);
    FloatList ycoords = new FloatList(derived_child_count);
    
    for (int i=0; i<derived_child_count; i++) {
      Object child = derived_parent.getChild(i);
      if (child instanceof DerivedSeqSymmetry) {
        DerivedSeqSymmetry derived_child = (DerivedSeqSymmetry) derived_parent.
            getChild(i);
        SeqSpan cspan = derived_child.getSpan(seq);
        if (cspan != null) {
          if (strands == '.' || (strands == '+' && cspan.isForward()) ||
              (strands == '-' && !cspan.isForward())) {
            xcoords.add(cspan.getMin());
            wcoords.add(cspan.getLength());
            IndexedSym original_child = (IndexedSym) derived_child.
                getOriginalSymmetry();
            // the index of this child in the original parent symmetry.
            // it is very possible that original_index==i in all cases,
            // but I'm not sure of that yet
            int original_index = original_child.getIndex();
            ycoords.add(original_scores[original_index]);
          }
        }
      }
    }
    GraphIntervalSym gsym = null;
    if (xcoords.size() != 0) {
      gsym = new GraphIntervalSym(xcoords.copyToArray(),
          wcoords.copyToArray(), ycoords.copyToArray(), id, seq);
      if (strands == '-') {
        gsym.setProperty(GraphSym.PROP_GRAPH_STRAND, GraphSym.GRAPH_STRAND_MINUS);
      } else if (strands == '+') {
        gsym.setProperty(GraphSym.PROP_GRAPH_STRAND, GraphSym.GRAPH_STRAND_PLUS);
      } else {
        gsym.setProperty(GraphSym.PROP_GRAPH_STRAND, GraphSym.GRAPH_STRAND_BOTH);
      }
    }
    return gsym;
  }
}
