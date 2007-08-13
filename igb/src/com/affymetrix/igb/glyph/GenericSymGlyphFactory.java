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

import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.IAnnotStyleExtended;
import java.awt.*;
import java.util.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genoviz.widget.tieredmap.ExpandedTierPacker;

import com.affymetrix.genometry.*;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.igb.tiers.*;
import com.affymetrix.igb.view.SeqMapView;

/**
 *  A factory that can display arbitrary nesting levels of any symmetry (at least along the given BioSeq).
 *  This is in contrast to GenericAnnotGlyphFactory, which only shows one or two levels of the symmetry
 *     (usually the leaf nodes and their parents).
 */
public class GenericSymGlyphFactory implements MapViewGlyphFactoryI  {
  protected ExpandedTierPacker packer;
  int min_height = 10;
  int diff_height = 6;
  int yoffset = 20;
  int glyph_height = 10;
  int separator_height = 3;
  Color default_tier_color = Color.black;
  Color[] symcolors = { Color.blue, Color.green, Color.red, Color.white, Color.yellow};

  public GenericSymGlyphFactory() {
    packer = new ExpandedTierPacker();
    packer.setParentSpacer(diff_height/2);
    packer.setStretchHorizontal(false);
  }

  public void init(Map options) {
  }

  boolean isContainer(SeqSymmetry sym) {
    if (sym instanceof SymWithProps) {
      SymWithProps swp = (SymWithProps) sym;
      if (Boolean.TRUE.equals(swp.getProperty(SimpleSymWithProps.CONTAINER_PROP))) {
        return true;
      }
    }
    return false;
  }

  public void createGlyph(SeqSymmetry sym, SeqMapView smv) {
    createGlyph(sym, smv, false);
  }

  public void createGlyph(SeqSymmetry sym, SeqMapView smv, boolean next_to_axis) {

    if (isContainer(sym)) {
      for (int i=0; i<sym.getChildCount(); i++) {
        createGlyph(sym.getChild(i), smv, next_to_axis);
      }
      return;
    }

    String meth = SeqMapView.determineMethod(sym);
    if (meth == null && sym.getChildCount() <= 0) {
      meth = "unknown method";
    }

    if (meth != null) {
      IAnnotStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(meth);
      TierGlyph[] tiers = smv.getTiers(meth, next_to_axis, style);
      int tier_index = (sym.getSpan(0).isForward()) ? 0 : 1;
      glyphifySymmetry(smv, sym, tiers[tier_index], 0, glyph_height);
    }
    else {  // keep recursing down into child syms if parent sym has no "method" property
      System.out.println("Ackk, no method for symmetry");
    }
  }

  public GlyphI glyphifySymmetry(SeqMapView gviewer, SeqSymmetry insym, GlyphI parent_glyph,
			       int depth, int glyph_height) {

    NeoMap map = gviewer.getSeqMap();
    if (insym == null) { return null; }

    SeqSymmetry transformed_sym = gviewer.transformForViewSeq(insym);

    SeqSpan span = gviewer.getViewSeqSpan(transformed_sym);
    if (span == null) { return null; }

    Color col = symcolors[depth % symcolors.length];   // cycle through colors
    //    GlyphI gl = new FillRectGlyph();
    GlyphI gl = new OutlineRectGlyph();
    gl.setColor(col);

    gl.setCoords(span.getMin(), 0, span.getLength(), glyph_height);
    
    if (parent_glyph == null) {
      map.addItem(gl);        // if no parent glyph, add directly to map (or tier...)
    }
    else {
      parent_glyph.addChild(gl);
    }

    int childCount = insym.getChildCount();
    if (childCount > 0) {
      gl.setPacker(packer);
      // now recursively call glyphifySymmetry on children
      for (int i=0; i<childCount; i++) {
	SeqSymmetry childsym = insym.getChild(i);
	glyphifySymmetry(gviewer, childsym, gl, depth+1, glyph_height);
      }
      gl.pack(map.getView());
    }
    gviewer.getSeqMap().setDataModelFromOriginalSym(gl, insym);
    return gl;
  }


}
