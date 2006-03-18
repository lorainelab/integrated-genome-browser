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

package com.affymetrix.igb.tiers;

import java.awt.Color;

/** An interface with the minimum information needed for AnnotStyle.
 *  This can be used to generalize it to the GraphStyles.
 */
public interface IAnnotStyle {
  public Color getColor();
  public void setColor(Color c);
  
  public boolean getShow();
  public void setShow(boolean b);
  
  public Color getBackground();
  public boolean getCollapsed();
  public String getHumanName();
  public int getMaxDepth();
}
