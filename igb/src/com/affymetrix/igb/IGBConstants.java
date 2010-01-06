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

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 *  Some global constants.
 */
public interface IGBConstants {
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("igb");

	public static final String APP_NAME         = BUNDLE.getString("appName");
	public static final String APP_NAME_SHORT   = BUNDLE.getString("appNameShort");
	public static final String APP_VERSION      = BUNDLE.getString("appVersion");
	public static final String APP_VERSION_FULL = MessageFormat.format(
			BUNDLE.getString("appVersionFull"),
			APP_VERSION);

	public static final String USER_AGENT = MessageFormat.format(
			BUNDLE.getString("userAgent"),
			APP_NAME_SHORT,
			APP_VERSION_FULL,
			System.getProperty("os.name"),
			System.getProperty("os.version"),
			System.getProperty("os.arch"),
			Locale.getDefault().toString());

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
}
