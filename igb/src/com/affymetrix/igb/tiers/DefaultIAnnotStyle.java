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

package com.affymetrix.igb.tiers;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/** Basic implementation of IAnnotStyle. */
public class DefaultIAnnotStyle implements IAnnotStyle {
  Color fg = Color.WHITE;
  Color bg = Color.BLACK;
  boolean show = true;
  boolean collapsed = false;
  boolean expandable = true;
  int max_depth = 0;
  String unique_name = "";
  String human_name = "";
  double height = 60;
  double y = 0.0f;
  boolean is_graph = false;
  Map transient_properties = null;
  
  public DefaultIAnnotStyle() {
    super();
    this.unique_name = Integer.toHexString(hashCode()); // a unique name, just in case it is ever needed
    this.fg = AnnotStyle.getDefaultInstance().getColor();
    this.bg = AnnotStyle.getDefaultInstance().getBackground();
  }
  
  public DefaultIAnnotStyle(String name, boolean graph) {
    this();
    this.unique_name = name;
    this.human_name = name;
    this.is_graph = graph;
  }
  
  public boolean isGraphTier() {
    return is_graph;
  }
  
  public Color getColor() { return fg; }
  public void setColor(Color c) { fg = c; }
  
  public boolean getShow() { return show; }
  public void setShow(boolean b) { show = b; }
  
  public String getUniqueName() { return unique_name; }
  
  public String getHumanName() { return human_name; }
  public void setHumanName(String s) { human_name = s; }
  
  public Color getBackground() { return bg; }
  public void setBackground(Color c) { bg = c; }
  
  public boolean getCollapsed() { return collapsed; }
  public void setCollapsed(boolean b) { collapsed = b; }
  
  public int getMaxDepth() { return max_depth; }
  public void setMaxDepth(int m) { max_depth = m; }
      
  public void setHeight(double h) { height = h; }
  public double getHeight() { return height; }

  public void setY(double yval) { y = yval; }
  public double getY() { return y; }

  public void setExpandable(boolean b) { this.expandable = b; }
  public boolean getExpandable() { return expandable; }
    

  public Map getTransientPropertyMap() {
    if (transient_properties == null) {
      transient_properties = new HashMap();
    }
    return transient_properties;
  }
  
  /** Copies all properties from the given style into this one, 
   *  including the transient properties. 
   */
  public void copyPropertiesFrom(IAnnotStyle g) {
    setColor(g.getColor());
    setShow(g.getShow());
    // don't copy unique name
    setHumanName(g.getHumanName());
    setBackground(g.getBackground());
    setCollapsed(g.getCollapsed());
    setMaxDepth(g.getMaxDepth());
    setHeight(g.getHeight());
    setY(g.getY());
    setExpandable(g.getExpandable());
    getTransientPropertyMap().putAll(g.getTransientPropertyMap());
  }
}
