/**
*   Copyright (c) 2001-2008 Affymetrix, Inc.
*    
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/

package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.util.NeoConstants.Placement;

/** Extends the LabelledGlyphI interface to allow the
 *  label to be turned on and off and for the position to
 *  be specified.
 * @author Ed Erwin
 */
public interface LabelledGlyph2 extends LabelledGlyphI {
  public Placement getLabelLocation();
  public void setLabelLocation(Placement loc);

  public boolean getShowLabel();
  public void setShowLabel(boolean b);
}
