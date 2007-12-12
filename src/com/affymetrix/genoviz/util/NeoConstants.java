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

/**
 * some constants for use in multiple genoviz packages.
 */
public interface NeoConstants {

  public static boolean flush = true;
  public static boolean dispose = true;

  // might want to make all these constants final as well...

  public enum Orientation {
    Horizontal, Vertical
  }
  
  public enum Direction {
    LEFT, RIGHT, UP, DOWN, CENTER, NONE,
    /** For flipping or reflecting things about a vertical axis. */
    MIRROR_VERTICAL,
    /** For flipping or reflecting things about a horizontal axis. */
    MIRROR_HORIZONTAL
  }


  public enum Placement {
    ABOVE, BELOW, CENTER, LEFT, RIGHT, NONE
  }
}
