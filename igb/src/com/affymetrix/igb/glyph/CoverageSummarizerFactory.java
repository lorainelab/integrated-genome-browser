/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.igb.view.*;
import com.affymetrix.igb.tiers.*;

//TODO: finish this class, use in TierLabelManager.addSymCoverageTier
public class CoverageSummarizerFactory implements MapViewGlyphFactoryI  {
  static Color default_color = Color.red;
  static Color default_tier_color = Color.darkGray;
  static int default_glyph_height = 50;

  Color glyph_color = default_color;
  int glyph_height = default_glyph_height;

  /**
   *  style options:
   *    COVERAGE: standard viualization, shows plot of for each pixel in view, base-pair coverage
   *        (coords covered by spans / total coords in pixel)
   *    SIMPLE: just shows what regions are covered by spans as solid blocks
   */
  int style = CoverageSummarizerGlyph.DEFAULT_STYLE;

  public CoverageSummarizerFactory() {
    System.out.println("CoverageSummarizerFactory constructor called");
  }

  public void init(Map options) {
    System.out.println("CoverageSummarizerFactory.init() called");
    glyph_color = (Color)options.get("color");
    if (glyph_color == null) { glyph_color = default_color; }
    System.out.println("CoverageSummarizerFactory color set: " + glyph_color);
    if (options.get("height") != null) {
      try {
	glyph_height = Integer.parseInt((String)options.get("height"));
      }
      catch (Exception ex) {
	ex.printStackTrace();
	glyph_height = default_glyph_height;
      }
    }
    System.out.println("coverage glyph height = " + glyph_height);

    String style_name = (String)options.get("style");
    if (style_name != null)  {
      System.out.println("coverage glyph style = " + style_name);
      if (style_name.equalsIgnoreCase("coverage")) { style = CoverageSummarizerGlyph.COVERAGE; }
      if (style_name.equalsIgnoreCase("simple")) { style = CoverageSummarizerGlyph.SIMPLE; }
    }
    else {
      System.out.println("coverage glyph style set to default");
    }
  }

  public void createGlyph(SeqSymmetry sym, SeqMapView gviewer) {
    System.out.println("CoverageSummarizerFactory.createGlyph() called");
    System.out.println("symmetry child count: " + sym.getChildCount());
    SeqSpan span = sym.getSpan(0);
    System.out.println("symmetry span: " + SeqUtils.spanToString(span));

    //    AffyTieredMap map = gviewer.getSeqMap();
    String meth = gviewer.determineMethod(sym);

    if (meth != null) {
      System.out.println("in CoverageSummarizerFactory, annot type = " + meth);
      TierGlyph[] tiers = gviewer.getTiers(meth,
					   false, // next_to_axis = false
					   true, // use_fast_packer = true
					   glyph_color,  // fg_color (sets label color)
					   default_tier_color);  // bg_color
      TierGlyph ftier = tiers[0];
      TierGlyph rtier = tiers[1];
      AffyTieredMap map = gviewer.getSeqMap();

      BioSeq annotseq = gviewer.getAnnotatedSeq();
      BioSeq coordseq = gviewer.getViewSeq();
      SeqSymmetry tsym = sym;
      // transform symmetry to coordseq if annotseq != coordseq, like in the slice viewer
      if (annotseq != coordseq) {
	tsym = gviewer.transformForViewSeq(sym);
      }

      int child_count = tsym.getChildCount();
      // initializing list internal array length to child count to reduce list expansions...
      java.util.List leaf_spans = new ArrayList(child_count);
      SeqUtils.collectLeafSpans(tsym, coordseq, leaf_spans);

      CoverageSummarizerGlyph cov = new CoverageSummarizerGlyph();
      cov.setCoveredIntervals(leaf_spans);
      cov.setColor(glyph_color);
      cov.setStyle(style);
      cov.setCoords(0, 0, coordseq.getLength(), glyph_height);
      ftier.addChild(cov);

      // make sure set data model to the original sym (if transform was nneded then tsym will 
      //    probably be a DerivedSeqSymmetry with a reference to the original sym
      if (tsym instanceof DerivedSeqSymmetry)  {
	map.setDataModel(cov, ((DerivedSeqSymmetry)tsym).getOriginalSymmetry());
      }
      else {
	map.setDataModel(cov, tsym);
      }
      
    }
  }

}
