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
import com.affymetrix.genoviz.widget.*;

import com.affymetrix.genometry.*;
import com.affymetrix.igb.genometry.SymWithProps;
import com.affymetrix.igb.genometry.TypeContainerAnnot;
import com.affymetrix.igb.tiers.*;
import com.affymetrix.igb.view.SeqMapView;
import java.util.*;


/**
 *  A factory for showing instances of GFF3Sym.
 */
public class GFF3GlyphFactory implements MapViewGlyphFactoryI  {
  SeqMapView gviewer;
  int glyph_height = 10;

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
      glyphifySymmetry(sym, style, tiers[0], tiers[1], 0, glyph_height);
    }
  }
  
  GlyphI glyphifySymmetry(SeqSymmetry insym, AnnotStyle style, TierGlyph ftier, TierGlyph rtier,
			       int depth, int glyph_height) {

    if (insym == null) { return null; }
    
    SeqSymmetry transformed_sym = gviewer.transformForViewSeq(insym);

    SeqSpan span = transformed_sym.getSpan(gviewer.getViewSeq());
    if (span == null) { return null; }
    boolean forward = span.isForward();

    String label_field = style.getLabelField();
    boolean use_label = (label_field != null && (label_field.trim().length()>0) && (insym instanceof SymWithProps));

    use_label = false; // labelling needs some improvement before I turn it on.
    
    GlyphI gl = null;
    if (! use_label) {
      gl = new EfficientFillRectGlyph();
    } else {
      EfficientLabelledGlyph lglyph = new EfficientLabelledLineGlyph();
      gl = lglyph;
      Object property = ((SymWithProps)insym).getProperty(label_field);
      String label = (property == null) ? "" : property.toString();
      if (forward)  { lglyph.setLabelLocation(LabelledGlyph.NORTH); }
      else { lglyph.setLabelLocation(LabelledGlyph.SOUTH); }
      //          System.out.println("using label: " + label);
      lglyph.setLabel(label);
    }
    gl.setColor(style.getColor());
    gl.setCoords(span.getMin(), 0, span.getLength(), glyph_height);

    if (forward) {
      ftier.addChild(gl);
    } else {
      rtier.addChild(gl);
    }

    int childCount = transformed_sym.getChildCount();
    if (childCount > 0) {
      // now recursively call glyphifySymmetry on children
      for (int i=0; i<childCount; i++) {
	SeqSymmetry childsym = insym.getChild(i); // should this be insym or transformed_sym ??
	glyphifySymmetry(childsym, style, ftier, rtier, depth+1, glyph_height);
      }
    }
    gl.setInfo(insym);
    return gl;
  }

}
