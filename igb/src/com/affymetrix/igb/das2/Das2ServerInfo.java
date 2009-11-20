/**
 *   Copyright (c) 2005-2007 Affymetrix, Inc.
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

package com.affymetrix.igb.das2;

import java.io.*;
import java.net.*;
import java.util.*;
import org.w3c.dom.*;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.util.LocalUrlCacher;
import com.affymetrix.igb.util.XMLUtils;

public final class Das2ServerInfo  {
  private static boolean DEBUG_SOURCES_QUERY = false;
	private URI server_uri;
	private String das_version;
	private String name;
	private final Map<String,Das2Source> sources = new LinkedHashMap<String,Das2Source>();  // map of URIs to Das2Sources, using LinkedHashMap for predictable iteration
	private final Map<String,Das2Source> name2source = new LinkedHashMap<String,Das2Source>();  // using LinkedHashMap for predictable iteration
	private boolean initialized = false;
	private String sessionId = null; //used to store a session id following authentication with a DAS2 server

	private static String URID = "uri";
	private static String ID = "id";
	private static String TITLE = "title";
	private static String NAME = "name";
	private static String TYPE = "type";
	private static String QUERY_URI = "query_uri";
	private static String QUERY_ID = "query_id";

	/** Creates an instance of Das2ServerInfo for the given DAS2 server.
	 *  @param init  whether or not to initialize the data right away.  If false
	 *    will not contact the server to initialize data until needed.
	 */
	public Das2ServerInfo(String uri, String name, boolean init) throws URISyntaxException {
		String root_string = uri;
		// FIXME: if you remove the trailing slash then relative URI resolution doesn't work
		// on the das.biopackages.net server!
		// all trailing "/" chars are stripped off the end if present
		while (root_string.endsWith("/")) {
			root_string = root_string.substring(0, root_string.length()-1);
		}

		this.server_uri = new URI(root_string);
		this.name = name;
		if (init) {
			initialize();
		}
	}

	/** Returns the root URL String.  Will not have any trailing "/" at the end. */
	public URI getURI() { return server_uri; }
	public String getID() { return server_uri.toString(); }
	public String getName() {
		return name;
	}

	@Override
	public String toString() { return getName(); }

	public synchronized Map<String,Das2Source> getSources() {
		if (!initialized) { initialize(); }
		return sources;
	}

	/** DAS/2 version is not currently used */
	private void setDasVersion(String version) {
		das_version = version;
	}

	/** DAS/2 version is not currently used */
	public String getDasVersion() {
		if (!initialized) { initialize(); }
		return das_version;
	}

	private void addDataSource(Das2Source ds) {
		sources.put(ds.getID(), ds);
		name2source.put(ds.getName(), ds);
	}

	/**
	 *  getVersionedSource()
	 *    assumes there is only one versioned source for each AnnotatedSeqGroup
	 *    if server allows multiple versioned sources per group, then should
	 *    use getVersionedSources()
	 **/
	public Das2VersionedSource getVersionedSource(AnnotatedSeqGroup group) {
		Collection<Das2VersionedSource> vsources = getVersionedSources(group);
		if (vsources.isEmpty()) { return null; }
		else { return vsources.iterator().next(); }
	}

	private Collection<Das2VersionedSource> getVersionedSources(AnnotatedSeqGroup group) {
		// should probably make a vsource2seqgroup hash,
		//   but for now can just iterate through sources and versions
		Set<Das2VersionedSource> results = new LinkedHashSet<Das2VersionedSource>();
		for (Das2Source source : getSources().values()) {
			for (Das2VersionedSource version : source.getVersions().values()) {
				AnnotatedSeqGroup version_group = version.getGenome();
				if (version_group == group) {
					results.add(version);
				}
			}
		}
		return results;
	}

	/**Checks to see if a particular DAS2 server handles authentication. If so, will prompt user for login info and then
	 * sends it to the server for validation.  If OK, fetches and sets the sessionId.*/
	private synchronized void login(){
    ArrayList<String> log = new ArrayList<String>();
    //log.add("Attempting login to server: "+server_uri);
    try{
      
      // The DAS2 server uses basic authentication.  When we attempt to connect, the servlet 
      // will send back a response, indicating that authenication is required. This will trigger 
      // the Authenticator (see com.affymetrics.igb.util.UnibrowAuthenticator)
      // to be invoked, presenting a dialog for enter a network login and password.
      // The Authenticator is responsible for password the username and password
      // to the servlet.  If authenication succeeds, a session will 
      // be established.      
      String das_query = server_uri+"/login";
      log.add("\t\tDas2 Authentication Request: " + das_query);
      

      LinkedHashMap headers = new LinkedHashMap();

      // We must connect to the URL w/o caching so that subsequent launches
      // of IGB from same machine do no use the old session that may have
      // been established with a different login (user).
      InputStream response = LocalUrlCacher.getInputStream(das_query, LocalUrlCacher.IGNORE_CACHE, false, headers);
      Document doc = XMLUtils.getDocument(response);
      
      String cookie = (String)headers.get("set-cookie");
        if(cookie != null) {
          sessionId = cookie.substring(0, cookie.indexOf(";"));
          log.add("\t\tSessionId: "+sessionId);
        }
        else {
          log.add("\t\tNo sessionId found in header. No authentication.");
        }
    }   catch (Exception ex) {
      log.add("\t\tServer doesn't support/recognize authentication.");
    } finally {
      for (int i=0; i< log.size(); i++) System.out.println(log.get(i));
    }
  }




	/**
	 * Return true if successfully initialized.
	 */
	private synchronized boolean initialize() {
		InputStream response = null;
		try {
			if (server_uri == null) { return false; }
			//      das_request = server_uri.toURL();
			String das_query = server_uri.toString();
			
			//does server support authentication? If so then throw up dialog box and attempt.
			login();

			if (DEBUG_SOURCES_QUERY) {
				System.out.println("Das2 Request: " + server_uri);
			}
			Map<String,String> headers = new LinkedHashMap<String,String>();
			response = LocalUrlCacher.getInputStream(das_query, headers);
			if (response == null) {
				System.out.println("WARNING: Could not find Das2 server " + server_uri);
				return false;
			}

			String content_type = headers.get("content-type");
			if (DEBUG_SOURCES_QUERY) {
				System.out.println("Das2 Response content type: " + content_type);
			}


			if (content_type != null) {
				// setting DAS2 version if present in content type header -- currently not used
				int vindex = content_type.indexOf("version=");
				if (vindex >= 0) {
					String das_version = content_type.substring(content_type.indexOf("version=") + 8, content_type.length());
					setDasVersion(das_version);
				}
			}

			//GAH March 2006:
			//   HACK: Affy das2 server has problems  w/ a trailing slash, but URI resolution
			//      doesn't work without trailing slash, so adding it back in here.
			if (!das_query.endsWith("/")) {
				das_query = das_query + "/";
			}
			//       Document doc = DasLoader.getDocument(request_con);
			System.out.println("Initializing " + server_uri);
			Document doc = XMLUtils.getDocument(response);

			Element top_element = doc.getDocumentElement();

			NodeList sources = doc.getElementsByTagName("SOURCE");
			parseSources(sources, das_query);
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;   // not successfully initialized if there was an exception.
		} finally {
			GeneralUtils.safeClose(response);
		}
		initialized = true;
		return initialized;
	}

	private void parseSources(NodeList sources, String das_query) {
		//      System.out.println("source count: " + sources.getLength());
		for (int i = 0; i < sources.getLength(); i++) {
			Element source = (Element) sources.item(i);
			//        System.out.println("source base URI: " + source.getBaseURI(das_query, source));
			String source_id = source.getAttribute(URID);
			if (source_id.length() == 0) {
				source_id = source.getAttribute(ID);
			}
			String source_name = source.getAttribute(TITLE);
			if (source_name.length() == 0) {
				source_name = source.getAttribute(NAME);
			}
			if (DEBUG_SOURCES_QUERY) {
				System.out.println("title: " + source_name + ",  length: " + source_name.length());
			}
			if (source_name == null || source_name.length() == 0) {
				source_name = source_id;
			}
			if (DEBUG_SOURCES_QUERY) {
				System.out.println("source_name: " + source_name);
			}

			URI source_uri = getBaseURI(das_query, source).resolve(source_id);
			Das2Source dasSource = new Das2Source(this, source_uri, source_name);
			this.addDataSource(dasSource);
			
			NodeList slist = source.getChildNodes();
			parseSourceChildren(slist, das_query, dasSource);
		}
	}


	private void parseSourceChildren(NodeList slist, String das_query, Das2Source dasSource) {
		for (int k = 0; k < slist.getLength(); k++) {
			if (!slist.item(k).getNodeName().equals("VERSION")) {
				continue;
			}

			Element version = (Element) slist.item(k);
			String version_id = version.getAttribute(URID);
			if (version_id.length() == 0) {
				version_id = version.getAttribute(ID);
			}
			String version_name = version.getAttribute(TITLE);
			if (version_name.length() == 0) {
				version_name = version.getAttribute(NAME);
			}
			if (version_name.length() == 0) {
				version_name = version_id;
			}
			if (DEBUG_SOURCES_QUERY) {
				System.out.println("version_name: " + version_name);
			}
			String version_desc = version.getAttribute("description");
			String version_info_url = version.getAttribute("doc_href");
			//	    setDasVersionedSource(dasSource, version_id, false);
			URI version_uri = getBaseURI(das_query, version).resolve(version_id);
			if (DEBUG_SOURCES_QUERY) {
				System.out.println("base URI for version element: " + getBaseURI(das_query, version));
				System.out.println("versioned source, name: " + version_name + ", URI: " + version_uri.toString());
			}
			NodeList vlist = version.getChildNodes();
			HashMap<String,Das2Capability> caps = new HashMap<String,Das2Capability>();
			URI coords_uri = null;
			for (int j = 0; j < vlist.getLength(); j++) {
				String nodename = vlist.item(j).getNodeName();
				// was CATEGORY, renamed CAPABILITY
				if (nodename.equals("CAPABILITY") || nodename.equals("CATEGORY")) {
					Element capel = (Element) vlist.item(j);
					String captype = capel.getAttribute(TYPE);
					String query_id = capel.getAttribute(QUERY_URI);
					if (query_id.length() == 0) {
						query_id = capel.getAttribute(QUERY_ID);
					}
					URI base_uri = getBaseURI(das_query, capel);
					URI cap_root = base_uri.resolve(query_id);
					if (DEBUG_SOURCES_QUERY) {
						System.out.println("Capability: " + captype + ", URI: " + cap_root);
					}
					// for now don't worry about format subelements
					Das2Capability cap = new Das2Capability(captype, cap_root);
					//		vsource.addCapability(cap);
					caps.put(captype, cap);
				} else if (nodename.equals("COORDINATES")) {
					Element coordel = (Element) vlist.item(j);
					String uri_att = coordel.getAttribute("uri");
					URI base_uri = getBaseURI(das_query, coordel);
					coords_uri = base_uri.resolve(uri_att);
				//		System.out.println("$$$$ Coordinates URI: " + coords_uri);
				}
			}

			Das2VersionedSource vsource = new Das2VersionedSource(dasSource, version_uri, coords_uri, version_name, version_desc, version_info_url, false);

			Iterator<Das2Capability> capiter = caps.values().iterator();
			while (capiter.hasNext()) {
				Das2Capability cap = capiter.next();
				vsource.addCapability(cap);
			}
			dasSource.addVersion(vsource);
		}

	}

	


	/**
	 * Attempt to retrieve base URI for an Element from a DOM-level2 model
	 */
	public static URI getBaseURI(String doc_uri, Node cnode) {
		Stack<String> xml_bases = new Stack<String>();
		Node pnode = cnode;
		while (pnode != null) {
			if (pnode instanceof Element) {
				Element el = (Element)pnode;
				String xbase = el.getAttribute("xml:base");
				if (xbase != null && !xbase.equals("")) { xml_bases.push(xbase); }
			}
			pnode = pnode.getParentNode();
		}

		URI base_uri;
		try  {
			base_uri = new URI(doc_uri);
			while (! (xml_bases.empty())) {
				String xbase = xml_bases.pop();
				base_uri = base_uri.resolve(xbase);
			}
		}
		catch (Exception ex)  {
			System.out.println("*** problem figuring out base URI, setting to null");
			base_uri = null;
		}
		return base_uri;
	}

	public boolean isInitialized() { return initialized; }

	/**Returns null or a JSESSIONID used in authentication*/
	public String getSessionId() {
		return sessionId;
	}

}
