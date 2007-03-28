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
import com.affymetrix.igb.view.SeqMapView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MatchElement implements Cloneable, XmlAppender {

//  <!ELEMENT MATCH (MATCH*, STYLE)>
  
  StyleElement styleElement;
  List subMatchList = new ArrayList();
  
  public Object clone() throws CloneNotSupportedException {
    MatchElement clone = (MatchElement) super.clone();
    clone.styleElement = (StyleElement) styleElement.clone();
    clone.subMatchList = new ArrayList(subMatchList.size());
    for (int i=0; i<subMatchList.size(); i++) {
      MatchElement me = (MatchElement) subMatchList.get(i);
      MatchElement new_me = (MatchElement) me.clone();
      clone.subMatchList.add(new_me);
    }
    return clone;
  }

  public MatchElement() {
  }
  
  /**
   *  If this MatchElement, or one of its children, matcheds the given symmetry,
   *  then a glyph will be created and returned.  Otherwise, will return null.
   */
  public GlyphI symToGlyph(SeqMapView gviewer, SeqSymmetry sym, GlyphI gl) {
    // If a sub_matcher matches, use it to make the glyph, 
    // otherwise if this matches, make it ourselves,
    // otherwise, if no match, return null
    return null;
  }

  public StyleElement getStyle() {
    return this.styleElement;
  }

  public void setStyle(StyleElement style) {
    this.styleElement = style;
  }

  public List getSubMatchList() {
    return this.subMatchList;
  }
  
  public StringBuffer appendXML(String indent, StringBuffer sb) {
    sb.append(indent).append("<MATCH ");
    sb.append(">\n");

//    if (this.propertyMap != null) {
//      propertyMap.appendXML(indent + "  ", sb);
//    }
    
    Iterator iter = this.getSubMatchList().iterator();
    while (iter.hasNext()) {
     MatchElement kid = (MatchElement) iter.next();
     kid.appendXML(indent + "  ", sb);
    }
    
    if (styleElement != null) {
      styleElement.appendXML(indent + "  ", sb);
    }

    sb.append(indent).append("</MATCH>\n");
    return sb;
  }
}
