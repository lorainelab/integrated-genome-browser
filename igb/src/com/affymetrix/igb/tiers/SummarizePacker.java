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

package com.affymetrix.igb.tiers;

import java.awt.*;
import java.util.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.widget.NeoMap;

import com.affymetrix.igb.glyph.GlyphSummarizer;

public class SummarizePacker implements PaddedPackerI {
  double maxHeight = 0;
  protected double parent_spacer = 2;
  protected double spacing = 2;
  NeoMap map;

  public SummarizePacker(NeoMap map) {
    this.map = map;
  }

  public Rectangle pack(GlyphI parent, ViewI view) {
    Rectangle2D pbox = parent.getCoordBox();
    parent.setCoords(pbox.x, 0, pbox.width, 2 * parent_spacer);
    GlyphSummarizer summarizer = new GlyphSummarizer();
    summarizer.setScaleFactor(1.0f);
    GlyphI sumglyph = summarizer.getSummaryGlyph(parent, map);
    sumglyph.setColor(Color.red);
    sumglyph.moveAbsolute(sumglyph.getCoordBox().x, 0.0f);
    parent.addChild(sumglyph);;
    maxHeight = sumglyph.getCoordBox().height;
    adjustHeight(parent);

    System.out.println("trying to pack summarizer");
    return null;
  }

  protected void adjustHeight(GlyphI parent) {
    Rectangle2D pbox = parent.getCoordBox();
    //    parent.getCoordBox().height = maxHeight + (2 * parent_spacer);
    parent.setCoords(pbox.x, pbox.y, pbox.width, maxHeight + (2 * parent_spacer));
  }

  public void setParentSpacer(double spacer) {
    this.parent_spacer = spacer;
  }
    
  public double getParentSpacer() {
    return parent_spacer;
  }

  public void setSpacing(double sp) {
    this.spacing = sp;
  }

  public double getSpacing() {
    return spacing;
  }


  public Rectangle pack(GlyphI parent, GlyphI child, ViewI view) { return null; }

}








