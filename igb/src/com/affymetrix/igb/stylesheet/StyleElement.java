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

package com.affymetrix.igb.stylesheet;

import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.prefs.WebLink;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.Color;
import java.util.*;


public class StyleElement implements Cloneable, XmlAppender {
  /*
<!ELEMENT STYLE (PROP*, GLYPH?)>
<!ATTLIST STYLE
    name CDATA #IMPLIED
    extends CDATA #IMPLIED
    color CDATA #IMPLIED
    color2 CDATA #IMPLIED
    color3 CDATA #IMPLIED
  >
  */

  static Map names2styles = new HashMap();
  
  PropertyMap propertyMap = new PropertyMap();
  GlyphElement glyphElement;

  String name;
  String extends_name;
  Color color = Color.CYAN;
  Color color2 = Color.YELLOW;
  Color color3 = Color.MAGENTA;
  
  
  public StyleElement() {
  }  

  /** Not yet implemented. Needs to do a deep copy. */
  public Object clone() throws CloneNotSupportedException {
    StyleElement clone = (StyleElement) super.clone();
    clone.propertyMap = new PropertyMap(this.propertyMap);
    if (glyphElement != null) {
      clone.glyphElement = (GlyphElement) this.glyphElement.clone();
    }
    
    return clone;
  }  
  
  public GlyphI symToGlyph(SeqMapView gviewer, SeqSymmetry sym, GlyphI container) {
    GlyphI glyph = null;
    //TODO
    return glyph;
  }
  
  public void setName(String name) {
    if (this.name != null) {
      throw new RuntimeException("You canot change the name of StyleElement '" +
          this.name + "' to '" + name +"'");
    }
    else {
      this.name = name;
      names2styles.put(name, this);
    }
  }
  
  public String getName() {
    return this.name;
  }

  public GlyphElement getGlyphElement() {
    return this.glyphElement;
  }

  public void setGlyphElement(GlyphElement glyphElement) {
    this.glyphElement = glyphElement;
  }
  
  public StringBuffer appendXML(String indent, StringBuffer sb) {
    sb.append(indent).append("<STYLE ");
    XmlStylesheetParser.appendAttribute(sb, "name", name);
    XmlStylesheetParser.appendAttribute(sb, "extends", extends_name);
    sb.append(">\n");
    this.propertyMap.appendXML(indent + "  ", sb);
    if (glyphElement != null) {
      this.glyphElement.appendXML(indent + "  ", sb);
    }
    sb.append(indent).append("</STYLE>\n");
    return sb;
  }
}
