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

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *  Some global constants.
 */
public abstract class IGBConstants {
  // These variables would make sense as final variables, but then we have
  // to re-compile all the referring classes everytime we re-compile this class.

  public static String APP_NAME = "Integrated Genome Browser";
  public static String APP_SHORT_NAME = "IGB";
  public static String IGB_VERSION;
  public static String BUILD_VERSION;
  private static final String DEFAULT_STRING = "UNKNOWN";

  /*
   * This is not the best way to do this -- It just requires the least
   * change to the current code base.
   */
  static {
	  InputStream is = IGBConstants.class.getResourceAsStream("/igb.properties");
	  Properties p = new Properties();

	  if (is == null) {
		  IGB_VERSION   = DEFAULT_STRING;
		  BUILD_VERSION = DEFAULT_STRING;
	  } else {
		  try {
			p.load(is);
			BUILD_VERSION = p.getProperty("build.version", DEFAULT_STRING);
			IGB_VERSION   = p.getProperty("igb.version", DEFAULT_STRING);
			IGB_VERSION  += "." + BUILD_VERSION;
		  } catch(IOException e) {
			  e.printStackTrace();
		  }finally {
			  try {
				is.close();
			  } catch (IOException e) {
				  e.printStackTrace();
			  }
		  }
	  }
  }
}
