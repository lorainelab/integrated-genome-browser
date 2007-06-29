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

package com.affymetrix.genometryImpl.util;

import java.util.Comparator;

/**
 * Similar to java.awt.geom.Point2D, but uses double precision.
 */
public class PointDouble {
    public double x;
    public double y;

    /**
     * Constructor.
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public PointDouble(double x, double y) {
      this.x = x;
      this.y = y;
    }

    public static Comparator getComparator(boolean sort_by_x, boolean ascending) {
      return new PointDoubleComparator(sort_by_x, ascending);
    }

    /**
     * Checks whether two points are equal.
     */
    public boolean equals(Object obj) {
      if (obj instanceof PointDouble) {
        PointDouble pt = (PointDouble)obj;
        return (x == pt.x) && (y == pt.y);
      }
      return false;
    }
    
    public boolean equals(PointDouble pt) {
      return (x == pt.x) && (y == pt.y);
    }

    /**
     * Returns the String representation of the coordinates.
     */
    public String toString() {
      return getClass().getName() + "[x=" + x + ",y=" + y + "]";
    }

static class PointDoubleComparator implements Comparator  {
  boolean compare_by_x;
  boolean ascending_order;

  public PointDoubleComparator(boolean xcomp, boolean ascending) {
    compare_by_x = xcomp;
    ascending_order = ascending;
  }

  public int compare(Object obj1, Object obj2) {
    PointDouble p1 = (PointDouble)obj1;
    PointDouble p2 = (PointDouble)obj2;
    if (compare_by_x) {
      if (ascending_order) {  // compare by x, ascending order
        if (p1.x < p2.x) { return -1; }
        else if (p1.x > p2.x) { return 1; }
        else { return 0; }
      }
      else { // compare by x, descending order
        if (p1.x < p2.x) { return 1; }
        else if (p1.x > p2.x) { return -1; }
        else { return 0; }
      }
    }
    else {
      if (ascending_order) {  // compare by y, ascending order
        if (p1.y < p2.y) { return -1; }
        else if (p1.y > p2.y) { return 1; }
        else { return 0; }
      }
      else { // compare by y, descending order
        if (p1.y < p2.y) { return 1; }
        else if (p1.y > p2.y) { return -1; }
        else { return 0; }
      }
    }
  }

}
}
