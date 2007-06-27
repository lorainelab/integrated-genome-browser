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

package com.affymetrix.genometryImpl.style;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class SimpleAnnotStyle extends DefaultIAnnotStyle implements IAnnotStyleExtended {

  public SimpleAnnotStyle(String name) {
    super(name, false);
  }
  
  public SimpleAnnotStyle(String name, boolean is_graph) {
    super(name, is_graph);
  }

  static Map instances = new HashMap();

  /**
   *  Returns a style for the given name.  These styles remain associated
   * with the given name while the program is running, but do not get
   * persisted to a permanent storage.
   */
  public static IAnnotStyleExtended getInstance(String name) {
    IAnnotStyleExtended style = (IAnnotStyleExtended) instances.get(name);
    if (style == null) {
      style = new SimpleAnnotStyle(name);
      instances.put(name.toLowerCase(), style);
    }
    return style;
  }
  
  public static IAnnotStyleExtended getDefaultInstance() {
    return getInstance("* default *");
  }
  
  String url;
  public void setUrl(String url) {this.url = url;}
  public String getUrl() { return url; }
  
  boolean colorByScore = false;
  public void setColorByScore(boolean b) {this.colorByScore = b;}
  public boolean getColorByScore() { return this.colorByScore;}
  
  /** Default implementation returns the same as {@link #getColor()}. */
  public Color getScoreColor(float f) { return getColor(); }
  
  int depth=2;
  public void setGlyphDepth(int i) {this.depth = i;}
  public int getGlyphDepth() {return this.depth;}

  boolean separate = true;
  public void setSeparate(boolean b) { this.separate = b; }
  public boolean getSeparate() { return this.separate; }
  
  String labelField = "id";
  public void setLabelField(String s) { this.labelField = s; }
  public String getLabelField() { return labelField; }

  public void copyPropertiesFrom(IAnnotStyle g) {
    super.copyPropertiesFrom(g);  
    if (g instanceof IAnnotStyleExtended) {
      IAnnotStyleExtended as = (IAnnotStyleExtended) g;
      setUrl(as.getUrl());
      setColorByScore(as.getColorByScore());
      setGlyphDepth(as.getGlyphDepth());
      setSeparate(as.getSeparate());
      setLabelField(as.getLabelField());
    }
  }

}
