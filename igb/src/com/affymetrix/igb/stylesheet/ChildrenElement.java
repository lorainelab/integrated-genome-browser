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
import com.affymetrix.igb.tiers.TierGlyph;
import com.affymetrix.igb.view.SeqMapView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChildrenElement implements Cloneable, XmlAppender {

/*
<!ELEMENT CHILDREN (MATCH*, STYLE?)>
<!ATTLIST CHILDREN
    container CDATA #IMPLIED
    position CDATA #IMPLIED
  >
*/
  
//  static String ARRANGEMENT_CENTER = "center";
//  static String ARRANGEMENT_FAR = "far";
//  static String ARRANGEMENT_NEAR = "near";
//  static String ARRANGEMENT_PACK = "bump";
//  
//  String child_arrangement = ARRANGEMENT_CENTER;
  
  String child_container = ".";
  String position; // becomes default position for children glyphs if they don't override it
  List matchElements;
  StyleElement styleElement;
  
  public Object clone() throws CloneNotSupportedException {
    ChildrenElement clone = (ChildrenElement) super.clone();
    if (styleElement != null) {
      clone.styleElement = (StyleElement) styleElement.clone();
    }
    if (matchElements != null) {
      clone.matchElements = new ArrayList(matchElements.size());
      for (int i=0; i<matchElements.size(); i++) {
        MatchElement me = (MatchElement) matchElements.get(i);
        MatchElement new_me = (MatchElement) me.clone();
        clone.matchElements.add(new_me);
      }
    }
    return clone;
  }
  
  public ChildrenElement() {
  }
  
  public void doChildren(SeqMapView gviewer, SeqSymmetry insym, GlyphI gl) {
    int childCount = insym.getChildCount();
    for (int i=0; i<childCount; i++) {
      SeqSymmetry childsym = insym.getChild(i);
      this.childSymToGlyph(gviewer, childsym, gl);
    }
  }

  public GlyphI childSymToGlyph(SeqMapView gviewer, SeqSymmetry childsym, GlyphI gl) {
    GlyphI container_glyph = findContainer(gl);
    
    if (matchElements != null) {
      Iterator iter = matchElements.iterator();
      while (iter.hasNext()) {
        MatchElement matchElement = (MatchElement) iter.next();
        
        // If the match element matches, it will return a glyph, otherwise will return null
        GlyphI match_glyph = matchElement.symToGlyph(gviewer, childsym, container_glyph);
        if (match_glyph != null) {
          return match_glyph;
        }
      }
    }
    
    // If none of the match elements matched, use the default child_factory
    return styleElement.symToGlyph(gviewer, childsym, container_glyph);
  }
  
  GlyphI findContainer(GlyphI gl) {
    GlyphI container_glyph = gl;
    
    if (".".equals(child_container)) {
      container_glyph = gl;
    } else if ("..".equals(child_container)) {
      container_glyph = parent(gl);
    } else if ("../..".equals(child_container)) {
      container_glyph = parent(parent(gl));
      
      /// etc.
      
    } else if ("/".equals(child_container)) {
      container_glyph = gl.getParent();
      while (!( container_glyph instanceof TierGlyph)) {
        container_glyph = parent(gl);
      }
    }
    return container_glyph;
  }  
  
  GlyphI parent(GlyphI gl) {
    if (gl instanceof TierGlyph) {
      return gl;
    } else {
      return gl.getParent();
    }
  }

  public StyleElement getStyleElement() {
    return this.styleElement;
  }

  public void setStyleElement(StyleElement styleElement) {
    this.styleElement = styleElement;
  }
  
  public List getMatchElements() {
    return this.matchElements;
  }
  
  public void addMatchElement(MatchElement me) {
    if (matchElements == null) {
      matchElements = new ArrayList();
    }
    matchElements.add(me);
  }
  
  public StringBuffer appendXML(String indent, StringBuffer sb) {
    sb.append(indent).append("<CHILDREN ");
    XmlStylesheetParser.appendAttribute(sb, "container", child_container);
    XmlStylesheetParser.appendAttribute(sb, "position", position);
    sb.append(">\n");

//    if (this.propertyMap != null) {
//      propertyMap.appendXML(indent + "  ", sb);
//    }
    
    if (matchElements != null) {
      Iterator iter = matchElements.iterator();
      while (iter.hasNext()) {
       MatchElement kid = (MatchElement) iter.next();
       kid.appendXML(indent + "  ", sb);
      }
    }
    
    if (styleElement != null) {
      styleElement.appendXML(indent + "  ", sb);
    }

    sb.append(indent).append("</CHILDREN>\n");
    return sb;
  }
}
