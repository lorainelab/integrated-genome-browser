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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *  An implementation of the Java Map class that also implements
 *  the Genometry Propertied interface.
 */
public class PropertyMap extends HashMap implements Map, Propertied, XmlAppender {

  public PropertyMap() {
  }
  
  public PropertyMap(PropertyMap p) {
    this();
    this.putAll(p);
  }
  
  public Map getProperties() {
    return this;
  }

  public Map cloneProperties() {
    return new HashMap((Map) this);
  }

  public Object getProperty(String key) {
    return get(key);
  }

  public boolean setProperty(String key, Object val) {
    put(key, val);
    return true; // why ?
  }
  
  public StringBuffer appendXML(String indent, StringBuffer sb) {
    Iterator iter = this.keySet().iterator();
    while (iter.hasNext()) {
     String key = (String) iter.next();
     String value = (String) this.getProperty(key);
     sb.append(indent).append("<PROPERTY ");
     XmlStylesheetParser.appendAttribute(sb, "key", key);
     XmlStylesheetParser.appendAttribute(sb, "value", value);
     sb.append("/>\n");
    }
    return sb;
  }
}
