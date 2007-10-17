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

package com.affymetrix.igb.das2;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.igb.util.*;

public class Das2Discovery {

    static Pattern tab_splitter = Pattern.compile("\t");
    static int DAS_NAME = 0;
    static int DAS_URI = 1;
    static int INFO_URI = 2;

  //  static public String DEFAULT_DAS2_SERVER_NAME = "localhost";
  static public String DEFAULT_DAS2_SERVER_NAME = "NetAffx";
  static Map name2url = new LinkedHashMap();
  static Map name2server = new LinkedHashMap();
  static Map url2server = new LinkedHashMap();
  static Map cap2version = new LinkedHashMap();

  static {
    name2url.put("NetAffx", "http://netaffxdas.affymetrix.com/das2/sources");
    name2url.put("localhost", "http://localhost:9092/das2/genome");
    //    name2url.put("biopackages", "http://das.biopackages.net/das/genome");
    //    name2url.put("Sanger registry", "http://www.spice-3d.org/dasregistry/das2/sources");
    //    name2url.put("HapMap-test", "http://brie5.cshl.edu:9191/hapmap/das2/sources");

    //    name2url.put("File based", "file:///C:/Documents%20and%20Settings/Ed%20Erwin/My%20Documents/genoviz/igb/test/test_files/sources.xml");
    //    name2url.put("NetAffx", "http://netaffxdas.affymetrix.com/das2/sequence");
    //    name2url.put("riva",  "http://riva.ev.affymetrix.com:9092/das2/genome");
    //    name2url.put("bad test", "http://this.is.a.test/hmmm");
    initServers();
  }

  /**
   *  Gets a Map of DAS servers.
   *  Map is from Strings (server names) to Das2ServerInfo's.
   */
  public static Map getDas2Servers() {
    return name2server;
  }

  public static Map getCapabilityMap() { return cap2version; }

  public static void addServersFromTabFile(String server_loc_url) {
    // System.out.println("------------ Adding servers from tab format file :"+server_loc_list);
    /*
    InputStream is = null;
    BufferedReader br = null;
    try {
      is = LocalUrlCacher.getInputStream(server_loc_url);
      if (is == null) {
        System.out.println("DasDiscovery: could not open this file: "+server_loc_url);
        return;
      }
      br = new BufferedReader(new InputStreamReader(is));
      String line;
      while ((line = br.readLine()) != null) {
        if (line.startsWith("#")) { continue; }
        String[] server_info = tab_splitter.split(line);
        String server_name = server_info[DAS_NAME];
        String server_url = server_info[DAS_URI];
        addDas2Server(server_name, server_url);
      }
    }
    catch (Exception ex) { ex.printStackTrace(); }
    finally {
      try {br.close();} catch (Exception ioe) {}
      try {is.close();} catch (Exception ioe) {}
    }
    */
  }

  /**
   *  Given an id string which should be the resolvable root URL for a DAS/2 server
   *     (but may optionally be the server name)
   *  Return the Das2ServerInfo object for the DAS/2 server
   */
  public static Das2ServerInfo getDas2Server(String id) {
    Map servers = getDas2Servers();
    Das2ServerInfo server = (Das2ServerInfo)url2server.get(id);
    if (server == null) { server = (Das2ServerInfo)name2server.get(id); }
    return server;
  }

  protected static void initServers() {
    Iterator names = name2url.keySet().iterator();
    while (names.hasNext()) {
      String name = (String)names.next();
      String url = (String)name2url.get(name);
      initServer(url, name);
    }
  }

  public static Das2ServerInfo addDas2Server(String name, String url)  {
    if (name2url.get(name) == null) {
      return initServer(url, name);
    }
    else {
      return null;
    }
  }


  protected static Das2ServerInfo initServer(String url, String name) {
    Das2ServerInfo server = null;
    try {
      server = new Das2ServerInfo(url, name, false);
      name2server.put(name, server);
      url2server.put(url, server);
    } catch (Exception e) {
      System.out.println("WARNING: Could not initialize DAS/2 server with address: " + url);
      e.printStackTrace(System.out);
    }
    return server;
  }

  /**
   *  Given a capability URI string, try to find a Das2VersionedSource that uses the input URI as a capability URI
   *  This is useful since versioned source is not directly derivable from a DAS/2 query...
   */
//  public static Das2VersionedSource getVersionedSource(String capability_uri, boolean try_unloaded_servers) {
//
//  }

  /**
   *  Given an AnnotatedSeqGroup, return a list of Das2VersionedSources that
   *    provide annotations for the group [ versioned_source.getGenome() = group ]
   *  if (try_unloaded_servers) then force retrieval of versioned sources info for
   *       all known servers, otherwise only check versioned sources whose info is already loaded
   */
  public static List getVersionedSources(AnnotatedSeqGroup group, boolean try_unloaded_servers) {
    return getVersionedSources(group, try_unloaded_servers, null);
  }

  /**
   *  Given an AnnotatedSeqGroup, return a list of Das2VersionedSources that
   *    provide annotations for the group [ versioned_source.getGenome() = group ]
   *  if (try_unloaded_servers) then force retrieval of versioned sources info for
   *       all known server, otherwise only check versioned sources whose info is already loaded
   *  CAPABILITY arg specified a capability that the versioned source must have to be included in the returned list
   *       if CAPABILITY is null, then no capability filter is applied
   */
  public static List getVersionedSources(AnnotatedSeqGroup group, boolean try_unloaded_servers, String capability) {

    List matches = new ArrayList();
    if (group != null) {
      Iterator servers = getDas2Servers().values().iterator();
      while (servers.hasNext()) {
	Das2ServerInfo server = (Das2ServerInfo)servers.next();
	//      System.out.println("  server: " + server.getName());
	boolean init = server.isInitialized();
	if ((! init) && try_unloaded_servers) {
	  server.initialize();
	  init = server.isInitialized();
	}
	if (init) {
	  Iterator sources = server.getSources().values().iterator();
	  while (sources.hasNext()) {
	    Das2Source source = (Das2Source)sources.next();
	    Iterator versioned_sources = source.getVersions().values().iterator();
	    while (versioned_sources.hasNext()) {
	      Das2VersionedSource version = (Das2VersionedSource)versioned_sources.next();
	      //	    System.out.println("     version: " + version.getName());
	      if (version.getGenome() == group) {
		if ((capability == null) || (version.getCapability(capability) != null)) {
		  matches.add(version);
		}
	      }
	    }
	  }
	}
      }
    }
    return matches;
  }


  /**
   *  Given an AnnotatedSeqGroup, return a list of Das2Sources that have at least one Das2VersionedSource that
   *    provides annotations for the group
   *  if (try_unloaded_servers) then force search of
   *       all known servers, otherwise only check info that is already loaded??
   */
  public static List getSources(AnnotatedSeqGroup group, boolean try_unloaded_servers) {
    List vsources = getVersionedSources(group, try_unloaded_servers);
    Set sourceset = new LinkedHashSet(vsources.size());
    Iterator iter = vsources.iterator();
    while (iter.hasNext()) {
      Das2VersionedSource version = (Das2VersionedSource)iter.next();
      Das2Source source = version.getSource();
      sourceset.add(source);
    }
    List results = new ArrayList(sourceset);
    return results;
  }

  /**
   *  Given an AnnotatedSeqGroup, return a list of Das2ServerInfos that have at least one Das2Source that has
   *    at least one Das2VersionedSource that
   *    provides annotations for the group
   *  if (try_unloaded_servers) then force search of
   *       all known servers, otherwise only check info that is already loaded??
   */
  public static List getServers(AnnotatedSeqGroup group, boolean try_unloaded_servers) {
    List vsources = getVersionedSources(group, try_unloaded_servers);
    Set serverset = new LinkedHashSet(vsources.size());
    Iterator iter = vsources.iterator();
    while (iter.hasNext()) {
      Das2VersionedSource version = (Das2VersionedSource)iter.next();
      Das2ServerInfo server = version.getSource().getServerInfo();
      serverset.add(server);
    }
    List results = new ArrayList(serverset);
    return results;
  }


  /** NOT YET IMPLEMENTED
  public void removeDas2Server(String url) { }
  */


}
