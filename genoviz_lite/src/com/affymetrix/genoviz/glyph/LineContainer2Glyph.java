/**
*   Copyright (c) 1998-2007 Affymetrix, Inc.
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

import java.awt.*;
import java.util.*;
import com.affymetrix.genoviz.bioviews.*;

/**
 * A LineContainerGlyph that recursively sets the colors of its children.
 */
public class LineContainer2Glyph extends LineContainerGlyph  {

  @Override
  public void setColor(Color col) {
    super.setBackgroundColor(col);
    colorRecurse(this, col);
  }

  public void colorRecurse(GlyphI parent, Color col)  {
    if (parent.getChildren() == null) { return; }
    for (GlyphI child : parent.getChildren()) {
      child.setColor(col);
      colorRecurse(child, col);
    }
  }

}
