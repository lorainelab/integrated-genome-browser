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

import com.affymetrix.genoviz.bioviews.GlyphI;

public interface LabelledGlyph extends GlyphI {
  public int getLabelLocation();
  public void setLabelLocation(int loc);
  public boolean getShowLabel();
  public void setShowLabel(boolean b);
  public String getLabel();
  public void setLabel(String str);
}
