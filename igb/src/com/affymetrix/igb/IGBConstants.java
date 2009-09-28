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

import com.affymetrix.genometryImpl.util.GeneralUtils;
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
  public static String IGB_FRIENDLY_VERSION;	// e.g., 5.4
	private static String BUILD_VERSION;	// e.g., 3579
	public static String IGB_VERSION;		// e.g., 5.4.3579

	private static final String DEFAULT_STRING = "UNKNOWN";

	public static final boolean DEBUG = false;

	public static final String DEFAULT_PREFS_API_RESOURCE = "/igb_default_APIprefs.xml";
	public static final String default_prefs_resource = "/igb_default_prefs.xml";

	//QuickLoad filenames
	public static final String contentsTxt = "contents.txt";
	public static final String annotsTxt = "annots.txt";
	public static final String annotsXml = "annots.xml";
	public static final String liftAllLft = "liftAll.lft";
	public static final String modChromInfoTxt = "mod_chromInfo.txt";

	public static final String UTF8 = "UTF-8";

	public final static String GENOME_SEQ_ID = "genome";
	public final static String ENCODE_REGIONS_ID = "encode_regions";

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
			IGB_FRIENDLY_VERSION = IGB_VERSION;
			IGB_FRIENDLY_VERSION += " BETA (hotfix 6)";
			IGB_VERSION  += "." + BUILD_VERSION;
		  } catch(IOException e) {
			  e.printStackTrace();
		  }finally {
			  GeneralUtils.safeClose(is);
		  }
	  }
  }
}
