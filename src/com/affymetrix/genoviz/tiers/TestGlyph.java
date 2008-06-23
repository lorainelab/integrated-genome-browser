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

package com.affymetrix.genoviz.tiers;

import java.awt.*;
import java.awt.event.*;
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;

public class TestGlyph extends FillRectGlyph {
  
  String label;

  public TestGlyph(String label) {
    super();
    this.label = label;
  }

  public String toString() {
    return (label + ", y= " + getCoordBox().y);
  }

  public boolean withinView(ViewI view) {
    //    System.out.println("within view: " + super.withinView(view));
    return super.withinView(view);
  }

  public void draw(ViewI view) {
    view.transformToPixels(coordbox, pixelbox);
    //    System.out.println("TestGlyph coords: y = " + coordbox.y + ", height = " + coordbox.height);
    //    System.out.println("TestGlyph pixels: y = " + pixelbox.y + ", height = " + pixelbox.height);
    super.draw(view);
  }

}


