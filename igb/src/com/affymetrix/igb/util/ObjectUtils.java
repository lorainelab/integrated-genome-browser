/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

package com.affymetrix.igb.util;

public class ObjectUtils {

  public static String objString(Object obj) {
    return obj.getClass().getName() + "@" + Integer.toHexString(obj.hashCode());
  }

  public static String objToString(Object obj) { return objString(obj); }
  public static String objectToString(Object obj) { return objString(obj); }

  
  /**
   *  Wrapper around {@link Class#forName(String)} that preserves backward compatibility
   *  of class names from Neo N-G-S-D-K and the old name of IGB.
   *  First tries to instantiate the class using the name given.  If that fails,
   *  next tries converting names with com.neo to com.affymetrix.genoviz, and
   *  from "uni brow" to igb.
   */
  public static Class classForName(String name) throws ClassNotFoundException {
    Class result = null;
    try {
      result = Class.forName(name);
    } catch (ClassNotFoundException cfe) {
      // The strings below contain references to some out-dated class names.
      // Those are broken in "str" + "ange" + " look" + "ing" ways to protect
      // them from automated search-and-replace.
      String name2 = name.replaceAll("com.neo" + "morphic", "com.affymetrix.genoviz");
      name2 = name2.replaceAll("com.affymetrix.uni" + "brow", "com.affymetrix.igb");
      if (name2.equals(name)) {
        // Create a brand-new exception so the stack trace will be shorter
        throw new ClassNotFoundException(name);
      }
      else {
        System.out.println("INFO: converted class name '"+name+"' to '"+name2+"'");
        try {
          result = Class.forName(name2);
        } catch (ClassNotFoundException cfe2) {
          throw new ClassNotFoundException("Could not find either class '"+name+"' or '"+name2+"'");
        }
      }
    }
    return result;
  }
}

