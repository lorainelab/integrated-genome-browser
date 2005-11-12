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

import com.affymetrix.igb.IGB;
import com.affymetrix.igb.util.DasUtils;
import com.affymetrix.igb.util.SynonymLookup;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.igb.util.ErrorHandler;

/**
 *
 * started with com.affymetrix.igb.das.DasSource and modified
 */
public class Das2Source {

  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  protected String id;
  protected String description;
  protected String info_url;
  protected String taxon;
  protected Map versions = new LinkedHashMap();
  Das2VersionedSource latest_version = null;

  Das2ServerInfo server;
  AnnotatedSeqGroup genome = null;
  //  boolean versions_initialized = false;

  public Das2Source(Das2ServerInfo source_server, String source_id, boolean init) {
    id = source_id;
    server = source_server;
    //    if (init) {
    // initVersions();
    //    }
  }

  public String getID() { return id; }
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
  public AnnotatedSeqGroup getGenome() {
    if (genome == null) {
      genome = gmodel.addSeqGroup(id);
    }
    return genome;
  }

  void setID(String id)  { this.id = id; }
  void setDescription(String desc) { this.description = desc; }
  void setInfoUrl(String url) { this.info_url = url; }
  void setTaxon(String taxon)  { this.taxon = taxon; }

  public Map getVersions() {
    //    if (! versions_initialized) {
    // initVersions();
    //    }
    return versions;
  }

  public void addVersion(Das2VersionedSource version) {
    versions.put(version.getID(), version);
  }


}
