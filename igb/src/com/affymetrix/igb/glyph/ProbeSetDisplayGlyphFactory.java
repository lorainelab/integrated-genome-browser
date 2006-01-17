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
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.widget.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.util.*;
import com.affymetrix.igb.tiers.*;
import com.affymetrix.igb.glyph.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.view.SeqMapView;

/**
 *  A factory for showing consensus (or exemplar) sequences mapped onto the genome with
 *  probe sets mapped onto the consensus sequences.  Could be used in general
 *  for showing any gapped alignments of sequences against the genome 
 *  along with annotations mapped onto those sequences.
 */
public class ProbeSetDisplayGlyphFactory implements MapViewGlyphFactoryI  {

/*
Algorithm for drawing probe-set-display data.

Find the annotations on the chromosome.
Recurse through each annotation down to depth=2
  Each of these is a consensus symmetry "CSym"
Transform each CSym into "View" coordinates: "CSym_x_view"
Each CSym points to a Consensus Seq: "CSeq"
Recurse through the annotations of CSeq down to depth=2
  Each of these is a probe set: "PS"
Transform each PS by the CSym giving "PS_x_Csym" with depth=3
Transform again for the view "(PS_x_Csym)_x_View" with depth unknown

In (PS_x_Csym)_x_View, the overal depth is unknown, but you do know
that the top level is probeset, then probe, then pieces of probes (if split
across introns)

If you try to skip a step and transform PS by Csym_x_View
giving PS_x_(CSym_x_View), you cannot predict at what depth to find
the probeset, probe and pieces of probes
*/  
  
  
  /** Any method name (track-line name) ending with this is taken as a poly_a_site. */
  public static final String POLY_A_SITE_METHOD = "netaffx poly_a_sites";
  /** Any method name (track-line name) ending with this is taken as a poly_a_stack. */
  public static final String POLY_A_STACK_METHOD = "netaffx poly_a_stacks";
  /** Any method name (track-line name) ending with this is taken as a consensus/exemplar sequence. */
  public static final String NETAFFX_CONSENSUS = " netaffx consensus";


  static Color ps_color = Color.PINK;
  static Color ps_s_color = Color.GREEN;
  static Color ps_x_color = Color.ORANGE;
  static Color poly_a_site_color = Color.BLUE;
  static Color poly_a_stack_color = Color.CYAN;
  
  SeqMapView gviewer;
  Color default_tier_color = Color.black;

  /** Whether to draw a glyph for probesets that floats free from the glyph
   *  for the consensus.  Two glyphs can be drawn for each probeset:
   *  one that is attached to the consensus sequence glyph, and one that
   *  is independent and thus gets packed separately in
   *  an independent tier.
   */
  boolean do_independent_probeset_glyphs = false;
  
  /** 
   * Whether to put an outline around the probe glyphs in the same probeset.
   */
  boolean outline_probes_in_probeset = false;
  
  /** The name of the property in the consensus seq SeqSymmetry to use to
   *  construct a label.  Default is "id".  Set to null to turn off labelling. 
   */
  String label_field = "id";

  String ps_label_field = null; // no longer used

  /** Color for the consensus sequence glyphs. */
  Color consensus_color;

  /** Color for the gaps in the consensus sequence alignment. 
   *  Simply a darker version of the consensus_color.
   */
  Color gap_color;

  int glyph_depth = 2;
  

  /** Initializes options based on given Map.
   *  Special notes: 
   *    "label_field" and defaults to "id", but you can set
   *     it to something else, or set to "" if you want to turn off labels.
   *    "color" sets the color of the consensus sequence glyphs; the colors
   *    of the probe set and poly_A glyphs are hard-wired.  (Color actually
   *    sets the color of the consensus glyph outlines: the centers are drawn
   *    in a darker shade.)
   */
  public void init(Map options) {
    //    System.out.println("called AbstractAnnotGlyphFactory.init()");
    consensus_color = (Color) options.get("color");
    if (consensus_color == null) { consensus_color = GenericAnnotGlyphFactory.default_annot_color; }

    gap_color = consensus_color.darker();

    label_field = (String)options.get("label_field");
    if (label_field==null) {label_field="id";}
    if ("".equals(label_field)) {label_field = null;} // turn off labels

//    ps_label_field = (String)options.get("ps_label_field");
//    if (ps_label_field==null) {ps_label_field="id";}
//    if ("".equals(ps_label_field)) {ps_label_field = null;} // turn them off
    
    do_independent_probeset_glyphs = setBooleanProperty(options, "probeset_glyphs", do_independent_probeset_glyphs);
    outline_probes_in_probeset = setBooleanProperty(options, "outline_probes", outline_probes_in_probeset);
  }

  // used by init()
  static private final boolean setBooleanProperty(Map m, String name, boolean def) {
    String s = (String) m.get(name);
    if (s==null) { return def; }
    else { return Boolean.valueOf(s).booleanValue(); }
  }
  
  public ProbeSetDisplayGlyphFactory() {
  }

  public void createGlyph(SeqSymmetry sym, SeqMapView smv) {
    createGlyph(sym, smv, false);
  }

  public void createGlyph(SeqSymmetry sym, SeqMapView smv, boolean next_to_axis) {
    setMapView(smv);
    AffyTieredMap map = gviewer.getSeqMap();
    String meth = SeqMapView.determineMethod(sym);
    if (meth == null) {
      meth = "unknown";
    } else { // strip off the " netaffx consensus" ending
      int n = meth.lastIndexOf(NETAFFX_CONSENSUS);
      if (n>0) meth = meth.substring(0, n);
    }
    if (meth != null) {
      boolean use_fast_packers = false; // Glyphs in tier may have varying heights
      AnnotStyle style = AnnotStyle.getInstance(meth);
      consensus_color = style.getColor();
      label_field = style.getLabelField();
      
      TierGlyph[] tiers = gviewer.getTiers(meth, next_to_axis, style);
      BioSeq seq = gviewer.getAnnotatedSeq();
      addLeafsToTier(sym, tiers[0], tiers[1], glyph_depth);
    }
  }

  public void setMapView(SeqMapView smv) {
    gviewer = smv;
  }

  /**
   * Recurses children of sym until SeqUtils.getDepth(sym) equals depth_of_consensuses, 
   * then calls addToTier.
   * @param sym a SeqSymmetry representing a consensus sequence (or a tree where
   *      consensus symmetries are at depth = desired_leaf_depth).
   *      Each consensus symmetry represents the mapping of a consensus onto the genome,
   *      and can contain "introns" and "exons".
   * @param depth_of_consensuses  Depth at which consensus sequences symmetries will
   *      be found. Normally should be set to 2.
   */
  void addLeafsToTier(SeqSymmetry sym,
                      TierGlyph ftier, TierGlyph rtier,
                      int depth_of_consensuses) {
    int depth = SeqUtils.getDepth(sym);
    if (depth > depth_of_consensuses) {
      for (int i=0; i<sym.getChildCount(); i++) {
        SeqSymmetry child = sym.getChild(i);
        addLeafsToTier(child, ftier, rtier, depth_of_consensuses);
      }
    }
    else if (depth < 1) {
      System.out.println("ERROR in ProbeSetDisplayGlyphFactory: at wrong depth.");
    }
    else {  // 1 <= depth <= depth_of_consensus
      addToTier(sym, ftier, rtier);
    }
  }
  
  /** 
   *  If given a SeqSymmetry with exactly two Spans, will return
   *  the AnnotatedBioSeq of the Span that is NOT the sequence you specify.
   *  TODO: This could return more than one consensus sequence
   */
  AnnotatedBioSeq getConsensusSeq(SeqSymmetry sym, BioSeq primary_seq) {
    assert primary_seq != null;
    assert sym != null;

    int span_count = sym.getSpanCount();
    if (span_count != 2) {
      // Although this is normally an error, there are conditions where this glyph factory
      // might be used to display things that are not consensus sequences (such as DAS queries)
      // so just return null.
      //System.out.println("Instead of the expected 2 spans, I see this many: "+span_count);
      //SeqUtils.printSymmetry(sym);
      return null;
    }

    BioSeq consensus_seq = null;
    for (int i=0; i<2; i++) {
      BioSeq seq = sym.getSpan(i).getBioSeq();
      if (seq != primary_seq) {
        consensus_seq = seq; 
        break;
      }
    }
    if (consensus_seq instanceof AnnotatedBioSeq) {
      return (AnnotatedBioSeq) consensus_seq;
    } else {
      System.out.println("ProbeSetDisplayGlyphFactory: Consensus Seq is not an annotated bio seq!");
      return null;
    }
  }
  
  private static int GLYPH_HEIGHT = 20;
  
  public GlyphI addToTier(SeqSymmetry consensus_sym, TierGlyph forward_tier, TierGlyph reverse_tier) {

    if (SeqUtils.getDepth(consensus_sym) != glyph_depth) {
      System.out.println("ProbeSetDisplayGlyphFactory: at wrong depth!");
      return null;
    }

    BioSeq annotseq = gviewer.getAnnotatedSeq();
    BioSeq coordseq = gviewer.getViewSeq();
    
    SeqSymmetry transformed_consensus_sym = gviewer.transformForViewSeq(consensus_sym);
    
    SeqSpan pspan = transformed_consensus_sym.getSpan(gviewer.getViewSeq());
    if (pspan == null) {
      // if no span corresponding to ViewSeq, then return.  
      // This can easily happen in the Sliced View and is not usually an error
      return null;
    }
    AffyTieredMap map = gviewer.getSeqMap();
    boolean forward = pspan.isForward();

    TierGlyph the_tier = forward ? forward_tier : reverse_tier;

    int parent_height = GLYPH_HEIGHT; // height of the consensus glyph
                 // if there is a label, this height value will be adjusted below
    int child_height = GLYPH_HEIGHT; // height of the consensus "exons"
    int parent_y = 100; // irrelevant because packing will move the glyphs around
    int child_y = 100; // relevant relative to parent_y

    boolean use_label = (label_field != null && (label_field.trim().length()>0) && 
      (consensus_sym instanceof SymWithProps));
    GlyphI pglyph;
    if (use_label) {
      EfficientLabelledLineGlyph lglyph = new EfficientLabelledLineGlyph();
      lglyph.setMoveChildren(false);
      if (forward) { 
        lglyph.setLabelLocation(LabelledGlyph.NORTH); 
        child_y += parent_height;
      }
      else {
        lglyph.setLabelLocation(LabelledGlyph.SOUTH);
      }
      String label = (String)((SymWithProps)consensus_sym).getProperty(label_field);
      lglyph.setLabel(label);
      parent_height = 2 * parent_height;
      pglyph = lglyph;
    } else {
      pglyph = new EfficientLineContGlyph();
      ((EfficientLineContGlyph)pglyph).setMoveChildren(false);
    }
    
    pglyph.setCoords(pspan.getMin(), parent_y, pspan.getLength(), parent_height);
    //System.out.println("PARENT: "+pglyph.getCoordBox().y+", "+pglyph.getCoordBox().height);
    pglyph.setColor(consensus_color);
    map.setDataModelFromOriginalSym(pglyph, transformed_consensus_sym);

    int childCount = transformed_consensus_sym.getChildCount();
        
    for (int i=0; i<childCount; i++) {
      SeqSymmetry child = transformed_consensus_sym.getChild(i);
      SeqSpan cspan = child.getSpan(coordseq);
      if (cspan == null) { continue; }
      EfficientOutlinedRectGlyph cglyph = new EfficientOutlinedRectGlyph();
      
      cglyph.setCoords(cspan.getMin(), child_y + child_height/4, cspan.getLength(), child_height/2);
      cglyph.setColor(consensus_color);
      pglyph.addChild(cglyph);
      map.setDataModelFromOriginalSym(cglyph, child);
    }

    // Add the pglyph to the tier before drawing probesets because probesets
    // calculate their positions relative to the coordinates of the pglyph's coordbox
    // and the coordbox can be moved around by adding the glyph to the tier
    the_tier.addChild(pglyph);
    
    AnnotatedBioSeq consensus_seq = getConsensusSeq(consensus_sym, annotseq);
    if (consensus_seq != null) {
      drawConsensusAnnotations(consensus_seq, consensus_sym, pglyph, the_tier, child_y, child_height);
    }

    return pglyph;
  }
  
  /**
   *  Finds the annotations at depth 2 on the consensus_seq, which are assumed to be 
   *  probe sets, and draws glyphs for them.
   *  @param consensus_seq  An AnnotatedBioSeq containing annotations which are probe sets.
   *  @param consensus_sym  A symmetry of depth 2 that maps the consensus onto the genome.
   *   (More generally, it maps the consensus onto SeqMapView.getAnnotatedSeq(). It should
   *    NOT have already been transformed to map onto SeqMapView.getViewSeq(), because then
   *    we couldn't guarantee that the depth would still be 2.)
   *  @param parent_glyph the Glyph representing the consensus sequence
   *  @param y coordinate of the "Exon" regions of the consensus glyph
   *  @param height height of the "Exon" regions of the consensus glyph
   */
  void drawConsensusAnnotations(AnnotatedBioSeq consensus_seq, SeqSymmetry consensus_sym, 
    GlyphI parent_glyph, TierGlyph tier, double y, double height) {
    int annot_count = consensus_seq.getAnnotationCount();
    for (int i=0; i<annot_count; i++) {
      SeqSymmetry sym = consensus_seq.getAnnotation(i);
      // probe sets and poly-A sites (and everything else) all get sent
      // to handleConsensusAnnotations, because the first few steps are the same
      handleConsensusAnnotations(sym, consensus_sym, parent_glyph, 
        y, height);
    }
  }

  void handleConsensusAnnotations(SeqSymmetry sym_with_probesets, SeqSymmetry consensus_sym, 
    GlyphI parent_glyph, double y, double height) {
    // Iterate until reaching depth=2 which represents a probeset (depth=2) containing probes (depth=1)
    int depth = SeqUtils.getDepth(sym_with_probesets);
    if (depth==2) {
      drawConsensusAnnotation(sym_with_probesets, consensus_sym, parent_glyph, y, height);
    } else {
      int child_count = sym_with_probesets.getChildCount();
      for (int i=0; i<child_count; i++) {
        SeqSymmetry child = sym_with_probesets.getChild(i);
        handleConsensusAnnotations(child, consensus_sym, parent_glyph, y, height);
      }
    }
  }
    
  /**
   *  Draws a probeset or poly-A site as a child of a parent_glyph.
   *  @param probeset  a symmetry representing a "probeset" containing "probes"
   *    or representing a "poly-A region".
   *    Should be of depth 2.  If depth>2, deeper children are ignored.
   *  @param consensus_sym a symmetry of depth 2 which can be used to transform the probeset
   *    to the SeqMapView.getAnnotatedSeq() coordinates.  If the depth is not 2,
   *    it is not likely that things would go well, so the method prints an error
   *    and returns.
   */
  void drawConsensusAnnotation(SeqSymmetry probeset, SeqSymmetry consensus_sym, 
    GlyphI parent_glyph, double y, double height) {
     int consensus_depth = SeqUtils.getDepth(consensus_sym);
     if (consensus_depth != 2) {
       System.out.println("***************** ERROR: consensus_depth is not 2, but is "+consensus_depth);
       return;
     }
    String meth = SeqMapView.determineMethod(probeset);
    DerivedSeqSymmetry probeset_sym = SeqUtils.copyToDerived(probeset);
    SeqUtils.transformSymmetry(probeset_sym, consensus_sym);
    // Note that the transformation generates a probeset_sym of depth 3

    String probeset_id = null;
//    boolean use_label = (ps_label_field != null && (probeset instanceof SymWithProps));
//    if (use_label) {
//      probeset_id = (String) ((SymWithProps) probeset).getProperty(ps_label_field);
//    }
    if (meth != null && meth.endsWith(POLY_A_SITE_METHOD)) {
      drawPolyA(probeset_sym, parent_glyph, probeset_id, y, height, poly_a_site_color);
    } else if (meth != null && meth.indexOf(POLY_A_STACK_METHOD) >= 0) {
      drawPolyA(probeset_sym, parent_glyph, probeset_id, y, height, poly_a_stack_color);
    } else {
      drawProbeSetGlyph(probeset_sym, parent_glyph, probeset_id, y, height);
    }
  }

  void drawPolyA(DerivedSeqSymmetry poly_A_sym, GlyphI consensus_glyph, 
    String probeset_id, double consensus_exon_y, double consensus_exon_height, Color color) {
    // The depth coming in should be 3
    SeqSymmetry transformed_sym = gviewer.transformForViewSeq(poly_A_sym);
    // After transformation, the depth is arbitrary, but we only deal with the top 3 levels

    SeqSpan span = transformed_sym.getSpan(gviewer.getViewSeq());
    if (span==null) {
      // this means the probeset doesn't map onto the coordinates of the view
      // In the Sliced view, this can happen easily and is not an error.
      return;
    }

    double height = consensus_exon_height/3;
    double y;

    if (span.isForward()) {y = consensus_exon_y;}
    else {y = consensus_exon_y + consensus_exon_height - height;}
    

    FillRectGlyph polyA_glyph_rect = new FillRectGlyph();
    polyA_glyph_rect.setColor(color);
    polyA_glyph_rect.setCoords(span.getMin(), y, span.getLength(), height);
    //polyA_glyph_rect.setAboveAxis(span.isForward());
    consensus_glyph.addChild(polyA_glyph_rect);
    gviewer.getSeqMap().setDataModelFromOriginalSym(polyA_glyph_rect, poly_A_sym);
  }  
  
  /**
   *  Draws glyphs for probeset and probes inside the parent glyph.
   *  @param probeset_sym  A symmetry of depth=3.
   *                   The top-level symmetry is assumed to be the probeset,
   *                   the children are assumed to be the probes, the probes
   *                   can be split into introns/exons.  Any levels deper than
   *                   this third level will be ignored.
   *                   This should NOT already have been mapped onto SeqMapView.getAnnotatedSeq().
   */
  void drawProbeSetGlyph(DerivedSeqSymmetry probeset_sym, GlyphI parent_glyph, 
    String probeset_id, double consensus_exon_y, double consensus_exon_height) {
    // The depth coming in should be 3
    SeqSymmetry transformed_probeset_sym = gviewer.transformForViewSeq(probeset_sym);
    // After transformation, the depth is arbitrary, but we only deal with the top 3 levels

    SeqSpan span = transformed_probeset_sym.getSpan(gviewer.getViewSeq());
    if (span==null) {
      // this means the probeset doesn't map onto the coordinates of the view
      // In the Sliced view, this can happen easily and is not an error.
      return;
    }

    //Rectangle2D parent_coords = parent_glyph.getCoordBox();
    double probe_height = consensus_exon_height/3;
    double probe_y = consensus_exon_y;
    if (span.isForward()) {probe_y = consensus_exon_y;}
    else {probe_y = consensus_exon_y + consensus_exon_height - probe_height;}

    Color probeset_color = ps_color;
    if (probeset_id != null) {
      String lc = probeset_id.toLowerCase();
      if (lc.indexOf("_s_") > 0) {
        probeset_color = ps_s_color;
      } else if (lc.indexOf("_x_") > 0) {
        probeset_color = ps_x_color;
      }
    }

    if (outline_probes_in_probeset) {
      GlyphI probeset_glyph = new EfficientOutlineContGlyph();
      probeset_glyph.setCoords(span.getMin(), probe_y, span.getLength(), probe_height);
      probeset_glyph.setColor(probeset_color);

      parent_glyph.addChild(probeset_glyph);
      gviewer.getSeqMap().setDataModelFromOriginalSym(probeset_glyph, probeset_sym);
      addProbesToProbeset(probeset_glyph, transformed_probeset_sym,
        probe_y, probe_height, probeset_color);
    } else {
      addProbesToProbeset(parent_glyph, transformed_probeset_sym,
        probe_y, probe_height, probeset_color);
    }

    // Optionally add a floating glyph that lines-up horizontally with the probeset
    if (do_independent_probeset_glyphs) {
      makeFloatingProbesetGlyph(probeset_color, span, probeset_id,
       probeset_sym, transformed_probeset_sym);
    }
  }

  /** @deprecated Not tested with AnnotStyle mechanism */
  void makeFloatingProbesetGlyph(Color probeset_color, SeqSpan span, String probeset_id,
    DerivedSeqSymmetry probeset_sym, SeqSymmetry transformed_probeset_sym) {
      GlyphI another_probeset_glyph = null;
      double floating_probeset_y = 100; // irrelevant due to packing
      double floating_probeset_height = GLYPH_HEIGHT;
      double child_y = floating_probeset_y;
      double child_height = floating_probeset_height;
      {
        if (ps_label_field != null) {
          LabelledGlyph lglyph = new EfficientLabelledGlyph();
          if (probeset_id != null) {
            lglyph.setLabel(probeset_id);
          } else {
            lglyph.setLabel("");
          }
          another_probeset_glyph =lglyph;        
          if (span.isForward()) { 
            child_y += floating_probeset_height;
            lglyph.setLabelLocation(LabelledGlyph.NORTH);
          } else {
            lglyph.setLabelLocation(LabelledGlyph.SOUTH);            
          }
          
          another_probeset_glyph.setCoords(span.getMin(), floating_probeset_y, span.getLength(), floating_probeset_height*2);
          another_probeset_glyph.setColor(probeset_color);

          GlyphI outline = new EfficientOutlineContGlyph();
          outline.setForegroundColor(probeset_color);
          gviewer.getSeqMap().setDataModelFromOriginalSym(outline, probeset_sym);
          outline.setCoords(span.getMin(), child_y, span.getLength(), child_height);
          addProbesToProbeset(outline, transformed_probeset_sym, child_y, child_height, probeset_color);

          another_probeset_glyph.addChild(outline);
        } else {
          another_probeset_glyph = new EfficientOutlineContGlyph();
          another_probeset_glyph.setCoords(span.getMin(), floating_probeset_y, span.getLength(), floating_probeset_height);
          another_probeset_glyph.setColor(probeset_color);
          addProbesToProbeset(another_probeset_glyph, transformed_probeset_sym, child_y, child_height, probeset_color);
        }
      }

      String meth = SeqMapView.determineMethod(probeset_sym.getOriginalSymmetry());
      if (meth==null) {meth = "unknown";}
      TierGlyph[] tiers = gviewer.getTiers(meth, false, null);
      if (span.isForward()) {
        tiers[0].addChild(another_probeset_glyph);
      } else {
        tiers[1].addChild(another_probeset_glyph);        
      }
      gviewer.getSeqMap().setDataModelFromOriginalSym(another_probeset_glyph, probeset_sym);
    }
  
  void addProbesToProbeset(GlyphI probeset_glyph, SeqSymmetry transformed_probeset_sym, 
    double probe_y, double probe_height, Color probeset_color) {
    int num_probes = transformed_probeset_sym.getChildCount();
    for (int i=0; i<num_probes; i++) {
      SeqSymmetry probe_sym = transformed_probeset_sym.getChild(i);
      GlyphI probe_glyph = drawProbeGlyph(probe_sym, probe_y, probe_height, probeset_color);
      if (probe_glyph == null) continue;
      probeset_glyph.addChild(probe_glyph);
      gviewer.getSeqMap().setDataModelFromOriginalSym(probe_glyph, probe_sym);
    }
  }
  
  GlyphI drawProbeGlyph(SeqSymmetry probe_sym, double probe_y, double probe_height, Color c) {
    SeqSpan probe_span = probe_sym.getSpan(gviewer.getViewSeq());
    if (probe_span == null) return null;

    int num_parts = probe_sym.getChildCount();
    GlyphI probe_glyph = null;
    if (num_parts > 1) {
      // Each probe can possibly be split into multiple exon/intron pieces,
      // so use something like a LineContainerGlyph
      probe_glyph = new LineContainerGlyph();
      probe_glyph.setCoords(probe_span.getMin(), probe_y, probe_span.getLength(), probe_height);
      probe_glyph.setColor(c);

      for (int i=0; i<num_parts; i++) {
        SeqSymmetry probe_part_sym = probe_sym.getChild(i);
        SeqSpan probe_part_span = probe_part_sym.getSpan(gviewer.getViewSeq());
        if (probe_part_span == null) continue;

        GlyphI probe_part_glyph = drawProbeSegmentGlyph(probe_part_span, probe_y, probe_height, c);
        probe_glyph.addChild(probe_part_glyph);
        gviewer.getSeqMap().setDataModelFromOriginalSym(probe_part_glyph, probe_part_sym);
      }
    } else {
      probe_glyph = drawProbeSegmentGlyph(probe_sym.getSpan(gviewer.getViewSeq()), probe_y, probe_height, c);
    }
    return probe_glyph;
  }
  
  /** Draws an individual segment of a probe.  Most probes will have only one segment,
   *  but probes that cover a region of a transcript that gets split into
   *  "Exons" can have multiple "parts".
   */
  GlyphI drawProbeSegmentGlyph(SeqSpan probe_part_span, double probe_y, double probe_height, Color c) {
    EfficientOutlinedRectGlyph probe_part_glyph = new EfficientOutlinedRectGlyph();
    probe_part_glyph.setCoords(probe_part_span.getMin(), probe_y, probe_part_span.getLength(), probe_height);
    probe_part_glyph.setColor(c);
    return probe_part_glyph;
  }
}
