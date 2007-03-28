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
import com.affymetrix.igb.das2.Das2FeatureRequestSym;
import com.affymetrix.igb.genometry.GFF3Sym;
import com.affymetrix.igb.genometry.SmartAnnotBioSeq;
import com.affymetrix.igb.genometry.SymWithProps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class Stylesheet implements Cloneable, XmlAppender {
  
  /*
<!ELEMENT STYLESHEET (IMPORT?, STYLES?, ASSOCIATIONS?)>
   */
  
  LinkedHashMap meth2stylename = new LinkedHashMap();
  LinkedHashMap regex2stylename = new LinkedHashMap();
  LinkedHashMap type2stylename = new LinkedHashMap();

  LinkedHashMap stylename2styleElement = new LinkedHashMap();

  public static final String SYM_TO_STYLE_PROPERTY_KEY = Stylesheet.class.getName();
  
  public Object clone() throws CloneNotSupportedException {
    Stylesheet clone = (Stylesheet) super.clone();
    clone.meth2stylename = new LinkedHashMap();
    clone.meth2stylename.putAll(meth2stylename);
    clone.regex2stylename = new LinkedHashMap();
    clone.regex2stylename.putAll(regex2stylename);
    clone.type2stylename = new LinkedHashMap();
    clone.type2stylename.putAll(type2stylename);
    clone.stylename2styleElement = new LinkedHashMap();
    clone.stylename2styleElement.putAll(stylename2styleElement);
    return clone;
  }
  
  public Stylesheet() {
  }
  
  public void importFromURL(String url) {
    throw new RuntimeException("import not implemented");
  }
  
  public StyleElement getStyleByName(String name) {
    return (StyleElement) stylename2styleElement.get(name);
  }
  
  public StyleElement createStyle(String name, String extends_name) {
    StyleElement se = null;
    
    if (extends_name != null) {
      StyleElement original = (StyleElement) getStyleByName(extends_name);
      if (original != null) {
        se = easyClone(getStyleByName(extends_name));
      }
    }
    if (se == null) {
      se = new StyleElement();
    }

    if (name != null) {
      se.name = name;
      stylename2styleElement.put(name, se);
    }
    se.extends_name = extends_name;
    return se;
  }

  public static StyleElement easyClone(StyleElement original) {
    try {
      StyleElement cloned = (StyleElement) original.clone();
      return cloned;
    } catch (CloneNotSupportedException e) {
      // Never happens !
      throw new RuntimeException(e);
    }
  }

  /**
   *  Tries to find a styleElement for the given seq symmetry.
   *  First looks for a styleElement stored in sym.getProperty(SYM_TO_styleElement_PROPERTY_KEY).
   *  Second looks for a match by feature type (such as an ontology term).
   *  Third looks for a match by feature "method" (i.e. the tier name).
   */
  public StyleElement getstyleElementForSym(SeqSymmetry sym) {
    StyleElement styleElement = null;
    if (sym instanceof SymWithProps) {
      SymWithProps proper = (SymWithProps) sym;
      Object o = proper.getProperty(SYM_TO_STYLE_PROPERTY_KEY);
      if (o instanceof StyleElement) {
        styleElement = (StyleElement) o;
      }
    }
    
    if (sym instanceof Das2FeatureRequestSym) {
      Das2FeatureRequestSym d2r = (Das2FeatureRequestSym) sym;
      String type = d2r.getType();
      styleElement = getstyleElementForType(type);
    } else if (sym instanceof GFF3Sym) {
      GFF3Sym gff = (GFF3Sym) sym;
      String type = gff.getFeatureType();
      styleElement = getstyleElementForType(type);
    }
    
    if (styleElement == null) {
      styleElement = getstyleElementForMethod(SmartAnnotBioSeq.determineMethod(sym));
    }
    
    System.out.println("XmlStylesheetParser: >> styleElement for sym "+sym+":  " + styleElement);
    return styleElement;
  }
  
  public StyleElement getstyleElementForMethod(String meth){
    if (meth == null) {
      return null;
    }
    
    String stylename = null;
    
    // First try to match styleElement based on exact name match
    stylename = (String) meth2stylename.get(meth);
    
    // Then try to match styleElement from regular expressions
    if (stylename == null) {
      java.util.List keyset = new ArrayList(regex2stylename.keySet());
      
      // Look for a matching pattern, going backwards, so that the
      // patterns from the last preferences read take precedence over the
      // first ones read (such as the default prefs).  Within a single
      // file, the last matching pattern will trump any earlier ones.
      for (int j=keyset.size()-1 ; j >= 0 && stylename == null; j--) {
        java.util.regex.Pattern regex = (java.util.regex.Pattern) keyset.get(j);
        if (regex.matcher(meth).find()) {
          stylename = (String) regex2stylename.get(regex);
          // Put the stylename in meth2stylename to speed things up next time through.
          meth2stylename.put(meth, stylename);
        }
      }
    }
    
    StyleElement styleElement = getStyleByName(stylename);
    
    System.out.println(">> styleElement for method "+meth+":  " + styleElement);
    return styleElement;
  }
  
  public StyleElement getstyleElementForType(String type){
    StyleElement styleElement = (StyleElement) type2stylename.get(type);
    System.out.println(">> styleElement for type "+type+":  " + styleElement);
    return styleElement;
  }
  
  public StyleElement getDefaultstyleElement() {
    return new StyleElement();
  }  

  public StringBuffer appendXML(String indent, StringBuffer sb) {
    sb.append("<?xml version=\"1.0\"?>\n");

    sb.append("<!DOCTYPE STYLESHEET SYSTEM \"igb_stylesheet_1.dtd\">\n");

    sb.append("\n");
    sb.append(indent).append("<STYLESHEET>\n");

    sb.append("\n");
    sb.append(indent).append("<STYLES>");
    Iterator iter = this.stylename2styleElement.values().iterator();
    while (iter.hasNext()) {
      sb.append("\n");
      StyleElement styleElement = (StyleElement) iter.next();
      styleElement.appendXML(indent + "  ", sb);
    }
    sb.append("\n").append(indent).append("</STYLES>\n");

    sb.append("\n");
    sb.append(indent).append("<ASSOCIATIONS>\n");
    
    Iterator m_iter = meth2stylename.keySet().iterator();
    while (m_iter.hasNext()) {
      String method_name = (String) m_iter.next();
      String style_name = (String) meth2stylename.get(method_name);
      sb.append(indent).append(indent + "  ").append("<METHOD_ASSOCIATION ");
      XmlStylesheetParser.appendAttribute(sb, "method", method_name);
      XmlStylesheetParser.appendAttribute(sb, "style", style_name);
      XmlStylesheetParser.appendAttribute(sb, "match_by", "exact");
      sb.append("/>\n");
    }
    
    Iterator r_iter = regex2stylename.keySet().iterator();
    while (r_iter.hasNext()) {
      String regex_name = (String) r_iter.next();
      String style_name = (String) regex2stylename.get(regex_name);
      sb.append(indent).append(indent + "  ").append("<METHOD_ASSOCIATION ");
      XmlStylesheetParser.appendAttribute(sb, "method", regex_name);
      XmlStylesheetParser.appendAttribute(sb, "style", style_name);
      XmlStylesheetParser.appendAttribute(sb, "match_by", "regex");
      sb.append("/>\n");
    }

    Iterator t_iter = type2stylename.keySet().iterator();
    while (t_iter.hasNext()) {
      String type_name = (String) t_iter.next();
      String style_name = (String) type2stylename.get(type_name);
      sb.append(indent).append(indent + "  ").append("<TYPE_ASSOCIATION ");
      XmlStylesheetParser.appendAttribute(sb, "type", type_name);
      XmlStylesheetParser.appendAttribute(sb, "style", style_name);
      sb.append("/>\n");
    }

    sb.append(indent).append("</ASSOCIATIONS>\n");
    sb.append("\n");

    sb.append(indent).append("</STYLESHEET>\n");
    sb.append("\n");
    return sb;
  }
}
