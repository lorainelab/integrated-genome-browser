/**
 *   Copyright (c) 2006-2007 Affymetrix, Inc.
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

package com.affymetrix.genometryImpl.style;

import java.awt.Color;
import java.util.Map;

/** 
 * Tier style information.  This interface can be used regardless of 
 * whether the tier contains annotation data or graph data.
 */
public interface IAnnotStyle {
  public Color getColor();
  public void setColor(Color c);
  
  public boolean getShow();
  public void setShow(boolean b);
  
  public String getUniqueName();
  
  public String getHumanName();
  public void setHumanName(String s);
  
  public Color getBackground();
  public void setBackground(Color c);
  
  public boolean getCollapsed();
  public void setCollapsed(boolean b);

  /** Gets maximum rows of annotations to stack in the tier. */
  public int getMaxDepth();
  /** Sets maximum rows of annotations to stack in the tier. */
  public void setMaxDepth(int m);

  public void setHeight(double h);
  public double getHeight();
  
  public void setY(double y);
  public double getY();

  /** Whether setCollapsed() is allowed. In some styles collapse/expand has
   *  no meaning.  So getCollapsed() and getMaxDepth() has no meaning for those
   *  styles.
   */
  public boolean getExpandable();
  public void setExpandable(boolean b);
  
  public boolean isGraphTier();

  /**
   *  Gets a reference to a Map that can be used to store any arbitrary 
   *  extra properties.  This can be used to
   *  store all the properties of a UCSC track-line, for example.
   *  (These properties are not persisted in the java prefs system.)
   */
  public Map getTransientPropertyMap();
  
  public void copyPropertiesFrom(IAnnotStyle s);
}
