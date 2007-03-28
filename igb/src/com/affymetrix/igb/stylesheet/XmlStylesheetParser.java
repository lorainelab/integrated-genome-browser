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

import com.affymetrix.genometry.Propertied;
import com.affymetrix.igb.das.DasLoader;
import java.awt.Color;
import java.io.*;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.apache.xerces.parsers.DOMParser;

/**
 *  Loads an XML document using the igb_stylesheet_1.dtd.
 */
public class XmlStylesheetParser {

  Stylesheet stylesheet = new Stylesheet();
    
  public Stylesheet parse(File fl) {
    FileInputStream fistr = null;
    BufferedInputStream bistr = null;
    try {
      fistr = new FileInputStream(fl);
      bistr = new BufferedInputStream(fistr);
      stylesheet = parse(bistr);
    }
    catch(Exception ex) {
      System.out.println("ERROR: Exception processing stylesheet file "+ex.toString());
      ex.printStackTrace();
    }
    finally {
      if (bistr != null) try {bistr.close();} catch (Exception e) {}
      if (fistr != null) try {fistr.close();} catch (Exception e) {}
    }
    return stylesheet;
  }

  public Stylesheet parse(InputStream istr) {
    try {
      InputSource insrc = new InputSource(istr);
      parse(insrc);
    }
    catch (Exception ex) {
      System.err.println("ERROR while reading stylesheet.");
      System.err.println("  "+ex.toString());
      ex.printStackTrace();
    }
    return stylesheet;
  }

  public Stylesheet parse(InputSource insource) {
    try {
      //DOMParser parser = new DOMParser();
      DOMParser parser = DasLoader.nonValidatingParser();

      parser.parse(insource);
      Document prefsdoc = parser.getDocument();
      processDocument(prefsdoc);
    }
    catch (Exception ex) {
      System.err.println("ERROR while reading stylesheet.");
      System.err.println("  "+ex.toString());
      ex.printStackTrace();
    }
    return stylesheet;
  }

  public void processDocument(Document prefsdoc) {
    Element top_element = prefsdoc.getDocumentElement();
    String topname = top_element.getTagName();
    if (! (topname.equalsIgnoreCase("stylesheet"))) {
      System.err.println("not a stylesheet file -- can't parse");
    }
    NodeList children = top_element.getChildNodes();

      // if red, green, blue attributes then val = color(red, green, blue)
      // else if has nested tags then val = (recursive hashtable into nesting)
      // else val = String(content)

    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      Object val = null;
      if (child instanceof Element) {
        Element el = (Element)child;

          if (name.equalsIgnoreCase("import")) {
            processImport(el);
          }
          else if (name.equalsIgnoreCase("styles")) {
            processStyles(el);
          }
          else if (name.equalsIgnoreCase("associations")) {
            processAssociations(el);
          }
          else {
            cantParse(el);
          }
      }
    }
  }
  
  void cantParse(Node n) {
    System.out.println("Stylesheet: Cannot parse element: " + n.getNodeName());
  }

  void notImplemented(String s) {
    System.out.println("Stylesheet: Not yet implemented: " + s);
  }
  
  void processImport(Element el) {
    notImplemented("<IMPORT>");
  }

  void processAssociations(Element associations) {

    NodeList children = associations.getChildNodes();

    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      if (child instanceof Element) {
        Element el = (Element) child;

        if (name.equalsIgnoreCase("TYPE_ASSOCIATION")) {
          String type = el.getAttribute("type");
          String style = el.getAttribute("style");
          stylesheet.type2stylename.put(type, style);
        }
        else if (name.equalsIgnoreCase("METHOD_ASSOCIATION")) {
          String method = el.getAttribute("method");
          String style = el.getAttribute("style");
          String match_by = el.getAttribute("match_by");
          if ("regex".equalsIgnoreCase(match_by)) {
            stylesheet.regex2stylename.put(method, style);
          } else if ("exact".equalsIgnoreCase(match_by) || match_by == null) {
            stylesheet.meth2stylename.put(method, style);
          } else {
            cantParse(el);
          }
        }
        else {
          cantParse(el);
        }
      }
    }
  }

  void processStyles(Element styleNode) {
    NodeList children = styleNode.getChildNodes();

    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      if (child instanceof Element) {
        Element el = (Element) child;
        
        if (name.equalsIgnoreCase("style")) {
          processStyle(el);
        }
      }
    }
  }

  Color string2Color(String s) {
    //TODO;
    return Color.RED;
  }
  
  StyleElement processStyle(Element styleel) {


    String styleName = styleel.getAttribute("name");
    String ext = styleel.getAttribute("extends");

    StyleElement se = stylesheet.createStyle(styleName, ext);
    
    Color c1 = string2Color(styleel.getAttribute("color"));
    Color c2 = string2Color(styleel.getAttribute("color2"));
    Color c3 = string2Color(styleel.getAttribute("color3"));
    if (c1 != null) {se.color = c1;}
    if (c2 != null) {se.color2 = c2;}
    if (c3 != null) {se.color3 = c3;}
    
    applyProperties(styleel, se.propertyMap);

    NodeList children = styleel.getChildNodes();
    

    // There should only be one child <GLYPH>, but
    // there can be multiple <PROPERTY> children
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      if (child instanceof Element) {
        Element el = (Element) child;
        
        if (name.equalsIgnoreCase("glyph")) {
          GlyphElement ge2 = processGlyph(el);
          se.setGlyphElement(ge2);
        } else if (name.equalsIgnoreCase("property")) {
          // handled elsewhere
        } else {
          cantParse(el);
        }
      }
    }
        
    return se;
  }
  
  GlyphElement processGlyph(Element glyphel) {
    GlyphElement ge = new GlyphElement();

    String type = glyphel.getAttribute("type");
    ge.setType(type);
    String position = glyphel.getAttribute("position");
    ge.setPosition(position);
    
    NodeList children = glyphel.getChildNodes();
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      if (child instanceof Element) {
        Element el = (Element) child;
        
        if (name.equalsIgnoreCase("glyph")) {
          GlyphElement ge2 = processGlyph(el);
          ge.addGlyphElement(ge2);
        } else if (name.equalsIgnoreCase("children")) {
          ChildrenElement ce = processChildrenElement(el);
          ge.setChildrenElement(ce);
        } else if (name.equalsIgnoreCase("property")) {
          // dealt with below
        } else {
          cantParse(el);
        }
      }
      
      applyProperties(glyphel, ge.propertyMap);
    }
    return ge;
  }
  
  ChildrenElement processChildrenElement(Element childel) {
    ChildrenElement ce = new ChildrenElement();
    
    NodeList children = childel.getChildNodes();
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      if (child instanceof Element) {
        Element el = (Element) child;
        
        if (name.equalsIgnoreCase("style")) {
          StyleElement se = processStyle(el);
          ce.setStyleElement(se);
        } else if (name.equalsIgnoreCase("match")) {
          MatchElement me = processMatchElement(el);
          ce.addMatchElement(me);
        } else {
          cantParse(el);
        }
      }
    }
    return ce;

  }
  
  MatchElement processMatchElement(Element matchel) {
    MatchElement me = new MatchElement();
    NodeList children = matchel.getChildNodes();
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      if (child instanceof Element) {
        Element el = (Element) child;
        
        if (name.equalsIgnoreCase("style")) {
          StyleElement se = processStyle(el);
          me.setStyle(se);
        } else if (name.equalsIgnoreCase("match")) {
          MatchElement me2 = processMatchElement(el);
          me.subMatchList.add(me2);
        } else {
          cantParse(el);
        }
      }
    }
    return me;
  }
  
  void applyProperties(Element el, Propertied proper) {
    NodeList children = el.getChildNodes();

    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      if (name.equalsIgnoreCase("property")) {
        if (child instanceof Element) {
          Element prop_el = (Element) child;
          String key = prop_el.getAttribute("key");
          String value = prop_el.getAttribute("value");
          if (key == null || proper == null) {
            System.out.println("ERROR: key or value of <PROPERTY> is null");
          }
          proper.setProperty(key, value);
        }
      }
    }
  }
  
  static String escapeXML(String s) {
    if (s==null) {
      return "";
    } else {
      return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt")
      .replaceAll("\"", "&quot;").replaceAll("'", "&apos;");
    }
  }

  
  public static void appendAttribute(StringBuffer sb, String name, String value) {
    if (value != null && value.trim().length() > 0) {
      sb.append(" ").append(name).append("='").append(escapeXML(value)).append("'");
    }
  }
  

}

