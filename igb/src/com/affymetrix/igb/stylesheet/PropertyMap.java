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

  private PropertyMap parentProperties;
  
  public PropertyMap() {
  }
  
  public PropertyMap(PropertyMap p) {
    this();
    this.parentProperties = p;
  }
  
  public void setContext(PropertyMap context) {
    if (context == null) {
      this.parentProperties = null;
    } else {
      // I don't know exactly why, but cloning prevents infinite recursion in some cases.
      this.parentProperties = (PropertyMap) context.clone();
    }
  }
  
  public PropertyMap getContext() {
    return this.parentProperties;
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
    // possibility of infinite recursion which is avoided here (I hope!).
    PropertyMap pm = parentProperties;
    while (o == null && pm != null && pm != this) {
      o = pm.getProperty(key, 0, this);
      pm = pm.parentProperties;
    }
    
    return o;
  }
    
  Object getProperty(String key, int recur, PropertyMap originator) {
    if (originator == this) {
      throw new RuntimeException("Caught an infinite loop!");
    }
    if (recur == 100) {
      throw new RuntimeException("Recursion too deep.");
    }
    
    Object o = super.get(key);
    
    //WARNING: the simple, obvious way of implementing recursion would have the
    // possibility of infinite recursion which is avoided here.
    PropertyMap pm = parentProperties;
    while (o == null && pm != null && pm != this) {
      o = pm.getProperty(key, recur + 1, originator);
      pm = pm.parentProperties;
    }

    return o;
  }

  public boolean setProperty(String key, Object val) {
    super.put(key, val);
    return true; // why ?
  }
    
  public Color getColor(String key) {
        
    Color c = null;
    Object o = getProperty(key);
    if ("".equals(o)) {
      // setting the value of color to "" means that you want to ignore the
      // color settings in any inherited context and revert to the default.
      return null;
    } else if (o instanceof Color) {
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
    // It does not seem necessary to clone the parent properties,
    // but I can revisit this later if necessary
    //clone.parentProperties = (PropertyMap) this.parentProperties.clone();
    clone.parentProperties = this.parentProperties;
    return clone;
  }
  
  public static String PROP_ELEMENT_NAME = "PROPERTY";
  public static String PROP_ATT_KEY = "key";
  public static String PROP_ATT_VALUE = "value";
  
  public StringBuffer appendXML(String indent, StringBuffer sb) {
    Iterator iter = this.keySet().iterator();
    while (iter.hasNext()) {
     String key = (String) iter.next();
     Object value = (Object) this.getProperty(key);
     sb.append(indent).append('<').append(PROP_ELEMENT_NAME);
     XmlStylesheetParser.appendAttribute(sb, PROP_ATT_KEY, key);
     XmlStylesheetParser.appendAttribute(sb, PROP_ATT_VALUE, "" + value);
     sb.append("/>\n");
    }
    return sb;
  }

  /** For diagnostic testing, appends the properties and the parent properties, etc. */
  public StringBuffer fullParentHeirarchy(String indent, StringBuffer sb) {
    sb.append(indent).append("<PROPERTIY_MAP>\n");
    Iterator iter = this.keySet().iterator();
    while (iter.hasNext()) {
      String key = (String) iter.next();
      Object value = (Object) this.getProperty(key);
      sb.append(indent + "  ").append("<PROPERTY ");
      XmlStylesheetParser.appendAttribute(sb, "key", key);
      XmlStylesheetParser.appendAttribute(sb, "value", "" + value);
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
