/**
*   Copyright (c) 2001-2005 Affymetrix, Inc.
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

import java.util.*;

public class Das2Discovery {

  static Map name2server = new LinkedHashMap();
  static Map name2url = new LinkedHashMap();
  static boolean servers_initialized = false;

  static {
    name2url.put("das.biopackages.net", "http://das.biopackages.net/das/genome");
    name2url.put("das.ev.affymetrix.com", "http://das.ev.affymetrix.com/das/genome");
  }

  /**
   *  Gets a Map of DAS servers.
   *  Map is from Strings (server names) to DasServerInfo's.
   */
  public static Map getDas2Servers() {
    if (! servers_initialized)  {
      initServers();
    }
    return name2server;
  }

  protected static void initServers() {
    Iterator names = name2url.keySet().iterator();
    while (names.hasNext()) {
      String name = (String)names.next();
      String url = (String)name2url.get(name);
      Das2ServerInfo server = new Das2ServerInfo(url, name, false);
      name2server.put(name, server);
    }
    servers_initialized = true;
  }

  /** NOT YET IMPLEMENTED
  public static void addDas2Server(String name, String url)  { }
  */

  /** NOT YET IMPLEMENTED
  public void removeDas2Server(String url) { }
  */


}
