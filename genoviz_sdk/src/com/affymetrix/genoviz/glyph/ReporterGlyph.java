/**
*   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.bioviews.*;

/**
 * A glyph for testing GenoViz drawing mechanism.
 * Prints notice to standard out whenever it is drawn.
 */
public class ReporterGlyph extends OutlineRectGlyph {
  int draw_count = 0;
  String label;

  public ReporterGlyph() {
    this("NoLabel");
  }

  public ReporterGlyph(String str) {
    super();
    this.label = str;
  }

  public void draw(ViewI view) {
    draw_count++;
    System.out.println("" + draw_count + "  ReporterGlyph.draw() called, label = " + label);
    super.draw(view);
  }

}
