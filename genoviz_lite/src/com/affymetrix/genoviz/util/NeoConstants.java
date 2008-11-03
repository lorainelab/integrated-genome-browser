/**
*   Copyright (c) 1998-2008 Affymetrix, Inc.
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

//TODO separate these enums into separate java files
public interface NeoConstants {
  
  /**
   * Represents a direction in Pixel space, 
   * not Widget Coordinate space.
   */
  public static enum Direction {
    LEFT, RIGHT, UP, DOWN, CENTER, NONE,
    /** For flipping or reflecting things about a vertical axis. */
    MIRROR_VERTICAL,
    /** For flipping or reflecting things about a horizontal axis. */
    MIRROR_HORIZONTAL
  }

  /**
   * Represents a relative placement in 
   * Pixel space, not Widget Coordinate space.
   */
  public static enum Placement {
    ABOVE, BELOW, CENTER, LEFT, RIGHT, NONE
  }
}
