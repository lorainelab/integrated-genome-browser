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

import java.util.Comparator;
import com.affymetrix.genoviz.bioviews.Point2D;
import com.affymetrix.genometry.*;

public class Point2DComparator implements Comparator  {
  boolean compare_by_x;
  boolean ascending_order;

  public Point2DComparator(boolean xcomp, boolean ascending) {
    compare_by_x = xcomp;
    ascending_order = ascending;
  }

  public int compare(Object obj1, Object obj2) {
    Point2D p1 = (Point2D)obj1;
    Point2D p2 = (Point2D)obj2;
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
