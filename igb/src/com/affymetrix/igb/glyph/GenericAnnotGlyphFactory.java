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

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.util.*;
import com.affymetrix.genometry.span.SimpleMutableSeqSpan;
import com.affymetrix.genometry.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.igb.tiers.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.util.ObjectUtils;
import com.affymetrix.igb.view.SeqMapView;

public class GenericAnnotGlyphFactory implements MapViewGlyphFactoryI  {
  //static boolean SUPPRESS_GLYPHS = false;
  static boolean USE_EFFICIENT_GLYPHS = true;
  static boolean SET_PARENT_INFO = true;
  static boolean SET_CHILD_INFO = true;
  static boolean ADD_CHILDREN = true;
  static boolean OPTIMIZE_CHILD_MODEL = false;

  static Color default_annot_color = Color.GREEN;
  static Color default_tier_color = Color.BLACK;

  static Class default_parent_class = (new ImprovedLineContGlyph()).getClass();
  static Class default_child_class = (new FillRectGlyph()).getClass();
  static Class default_eparent_class = (new EfficientLineContGlyph()).getClass();
  static Class default_echild_class = (new EfficientFillRectGlyph()).getClass();
  static Class default_labelled_parent_class = (new LabelledLineContGlyph2()).getClass();
  static Class default_elabelled_parent_class = (new EfficientLabelledLineGlyph()).getClass();

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
    //    return createGlyph(sym, smv, false);
    createGlyph(sym, smv, false);
  }

  public void createGlyph(SeqSymmetry sym, SeqMapView smv, boolean next_to_axis) {
    //if (SUPPRESS_GLYPHS) { return; }
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
      
//      TierGlyph[] tiers = smv.getTiers(meth, next_to_axis, true, state.getColor(), default_tier_color);
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
        //        addAnnotationTiers(childSym);
        createGlyph(childSym, gviewer);

      }
    }
  }


  public void setMapView(SeqMapView smv) {
    gviewer = smv;
  }

  public void addLeafsToTier(SeqSymmetry sym,
                             TierGlyph ftier, TierGlyph rtier,
                             int desired_leaf_depth) {
    int depth = SeqUtils.getDepth(sym);
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
      addToTier(sym, ftier, rtier);
    }
  }

  int optimized_child_count = 0;
    
  public GlyphI addToTier(SeqSymmetry insym,
                          TierGlyph forward_tier,
                          TierGlyph reverse_tier) {
                            
    AffyTieredMap map = gviewer.getSeqMap();
    BioSeq annotseq = gviewer.getAnnotatedSeq();
    BioSeq coordseq = gviewer.getViewSeq();
    SeqSymmetry sym = gviewer.transformForViewSeq(insym);

    SeqSpan pspan = sym.getSpan(coordseq);
    if (pspan == null) {
      return null;
    }  // if no span corresponding to seq, then return;

    boolean forward = pspan.isForward();
    TierGlyph the_tier = forward ? forward_tier : reverse_tier;
        
    GlyphI pglyph = null;

    // Note: Setting parent height (pheight) larger than the child height (cheight)
    // allows the user to select both the parent and the child as separate entities
    // in order to look at the properties associated with them.  Otherwise, the method
    // EfficientGlyph.pickTraversal() will only allow one to be chosen.
    double pheight = 25.0001;

    boolean use_label = (label_field != null && (label_field.trim().length()>0) && (insym instanceof SymWithProps));

    if (SeqUtils.getDepth(sym) >= 2) {
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
          SeqSymmetry[] transform_path = gviewer.getTransformPath();
          SeqUtils.transformSymmetry(tempsym, transform_path);
          cdsSpan = tempsym.getSpan(coordseq);
        }
        cds_sym = tempsym;
      }

      if (ADD_CHILDREN) {
        int childCount = sym.getChildCount();
        for (int i=0; i<childCount; i++) {
          SeqSymmetry child = null;
          SeqSpan cspan = null;
          if (OPTIMIZE_CHILD_MODEL && (sym instanceof UcscPslSym)) {
            optimized_child_count++;
            //            if (optimized_child_count % 10000 == 0) {
            //              System.out.println("optimized child count: " + optimized_child_count);
            //            }
            UcscPslSym psym = (UcscPslSym)sym;
            psym.getChildSpan(i, coordseq, model_span);
            cspan = model_span;
            child = placeholder;
          }
          else {
            child = sym.getChild(i);
            cspan = child.getSpan(coordseq);
          }
          if (cspan == null) { continue; }
          GlyphI cglyph = null;

          try  { cglyph = (GlyphI)child_glyph_class.newInstance(); }
          catch (Exception ex) { ex.printStackTrace(); }

          int cheight = 25;
          if (cdsSpan != null) {
            cheight = 15;
            if (SeqUtils.contains(cdsSpan, cspan)) { cheight = 25; }
            else if (SeqUtils.overlap(cdsSpan, cspan)) {
              try {
              SeqSymmetry cds_sym_2 = SeqUtils.intersection(cds_sym, child, annotseq);
              SeqSymmetry cds_sym_3 = gviewer.transformForViewSeq(cds_sym_2);
              //SeqSpan cds_span = SeqUtils.intersection(cdsSpan, cspan);
              SeqSpan cds_span = cds_sym_3.getSpan(coordseq);
              GlyphI cds_glyph = (GlyphI) child_glyph_class.newInstance();
              cds_glyph.setCoords(cds_span.getMin(), 0, cds_span.getLength(), 25);
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
  
}
