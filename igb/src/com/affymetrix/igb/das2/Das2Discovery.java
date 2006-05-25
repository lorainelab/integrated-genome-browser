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
    name2url.put("biopackage", "http://das.biopackages.net/codesprint/sequence");
    //    name2url.put("localhost", "http://localhost:9092/das2/sequence");
    //name2url.put("Affy-test", "http://205.217.46.81:9091/das2/sequence");  
    //name2url.put("Affy-test", "http://unibrow.dmz2.ev.affymetrix.com:9091/das2/sequence");  
    name2url.put("NetAffx", "http://netaffxdas.affymetrix.com/das2/sequence");
    name2url.put("Sanger registry", "http://www.spice-3d.org/dasregistry/das2/sources");
    //    name2url.put("das.biopackages.net", "http://das.biopackages.net/das");
    //    name2url.put("riva",  "http://riva.ev.affymetrix.com:9092/das2/genome");
    //    name2url.put("bad test", "http://this.is.a.test/hmmm");
  }

  /**
   *  Gets a Map of DAS servers.
   *  Map is from Strings (server names) to Das2ServerInfo's.
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
