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

import java.util.Comparator;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.Rectangle2D;

public class GlyphMinComparator implements Comparator {

  public int compare(Object obj1, Object obj2) {
    GlyphI g1 = (GlyphI)obj1;
    GlyphI g2 = (GlyphI)obj2;
    if (g1.getCoordBox().x < g2.getCoordBox().x) { return -1; }
    else if (g1.getCoordBox().x > g2.getCoordBox().x) { return 1; }
    else { return 0; }
  }

}
