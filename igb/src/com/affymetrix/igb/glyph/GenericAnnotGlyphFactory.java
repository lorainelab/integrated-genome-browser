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
  

  static Color default_annot_color = Color.GREEN;
  static Color default_tier_color = Color.BLACK;

  static Class default_parent_class = (new ImprovedLineContGlyph()).getClass();
  static Class default_child_class = (new FillRectGlyph()).getClass();
  static Class default_eparent_class = (new EfficientLineContGlyph()).getClass();
  static Class default_echild_class = (new EfficientFillRectGlyph()).getClass();
  static Class default_labelled_parent_class = (new LabelledLineContGlyph2()).getClass();
  static Class default_elabelled_parent_class = (new EfficientLabelledLineGlyph()).getClass();

  static int DEFAULT_THICK_HEIGHT = 25;
  static int DEFAULT_THIN_HEIGHT = 15;

  SeqMapView gviewer;
  Color parent_color = default_annot_color;
  Color child_color = default_annot_color;
  String label_field = null;
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
    //parent_color = (Color)options.get("parent_color");
    //child_color = (Color)options.get("child_color");
    //if (parent_color == null) { parent_color = (Color)options.get("color"); }
    //if (child_color == null) { child_color = (Color)options.get("color"); }
    //if (parent_color == null) { parent_color = default_annot_color; }
    //if (child_color == null) { child_color = default_annot_color; }

    //label_field = (String)options.get("label_field");

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
        System.err.println("Class for parent glyph not found: " + parent_glyph_name);
        parent_glyph_class = default_parent_class;
      }
    }
    String child_glyph_name = (String)options.get("child_glyph");
    if (child_glyph_name != null) {
      try {
        child_glyph_class = ObjectUtils.classForName(child_glyph_name);
      }
      catch (Exception ex) {
        System.err.println("Class for child glyph not found: " + child_glyph_name);
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
    AffyTieredMap map = gviewer.getSeqMap();
    String meth = gviewer.determineMethod(sym);
    // System.out.println("method: " + meth);

    if (meth != null) {
      AnnotStyle style = AnnotStyle.getInstance(meth);
      parent_color = style.getColor();
      child_color = style.getColor();
      glyph_depth = style.getGlyphDepth();
      label_field = style.getLabelField();

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
    int depth = getDepth(sym);
    if (depth > desired_leaf_depth) {
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

    AffyTieredMap map = gviewer.getSeqMap();
    BioSeq annotseq = gviewer.getAnnotatedSeq();
    BioSeq coordseq = gviewer.getViewSeq();
    SeqSymmetry sym = insym;
    if (annotseq != coordseq) {
      sym = gviewer.transformForViewSeq(insym, annotseq);
    }

    SeqSpan pspan = sym.getSpan(coordseq);
    if (pspan == null) {
      return null;
    }  // if no span corresponding to seq, then return;

    // Find boundaries of the splices.  Used to draw glyphs for deletions.
    int[][] boundaries = null;
    if (DRAW_DELETION_GLYPHS && annotseq != coordseq && ADD_CHILDREN && sym.getChildCount() > 0) {
      boundaries = determineBoundaries(annotseq, coordseq);
    }
    
    boolean forward = pspan.isForward();
    TierGlyph the_tier = forward ? forward_tier : reverse_tier;

    GlyphI pglyph = null;

    // Note: Setting parent height (pheight) larger than the child height (cheight)
    // allows the user to select both the parent and the child as separate entities
    // in order to look at the properties associated with them.  Otherwise, the method
    // EfficientGlyph.pickTraversal() will only allow one to be chosen.
    double pheight = DEFAULT_THICK_HEIGHT + 0.0001;

    boolean use_label = (label_field != null && (label_field.trim().length()>0) && (insym instanceof SymWithProps));

    if (parent_and_child) {
      try  {
        if (use_label) {
          LabelledGlyph lglyph = (LabelledGlyph)parent_labelled_glyph_class.newInstance();
          Object property = ((SymWithProps)insym).getProperty(label_field);
          String label = (property == null) ? "" : property.toString();
          if (forward)  { lglyph.setLabelLocation(LabelledGlyph.NORTH); }
          else { lglyph.setLabelLocation(LabelledGlyph.SOUTH); }
          //          System.out.println("using label: " + label);
          lglyph.setLabel(label);
          pheight = 2 * pheight;
          pglyph = lglyph;
        }
        else {
          pglyph = (GlyphI)parent_glyph_class.newInstance();
        }
      }
      catch (Exception ex) { ex.printStackTrace(); }
      pglyph.setCoords(pspan.getMin(), 0, pspan.getLength(), pheight);
      //      pglyph.setColor(glyph_col);
      pglyph.setColor(parent_color);
      if (SET_PARENT_INFO) {
        map.setDataModelFromOriginalSym(pglyph, sym);
      }

      SeqSpan cdsSpan = null;
      SeqSymmetry cds_sym = null;

      if ((insym instanceof SupportsCdsSpan) && ((SupportsCdsSpan)insym).hasCdsSpan() )  {
        cdsSpan = ((SupportsCdsSpan)insym).getCdsSpan();
        MutableSeqSymmetry tempsym = new SimpleMutableSeqSymmetry();
        tempsym.addSpan(new SimpleMutableSeqSpan(cdsSpan));
        if (annotseq != coordseq) {
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
            
            if (DRAW_DELETION_GLYPHS && annotseq != coordseq) {
              // There is a missing child, so indicate it with a little glyph.
              
              int annot_span_min = child.getSpan(annotseq).getMin();
              while (j+1 < boundaries.length && annot_span_min >= boundaries[j+1][0]) {
                j++;
              }
              int gap_location = boundaries[j][1];
              
              EfficientFillRectGlyph boundary_glyph = new EfficientFillRectGlyph();
              boundary_glyph.setCoords(gap_location, -2, 1, DEFAULT_THICK_HEIGHT+4);
              boundary_glyph.setColor(child_color);
              boundary_glyph.setHitable(false);
              pglyph.addChild(boundary_glyph);
              
              Rectangle2D cb = pglyph.getCoordBox();
              if (cb.x > gap_location) {
                double end = cb.x + cb.width;
                cb.x = gap_location;
                cb.width = end - cb.x;
              } else if (cb.x + cb.width < gap_location) {
                cb.width = gap_location - cb.x;
              }
            }            
            
            continue;
          }
          
          GlyphI cglyph = null;

          try  { cglyph = (GlyphI)child_glyph_class.newInstance(); }
          catch (Exception ex) { ex.printStackTrace(); }

          int cheight = DEFAULT_THICK_HEIGHT;
          if (cdsSpan != null) {
            cheight = DEFAULT_THIN_HEIGHT;
            if (SeqUtils.contains(cdsSpan, cspan)) { cheight = DEFAULT_THICK_HEIGHT; }
            else if (SeqUtils.overlap(cdsSpan, cspan)) {
              try {
		SeqSymmetry cds_sym_2 = SeqUtils.intersection(cds_sym, child, annotseq);
		SeqSymmetry cds_sym_3 = cds_sym_2;
		if (annotseq != coordseq) {
		  cds_sym_3 = gviewer.transformForViewSeq(cds_sym_2, annotseq);
		}
		//SeqSpan cds_span = SeqUtils.intersection(cdsSpan, cspan);
		SeqSpan cds_span = cds_sym_3.getSpan(coordseq);
		GlyphI cds_glyph = (GlyphI)child_glyph_class.newInstance();
		cds_glyph.setCoords(cds_span.getMin(), 0, cds_span.getLength(), DEFAULT_THICK_HEIGHT);
		cds_glyph.setColor(parent_color);
		pglyph.addChild(cds_glyph);
		if (SET_CHILD_INFO) {
		  map.setDataModelFromOriginalSym(cds_glyph, cds_sym_3);
		}

              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          }
          cglyph.setCoords(cspan.getMin(), 0, cspan.getLength(), cheight);
          cglyph.setColor(child_color);
          pglyph.addChild(cglyph);
          if (SET_CHILD_INFO) {
            map.setDataModelFromOriginalSym(cglyph, child);
          }                    
        }
      }
    }
    else {  // depth !>= 2, so depth <= 1, so _no_ parent, use child glyph instead...
      try  {
        if (use_label) {
          LabelledGlyph lglyph = (LabelledGlyph)parent_labelled_glyph_class.newInstance();
          Object property = ((SymWithProps)insym).getProperty(label_field);
          String label = (property == null) ? "" : property.toString();
          if (forward)  { lglyph.setLabelLocation(LabelledGlyph.NORTH); }
          else { lglyph.setLabelLocation(LabelledGlyph.SOUTH); }
          //          System.out.println("using label: " + label);
          lglyph.setLabel(label);
          pheight = 2 * pheight;
          pglyph = lglyph;
        }
        else {
          pglyph = (GlyphI)child_glyph_class.newInstance();
        }
      }
      catch (Exception ex) { ex.printStackTrace(); }

      pglyph.setCoords(pspan.getMin(), 0, pspan.getLength(), pheight);
      //      pglyph.setColor(glyph_col);
      pglyph.setColor(parent_color);
      if (SET_PARENT_INFO) {
        map.setDataModelFromOriginalSym(pglyph, sym);
      }
    }

    if (forward)  { forward_tier.addChild(pglyph); }
    else { reverse_tier.addChild(pglyph); }
    return pglyph;
  }

  // a helper function used in drawing the "deletion" glyphs
  int[][] determineBoundaries(BioSeq annotseq, BioSeq coordseq) {
    int[][] boundaries = null;
    if (annotseq != coordseq) {
      MutableSeqSymmetry simple_sym = new SimpleMutableSeqSymmetry();
      simple_sym.addSpan(new SimpleMutableSeqSpan(0, annotseq.getLength(), annotseq));
      SeqSymmetry bounds_sym = gviewer.transformForViewSeq(simple_sym, annotseq);
      
      boundaries = new int[bounds_sym.getChildCount()+1][];
      
      SeqSymmetry child = bounds_sym.getChild(0);

      boundaries[0] = new int[2];
      boundaries[0][0] = Integer.MIN_VALUE;
      boundaries[0][1] = child.getSpan(coordseq).getMin();
      for (int qq = 1 ; qq < boundaries.length; qq++) {
        child = bounds_sym.getChild(qq-1);
        SeqSpan annot_span = child.getSpan(annotseq);
        SeqSpan coord_span = child.getSpan(coordseq);
        
        boundaries[qq] = new int[2];
        boundaries[qq][0] = annot_span.getMax();
        boundaries[qq][1] = coord_span.getMax();
      }
    }
    return boundaries;    
  }
}
