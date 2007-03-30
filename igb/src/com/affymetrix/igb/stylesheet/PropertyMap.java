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
import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *  A cascading implementation of the Java Map class that also implements
 *  the Genometry Propertied interface.
 *  Will store properties in its own Map.  Will look for properties
 *  first in its own Map, then in it's parent Map, then the parent's parent, etc.
 *  All keys and values must be String's.
 */
public class PropertyMap extends HashMap implements Map, Propertied, Cloneable, XmlAppender {

  PropertyMap parentProperties;
  
  public PropertyMap() {
  }
  
  public PropertyMap(PropertyMap p) {
    this();
    this.parentProperties = p;
  }
  
  /** Returns a Map containing all properties (including inherited properties
   *  of the parents), but changing anything in this map will have no 
   *  effect on this object. 
   */
  public Map getProperties() {
    HashMap m = new HashMap();
    if (parentProperties != null) {
      m.putAll(parentProperties);
    }
    m.putAll(this);
    return Collections.unmodifiableMap(m);
  }

  /** Equivalent to getProperties(). */
  public Map cloneProperties() {
    return getProperties();
  }

  public Object getProperty(String key) {
    Object o = get(key);
    if (o == null && parentProperties != null) {
      return parentProperties.getProperty(key);
    }
    else return o;
  }

  public boolean setProperty(String key, Object val) {
    put(key, val);
    return true; // why ?
  }
  
  public Color getColor(String key) {
    Object o = get(key);
    if (o instanceof Color) {
      return (Color) o;
    } else if (o instanceof String) {
      Color c = Color.decode("0x"+o);
      // replace the color string with a Color object for speed in next call.
      // But be careful to do thiw only when the key->color entry was found
      // in THIS map, not the parent map.
   //   put(key, c);
      return c;
    } else if (parentProperties != null) {
      return parentProperties.getColor(key);
    }
    return null;
  }
  
  public Object clone() {
    PropertyMap clone = (PropertyMap) super.clone();
    clone.parentProperties = this.parentProperties;
    return clone;
  }
  
  public StringBuffer appendXML(String indent, StringBuffer sb) {
    Iterator iter = this.keySet().iterator();
    while (iter.hasNext()) {
     String key = (String) iter.next();
     Object value = (Object) this.getProperty(key);
     sb.append(indent).append("<PROPERTY ");
     XmlStylesheetParser.appendAttribute(sb, "key", key);
     XmlStylesheetParser.appendAttribute(sb, "value", ""+value);
     sb.append("/>\n");
    }
    return sb;
  }

  /** For diagnostic testing, prints the properties and the parent properties, etc. */
  public StringBuffer fullParentHeirarchy(String indent, StringBuffer sb) {
    sb.append(indent).append("<PROPERTIY_MAP>\n");
    Iterator iter = this.keySet().iterator();
    while (iter.hasNext()) {
      String key = (String) iter.next();
      Object value = (Object) this.getProperty(key);
      sb.append(indent + "  ").append("<PROPERTY ");
      XmlStylesheetParser.appendAttribute(sb, "key", key);
      XmlStylesheetParser.appendAttribute(sb, "value", ""+value);
      sb.append("/>\n");
    }
    if (parentProperties == this) {
      System.out.println("********************* INFINITE LOOP !");
    }
    else if (parentProperties != null) {
      parentProperties.fullParentHeirarchy(indent + "  ", sb);
    }
    sb.append(indent).append("</PROPERTIY_MAP>\n");
    return sb;
  }
}
