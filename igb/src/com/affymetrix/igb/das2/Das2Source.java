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

import java.io.*;
import java.net.*;
import java.util.*;
import org.xml.sax.*;
import org.w3c.dom.*;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonGenometryModel;

/**
 *
 * started with com.affymetrix.igb.das.DasSource and modified
 */
public class Das2Source {

  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  protected URI source_uri;
  protected String name;
  protected String description;
  protected String info_url;
  protected String taxon;
  protected Map versions = new LinkedHashMap();
  protected Map name2version = new LinkedHashMap();
  Das2VersionedSource latest_version = null;

  Das2ServerInfo server;
  AnnotatedSeqGroup genome = null;


  public Das2Source(Das2ServerInfo source_server, URI src_uri, String source_name,
		    String source_href, String source_taxid, String desc) {
    //    System.out.println("source: name = " + source_name + ", uri = " + src_uri);
    source_uri = src_uri;
    server = source_server;
    name = source_name;
    info_url = source_href;
    taxon = source_taxid;
    description = desc;
  }

  public URI getURI() { return source_uri; }
  public String getID() { return source_uri.toString(); }
  public String getName() { return name; }
  public String toString() { return getName(); }
  public String getDescription() { return description; }
  public String getInfoUrl() { return info_url; }
  public String getTaxon() { return taxon; }

  public Das2ServerInfo getServerInfo() { return server; }

  /** NOT YET IMPLEMENTED */
  public Das2VersionedSource getLatestVersion()  { return latest_version; }

  /**
   *  Equivalent to {@link SingletonGenometryModel#addSeqGroup(String)} with the
   *  id from {@link #getID()}.  Caches the result.
   */
  /*
  public AnnotatedSeqGroup getGenome() {
    if (genome == null) {
      genome = gmodel.addSeqGroup(source_uri.toString());
    }
    return genome;
  }
  */

  /** returns Map of version ID to Das2VersionedSource */
  public synchronized Map getVersions() {
    return versions;
  }

  /*
   *  Given an id string which should be the URI for a DAS/2 versioned source
   *     (but may optionally be the versioned source name)
   *  Return the Das2VersionedSource object for the DAS/2 versioned source
   *  Will return null if this Das2Source has no Das2VersionedSource with that id or name
   */
  public synchronized Das2VersionedSource getVersion(String id) {
    Das2VersionedSource version = (Das2VersionedSource)versions.get(id);
    if (version == null) { version = (Das2VersionedSource)name2version.get(id); }
    return version;
  }

  public synchronized void addVersion(Das2VersionedSource version) {
    versions.put(version.getID(), version);
    name2version.put(version.getName(), version);
  }


}
