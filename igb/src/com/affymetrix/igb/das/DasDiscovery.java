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
import java.util.*;
import java.util.regex.*;

import com.affymetrix.igb.util.LocalUrlCacher;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import java.util.prefs.*;

public class DasDiscovery {

  static Map das_servers;
  static Pattern tab_splitter = Pattern.compile("\t");

  static int DAS_NAME = 0;
  static int DAS_URI = 1;
  static int INFO_URI = 2;

  public final static String KEY_NAME = "name";
  public final static String KEY_URL = "url";
  public final static String KEY_ENABLED = "enabled";

  /**
   *  Gets a Map of DAS servers.
   *  Map is from Strings (server names) to DasServerInfo's.
   *  The first time this routine is called, data will be derived from
   *  Java's persistent preferences.
   *  Subsequent calls will return the cached Map.
   */
  public static Map getDasServers() {

    if (das_servers == null) {
      das_servers = new LinkedHashMap();
      addServersFromPreferences();
    }

    return das_servers;
  }
  
  /** Forget about all known DAS servers. Next call to getDasServers will 
   *  re-generate a list based on persistent Preferences.
   */
  public static void reset() {
    // note: there is no need-to re-get the list from the QuickLoad server, since
    // everything added that way, or any other way, will have become persistent.
    das_servers = null;
  }

  /**
   *  Reads a tab-formatted list of DAS servers and adds the servers to
   *  the persistent list of known servers.
   */
  public static void addServersFromTabFile(String server_loc_list) throws IOException {
    // System.out.println("------------ Adding servers from tab format file :"+server_loc_list);
    InputStream is = null;
    BufferedReader br = null;
    try {
      is = LocalUrlCacher.getInputStream(server_loc_list);
      if (is == null) {
        System.out.println("DasDiscovery: could not open this file: "+server_loc_list);
        return;
      }
      br = new BufferedReader(new InputStreamReader(is));
      String line;
      while ((line = br.readLine()) != null) {
        if (line.startsWith("#")) { continue; }
        String[] server_info = tab_splitter.split(line);
        String server_name = server_info[DAS_NAME];
        String server_url = server_info[DAS_URI];
        addDasServer(server_name, server_url);
      }
    }
    finally {
      try {br.close();} catch (Exception ioe) {}
      try {is.close();} catch (Exception ioe) {}
    }
  }
  
  public static Preferences getPreferencesNode() {
    return UnibrowPrefsUtil.getTopNode().node("DAS Servers");
  }
  
  static void addServersFromPreferences() {
    Preferences top_node = getPreferencesNode();

    try {
      String[] node_names = top_node.childrenNames();
      for (int i = 0 ; i < node_names.length ; i++) {
        Preferences kid = top_node.node(node_names[i]);
        String name = kid.get(KEY_NAME, "<unnamed DAS Server>");
        String url = kid.get(KEY_URL, null);
        boolean enabled = kid.getBoolean(KEY_ENABLED, true);
        if (url == null) {
          System.out.println("Found null URL in DAS preferences !! "+name);
          kid.removeNode();
        }
        else {
          //System.out.println("------------ Adding DAS server:  "+name);
          addDasServer(name, url);
        }
      }
    } catch (BackingStoreException bse) {
      UnibrowPrefsUtil.handleBSE(null, bse);
    }
  }
  
  
  /**
   *  Adds a DAS server to the list that will be returned by {@link #getDasServers()}.
   *  If this is called before the first time {@link #getDasServers()} is
   *  called, it forces a call to that method to load the default servers.
   */
  public static void addDasServer(String name, String url)  {
    if (das_servers == null) { 
      // if this routine is ever called before getDasServers(), then
      // force a call to getDasServers() here to load the default values
      // before adding the new one.
      das_servers = getDasServers(); 
    }
    if (url == null || name == null) {
      throw new IllegalArgumentException();
    }
    Preferences p = getNodeForURL(url, true);
    String preferred_name = p.get(KEY_NAME, null);
    if (preferred_name == null) {
      // Store the name for this DAS URL only if it doesn't already have a name
      p.put(KEY_NAME, name);
      preferred_name = name;
    }
    if (p.getBoolean(KEY_ENABLED, true)) {
      DasServerInfo server = new DasServerInfo(url, preferred_name, false);
      das_servers.put(preferred_name, server);
    }
  }
  
  /**
   *  Remove a given Das URL, both from the in-memory list and the persistent
   *  storage.
   */
  public void removeDasServer(String url) {
    if (url == null) {
      throw new IllegalArgumentException();
    }
    Preferences p = getNodeForURL(url, false);
    if (p != null) try {
      p.removeNode();
    } catch (BackingStoreException bse) {
      UnibrowPrefsUtil.handleBSE(null, bse);
    }
    if (das_servers.containsValue(url)) {
      Iterator iter = das_servers.keySet().iterator();
      while (iter.hasNext()) {
        String key = (String) iter.next();
        if (das_servers.get(key).equals(url)) {
          das_servers.remove(key);
          break;
        }
      }
    }
  }
  
  // Gets a node for the url, possibly making it if it didn't exist already.
  public static Preferences getNodeForURL(String url, boolean create) {
    Preferences p = null;
    try {
      String[] node_names = getPreferencesNode().childrenNames();
      //String code = getCodeForURL(url);
      for (int i=0; i<node_names.length; i++) {
        Preferences node = getPreferencesNode().node(node_names[i]);
        if (node.get(DasDiscovery.KEY_URL, "").equals(url)) {
          p = node;
          break;
        }
      }
    } catch (BackingStoreException bse) {
      // if couldn't get list of nodes, don't worry too much, just create a new node
      UnibrowPrefsUtil.handleBSE(null, bse);
    }
    if ((p == null) && create) {
      String name = "0";
      try {
        name = getUnusedNodeName();
      } catch (BackingStoreException bse) {
        UnibrowPrefsUtil.handleBSE(null, bse);
        // if no ununsed name could be found due to an Exception, use a random number
        // in the rare case that it conflicts with an existing number, it probably
        // won't be persisted anyway.
        name = Integer.toString((new Random()).nextInt());
      }
      p = getPreferencesNode().node(name);
      p.put(KEY_URL, url);
    }
    return p;
  }
  
  // the node names are meaningless, so just pick some integer that hasn't
  // been used yet
  static String getUnusedNodeName() throws BackingStoreException {
    int i=1000;
    String s = Integer.toString(i);
    while (getPreferencesNode().nodeExists(s)) {
      s = Integer.toString( ++i );
    }
    return s;
  }
}
