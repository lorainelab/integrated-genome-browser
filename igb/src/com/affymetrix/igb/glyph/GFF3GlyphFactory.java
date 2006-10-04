/**
*   Copyright (c) 2006 Affymetrix, Inc.
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

import com.affymetrix.genoviz.bioviews.*;

import com.affymetrix.genometry.*;
import com.affymetrix.igb.genometry.GFF3Sym;
import com.affymetrix.igb.genometry.SymWithProps;
import com.affymetrix.igb.genometry.TypeContainerAnnot;
import com.affymetrix.igb.parsers.GFF3Parser;
import com.affymetrix.igb.tiers.*;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.Color;
import java.util.*;


/**
 *  A factory for showing instances of GFF3Sym.
 */
public class GFF3GlyphFactory implements MapViewGlyphFactoryI  {
  SeqMapView gviewer;

  static GFF3GlyphFactory static_instance = null;
  
  public static GFF3GlyphFactory getInstance() {
    if (static_instance == null) {
      static_instance = new GFF3GlyphFactory();
    }
    return static_instance;
  }
  
  public GFF3GlyphFactory() {
  }

  public void init(Map options) {
  }

  public void setMapView(SeqMapView smv) {
    gviewer = smv;
  }

  public void createGlyph(SeqSymmetry sym, SeqMapView smv) {
    createGlyph(sym, smv, false);
  }

  public void createGlyph(SeqSymmetry sym, SeqMapView smv, boolean next_to_axis) {
    setMapView(smv);

    String meth = SeqMapView.determineMethod(sym);
    if (meth == null && sym.getChildCount() <= 0) {
      // this only happens if we recurse all the way to the bottom of the
      // feature tree without finding a method name.
      meth = "unknown method";
    }
    
    if (meth == null || sym instanceof TypeContainerAnnot) {
      // descend in feature tree until method(s) is found.
      int childCount = sym.getChildCount();
      if (childCount > 0) {
        for (int i=0; i<childCount; i++) {
          SeqSymmetry child = sym.getChild(i);
          createGlyph(child, smv, next_to_axis);
        }
      }
      return;
    }
    else { // meth != null
      AnnotStyle style = AnnotStyle.getInstance(meth);
      TierGlyph[] tiers = gviewer.getTiers(meth, next_to_axis, style);
      glyphifySymmetry(sym, style, tiers[0], tiers[1]);
    }
  }
  
  double thick_height = GenericAnnotGlyphFactory.DEFAULT_THICK_HEIGHT;
  double thin_height = GenericAnnotGlyphFactory.DEFAULT_THIN_HEIGHT;
  
  GlyphI glyphifySymmetry(SeqSymmetry insym, AnnotStyle style, TierGlyph ftier, TierGlyph rtier) {
    return glyphifySymmetry((GFF3Sym) insym, style, ftier, rtier, null);
  }
  
  GlyphI glyphifySymmetry(GFF3Sym insym, AnnotStyle style, TierGlyph ftier, TierGlyph rtier, GlyphI parent_glyph) {

    if (insym == null) { return null; }

    SeqSymmetry transformed_sym = gviewer.transformForViewSeq(insym);

    SeqSpan span = transformed_sym.getSpan(gviewer.getViewSeq());
    if (span == null) { 
      return null; 
    }
    boolean forward = span.isForward();

    String label_field = style.getLabelField();
    boolean use_label = (label_field != null && (label_field.trim().length()>0) && (insym instanceof SymWithProps));

    if (parent_glyph != null) {
      // if parent_glyph != null, then new glyph should not get labelled
      // only the parent can have a label. (mRNA has a label, but not its Exons,
      // but a free-floating Exon can have a label.)
      use_label = false;
    }

// For the initial version, all labels are forced to be OFF
use_label = false;

    String label = null;
    if (use_label) {
      Object property = insym.getProperty(label_field);
      label = (property == null) ? "" : property.toString();
    }
    
    GlyphI gl = null;
    GlyphI new_parent = null;
    double glyph_height = thick_height;
    Color c = style.getColor();

    if (GFF3Sym.FEATURE_TYPE_GENE.equalsIgnoreCase(insym.getFeatureType())) {
      //gl = new com.affymetrix.igb.glyph.DoublePointedGlyph();
      gl = makeSimpleGlyph(span, forward, label, style);
      new_parent = gl;
      glyph_height = thick_height + 0.0001;
    } else if (GFF3Sym.FEATURE_TYPE_MRNA.equalsIgnoreCase(insym.getFeatureType())) {
      if (insym.getChildCount() > 0) {
        gl = makeGroupGlyph(span, forward, label, style);
      } else {
        gl = makeSimpleGlyph(span, forward, label, style);
      }
      new_parent = gl;
      glyph_height = thick_height + 0.0001;
    }
    else if (GFF3Parser.GROUP_FEATURE_TYPE.equalsIgnoreCase(insym.getFeatureType())) {
      if (insym.getChildCount() > 0) {
        gl = makeGroupGlyph(span, forward, label, style);
      } else {
        gl = makeSimpleGlyph(span, forward, label, style);
      }
      new_parent = gl;
      glyph_height = thick_height + 0.0003;
    } else if (GFF3Sym.FEATURE_TYPE_EXON.equalsIgnoreCase(insym.getFeatureType())) {
      // Exons with a parent are thin (to contrast with CDS's)
      // Exons without a parent are thick (to avoid problems with Efficient packer)
      gl = makeSimpleGlyph(span, forward, label, style);
      new_parent = null;
      //if (parent_glyph == null) {
        glyph_height = thick_height + 0.0002; 
      //} else {
      //  glyph_height = thin_height + 0.0002;
      //}
    }
    else {
      gl = makeSimpleGlyph(span,forward,label,style);
      new_parent = null;
      if (GFF3Sym.FEATURE_TYPE_CDS.equalsIgnoreCase(insym.getFeatureType())) {
        glyph_height = thick_height;
      } else {
        glyph_height = thick_height;
      }
    }
    gl.setColor(c);
    gl.setCoords(span.getMin(), 0, span.getLength(), glyph_height);
    gviewer.getSeqMap().setDataModelFromOriginalSym(gl, insym);
    //gviewer.getSeqMap().setDataModel(gl, insym);

    
    if (! "source".equalsIgnoreCase(insym.getFeatureType())) {
      // draw ever featuer except "source" features, because they are ridiculously long
      
      if (parent_glyph != null) {
        parent_glyph.addChild(gl);
      } else {
        if (forward) {
          ftier.addChild(gl);
        } else {
          rtier.addChild(gl);
        }
      }
      
      gviewer.getSeqMap().setDataModelFromOriginalSym(gl, insym);
    }
    
    int childCount = insym.getChildCount();
    if (childCount > 0) {
      // now recursively call glyphifySymmetry on children
      for (int i=0; i<childCount; i++) {
	GFF3Sym childsym = (GFF3Sym) insym.getChild(i);
        if (GFF3Parser.GROUP_FEATURE_TYPE.equalsIgnoreCase(insym.getFeatureType())) {
           glyphifySymmetry(childsym, style, ftier, rtier, new_parent);
        }
        if (GFF3Sym.FEATURE_TYPE_MRNA.equalsIgnoreCase(insym.getFeatureType()) &&
            GFF3Sym.FEATURE_TYPE_EXON.equalsIgnoreCase(childsym.getFeatureType())) {
           glyphifySymmetry(childsym, style, ftier, rtier, new_parent);
        } else {
          //Only MRNA's and CDS-Groups are allowed to be parents.
          //(Multi-level nesting might make sense, but it is really hard to
          //visualize an mRNA that has two or more different CDS-groups as children.
          //So just make each CDS-Group be a different glyph.)
           glyphifySymmetry(childsym, style, ftier, rtier, null);          
        }
      }
    }
    return gl;
  }
  
  GlyphI makeGeneGlyph(SeqSpan span, boolean forward, AnnotStyle style) {
    GlyphI gl;
    gl = new com.affymetrix.igb.glyph.DoublePointedGlyph();
    return gl;
  }
  
  GlyphI makeSimpleGlyph(SeqSpan span, boolean forward, String label, AnnotStyle style) {
    GlyphI gl;
    if (label == null) {
      gl = new EfficientFillRectGlyph();
    } else {
      EfficientLabelledGlyph lglyph = new EfficientLabelledLineGlyph();
      gl = lglyph;
      if (forward)  { lglyph.setLabelLocation(LabelledGlyph.NORTH); }
      else { lglyph.setLabelLocation(LabelledGlyph.SOUTH); }
      lglyph.setLabel(label);
    }
    return gl;
  }
  
  GlyphI makeGroupGlyph(SeqSpan span, boolean forward, String label, AnnotStyle style) {
    GlyphI gl = null;

    if (label == null) {
      gl = new ImprovedLineContGlyph();
    } else {
      EfficientLabelledGlyph lglyph = new EfficientLabelledLineGlyph();
      gl = lglyph;
      //Object property = ((SymWithProps)insym).getProperty(label_field);
      //String label = (property == null) ? "" : property.toString();
      if (forward)  { lglyph.setLabelLocation(LabelledGlyph.NORTH); }
      else { lglyph.setLabelLocation(LabelledGlyph.SOUTH); }
      lglyph.setLabel(label);
    }
    return gl;
  }

}
