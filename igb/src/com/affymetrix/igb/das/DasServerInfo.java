/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import org.w3c.dom.*;

import com.affymetrix.genometry.AnnotatedBioSeq;
import com.affymetrix.igb.util.SynonymLookup;  //  just for testing via main()
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.util.ErrorHandler;

  //  just for testing via main()


public class DasServerInfo {
  static boolean REPORT_SOURCES = false;
  static boolean REPORT_CAPS = true;

  static Pattern cap_splitter = Pattern.compile("; *");
  static Pattern name_version_splitter = Pattern.compile("/");

  String root_url;
  String das_version;
  String name;
  String description;
  Map capabilities = new LinkedHashMap();  // using LinkedHashMap for predictable iteration
  Map sources = new LinkedHashMap();  // using LinkedHashMap for predictable iteration
  boolean initialized = false;

  /** Creates an instance of DasServerInfo for the given DAS server.
   *  @param init  whether or not to initialize the data right away.  If false
   *    will not contact the server to initialize data until needed.
   */
  public DasServerInfo(String url, String name, boolean init) {
    root_url = url;
    this.name = name;
    // all trailing "/" chars are stripped off the end if present
    while (root_url.endsWith("/")) {
      root_url = root_url.substring(0, root_url.length()-1);
    }
    if (init) {
      initialize();
    }
  }

  /** Returns the root URL String.  Will not have any trailing "/" at the end. */
  public String getRootUrl() {
    return root_url;
  }

  public String getName() {
    return name;
  }

  public String getDasVersion() {
    if (!initialized) { initialize(); }
    return das_version;
  }

  public String getDescription() { return description; }

  public Map getCapabilities() {
    if (!initialized) { initialize(); }
    return capabilities;
  }

  public String getCapability(String cap) {
    if (!initialized) { initialize(); }
    return (String) capabilities.get(cap);
  }

  public Map getDataSources() {
    if (!initialized) { initialize(); }
    return sources;
  }


  protected void setCapability(String cap, String version) {
    capabilities.put(cap, version);
  }

  protected void setDasVersion(String version) {
    das_version = version;
  }

  protected void addDataSource(DasSource ds) {
    sources.put(ds.getID(), ds);
  }

  protected void setDescription(String desc) {

  }

  /**
   * Return true if successfully initialized.
   * see DAS specification for returned XML format in response to "dsn" command:
   *      http://biodas.org/documents/spec.html
   */
  public boolean initialize() {
    //TODO: think about whether this needs synchronization.
    //TODO: clean-up streams in finally block
    try {
      //      System.out.println("in DasUtils.findDasSource()");
      //      SynonymLookup lookup = SynonymLookup.getDefaultLookup();
      String request_str = root_url + "/dsn";
      System.out.println("Das Request: " + request_str);
      URL das_request = new URL(request_str);
      URLConnection request_con = das_request.openConnection();
      String das_version = request_con.getHeaderField("X-DAS-Version");
      String das_status = request_con.getHeaderField("X-DAS-Status");
      String das_capabilities = request_con.getHeaderField("X-DAS-Capabilities");

      setDasVersion(das_version);

      System.out.println("DAS server version: " + das_version + ", status: " + das_status);
      if (REPORT_CAPS)  { System.out.println("DAS capabilities: " + das_capabilities); }
      if (das_capabilities != null) {
	String[] cap_array = cap_splitter.split(das_capabilities);
	for (int i=0; i<cap_array.length; i++) {
	  String tagval = cap_array[i];
	  String[] name_version = name_version_splitter.split(tagval);
	  String cap_name = name_version[0];
	  String cap_version = name_version[1];
	  setCapability(cap_name, cap_version);
	  if (REPORT_CAPS) {
	    System.out.println("cap: " + cap_name + ", version: " + cap_version);
	  }
	}
      }
      Document doc = DasLoader.getDocument(request_con);

      Element top_element = doc.getDocumentElement();
      NodeList dsns = doc.getElementsByTagName("DSN");
      System.out.println("dsn count: " + dsns.getLength());
      for (int i=0; i< dsns.getLength(); i++)  {
        Element dsn = (Element)dsns.item(i);
        NodeList sourcelist = dsn.getElementsByTagName("SOURCE");
        Element source = (Element)sourcelist.item(0);
        String sourceid = source.getAttribute("id");
        String source_version = source.getAttribute("version");
        String sourcename = null;
        Text nametext = (Text)source.getFirstChild();
        if (nametext != null)  { sourcename = nametext.getData(); }
        NodeList masterlist = dsn.getElementsByTagName("MAPMASTER");
        String master_url = null;
        Element master = (Element)masterlist.item(0);
        Text mastertext = (Text)master.getFirstChild();
        if (mastertext != null)  { master_url = mastertext.getData(); }
        NodeList desclist = dsn.getElementsByTagName("DESCRIPTION");
        String info_url = null;
        String description = null;
        if (desclist.getLength() > 0)  {
          Element desc = (Element)desclist.item(0);
          info_url = desc.getAttribute("href");
          Text desctext = (Text)desc.getFirstChild();
          if (desctext != null)  { description = desctext.getData(); }
        }

	DasSource das_source =
	  new DasSource(this, sourceid, false);
	das_source.setVersion(source_version);
	das_source.setName(sourcename);
	das_source.setDescription(description);
	das_source.setInfoUrl(info_url);
	das_source.setMapMaster(master_url);
	this.addDataSource(das_source);

	if (REPORT_SOURCES) {
	  System.out.println("sourceid = " + sourceid +
			     ", version = " + source_version +
			     ", name = " + sourcename);
	  System.out.println("   mapmaster = " + master_url +
			     ", info_url = " + info_url +
			     ", description = " + description);
	}
      }
    }
    catch (Exception ex) {
      ErrorHandler.errorPanel("Error initializing DAS server info for\n"+root_url, ex);
    }
    initialized = true;
    return initialized;
  }

  /**
   *  For testing.
   */
  public static void main(String[] args) {
    String test_url = "http://205.217.46.81:9091/QueryServlet/das";

    SynonymLookup dlookup = new SynonymLookup();
    dlookup.loadSynonyms("http://147.208.165.250/quickload_data/synonyms.txt");
    
    SynonymLookup.setDefaultLookup(dlookup);
    DasServerInfo test = new DasServerInfo(test_url, "name unknown", true);
    System.out.println("***** DAS Server Info *****");
    System.out.println("  root URL: " + test.getRootUrl());
    System.out.println("  DAS version: " + test.getDasVersion());
    System.out.println("  description: " + test.getDescription());
    System.out.println("  capabilities: ");
    Iterator caps = test.getCapabilities().entrySet().iterator();
    while (caps.hasNext()) {
      Map.Entry cap = (Map.Entry) caps.next();
      System.out.println("     key = " + cap.getKey() + ", val = " +
                         cap.getValue());
    }
    Iterator sources = test.getDataSources().values().iterator();
    System.out.println("  data sources: ");
    while (sources.hasNext()) {
      DasSource source = (DasSource)sources.next();
      System.out.println("     id = " + source.getID() + ", version = " + source.getVersion() +
			 ", name = " + source.getName() + ", description = " + source.getDescription() +
			 ", info_url = " + source.getInfoUrl() + ", mapmaster = " + source.getMapMaster());
    }

    sources = test.getDataSources().values().iterator();
    DasSource first_source = (DasSource)sources.next();
    //    first_source.initialize();
    Map entryhash = first_source.getEntryPoints();
    Iterator entries = entryhash.values().iterator();
    while (entries.hasNext()) {
      DasEntryPoint entry_point = (DasEntryPoint)entries.next();
      System.out.println("entry point:  id = " + entry_point.getID());
      AnnotatedBioSeq seq = (AnnotatedBioSeq)entry_point.getAnnotatedSeq();
      System.out.println("seq: " + seq.getID() + ", length = " + seq.getLength());
    }
    Map typehash = first_source.getTypes();
    Iterator types = typehash.values().iterator();
    while (types.hasNext()) {
      DasType type = (DasType)types.next();
      System.out.println("type:  id = " + type.getID());
    }

    AnnotatedSeqGroup genome = first_source.getGenome();
    System.out.println("current genome: " + genome);
    
    Iterator iter = genome.getSeqs().values().iterator();
    while (iter.hasNext()) {
      AnnotatedBioSeq seq = (AnnotatedBioSeq)iter.next();
      System.out.println("seq: " + seq.getID() + ", length = " + seq.getLength());
    }

    System.out.println("**************************");
  }
}
