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

  public Object get(Object key) {
    return this.getProperty((String) key);
  }
  
  public Object getProperty(String key) {
    Object o = super.get(key);
    
    //WARNING: the simple, obvious way of implementing recursion would have the
    // possibility of infinite recursion which is avoided here.
    PropertyMap pm = parentProperties;
    while (o == null && pm != null && pm != this) {
      o = pm.getProperty(key);
      pm = pm.parentProperties;
    }

    if ("".equals(o)) {
      o = null; // this allows a way to ignore properties set in a higher level parent
    }
    return o;
  }

  public boolean setProperty(String key, Object val) {
    super.put(key, val);
    return true; // why ?
  }
  
  
//  Color convertToColor() {}
  
  public Color getColor(String key) {
        
    Color c = null;
    Object o = getProperty(key);
    if (o instanceof Color) {
      c = (Color) o;
    } else if (o instanceof String) {
      c = Color.decode("0x"+o);
    }

    PropertyMap pm = this;
    if (c != null) {
      // replace the color string with a Color object for speed in next call.
      // But be careful to do thiw only where the key->color entry was found
      // in THIS map, not a parent or child map.
      while (pm != null && pm != this) {
        if (pm.get(key) != null) {
          pm.put(key, c);
          pm = null; // to end the loop
        } else {
          pm = pm.parentProperties;
        }
      }
    }

    return c;
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
