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

//import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
//import com.affymetrix.igb.util.*;

public final class Das2Discovery {

    static Pattern tab_splitter = Pattern.compile("\t");
    static int DAS_NAME = 0;
    static int DAS_URI = 1;
    static int INFO_URI = 2;

  //  static public String DEFAULT_DAS2_SERVER_NAME = "localhost";
  static public String DEFAULT_DAS2_SERVER_NAME = "NetAffx";
  static Map<String,String> name2url = new LinkedHashMap<String,String>();
  static Map<String,Das2ServerInfo> name2server = new LinkedHashMap<String,Das2ServerInfo>();
  static Map<String,Das2ServerInfo> url2server = new LinkedHashMap<String,Das2ServerInfo>();
  static Map<String,Das2VersionedSource> cap2version = new LinkedHashMap<String,Das2VersionedSource>();

  static {
    //name2url.put("NetAffx", "http://netaffxdas.affymetrix.com/das2/genome");
    //    name2url.put("localhost", "http://localhost:9092/das2/genome");
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
   *  Gets a Map of DAS2 servers.
   *  Map is from Strings (server names) to Das2ServerInfo's.
   */
  public static Map<String,Das2ServerInfo> getDas2Servers() {
    return name2server;
  }

  public static Map<String,String> getDas2Urls() {
    return name2url;
  }

  public static Map<String,Das2VersionedSource> getCapabilityMap() { return cap2version; }

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
    Das2ServerInfo server = url2server.get(id);
    if (server == null) { server = name2server.get(id); }
    return server;
  }

  protected static void initServers() {
      for (String name : name2url.keySet()) {
          String url = name2url.get(name);
          initServer(url, name);
    }
  }

  public static Das2ServerInfo addDas2Server(String name, String url)  {
      if (name2url.get(name) == null) {
          name2url.put(url, name);
          return initServer(url, name);
      } else {
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
  public static List<Das2VersionedSource> getVersionedSources(AnnotatedSeqGroup group, boolean try_unloaded_servers) {
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
  public static List<Das2VersionedSource> getVersionedSources(AnnotatedSeqGroup group, boolean try_unloaded_servers, String capability) {
        List<Das2VersionedSource> matches = new ArrayList<Das2VersionedSource>();
        if (group == null) {
            return matches;
        }

        for (Das2ServerInfo server : getDas2Servers().values()) {
            boolean init = server.isInitialized();
            if ((!init) && try_unloaded_servers) {
                server.initialize();
                init = server.isInitialized();
            }
            if (!init) {
                continue;
            }

            for (Das2Source source : server.getSources().values()) {
                for (Das2VersionedSource version : source.getVersions().values()) {
                    if (version.getGenome() == group) {
                        if ((capability == null) || (version.getCapability(capability) != null)) {
                            matches.add(version);
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
  public static List<Das2Source> getSources(AnnotatedSeqGroup group, boolean try_unloaded_servers) {
    List<Das2VersionedSource> vsources = getVersionedSources(group, try_unloaded_servers);
    Set<Das2Source> sourceset = new LinkedHashSet<Das2Source>(vsources.size());
    for (Das2VersionedSource version : vsources) {
      Das2Source source = version.getSource();
      sourceset.add(source);
    }
    List<Das2Source> results = new ArrayList<Das2Source>(sourceset);
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
    List<Das2VersionedSource> vsources = getVersionedSources(group, try_unloaded_servers);
    Set<Das2ServerInfo> serverset = new LinkedHashSet<Das2ServerInfo>(vsources.size());
    for (Das2VersionedSource version : vsources) {
      Das2ServerInfo server = version.getSource().getServerInfo();
      serverset.add(server);
    }
    List<Das2ServerInfo> results = new ArrayList<Das2ServerInfo>(serverset);
    return results;
  }


  /** NOT YET IMPLEMENTED
  public void removeDas2Server(String url) { }
  */


}
