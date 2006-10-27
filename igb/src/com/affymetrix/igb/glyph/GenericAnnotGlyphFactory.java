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

package com.affymetrix.igb.glyph;

import java.awt.*;
import java.util.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.util.*;
import com.affymetrix.genometry.span.SimpleMutableSeqSpan;
import com.affymetrix.genometry.symmetry.SimpleDerivedSeqSymmetry;
import com.affymetrix.genometry.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.igb.tiers.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.util.ObjectUtils;
import com.affymetrix.igb.view.SeqMapView;

public class GenericAnnotGlyphFactory implements MapViewGlyphFactoryI  {
  static boolean USE_EFFICIENT_GLYPHS = true;
  static boolean SET_PARENT_INFO = true;
  static boolean SET_CHILD_INFO = true;
  static boolean ADD_CHILDREN = true;
  static boolean OPTIMIZE_CHILD_MODEL = false;
  
  /** Set to true if the we can assume the container SeqSymmetry being passed
   *  to addLeafsToTier has all its leaf nodes at the same depth from the top.
   */
  static final boolean ASSUME_CONSTANT_DEPTH = true;
  
  /**
   * Set to true to draw glyphs at locations of deletions.
   */
  static final boolean DRAW_DELETION_GLYPHS = true;
  
  static Class default_parent_class = (new ImprovedLineContGlyph()).getClass();
  static Class default_child_class = (new FillRectGlyph()).getClass();
  static Class default_eparent_class = (new EfficientLineContGlyph()).getClass();
  static Class default_echild_class = (new EfficientFillRectGlyph()).getClass();
  static Class default_labelled_parent_class = (new LabelledLineContGlyph2()).getClass();
  static Class default_elabelled_parent_class = (new EfficientLabelledLineGlyph()).getClass();

  static int DEFAULT_THICK_HEIGHT = 25;
  static int DEFAULT_THIN_HEIGHT = 15;

  SeqMapView gviewer;
  int glyph_depth = 2;  // default is depth = 2 (only show leaf nodes and parents of leaf nodes)

  MutableSeqSpan model_span = new SimpleMutableSeqSpan();
  SymWithProps placeholder = new SimpleSymWithProps();
  Class parent_glyph_class;
  Class child_glyph_class;
  Class parent_labelled_glyph_class;

  public GenericAnnotGlyphFactory() {
    if (USE_EFFICIENT_GLYPHS) {
      parent_glyph_class = default_eparent_class;
      child_glyph_class = default_echild_class;
      parent_labelled_glyph_class = default_elabelled_parent_class;
    }
    else {
      parent_glyph_class = default_parent_class;
      child_glyph_class = default_child_class;
      parent_labelled_glyph_class = default_labelled_parent_class;
    }
  }

  public void init(Map options) {

    String glyph_depth_string = (String)options.get("glyph_depth");
    if (glyph_depth_string != null) {
      glyph_depth = Integer.parseInt(glyph_depth_string);
    }

    String parent_glyph_name = (String)options.get("parent_glyph");
    if (parent_glyph_name != null) {
      try {
        parent_glyph_class = ObjectUtils.classForName(parent_glyph_name);
      }
      catch (Exception ex) {
        System.err.println();
        System.err.println("WARNING: Class for parent glyph not found: " + parent_glyph_name);
        System.err.println();
        parent_glyph_class = default_parent_class;
      }
    }
    String child_glyph_name = (String)options.get("child_glyph");
    if (child_glyph_name != null) {
      try {
        child_glyph_class = ObjectUtils.classForName(child_glyph_name);
      }
      catch (Exception ex) {
        System.err.println();
        System.err.println("WARNING: Class for child glyph not found: " + child_glyph_name);
        System.err.println();
        child_glyph_class = default_child_class;
      }
    }
  }

  public void createGlyph(SeqSymmetry sym, SeqMapView smv) {
    BioSeq aseq = smv.getAnnotatedSeq();
    BioSeq vseq = smv.getViewSeq();
    if (SeqMapView.DEBUG_COMP)  {
      System.out.println("called GenericAnnotGlyphFactory.createGlyph(sym,smv), " +
			 "annotated_seq = " + aseq.getID() + ", view_seq = " + vseq.getID() + ", " + (aseq == vseq));
      if (aseq != vseq) {
	SeqSymmetry comp = ((CompositeBioSeq)vseq).getComposition();
	SeqUtils.printSymmetry(comp);
      }
    }
    createGlyph(sym, smv, false);
  }
  
  public void createGlyph(SeqSymmetry sym, SeqMapView smv, boolean next_to_axis) {
    setMapView(smv);
    //AffyTieredMap map = gviewer.getSeqMap();

    if (sym instanceof GFF3Sym) {
      GFF3GlyphFactory.getInstance().createGlyph(sym, smv, next_to_axis);
      return;
    }
    
    String meth = gviewer.determineMethod(sym);
    if (meth != null) {
      AnnotStyle style = AnnotStyle.getInstance(meth);
      glyph_depth = style.getGlyphDepth();
      
      TierGlyph[] tiers = smv.getTiers(meth, next_to_axis, style);
      if (style.getSeparate()) {
        addLeafsToTier(sym, tiers[0], tiers[1], glyph_depth);
      } else {
        // use only one tier
        addLeafsToTier(sym, tiers[0], tiers[0], glyph_depth);
      }
    }
    else {  // keep recursing down into child syms if parent sym has no "method" property
      int childCount = sym.getChildCount();
      for (int i=0; i<childCount; i++) {
        SeqSymmetry childSym = sym.getChild(i);
        createGlyph(childSym, gviewer, false);
	//        createGlyph(childSym, gviewer, next_to_axis);
      }
    }
  }


  public void setMapView(SeqMapView smv) {
    gviewer = smv;
  }

  int getDepth(SeqSymmetry sym) {
    int depth = 1;
    SeqSymmetry current = sym;
    if (ASSUME_CONSTANT_DEPTH) {
      while (current.getChildCount() != 0) {
        current = current.getChild(0);
        depth++;
      }
    } else {
      depth = SeqUtils.getDepth(sym);
    }
    return depth;
  }

  public void addLeafsToTier(SeqSymmetry sym,
                             TierGlyph ftier, TierGlyph rtier,
                             int desired_leaf_depth) {

    if (sym instanceof GFF3Sym) {
      GFF3GlyphFactory.getInstance().createGlyph(sym, gviewer);
      return;
    }

    int depth = getDepth(sym);
    if (depth > desired_leaf_depth || sym instanceof TypeContainerAnnot) {
      for (int i=0; i<sym.getChildCount(); i++) {
        SeqSymmetry child = sym.getChild(i);
        addLeafsToTier(child, ftier, rtier, desired_leaf_depth);
      }
    }
    else if (depth < 1) {
      System.out.println("############## in GenericAnnotGlyphFactory, should never get here???");
    }
    else {  // depth == desired_leaf_depth
      addToTier(sym, ftier, rtier, (depth >= 2));
    }
  }
  
  static Color getScoreColor(SeqSymmetry insym, AnnotStyle style) {
    SeqSymmetry sym = insym;
    if (insym instanceof DerivedSeqSymmetry) {
      sym = ((DerivedSeqSymmetry) insym).getOriginalSymmetry();
    }
    
    if (sym instanceof Scored) {
      float score = ((Scored) sym).getScore();
      if (score != Float.NEGATIVE_INFINITY && score > 0.0f) {
        return style.getScoreColor(score);
      }
    }

    return style.getColor();
  }
  
  boolean allows_labels = true;

  /**
   *  @param parent_and_child  Whether to draw this sym as a parent and 
   *    also draw its children, or to just draw the sym itself 
   *   (using the child glyph style).  If this is set to true, then
   *    the symmetry must have a depth of at least 2.
   */
  public GlyphI addToTier(SeqSymmetry insym,
                          TierGlyph forward_tier,
                          TierGlyph reverse_tier, 
                          boolean parent_and_child) {
    
    GlyphI g = null;
    
    try {
      if (parent_and_child && insym.getChildCount() > 0) {
        g = doTwoLevelGlyph(insym, forward_tier, reverse_tier);
      } else {
        // depth !>= 2, so depth <= 1, so _no_ parent, use child glyph instead...
        g = doSingleLevelGlyph(insym, forward_tier, reverse_tier);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return g;
  }
  
  
  public GlyphI doTwoLevelGlyph(SeqSymmetry insym, TierGlyph forward_tier, TierGlyph reverse_tier)
  throws java.lang.InstantiationException, java.lang.IllegalAccessException {
    
    AffyTieredMap map = gviewer.getSeqMap();
    BioSeq annotseq = gviewer.getAnnotatedSeq();
    BioSeq coordseq = gviewer.getViewSeq();
    SeqSymmetry sym = insym;
    boolean same_seq = (annotseq == coordseq);
    
    if (! same_seq) {
      sym = gviewer.transformForViewSeq(insym, annotseq);
    }
    
    SeqSpan pspan = sym.getSpan(coordseq);
    if (pspan == null) {
      return null;
    }  // if no span corresponding to seq, then return;
    
    // Find boundaries of the splices.  Used to draw glyphs for deletions.
    int[][] boundaries = null;
    if (DRAW_DELETION_GLYPHS && ! same_seq && ADD_CHILDREN && sym.getChildCount() > 0) {
      boundaries = determineBoundaries(gviewer, annotseq);
    }
    
    boolean forward = pspan.isForward();
    TierGlyph the_tier = forward ? forward_tier : reverse_tier;
    
    GlyphI pglyph = null;
    
    double thick_height = DEFAULT_THICK_HEIGHT;
    double thin_height = DEFAULT_THIN_HEIGHT;
    
    // I hate having to do this cast to AnnotStyle.  But how can I avoid it?
    AnnotStyle the_style = (AnnotStyle) the_tier.getAnnotStyle();
    
    //double thick_height = the_style.getHeight();
    //double thin_height = the_style.getHeight() * 3.0/5.0;
    
    boolean use_score_colors = the_style.getColorByScore();
    
    // Note: Setting parent height (pheight) larger than the child height (cheight)
    // allows the user to select both the parent and the child as separate entities
    // in order to look at the properties associated with them.  Otherwise, the method
    // EfficientGlyph.pickTraversal() will only allow one to be chosen.
    double pheight = thick_height + 0.0001;
    
    boolean use_label = false;
    String label_field = the_style.getLabelField();
    if (allows_labels) {
      use_label = (label_field != null && (label_field.trim().length()>0) && (insym instanceof SymWithProps));
    }
      
    if (use_label) {
      LabelledGlyph lglyph = (LabelledGlyph)parent_labelled_glyph_class.newInstance();
      Object property = null;
      if (insym instanceof DerivedSeqSymmetry) {
        SymWithProps swp = (SymWithProps) ((DerivedSeqSymmetry) insym).getOriginalSymmetry();
        property = swp.getProperty(label_field);
      } else {
        property = ((SymWithProps) insym).getProperty(label_field);
      }
      String label = (property == null) ? "" : property.toString();
      if (the_tier.getDirection() == TierGlyph.DIRECTION_REVERSE) {
        lglyph.setLabelLocation(LabelledGlyph.SOUTH);
      } else { lglyph.setLabelLocation(LabelledGlyph.NORTH); }
      //          System.out.println("using label: " + label);
      lglyph.setLabel(label);
      pheight = 2 * pheight;
      pglyph = lglyph;
    } else {
      pglyph = (GlyphI)parent_glyph_class.newInstance();
    }

    pglyph.setCoords(pspan.getMin(), 0, pspan.getLength(), pheight);
    if (use_score_colors) {
      pglyph.setColor(getScoreColor(insym, the_style));
    } else {
      pglyph.setColor(the_style.getColor());
    }
    if (SET_PARENT_INFO) {
      map.setDataModelFromOriginalSym(pglyph, sym);
    }
    
    SeqSpan cdsSpan = null;
    SeqSymmetry cds_sym = null;
    
    if ((insym instanceof SupportsCdsSpan) && ((SupportsCdsSpan)insym).hasCdsSpan() )  {
      cdsSpan = ((SupportsCdsSpan)insym).getCdsSpan();
      MutableSeqSymmetry tempsym = new SimpleMutableSeqSymmetry();
      tempsym.addSpan(new SimpleMutableSeqSpan(cdsSpan));
      if (! same_seq) {
        SeqUtils.transformSymmetry(tempsym, gviewer.getTransformPath());
        cdsSpan = tempsym.getSpan(coordseq);
      }
      cds_sym = tempsym;
    }
    
    if (ADD_CHILDREN) {
      int childCount = sym.getChildCount();
      int j = 0;
      for (int i=0; i<childCount; i++) {
        SeqSymmetry child = null;
        SeqSpan cspan = null;
        child = sym.getChild(i);
        cspan = child.getSpan(coordseq);
        
        if (cspan == null) {
          
          if (DRAW_DELETION_GLYPHS && ! same_seq && boundaries != null) {
            // There is a missing child, so indicate it with a little glyph.
            
            int annot_span_min = child.getSpan(annotseq).getMin();
            while (j+1 < boundaries.length && annot_span_min >= boundaries[j+1][0]) {
              j++;
            }
            int gap_location = boundaries[j][1];
            
            DeletionGlyph boundary_glyph = new DeletionGlyph();
            boundary_glyph.setCoords((double) gap_location, 0.0, 1.0, (double) thin_height);
            boundary_glyph.setColor(pglyph.getColor());
            //boundary_glyph.setHitable(false);
            pglyph.addChild(boundary_glyph);
            
            // Now stretch the parent glyph so that it includes this glyph.
            Rectangle2D cb = pglyph.getCoordBox();
            if (cb.x > gap_location) {
              cb.width += cb.x - gap_location;
              cb.x = gap_location;
            } else if (cb.x + cb.width < gap_location) {
              cb.width = gap_location - cb.x;
            }
          }
          
          continue;
        }
        
        GlyphI cglyph = (GlyphI)child_glyph_class.newInstance();
        
        double cheight = thick_height;
        if (cdsSpan != null) {
          cheight = thin_height;
          if (SeqUtils.contains(cdsSpan, cspan)) { cheight = thick_height; } else if (SeqUtils.overlap(cdsSpan, cspan)) {
            
            SeqSymmetry cds_sym_2 = SeqUtils.intersection(cds_sym, child, annotseq);
            SeqSymmetry cds_sym_3 = cds_sym_2;
            if (! same_seq) {
              cds_sym_3 = gviewer.transformForViewSeq(cds_sym_2, annotseq);
            }
            SeqSpan cds_span = cds_sym_3.getSpan(coordseq);
            if (cds_span != null) {
              GlyphI cds_glyph = (GlyphI)child_glyph_class.newInstance();
              cds_glyph.setCoords(cds_span.getMin(), 0, cds_span.getLength(), thick_height);
              if (use_score_colors) {
                cds_glyph.setColor(getScoreColor(cds_sym, the_style));
              } else {
                cds_glyph.setColor(the_style.getColor());
              }
              pglyph.addChild(cds_glyph);
              if (SET_CHILD_INFO) {
                map.setDataModelFromOriginalSym(cds_glyph, cds_sym_3);
              }
            }
          }
        }
        cglyph.setCoords(cspan.getMin(), 0, cspan.getLength(), cheight);
        if (use_score_colors) {
          cglyph.setColor(getScoreColor(child, the_style));
        } else {
          cglyph.setColor(the_style.getColor());
        }
        pglyph.addChild(cglyph);
        if (SET_CHILD_INFO) {
          map.setDataModelFromOriginalSym(cglyph, child);
        }
      }
    }
    
    the_tier.addChild(pglyph);
    return pglyph;
  }

  GlyphI doSingleLevelGlyph(SeqSymmetry insym, TierGlyph forward_tier, TierGlyph reverse_tier)
    throws java.lang.InstantiationException, java.lang.IllegalAccessException {
    
    AffyTieredMap map = gviewer.getSeqMap();
    BioSeq annotseq = gviewer.getAnnotatedSeq();
    BioSeq coordseq = gviewer.getViewSeq();
    SeqSymmetry sym = insym;
    boolean same_seq = (annotseq == coordseq);
    
    if (! same_seq) {
      sym = gviewer.transformForViewSeq(insym, annotseq);
    }
    
    SeqSpan pspan = sym.getSpan(coordseq);
    if (pspan == null) {
      return null;
    }  // if no span corresponding to seq, then return;
    
    boolean forward = pspan.isForward();
    TierGlyph the_tier = forward ? forward_tier : reverse_tier;
    
    GlyphI pglyph = null;
    
    // I hate having to do this cast to AnnotStyle.  But how can I avoid it?
    AnnotStyle the_style = (AnnotStyle) the_tier.getAnnotStyle();
    
    boolean use_score_colors = the_style.getColorByScore();
    
    // Note: Setting parent height (pheight) larger than the child height (cheight)
    // allows the user to select both the parent and the child as separate entities
    // in order to look at the properties associated with them.  Otherwise, the method
    // EfficientGlyph.pickTraversal() will only allow one to be chosen.
    double pheight = DEFAULT_THICK_HEIGHT + 0.0001;
    
    boolean use_label = false;
    String label_field = the_style.getLabelField();
    if (allows_labels) {
      use_label = (label_field != null && (label_field.trim().length()>0) && (insym instanceof SymWithProps));
    }

    if (use_label) {
      LabelledGlyph lglyph = (LabelledGlyph)parent_labelled_glyph_class.newInstance();
      Object property = null;
      if (insym instanceof DerivedSeqSymmetry) {
        SymWithProps swp = (SymWithProps)  ((DerivedSeqSymmetry) insym).getOriginalSymmetry();
        property = swp.getProperty(label_field);
      } else {
        property = ((SymWithProps)insym).getProperty(label_field);
      }
      String label = (property == null) ? "" : property.toString();
      if (the_tier.getDirection() == TierGlyph.DIRECTION_REVERSE) {
        lglyph.setLabelLocation(LabelledGlyph.SOUTH);
      } else { lglyph.setLabelLocation(LabelledGlyph.NORTH); }
      lglyph.setLabel(label);
      pheight = 2 * pheight;
      pglyph = lglyph;
    } else {
      pglyph = (GlyphI)child_glyph_class.newInstance();
    }
    
    pglyph.setCoords(pspan.getMin(), 0, pspan.getLength(), pheight);
    if (use_score_colors) {
      // is the insym or sym?
      pglyph.setColor(getScoreColor(insym, the_style));
    } else {
      pglyph.setColor(the_style.getColor());
    }
    if (SET_PARENT_INFO) {
      map.setDataModelFromOriginalSym(pglyph, sym);
    }
    
    the_tier.addChild(pglyph);
    return pglyph;
  }
  
  
  //TODO: Move this to SeqMapView or AltSpliceView, 
  // so boundaries don't have to be re-computed over and over  
  
  // a helper function used in drawing the "deletion" glyphs
  static int[][] determineBoundaries(SeqMapView gviewer, BioSeq annotseq) {
    int[][] boundaries = null;
    if (annotseq != gviewer.getViewSeq()) {
      MutableSeqSymmetry simple_sym = new SimpleMutableSeqSymmetry();
      simple_sym.addSpan(new SimpleMutableSeqSpan(0, annotseq.getLength(), annotseq));
      SeqSymmetry bounds_sym = gviewer.transformForViewSeq(simple_sym, annotseq);
      
      boundaries = determineBoundaries(bounds_sym, annotseq, gviewer.getViewSeq());
    }
    return boundaries;
  }

  // a helper function used in drawing the "deletion" glyphs
  // Returns an int array of size [n][2] where [n][0] is the boundary in terms of seq0
  // and [n][1] is the corresponding boundary in terms of seq1.
  // Value [0][0] = Integer.MIN_VALUE and [0][1] = minimum of first span in seq1.
  // For all other [n][0] and [n][1] it is the value of the Maximum of the boundary
  // span in the seq0 or seq1.
  static int[][] determineBoundaries(SeqSymmetry bounds_sym, BioSeq seq0, BioSeq seq1) {
    int[][] boundaries = null;
    if (seq0 != seq1) {
      MutableSeqSymmetry simple_sym = new SimpleMutableSeqSymmetry();
      simple_sym.addSpan(new SimpleMutableSeqSpan(0, seq0.getLength(), seq0));

      int child_count = bounds_sym.getChildCount();
      if (child_count == 0) {
        return null;
      }
      
      boundaries = new int[child_count+1][];
      
      SeqSymmetry child = bounds_sym.getChild(0);

      boundaries[0] = new int[2];
      boundaries[0][0] = Integer.MIN_VALUE;
      boundaries[0][1] = child.getSpan(seq1).getMin();
      for (int qq = 1 ; qq < boundaries.length; qq++) {
        child = bounds_sym.getChild(qq-1);
        SeqSpan annot_span = child.getSpan(seq0);
        SeqSpan coord_span = child.getSpan(seq1);
        
        boundaries[qq] = new int[2];
        boundaries[qq][0] = annot_span.getMax();
        boundaries[qq][1] = coord_span.getMax();
      }
    }
    return boundaries;    
  }

  // Copies to a derived SeqSymmetry without copying any of the children.
  public static DerivedSeqSymmetry copyToDerivedNonRecursive(SeqSymmetry sym) {
    DerivedSeqSymmetry der = getDerived();

    der.clear(); // redundant, but ok

    if (sym instanceof DerivedSeqSymmetry) {
      der.setOriginalSymmetry(((DerivedSeqSymmetry)sym).getOriginalSymmetry());
    }
    else {
      der.setOriginalSymmetry(sym);
    }
    int spanCount = sym.getSpanCount();
    for (int i=0; i<spanCount; i++) {
      // just point to spans rather than copying them.
      // we can trust that the glyph factories won't modify these spans,
      // although it may add new spans.
      SeqSpan span = sym.getSpan(i);
      der.addSpan(span);
    }
    return der;
  }

  static java.util.Stack derived_stack = new Stack();
  static final int max_stack_size = 100;
  
  static DerivedSeqSymmetry getDerived() {
    if (derived_stack.isEmpty()) {
      return new SimpleDerivedSeqSymmetry();
    } else {
      return (DerivedSeqSymmetry) derived_stack.pop();
    }
  }
  
  static void recycleDerived(DerivedSeqSymmetry sym) {
    for (int i=sym.getChildCount()-1; i>=0; i--) {
      recycleDerived((DerivedSeqSymmetry) sym.getChild(i));
    }
    
    sym.clear();
    sym.setOriginalSymmetry(null);
    if (derived_stack.size() < max_stack_size) {
      derived_stack.push(sym);
    }
  }

  /** A small x-shaped glyph that can be used to indicate a deleted exon 
   * in the slice view. 
   */
  public static class DeletionGlyph extends SolidGlyph {

    /** Draws a small "X". */
    public void draw(ViewI view) {
      Rectangle pixelbox = view.getScratchPixBox();
      view.transformToPixels(this.coordbox, pixelbox);
      Graphics g = view.getGraphics();
      
      // Unlikely this will ever be big enough to need the fix.
      //EfficientSolidGlyph.fixAWTBigRectBug(view, pixelbox);
      
      //pixelbox.width = Math.max( pixelbox.width, min_pixels_width );
      pixelbox.height = Math.max( pixelbox.height, min_pixels_height );
      
      final int half_height = pixelbox.height/2;
      final int h = Math.min(half_height, 4);
      
      final int x1 = pixelbox.x - h;
      final int x2 = pixelbox.x + h;

      final int y1 = pixelbox.y + half_height - h;
      final int y2 = pixelbox.y + half_height + h;

      g.setColor(getBackgroundColor()); // this is the tier foreground color

      g.drawLine(x1, y1, x2, y2);
      g.drawLine(x1, y2, x2, y1);
    
      super.draw(view);
    }
    
    /** Overridden to always return false. */
    public boolean isHitable() {
      return false;
    }
  }
}
