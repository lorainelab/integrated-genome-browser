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

package com.affymetrix.genoviz.tiers;

import java.util.Comparator;
import com.affymetrix.genoviz.bioviews.GlyphI;

public class GlyphMinComparator implements Comparator<GlyphI> {

  public int compare(GlyphI g1, GlyphI g2) {
    if (g1.getCoordBox().x < g2.getCoordBox().x) { return -1; }
    else if (g1.getCoordBox().x > g2.getCoordBox().x) { return 1; }
    else { return 0; }
  }

}
