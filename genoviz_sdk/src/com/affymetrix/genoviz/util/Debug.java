/**
*   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package com.affymetrix.genoviz.util;

import java.io.PrintStream;
import java.lang.Error;
import java.util.Stack;

/**
 * This class is for tracing and testing state.
 * Adapted from Blossom Associates West.
 */
public class Debug {
  private static Stack stack = new Stack();
  public final static int OFF = 0;
  public final static int WARN = OFF + 1;
  public final static int INFORM = WARN + 1;
  public final static int TRACE = INFORM + 1;
  public final static int ON = TRACE;

  private static int level = OFF;

  private static PrintStream pstr = System.err;

  private Debug () { }

  public static void setPrintStream(PrintStream pstr) {
    Debug.pstr = pstr;
  }

  public static PrintStream getPrintStream() {
    return pstr;
  }

  public static void setLevel(int level) {
    Debug.level = level;
  }

  public static int getLevel() {
    return level;
  }

  public static void warn(String s) {
    if (WARN <= level) {
      if (null!=pstr) {pstr.println(s);
      }
    }
  }

  public static void inform(String s) {
   if (INFORM <= level) {
      if (null!=pstr) {pstr.println(s);
      }
    }
  }

  public static void trace(String s) {
    if (TRACE <= level) {
      if (null!=pstr) {pstr.println(s);
      }
    }
  }

  public static void showStack(String s) {
    Exception e = new RuntimeException(s);
    if (null != pstr) {
      e.printStackTrace(pstr);
    }
  }

  public static void test(boolean cond, String s) {
    if (!cond) { warn(s); }
  }

  public static void test(boolean cond) {
    test(cond, "Assertion Failed");
  }

  /**
   * sets the debug level to newLevel and saves the old value in a stack.
   */
  public static void push(int newLevel) {
    stack.push(new Integer(level));
    level = newLevel;
  }

  /**
   * restores previous debug level
   */
  public static void pop() {
    level = ((Integer) stack.pop()).intValue();
  }

}
