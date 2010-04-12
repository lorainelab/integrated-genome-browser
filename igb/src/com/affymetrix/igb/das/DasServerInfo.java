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
package com.affymetrix.igb.das;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.igb.util.XMLUtils;
import java.io.InputStream;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.*;

/**
 *
 * @version $Id$
 */
public final class DasServerInfo {

	private static final boolean REPORT_SOURCES = false;
	private static final boolean REPORT_CAPS = true;
	private URL serverURL;
	private final Map<String, DasSource> sources = new LinkedHashMap<String, DasSource>();  // using LinkedHashMap for predictable iteration
	private boolean initialized = false;

	/**
	 * Creates an instance of DasServerInfo for the given DAS server url.
	 * @param url
	 */
	public DasServerInfo(String url) {
		try {
			serverURL = new URL(url);
		} catch (MalformedURLException e) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Unable to convert URL '" + url + "' to URI", e);
		}

	}

	/**
	 * Returns the URL of the server.
	 *
	 * @return the URL of the server
	 */
	public URL getURL() {
		return serverURL;
	}

	public Map<String, DasSource> getDataSources() {
		if (!initialized) {
			initialize();
		}
		return sources;
	}

	/**
	 * Return true if successfully initialized.
	 * see DAS specification for returned XML format in response to "dsn" command:
	 *      http://biodas.org/documents/spec.html
	 */
	private void initialize() {
		InputStream stream = null;
		try {
			System.out.println("Das Request: " + serverURL);
			Map<String, List<String>> headers = new HashMap<String, List<String>>();
			stream = LocalUrlCacher.getInputStream(serverURL, true, null, headers);

			List<String> list;
			String das_version = "";
			String das_status = "";
			String das_capabilities = "";

			list = headers.get("X-DAS-Version");
			if (list != null) {
				das_version = list.toString();
			}
			list = headers.get("X-DAS-Status");
			if (list != null) {
				das_status = list.toString();
			}
			list = headers.get("X-DAS-Capabilities");
			if (list != null) {
				das_capabilities = list.toString();
			}

			System.out.println("DAS server version: " + das_version + ", status: " + das_status);
			if (REPORT_CAPS) {
				System.out.println("DAS capabilities: " + das_capabilities);
			}

			Document doc = XMLUtils.getDocument(stream);

			NodeList dsns = doc.getElementsByTagName("DSN");
			System.out.println("dsn count: " + dsns.getLength());
			for (int i = 0; i < dsns.getLength(); i++) {
				Element dsn = (Element) dsns.item(i);
				try {
					parseDSNElement(dsn);
				} catch (Exception ex) {
					// log and continue with remainder of parsing.
					System.out.println("Error initializing DAS server info for\n" + serverURL);
					ex.printStackTrace();
				}
			}
		} catch (Exception ex) {
			System.out.println("Error initializing DAS server info for\n" + serverURL);
			ex.printStackTrace();
			return;
		} finally {
			GeneralUtils.safeClose(stream);
		}
		initialized = true;
	}

	private void parseDSNElement(Element dsn) throws DOMException {
		NodeList sourcelist = dsn.getElementsByTagName("SOURCE");
		Element source = (Element) sourcelist.item(0);
		if (source == null) {
			// SOURCE tag is required.
			System.out.println("Missing SOURCE element.  Ignoring.");
			return;
		}
		String sourceid = source.getAttribute("id");

		NodeList masterlist = dsn.getElementsByTagName("MAPMASTER");
		Element master = (Element) masterlist.item(0);
		if (master == null) {
			// MAPMASTER tag is required.
			System.out.println("Missing MAPMASTER element.  Ignoring " + sourceid);
			return;
		}
		Text mastertext = (Text) master.getFirstChild();
		String master_url = null;
		if (mastertext != null) {
			master_url = mastertext.getData();
		}
		try {
			URL masterURL = new URL(master_url);
			if (DasSource.getID(masterURL).isEmpty()) {
				Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Skipping " + sourceid + " as MAPMASTER could not be parsed");
				return;
			}
			DasSource das_source = sources.get(DasSource.getID(masterURL));
			if (das_source == null) {
				das_source = new DasSource(serverURL, masterURL);
				sources.put(DasSource.getID(masterURL), das_source);
			}
			das_source.add(sourceid);
		} catch (MalformedURLException ex) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "", ex);
		}
		if (REPORT_SOURCES) {
			System.out.println("sourceid = " + sourceid + ", mapmaster = " + master_url);
		}
	}
}
