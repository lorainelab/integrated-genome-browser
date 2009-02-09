/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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

package com.affymetrix.genometryImpl;

import java.util.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.symmetry.SimpleMutableSeqSymmetry;

public class SimpleSymWithProps extends SimpleMutableSeqSymmetry
  implements SymWithProps {

  /** When this option is true, the convertToObject() method will automatically
   *  be applied to property values in setProperty().
   */
  static boolean OPT_CONVERT_OBJECTS = false;

  /** Set this property to Boolean.TRUE to indicate that the Symmetry is being
   *  used simply to group other Symmetry's together, and that this Symmetry
   *  does not represent any biological feature and should typically not be drawn
   *  as a glyph.
   */
  public static final String CONTAINER_PROP = "container sym";

  protected Map<String,Object> props;

  public SimpleSymWithProps() {
    super();
  }

  public SimpleSymWithProps(int estimated_child_count) {
    this();
    children = new Vector<SeqSymmetry>(estimated_child_count);
  }

  /** Returns the properties map, or null. */
  public Map<String,Object> getProperties() {
    return props;
  }

  /**
   *  Creates a clone of the properties Map.
   *  Uses the same type of Map class (HashMap, TreeMap, etc.)
   *  as the original.
   */
  public Map<String,Object> cloneProperties() {
    if (props == null) { return null; }
    // quick check for efficient Hashtable cloning
    else if (props instanceof Hashtable) {
      return (Map<String,Object>)((Hashtable<String,Object>)props).clone();
    }
    // quick check for efficient HashMap cloning
    else if (props instanceof HashMap) {
      return (Map<String,Object>)((HashMap<String,Object>)props).clone();
    }
    // quick check for efficient TreeMap cloning
    else if (props instanceof TreeMap) {
      return (Map<String,Object>)((TreeMap<String,Object>)props).clone();
    }
    else {
      try {
        Map<String,Object> newprops = (Map<String,Object>) props.getClass().newInstance();
        newprops.putAll(props);
        return newprops;
      }
      catch (Exception ex) {
        System.out.println("problem trying to clone SymWithProps properties, " +
          "returning null instead");
        return null;
      }
    }
  }

  /** Sets the properties to the given Map.
   *  This does not copy the properties, but rather maintains a reference
   *  to the actual Map passed-in.
   *  @param propmap  a Map of String's to String's.  This class is designed to not throw exceptions
   *  if the map is null.
   */
  public boolean setProperties(Map<String,Object> propmap) {
    this.props = propmap;
    return true;
  }

  /** Retrieves the property called "id". */
  public String getID() { return (String)getProperty("id"); }
  public void setID(String id) { setProperty("id", id); }

  public boolean setProperty(String name, Object val) {
    if (name == null)  { return false; }
    if (props == null) {
      props = new HashMap<String,Object>();
    }
    if (OPT_CONVERT_OBJECTS) {
      props.put(name, convertToObject(val));
    } else {
      props.put(name, val);
    }
    return true;
  }

  public Object getProperty(String name) {
    if (props == null) { return null; }
    return props.get(name);
  }

  public void removeProperty(String name) {
    if (props != null) {
      props.remove(name);
    }
  }

  /** Prints all the properties to System out.
   *  Mainly for debugging.
   */
  public void printProps() {
    if (props == null) { System.out.println("no props"); return; }
    Set keys = props.keySet();
    Iterator iter = keys.iterator();
    while (iter.hasNext()) {
      String key = (String)iter.next();
      System.out.println(key + " --> " + props.get(key));
    }
  }

  /**
   *  Converts some Strings to more memory-efficient objects.
   *  This can be useful in reducing the amount of memory required to store
   *  the properties mappings.
   *  If the given value is not a String, it is left alone.
   *  If it is a one-character string, it is converted to a Character.
   *  If it is a String representing an Integer, then an Integer is returned.
   */
  static public Object convertToObject(Object val) {
    Object result = val;
    if (val instanceof String) {
      String str = (String) val;
      if ("".equals(str)) {
        result = "";
      }
      else if (str.length() == 1) {
        return new Character(str.charAt(0));
      }
      else if (Character.isDigit(str.charAt(0))) {
        Integer the_int = null;
        try {
          the_int =  new Integer(str);
        }
        catch (NumberFormatException e) { the_int = null; }
        if (the_int != null) { result = the_int; }
      }
    }
    return result;
  }

}
