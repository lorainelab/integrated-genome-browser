/**
*   Copyright (c) 2001-2006 Affymetrix, Inc.
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

package com.affymetrix.igb;


/**
 *  Some global constants.
 */
public abstract class IGBConstants {
  // These variables would make sense as final variables, but then we have
  // to re-compile all the referring classes everytime we re-compile this class.

  public static String APP_NAME = "Integrated Genome Browser";
  public static String APP_SHORT_NAME = "IGB";
  public static String IGB_VERSION = "4.34";
}
