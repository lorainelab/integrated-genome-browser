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

/** Basic implementation of IAnnotStyle. */
public class DefaultIAnnotStyle implements IAnnotStyle {
  Color fg = Color.WHITE;
  Color bg = Color.BLACK;
  boolean show = true;
  boolean collapsed = false;
  int max_depth = 0;
  String name = "";
  
  public DefaultIAnnotStyle() {
    super();
  }
  
  public DefaultIAnnotStyle(String name) {
    this.name = name;
  }
  
  public Color getColor() { return fg; }
  public void setColor(Color c) { fg = c; }
  
  public boolean getShow() { return show; }
  public void setShow(boolean b) { show = b; }
  
  public String getHumanName() { return name; }
  public void setHumanName(String s) { name = s; }
  
  public Color getBackground() { return bg; }
  public void setBackground(Color c) { bg = c; }
  
  public boolean getCollapsed() { return collapsed; }
  public void setCollapsed(boolean b) { collapsed = b; }
  
  public int getMaxDepth() { return max_depth; }
  public void setMaxDepth(int m) { max_depth = m; }
}
