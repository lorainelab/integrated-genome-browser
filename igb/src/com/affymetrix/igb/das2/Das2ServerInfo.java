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
import java.util.regex.*;
import org.w3c.dom.*;

import com.affymetrix.igb.das.DasLoader;
import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.igb.util.LocalUrlCacher;

public class Das2ServerInfo  {
  static boolean DEBUG_SOURCES_QUERY = false;

  protected static String SOURCES_QUERY = "sequence";

  protected URI server_uri;
  protected String das_version;
  protected String name;
  protected Map sources = new LinkedHashMap();  // map of URIs to Das2Sources, using LinkedHashMap for predictable iteration
  protected Map name2source = new LinkedHashMap();  // using LinkedHashMap for predictable iteration
  protected boolean initialized = false;

  static String URID = "uri";
  static String ID = "id";
  static String TITLE = "title";
  static String NAME = "name";
  static String TYPE = "type";
  static String QUERY_URI = "query_uri";
  static String QUERY_ID = "query_id";

  /** Creates an instance of Das2ServerInfo for the given DAS server.
   *  @param init  whether or not to initialize the data right away.  If false
   *    will not contact the server to initialize data until needed.
   */
  public Das2ServerInfo(String uri, String name, boolean init)
  throws URISyntaxException {
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

  public String toString() { return getName(); }

  public synchronized Map getSources() {
    if (!initialized) { initialize(); }
    return sources;
  }

  /** DAS/2 version is not currently used */
  protected void setDasVersion(String version) {
    das_version = version;
  }

  /** DAS/2 version is not currently used */
  public String getDasVersion() {
    if (!initialized) { initialize(); }
    return das_version;
  }

  protected void addDataSource(Das2Source ds) {
    sources.put(ds.getID(), ds);
    name2source.put(ds.getName(), ds);
  }

  /**
   *  source_id may be either the URI for the source or optionally the source name
   *  If multiple sources in this server have the same name, then this method will only 
   *     return one of the sources that match
   */
  public Das2Source getSource(String id) {
    if (!initialized) { initialize(); }
    Das2Source source = (Das2Source)sources.get(id);
    if (source == null) { source = (Das2Source)name2source.get(id); }
    return source;
  }

  /**
   *  getVersionedSource()
   *    assumes there is only one versioned source for each AnnotatedSeqGroup
   *    if server allows multiple versioned sources per group, then should
   *    use getVersionedSources()
   **/
  public Das2VersionedSource getVersionedSource(AnnotatedSeqGroup group) {
    Collection vsources = getVersionedSources(group);
    if (vsources.size() == 0) { return null; }
    else { return (Das2VersionedSource)vsources.iterator().next(); }
  }

  public Collection getVersionedSources(AnnotatedSeqGroup group) {
    // should probably make a vsource2seqgroup hash,
    //   but for now can just iterate through sources and versions
    //    Das2VersionedSource result = null;
    Set results = new LinkedHashSet();
    Iterator siter = getSources().values().iterator();
    while (siter.hasNext()) {
      Das2Source source = (Das2Source)siter.next();
      Iterator viter = source.getVersions().values().iterator();
      while (viter.hasNext()) {
	Das2VersionedSource version = (Das2VersionedSource)viter.next();
	AnnotatedSeqGroup version_group = version.getGenome();
	if (version_group == group) {
	  //	  result = version;
	  //	  break;
	  results.add(version);
	}
      }
    }
    return results;
  }

  public Das2VersionedSource getVersionedSource(String version_id) {
    Iterator siter = this.getSources().values().iterator();
    while (siter.hasNext()) {
      Das2Source source = (Das2Source)siter.next();
      Das2VersionedSource version = (Das2VersionedSource)source.getVersion(version_id);
      if (version != null) {
	return version;
      }
    }
    return null;
  }

//  public String getDescription() { return description; }
//  protected void setDescription(String desc)  { }

  /**
   * Return true if successfully initialized.
   */
  public synchronized boolean initialize() {
    //TODO: clean-up streams in finally block
    try {
      if (server_uri == null) { return false; }
      //      das_request = server_uri.toURL();
      String das_query = server_uri.toString();

      if (DEBUG_SOURCES_QUERY)  { System.out.println("Das Request: " + server_uri); }
      Map headers = new LinkedHashMap();
      InputStream response = LocalUrlCacher.getInputStream(das_query, headers);

      String content_type = (String)headers.get("content-type");
      if (DEBUG_SOURCES_QUERY) { System.out.println("Das Response content type: " + content_type); }


      if (content_type != null) {
      // setting DAS version if present in content type header -- currently not used
	int vindex = content_type.indexOf("version=");
	if (vindex >= 0) {
	  String das_version = content_type.substring(content_type.indexOf("version=")+8, content_type.length());
	  setDasVersion(das_version);
	}
      }

      //GAH March 2006:
      //   HACK: Affy das server has problems  w/ a trailing slash, but URI resolution
      //      doesn't work without trailing slash, so adding it back in here.
      if (! das_query.endsWith("/"))  { das_query = das_query+"/"; }
      //       Document doc = DasLoader.getDocument(request_con);
      Document doc = DasLoader.getDocument(response);

      Element top_element = doc.getDocumentElement();
      NodeList sources= doc.getElementsByTagName("SOURCE");
      //      System.out.println("source count: " + sources.getLength());
      for (int i=0; i< sources.getLength(); i++)  {
        Element source = (Element)sources.item(i);
        //        System.out.println("source base URI: " + source.getBaseURI(das_query, source));
        String source_id = source.getAttribute(URID);
	if (source_id.length() == 0) { source_id = source.getAttribute(ID); }
	String source_name = source.getAttribute(TITLE);
	if (source_name.length() == 0) { source_name = source.getAttribute(NAME); }
	if (DEBUG_SOURCES_QUERY) { System.out.println("title: " + source_name + ",  length: " + source_name.length()); }
	if (source_name == null || source_name.length() == 0) { source_name = source_id; }
	if (DEBUG_SOURCES_QUERY)  { System.out.println("source_name: " + source_name); }
        String source_info_url = source.getAttribute("doc_href");
        String source_description = source.getAttribute("description");
        String source_taxon = source.getAttribute("taxid");

	URI source_uri = getBaseURI(das_query, source).resolve(source_id);

	Das2Source dasSource = new Das2Source(this, source_uri, source_name, source_info_url,
					      source_taxon, source_description);
	this.addDataSource(dasSource);
	NodeList slist = source.getChildNodes();
	for (int k=0; k < slist.getLength(); k++) {
	  if (slist.item(k).getNodeName().equals("VERSION"))  {
	    Element version = (Element)slist.item(k);
	    String version_id = version.getAttribute(URID);
	    if (version_id.length() == 0) { version_id = version.getAttribute(ID); }
	    String version_name = version.getAttribute(TITLE);
	    if (version_name.length() == 0) { version_name = version.getAttribute(NAME); }
	    if (version_name.length() == 0) { version_name = version_id; }
	    if (DEBUG_SOURCES_QUERY)  { System.out.println("version_name: " + version_name); }

	    String version_desc = version.getAttribute("description");
	    String version_info_url = version.getAttribute("doc_href");
	    //	    setDasVersionedSource(dasSource, version_id, false);
	    URI version_uri = getBaseURI(das_query, version).resolve(version_id);
	    if (DEBUG_SOURCES_QUERY) {
	      System.out.println("base URI for version element: " + getBaseURI(das_query, version));
	      System.out.println("versioned source, name: " + version_name + ", URI: " + version_uri.toString());
	    }


	    NodeList vlist = version.getChildNodes();
	    HashMap caps = new HashMap();
	    URI coords_uri = null;
	    for (int j=0; j<vlist.getLength(); j++) {
	      String nodename = vlist.item(j).getNodeName();
	      // was CATEGORY, renamed CAPABILITY
	      if (nodename.equals("CAPABILITY") || nodename.equals("CATEGORY")) {
		Element capel = (Element)vlist.item(j);
		String captype = capel.getAttribute(TYPE);
		String query_id = capel.getAttribute(QUERY_URI);
		if (query_id.length() == 0) { query_id = capel.getAttribute(QUERY_ID); }
		URI base_uri = getBaseURI(das_query, capel);
		URI cap_root = base_uri.resolve(query_id);
		if (DEBUG_SOURCES_QUERY) {
		  System.out.println("Capability: " + captype + ", URI: " + cap_root);
		}
		// for now don't worry about format subelements
		Das2Capability cap = new Das2Capability(captype, cap_root, null);
		//		vsource.addCapability(cap);
		caps.put(captype, cap);
	      }
	      else if (nodename.equals("COORDINATES")) {
		Element coordel = (Element)vlist.item(j);
		String uri_att = coordel.getAttribute("uri");
		URI base_uri = getBaseURI(das_query, coordel);
		coords_uri = base_uri.resolve(uri_att);
		//		System.out.println("$$$$ Coordinates URI: " + coords_uri);
	      }
	    }
	    Das2VersionedSource vsource;
	    if (caps.get(Das2WritebackVersionedSource.WRITEBACK_CAP_QUERY) != null) {
	      vsource = new Das2WritebackVersionedSource(dasSource, version_uri, coords_uri, version_name,
							 version_desc, version_info_url, false);
	    }
	    else {
	      vsource = new Das2VersionedSource(dasSource, version_uri, coords_uri, version_name,
						version_desc, version_info_url, false);
	    }
	    Iterator capiter = caps.values().iterator();
	    while (capiter.hasNext()) {
	      Das2Capability cap = (Das2Capability)capiter.next();
	      vsource.addCapability(cap);
	    }
	    dasSource.addVersion(vsource);
	  }
	}
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    initialized = true;
    return initialized;
  }

  /**
	    static boolean TEST_WRITEBACK_SERVER = false;

	    // hardwired hack to see test writeback server (which has version_id "yeast/S228C-writeback");
	    Das2VersionedSource write_hack_src = null;
	    boolean add_writeback_hack = false;
	    if (TEST_WRITEBACK_SERVER && version_id.endsWith("yeast/S228C")) {
	      System.out.println("adding writeback source hack");
	      add_writeback_hack = true;
	      URI hack_uri = new URI(version_uri.toString() + "-writeback");
	      write_hack_src = new Das2VersionedSource(dasSource, hack_uri, version_name + "-writeback",
						       null, null, false);
	      dasSource.addVersion(write_hack_src);
	    }

		// hardwired hack to see test writeback server (which has version_id "yeast/S228C-writeback");
		if (add_writeback_hack) {
		  System.out.println("adding writeback capability hack");
		  Pattern pat = Pattern.compile("yeast/S228C");
		  Matcher mat = pat.matcher(cap_root.toString());
		  String hack_root = mat.replaceAll("yeast/S228C-writeback");
		  Das2Capability write_cap = new Das2Capability(captype, new URI(hack_root), null);
		  write_hack_src.addCapability(write_cap);
		}
  */


  /**
   * Attempt to retrieve base URI for an Element from a DOM-level2 model
   */
  public static URI getBaseURI(String doc_uri, Node cnode) {
    Stack xml_bases = new Stack();
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
	String xbase = (String) xml_bases.pop();
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

  /**
   *  For testing.
   */
  public static void main(String[] args) {
    System.out.println("This is deprecated, use the JUnit test for Das2ServerInfo instead");
    }
}


