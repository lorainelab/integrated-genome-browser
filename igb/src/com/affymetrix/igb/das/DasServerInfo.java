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

import com.affymetrix.igb.util.LocalUrlCacher;
import java.net.*;
import java.util.*;
import org.w3c.dom.*;

public final class DasServerInfo {

	private static final boolean REPORT_SOURCES = false;
	private static final boolean REPORT_CAPS = true;
	private String root_url;
	private String name;
	private Map<String, DasSource> sources = new LinkedHashMap<String, DasSource>();  // using LinkedHashMap for predictable iteration
	private boolean initialized = false;

	/** Creates an instance of DasServerInfo for the given DAS server.
	 *  @param init  whether or not to initialize the data right away.  If false
	 *    will not contact the server to initialize data until needed.
	 */
	public DasServerInfo(String url, String name) {
		root_url = url;
		this.name = name;
		// all trailing "/" chars are stripped off the end if present
		while (root_url.endsWith("/")) {
			root_url = root_url.substring(0, root_url.length() - 1);
		}
		/*if (init) {
			initialize();
		}*/
	}

	/** Returns the root URL String.  Will not have any trailing "/" at the end. */
	public String getRootUrl() {
		return root_url;
	}

	/*public String getName() {
		return name;
	}*/


	public Map<String, DasSource> getDataSources() {
		if (!initialized) {
			initialize();
		}
		return sources;
	}

	private void addDataSource(DasSource ds) {
		sources.put(ds.getID(), ds);
	}

	/**
	 * Return true if successfully initialized.
	 * see DAS specification for returned XML format in response to "dsn" command:
	 *      http://biodas.org/documents/spec.html
	 */
	private void initialize() {
		//TODO: think about whether this needs synchronization.
		//TODO: clean-up streams in finally block
		try {
			//      System.out.println("in DasUtils.findDasSource()");
			//      SynonymLookup lookup = SynonymLookup.getDefaultLookup();
			String request_str = root_url;
			if (!request_str.contains("/dsn")) {
				request_str += "/dsn";
			}
			System.out.println("Das Request: " + request_str);
			URL das_request = new URL(request_str);
			URLConnection request_con = das_request.openConnection();
			request_con.setConnectTimeout(LocalUrlCacher.CONNECT_TIMEOUT);
			request_con.setReadTimeout(LocalUrlCacher.READ_TIMEOUT);
			String das_version = request_con.getHeaderField("X-DAS-Version");
			String das_status = request_con.getHeaderField("X-DAS-Status");
			String das_capabilities = request_con.getHeaderField("X-DAS-Capabilities");

			//setDasVersion(das_version);

			System.out.println("DAS server version: " + das_version + ", status: " + das_status);
			if (REPORT_CAPS) {
				System.out.println("DAS capabilities: " + das_capabilities);
			}
		
			Document doc = DasLoader.getDocument(request_con);

			Element top_element = doc.getDocumentElement();
			NodeList dsns = doc.getElementsByTagName("DSN");
			System.out.println("dsn count: " + dsns.getLength());
			for (int i = 0; i < dsns.getLength(); i++) {
				Element dsn = (Element) dsns.item(i);
				try {
				parseDSNElement(dsn);
				} catch (Exception ex) {
					// log and continue with remainder of parsing.
					System.out.println("Error initializing DAS server info for\n" + root_url);
					ex.printStackTrace();
				}
			}
		} catch (Exception ex) {
			System.out.println("Error initializing DAS server info for\n" + root_url);
			ex.printStackTrace();
			return;
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
		String source_version = source.getAttribute("version");
		String sourcename = null;
		Text nametext = (Text) source.getFirstChild();
		if (nametext != null) {
			sourcename = nametext.getData();
		}

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
		NodeList desclist = dsn.getElementsByTagName("DESCRIPTION");
		String info_url = null;
		String description = null;
		if (desclist.getLength() > 0) {
			Element desc = (Element) desclist.item(0);
			info_url = desc.getAttribute("href");
			Text desctext = (Text) desc.getFirstChild();
			if (desctext != null) {
				description = desctext.getData();
			}
		}
		DasSource das_source = new DasSource(this, sourceid, false);
		das_source.setVersion(source_version);
		das_source.setName(sourcename);
		das_source.setDescription(description);
		das_source.setInfoUrl(info_url);
		das_source.setMapMaster(master_url);
		this.addDataSource(das_source);
		if (REPORT_SOURCES) {
			System.out.println("sourceid = " + sourceid + ", version = " + source_version + ", name = " + sourcename);
			System.out.println("   mapmaster = " + master_url + ", info_url = " + info_url + ", description = " + description);
		}
	}
}
