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
import com.affymetrix.igb.genometry.TypeContainerAnnot;
import com.affymetrix.igb.parsers.GFF3Parser;
import com.affymetrix.igb.tiers.*;
import com.affymetrix.igb.view.SeqMapView;
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
    default_glyph_factory.setMapView(smv);
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

  GenericAnnotGlyphFactory default_glyph_factory = new GenericAnnotGlyphFactory();
  
  GlyphI glyphifySymmetry(GFF3Sym insym, AnnotStyle style, TierGlyph ftier, TierGlyph rtier, GlyphI parent_glyph) {

    if (insym == null) { return null; }
    
    GlyphI gl = null;

    String feat_type = insym.getFeatureType();
    if (feat_type == null) { feat_type = "";}
    else {feat_type = feat_type.toLowerCase();}
    
    if (GFF3Sym.FEATURE_TYPE_MRNA.equalsIgnoreCase(insym.getFeatureType())) {
      gl = drawMRNA(insym, style, ftier, rtier);
      return gl; // recursion handled inside that method
    }
    else if (insym.isMultiLine()) {
      // If this is a CDS group, it will be drawn as an mRNA-type thing,
      // otherwise just recurses down to the children and draws them.
      gl = drawGroup(insym, style, ftier, rtier);
      return gl; // recursion handled inside that method
    }
    
    else {
      // do a general case: 
      gl = default_glyph_factory.addToTier(insym, ftier, rtier, false);
      // now allow recursion;
    }


    // now recursively call glyphifySymmetry on children
    for (int i=0; i<insym.getChildCount(); i++) {
      SeqSymmetry childsym = insym.getChild(i);
      glyphifySymmetry((GFF3Sym) childsym, style, ftier, rtier, null);
    }
    return gl;
  }
  
  GlyphI drawMRNA(GFF3Sym insym, AnnotStyle style, TierGlyph ftier, TierGlyph rtier) {
    
    GlyphI gl = null;
    
    MutableSeqSymmetry mrnaSym = GenericAnnotGlyphFactory.copyToDerivedNonRecursive(insym);
    int childCount = insym.getChildCount();
    ArrayList other_children = new ArrayList(childCount);
    for (int i=0; i<childCount; i++) {
      GFF3Sym childsym = (GFF3Sym) insym.getChild(i);
      if (GFF3Sym.FEATURE_TYPE_EXON.equalsIgnoreCase(childsym.getFeatureType())) {
        MutableSeqSymmetry exonSym = GenericAnnotGlyphFactory.copyToDerivedNonRecursive(childsym);
        mrnaSym.addChild(exonSym);
        
        // collect any children of the childsym to draw later
        for (int j=0; j<childsym.getChildCount(); j++) {
          other_children.add(childsym.getChild(j));
        }

      } else {
        // collect any "other" children of the groupsym to draw later
        other_children.add(childsym);
      }
    }
    gl = default_glyph_factory.addToTier(mrnaSym, ftier, rtier, true);
    
    for (int i=0; i<other_children.size(); i++) {
      GFF3Sym childsym = (GFF3Sym) other_children.get(i);
      glyphifySymmetry(childsym, style, ftier, rtier);
    }
    return gl;
  }
  
  // Draws a CDS group feature
  // (If this is a group of something other than CDS segments, this just recurses
  //  down to the children and draws them in their normal way.)
  GlyphI drawGroup(GFF3Sym insym, AnnotStyle style, TierGlyph ftier, TierGlyph rtier) {
    // Theoretically, this method could be simplified because the "group" symmetries
    // created from multi-line features should have children that are all of the same type,
    // and which have no children of their own.  (The group itself can have other children.)
    // But I'm treating the most general case, even if it shouldn't occur.
    GlyphI gl = null;
    
    MutableSeqSymmetry groupSym = GenericAnnotGlyphFactory.copyToDerivedNonRecursive(insym);
    int childCount = insym.getChildCount();
    ArrayList other_children = new ArrayList(childCount);
    for (int i=0; i<childCount; i++) {

      GFF3Sym childsym = (GFF3Sym) insym.getChild(i);
      if (GFF3Sym.FEATURE_TYPE_CDS.equalsIgnoreCase(childsym.getFeatureType())) {
        MutableSeqSymmetry cdsSym = GenericAnnotGlyphFactory.copyToDerivedNonRecursive(childsym);
        groupSym.addChild(cdsSym);
        
        // collect any children of the childsym to draw later
        for (int j=0; j<childsym.getChildCount(); j++) {
          other_children.add(childsym.getChild(j));
        }

      } else {
        // collect any "other" children of the groupsym to draw later
        other_children.add(childsym);
      }

    }
    if (groupSym.getChildCount() >= 1) {
      gl = default_glyph_factory.addToTier(groupSym, ftier, rtier, true);
    } else {
      // do what?
    }

    for (int i=0; i<other_children.size(); i++) {
      GFF3Sym childsym = (GFF3Sym) other_children.get(i);
      glyphifySymmetry(childsym, style, ftier, rtier);
    }
    return gl;
  }
}
