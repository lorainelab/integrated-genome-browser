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

import com.affymetrix.genometryImpl.SeqSymmetry;
//import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.view.SeqMapView;
import java.util.*;


public class StyleElement implements DrawableElement {
  /*
<!ELEMENT STYLE (PROPERTY*, ((MATCH+,ELSE?) | GLYPH))>
<!ATTLIST STYLE
    name CDATA #IMPLIED
    container CDATA #IMPLIED
  >
  */

  public static String NAME="STYLE";
  
  public static String ATT_NAME="name";
  public static String ATT_CONTAINER="container";
  
  static Map<String,StyleElement> names2styles = new HashMap<String,StyleElement>();
  
  String childContainer = ".";
  PropertyMap propertyMap;
  List<MatchElement> matchElements;
  GlyphElement glyphElement;

  String name;
    
  public StyleElement() {
    this.propertyMap = new PropertyMap();
  }

  /** Not yet implemented. Needs to do a deep copy. */
  public Object clone() throws CloneNotSupportedException {
    StyleElement clone = (StyleElement) super.clone();
    if (propertyMap != null) {
      clone.propertyMap = (PropertyMap) this.propertyMap.clone();
    }
    if (glyphElement != null) {
      clone.glyphElement = (GlyphElement) this.glyphElement.clone();
    }
    if (matchElements != null) {
      clone.matchElements = new ArrayList<MatchElement>(matchElements.size());
      for (int i=0; i<matchElements.size(); i++) {
        MatchElement me = matchElements.get(i);
        MatchElement new_me = (MatchElement) me.clone();
        clone.matchElements.add(new_me);
      }
    }
    return clone;
  }  
    
  public GlyphI symToGlyph(SeqMapView gviewer, SeqSymmetry sym, GlyphI container,
      Stylesheet stylesheet, PropertyMap context) {
    GlyphI glyph = null;

    PropertyMap oldContext = propertyMap.getContext();
    this.propertyMap.setContext(context);
    
    if (matchElements != null && ! matchElements.isEmpty()) {
      Iterator iter = matchElements.iterator();
      while (iter.hasNext() && glyph == null) {
        MatchElement matchElement = (MatchElement) iter.next();
        
        // If the match element matches, it will return a glyph, otherwise will return null
        GlyphI match_glyph = matchElement.symToGlyph(gviewer, sym,
            container, stylesheet, propertyMap);
        if (match_glyph != null) {
          glyph = match_glyph;
        }
      }
    }

    // This test is partially redundant because it is invalid to specify
    // some match elements and also specify a glyph element, or to omit
    // the match elements and also omit the glyphElement
    else if (glyphElement != null) {
      try {
        container = ChildrenElement.findContainer(container, childContainer);
        
        glyph = glyphElement.symToGlyph(gviewer, sym, container, stylesheet, this.propertyMap);
      } catch (RuntimeException re) {
        //System.out.println("Exception: " + re.toString());
        re.printStackTrace();
        System.out.println("Exception in style: " + name + "  for " + sym);
        //SeqUtils.printSymmetry(sym);
      }
    }
    
    this.propertyMap.setContext(oldContext);
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

  
 public List getMatchElements() {
   return this.matchElements;
 }

 public void addMatchElement(MatchElement me) {
    if (matchElements == null) {
      matchElements = new ArrayList<MatchElement>();
    }
    matchElements.add(me);
  }
  
  public String toString() {
    return "StyleElement [name="+name+"]";
  }
 
  public StringBuffer appendXML(String indent, StringBuffer sb) {
    sb.append(indent).append('<').append(NAME);
    XmlStylesheetParser.appendAttribute(sb, ATT_NAME, name);
    XmlStylesheetParser.appendAttribute(sb, ATT_CONTAINER, childContainer);
    sb.append(">\n");
    this.propertyMap.appendXML(indent + "  ", sb);
    if (matchElements != null) {
      Iterator iter = matchElements.iterator();
      while (iter.hasNext()) {
       MatchElement kid = (MatchElement) iter.next();
       kid.appendXML(indent + "  ", sb);
      }
    }
    if (glyphElement != null) {
      this.glyphElement.appendXML(indent + "  ", sb);
    }
    sb.append(indent).append("</").append(NAME).append(">\n");
    return sb;
  }
}
