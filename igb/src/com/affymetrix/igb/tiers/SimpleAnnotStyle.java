/**
 *   Copyright (c) 2007 Affymetrix, Inc.
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

public class SimpleAnnotStyle implements IAnnotStyleExtended {
  String uName; // Unique name.  Can be read, but not reset.

  public SimpleAnnotStyle(String name) {
    this.uName = name;
  }
  
  public SimpleAnnotStyle(String name, boolean is_graph) {
    this.uName = name;
    this.is_graph = is_graph;
  }

  public String getUniqueName() { return uName; }
  
  String url;
  public void setUrl(String url) {this.url = url;}
  public String getUrl() { return url; }
  
  boolean colorByScore;
  public void setColorByScore(boolean b) {this.colorByScore = b;}
  public boolean getColorByScore() { return this.colorByScore;}
  
  int depth=2;
  public void setGlyphDepth(int i) {this.depth = i;}
  public int getGlyphDepth() {return this.depth;}
  
  Color c;
  public Color getColor() { return this.c;}
  public void setColor(Color c) { this.c = c; }
  
  boolean show;
  public boolean getShow() {return show;}
  public void setShow(boolean b) {this.show = show;}
    
  String humanName;
  public String getHumanName() { return humanName; }
  public void setHumanName(String s) { this.humanName = s; }
  
  Color bg;
  public Color getBackground() { return bg; }
  public void setBackground(Color c) { this.bg = c; }
  
  boolean collapsed;
  public boolean getCollapsed() { return collapsed; }
  public void setCollapsed(boolean b) { collapsed = b; }
  
  int maxDepth = 10;
  public int getMaxDepth() { return maxDepth; }
  public void setMaxDepth(int m) { maxDepth = m; }
  
  double height;
  public void setHeight(double h) { height = h; }
  public double getHeight() { return height; }
  
  double y;
  public void setY(double y) { this.y = y; }
  public double getY() { return y; }
  
  boolean expandable;
  public boolean getExpandable() { return expandable; }
  public void setExpandable(boolean b) { expandable = b; }
  
  boolean is_graph = false;
  public boolean isGraphTier() { return is_graph; }
  
  Map transientProperties = new HashMap();
  public Map getTransientPropertyMap() {
    return transientProperties;
  }
  
  public void copyPropertiesFrom(IAnnotStyle g) {
    setColor(g.getColor());
    setShow(g.getShow());
    setHumanName(g.getHumanName());
    setBackground(g.getBackground());
    setCollapsed(g.getCollapsed());
    setMaxDepth(g.getMaxDepth());
    setHeight(g.getHeight());
    setY(g.getY());
    setExpandable(g.getExpandable());
    
    if (g instanceof IAnnotStyleExtended) {
      IAnnotStyleExtended as = (IAnnotStyleExtended) g;
      setColorByScore(as.getColorByScore());
      setGlyphDepth(as.getGlyphDepth());
    }

    getTransientPropertyMap().putAll(g.getTransientPropertyMap());
  }
}
