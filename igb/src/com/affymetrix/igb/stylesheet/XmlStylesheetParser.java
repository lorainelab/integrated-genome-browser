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
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.das.DasLoader;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.Color;
import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.apache.xerces.parsers.DOMParser;

/**
 *  Loads an XML document using the igb_stylesheet_1.dtd.
 */
public class XmlStylesheetParser {

  Stylesheet stylesheet = new Stylesheet();
  static Stylesheet system_stylesheet = null;
  static Stylesheet user_stylesheet = null;
  
  // This resource should in the top-level igb source directory, or top level of jar file
  static final String system_stylesheet_resource_name = "/igb_system_stylesheet.xml";

  public static synchronized Stylesheet getSystemStylesheet() {
    if (system_stylesheet == null) {
      try {
        XmlStylesheetParser parser = new XmlStylesheetParser();
        // If using class.getResource... use name beginning with "/"
        InputStream istr = XmlStylesheetParser.class.getResourceAsStream(system_stylesheet_resource_name);
        // If using getContextClassLoader... use name NOT beginning with "/"
        //InputStream istr = Thread.currentThread().getContextClassLoader().getResourceAsStream(system_stylesheet_resource_name);
        system_stylesheet = parser.parse(istr);
      } catch (Exception e) {
        System.out.println("ERROR: Couldn't initialize system stylesheet.");
        e.printStackTrace();
        system_stylesheet = null;
      }
    }
    if (system_stylesheet == null) {
      system_stylesheet = new Stylesheet();
    }
    return system_stylesheet;
  }
  
  public static Stylesheet getUserStylesheet() {
    return new Stylesheet();
  }
  
  public Stylesheet parse(File fl) throws IOException {
    FileInputStream fistr = null;
    BufferedInputStream bistr = null;
    try {
      fistr = new FileInputStream(fl);
      bistr = new BufferedInputStream(fistr);
      stylesheet = parse(bistr);
    }
    finally {
      if (bistr != null) try {bistr.close();} catch (Exception e) {}
      if (fistr != null) try {fistr.close();} catch (Exception e) {}
    }
    return stylesheet;
  }

  public Stylesheet parse(InputStream istr) throws IOException {
    InputSource insrc = new InputSource(istr);
    parse(insrc);
    return stylesheet;
  }

  public Stylesheet parse(InputSource insource) throws IOException {
    try {
      //DOMParser parser = new DOMParser();
      DOMParser parser = DasLoader.nonValidatingParser();

      parser.parse(insource);
      Document prefsdoc = parser.getDocument();
      processDocument(prefsdoc);
    }
    catch (IOException ioe) {
      throw ioe;
    }
    catch (Exception ex) {
      IOException ioe = new IOException("Error processing stylesheet file");
      ioe.initCause(ex);
      throw ioe;
    }
    return stylesheet;
  }

  public void processDocument(Document prefsdoc) throws IOException {

    Element top_element = prefsdoc.getDocumentElement();
    String topname = top_element.getTagName();
    if (! (topname.equalsIgnoreCase("igb_stylesheet"))) {
      throw new IOException("Can't parse file: Initial Element is not <IGB_STYLESHEET>.");
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
  
  void cantParse(Element n) {
    System.out.println("WARNING: Stylesheet: Cannot parse element: " + n.getNodeName());
  }

  void notImplemented(String s) {
    System.out.println("WARNING: Stylesheet: Not yet implemented: " + s);
  }
  
  void processImport(Element el) throws IOException {
    notImplemented("<IMPORT>");
  }

  void processAssociations(Element associations) throws IOException {

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
            try {
              Pattern pattern = Pattern.compile(method);
              stylesheet.regex2stylename.put(pattern, style);
            } catch (PatternSyntaxException pse) {
              throw new IOException("ERROR in stylesheet: Regular Expression not valid: '" +
                  method + "'");
            }
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

  void processStyles(Element stylesNode) throws IOException {
    NodeList children = stylesNode.getChildNodes();

    //applyProperties(stylesNode, ...);
    
    // There could be a top-level property map that applies to the
    // whole stylesheet, but that isn't implemented now
    PropertyMap top_level_property_map = null;

    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      if (child instanceof Element) {
        Element el = (Element) child;
        
        if (name.equalsIgnoreCase("style") || name.equalsIgnoreCase("copy_style")) {
          processStyle(el, top_level_property_map, true);
        }
      }
    }
  }

  Color string2Color(String s) {
    if (s==null || s.trim().length() == 0) {
      return null;
    }
    if (s.startsWith("Ox")) {
      return Color.decode(s);
    } else {
      return Color.decode("0x"+s);
    }
  }
  
  StyleElement processStyle(Element styleel, PropertyMap pm, boolean top_level) throws IOException {

    // node name should be STYLE, COPY_STYLE or USE_STYLE
    String node_name = styleel.getNodeName();

    
    StyleElement se = null;
    if ("STYLE".equalsIgnoreCase(node_name)) {
      String styleName = styleel.getAttribute("name");
      se = stylesheet.createStyle(styleName, pm, top_level);

    } else if ("COPY_STYLE".equalsIgnoreCase(node_name)) {
      String newName = styleel.getAttribute("new_name");
      String extendsName = styleel.getAttribute("extends");
      se = stylesheet.getStyleByName(extendsName);
      
      if (se == null) {
        se = stylesheet.createStyle(newName, pm, top_level);
      } else {
        se = StyleElement.clone(se, newName);
      }
      
    } else if ("USE_STYLE".equalsIgnoreCase(node_name)) {
      String styleName = styleel.getAttribute("name");
      if (styleName==null || styleName.trim().length()==0) {
        throw new IOException("Can't have a USE_STYLE element with no name");
      }
      
      se = stylesheet.getWrappedStyle(styleName);
            
      return se; // do not do any other processing on a USE_STYLE element
    } else {
      cantParse(styleel);
    }

    if (se == null) {
      cantParse(styleel);
    }

    if (top_level) {
      if ((se.getName() == null || se.getName().trim().length() == 0)) {
        System.out.println("WARNING: Stylesheet: All top-level styles must have a name!");
      } else {
        stylesheet.addToIndex(se);
      }
    }
    
    NodeList children = styleel.getChildNodes();
    

    // There should only be one child <GLYPH>, but
    // there can be multiple <PROPERTY> children
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      if (child instanceof Element) {
        Element el = (Element) child;
        
        if (name.equalsIgnoreCase("glyph")) {
          GlyphElement ge2 = processGlyph(el, se.propertyMap);
          se.setGlyphElement(ge2);
        } else if (name.equalsIgnoreCase("property")) {
          processProperty(el, se.propertyMap);
        } else {
          cantParse(el);
        }
      }
    }
        
    return se;
  }
  
  GlyphElement processGlyph(Element glyphel, PropertyMap pm) throws IOException {
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
          GlyphElement ge2 = processGlyph(el, ge.propertyMap);
          ge.addGlyphElement(ge2);
        } else if (name.equalsIgnoreCase("children")) {
          ChildrenElement ce = processChildrenElement(el, ge.propertyMap);
          ge.setChildrenElement(ce);
        } else if (name.equalsIgnoreCase("property")) {
          processProperty(el, ge.propertyMap);
        } else {
          cantParse(el);
        }
      }
    }

    return ge;
  }
  
  ChildrenElement processChildrenElement(Element childel, PropertyMap pm) throws IOException {
    ChildrenElement ce = new ChildrenElement();
    
    String position = childel.getAttribute("child_positions");
    ce.setPosition(position);
    String container = childel.getAttribute("container");
    ce.setChildContainer(container);

    NodeList children = childel.getChildNodes();
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      if (child instanceof Element) {
        Element el = (Element) child;
        
        if (name.equalsIgnoreCase("style") || name.equalsIgnoreCase("copy_style") || name.equalsIgnoreCase("use_style")) {
          StyleElement se = processStyle(el, ce.propertyMap, false);
          ce.setStyleElement(se);
        } else if (name.equalsIgnoreCase("match")) {
          MatchElement me = processMatchElement(el, ce.propertyMap);
          ce.addMatchElement(me);
        } else if (name.equalsIgnoreCase("property")) {
          processProperty(el, ce.propertyMap);
        } else {
          cantParse(el);
        }
      }
    }
    return ce;

  }
  
  MatchElement processMatchElement(Element matchel, PropertyMap pm) throws IOException {
    MatchElement me = new MatchElement();
    NodeList children = matchel.getChildNodes();

    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      if (child instanceof Element) {
        Element el = (Element) child;
        
        if (name.equalsIgnoreCase("style") || name.equalsIgnoreCase("copy_style") || name.equalsIgnoreCase("use_style")) {
          StyleElement se = processStyle(el, pm, false);
          me.setStyle(se);
        } else if (name.equalsIgnoreCase("match")) {
          MatchElement me2 = processMatchElement(el, me.propertyMap);
          me.subMatchList.add(me2);
        } else if (name.equalsIgnoreCase("property")) {
          processProperty(el, me.propertyMap);
        } else {
          cantParse(el);
        }
      }
    }
    return me;
  }
  
  void processProperty(Element properElement, PropertyMap propertied) 
  throws IOException {
    String key = properElement.getAttribute("key");
    String value = properElement.getAttribute("value");
    if (key == null) {
       throw new IOException("ERROR: key or value of <PROPERTY> is null");
    }
    propertied.setProperty(key, value);
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

