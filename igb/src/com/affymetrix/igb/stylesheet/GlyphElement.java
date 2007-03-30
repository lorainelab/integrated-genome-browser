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

import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.glyph.*;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GlyphElement implements Cloneable, XmlAppender {
  /*
   
<!ELEMENT GLYPH (PROP*, GLYPH*, CHILDREN?)>
<!ATTLIST GLYPH
    position CDATA #IMPLIED
    type CDATA #REQUIRED
  >

   */
  
  public static String TYPE_BOX = "box";
  public static String TYPE_FILLED_BOX = "filled box";
  public static String TYPE_LINE = "line";
  public static String TYPE_NOT_DRAWN = "hidden";

  public static Color default_color = Color.GREEN;
  
  PropertyMap propertyMap;
  List enclosedGlyphElements = null;
  ChildrenElement children = null;
  String position;
  String type;

  int glyph_height = 10;
  static int diff_height = 3;
  
//  Color color = null;
  
  public Object clone() throws CloneNotSupportedException {
    GlyphElement clone = (GlyphElement) super.clone();
    if (this.enclosedGlyphElements != null) {
      clone.enclosedGlyphElements = new ArrayList(enclosedGlyphElements.size());
      for (int i=0; i<enclosedGlyphElements.size(); i++) {
        GlyphElement ge = (GlyphElement) enclosedGlyphElements.get(i);
        GlyphElement new_glyph_element = (GlyphElement) ge.clone();
        clone.enclosedGlyphElements.add(new_glyph_element);
      }
    }
    if (propertyMap != null) {
      clone.propertyMap = (PropertyMap) this.propertyMap.clone();
    }
    if (children != null) {
      clone.children = (ChildrenElement) this.children.clone();
    }
    
    return clone;
  }
  
  public GlyphElement() {
    this.propertyMap = new PropertyMap();
  }

  public String getPosition() {
    return this.position;
  }

  public void setPosition(String position) {
    this.position = position;
  }

  public String getType() {
    return this.type;
  }

  public void setType(String type) {
    this.type = type;
  }
  
  public List getEnclosedGlyphElements() {
    return this.enclosedGlyphElements;
  }
  
  public void addGlyphElement(GlyphElement ge) {
    if (enclosedGlyphElements == null) {
      enclosedGlyphElements = new ArrayList();
    }
    enclosedGlyphElements.add(ge);
  }
  
  public void setChildrenElement(ChildrenElement c) {
    this.children = c;
  }
  
  public ChildrenElement getChildrenElement() {
    return this.children;
  }
  
  static String[] knownTypes = new String[] {
    TYPE_BOX, TYPE_FILLED_BOX, TYPE_LINE, TYPE_NOT_DRAWN,
  };
  
  static boolean knownGlyphType(String type) {
    for (int i=0; i<knownTypes.length; i++) {
      if (type.equals(knownTypes[i])) { return true; }
    }
    return false;
  }
  
  public static GlyphI makeGlyph(String type) {
    GlyphI gl = null;
    if (TYPE_BOX.equals(type)) {
      gl = new EfficientOutlineContGlyph();
    } else if (TYPE_FILLED_BOX.equals(type)) {
      gl = new EfficientOutlinedRectGlyph(); // shouldn't be used as a container
    } else if (TYPE_LINE.equals(type)) {
      gl = new EfficientLineContGlyph();
    } else if (TYPE_NOT_DRAWN.equals(type)) {
      gl = null; //TODO: Needs to be an invisible non-null glyph ?
    } else {
      // this will be caught by knownGlyphType() method
      System.out.println("GLYPH Type Not Known: " + type);
    }
    return gl;
  }
  
  public GlyphI symToGlyph(SeqMapView gviewer, SeqSymmetry insym, GlyphI parent_glyph, PropertyMap context) {
    
    if (insym == null) { return null; }

    
    GlyphI gl = null;
    if (knownGlyphType(type)) {
      SeqSymmetry transformed_sym = gviewer.transformForViewSeq(insym);

      SeqSpan span = transformed_sym.getSpan(gviewer.getViewSeq());
      if (span == null) { return null; } // ???????  maybe try children anyway?

      propertyMap.parentProperties = context;

      gl = makeGlyph(type);

      gl.setCoords(span.getMin(), 0, span.getLength(), glyph_height);
      Color color = (Color) propertyMap.getColor("color");
      if (color == null) {
        color = default_color;
      }
      
      gl.setColor(color);

      addToParent(parent_glyph, gl, this.position);
      gl.setInfo(insym);
      gviewer.getSeqMap().setDataModelFromOriginalSym(gl, insym);
      
      //TODO:  Now do encolosed glyphs
      if (enclosedGlyphElements != null) {
        Iterator iter = enclosedGlyphElements.iterator();
        while (iter.hasNext()) {
          GlyphElement kid = (GlyphElement) iter.next();
          kid.symToGlyph(gviewer, insym, parent_glyph, this.propertyMap);
        }
      }
      
      if (children != null) {
        //TODO; should we use "insym" or "transformed_sym" here?
        // insym should always work
        // transformed_sym should work faster (avoids redundant transformations),
        // but may not work in some cases where the earlier transform has
        // changed the number of levels of symmetry.
        children.childSymsToGlyphs(gviewer, insym, gl, propertyMap);
      }

      propertyMap.parentProperties = null; // for possible garbage collection
    }

    return gl;
  }

  void addToParent(GlyphI parent, GlyphI child, String position) {
    parent.addChild(child);
    //TODO: use position
    // One way to do it: make a special glyph interface StyledGlyphI where
    // all implementations of that interface know how to position themselves
    // inside their parents
  }

  public StringBuffer appendXML(String indent, StringBuffer sb) {
    sb.append(indent).append("<GLYPH ");
    XmlStylesheetParser.appendAttribute(sb, "type", type);
    XmlStylesheetParser.appendAttribute(sb, "position", position);
    sb.append(">\n");
    if (this.propertyMap != null) {
      propertyMap.appendXML(indent + "  ", sb);
    }
    
    if (this.enclosedGlyphElements != null) {
      Iterator iter = enclosedGlyphElements.iterator();
      while (iter.hasNext()) {
       GlyphElement kid = (GlyphElement) iter.next();
       kid.appendXML(indent + "  ", sb);
      }
    }
    
    if (children != null) {
      children.appendXML(indent + "  ", sb);
    }

    sb.append(indent).append("</GLYPH>\n");
    return sb;
  }
}
