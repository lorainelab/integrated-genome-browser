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

  String id;
  String description;
  String info_url;
  String taxon;
  Map versions = new LinkedHashMap();

  Das2ServerInfo server;
  AnnotatedSeqGroup genome = null;
  //  Map entry_points = new LinkedHashMap();
  //  Map types = new LinkedHashMap();
  boolean versions_initialized = false;

  public Das2Source(Das2ServerInfo source_server, String source_id, boolean init) {
    id = source_id;
    server = source_server;
    if (init) {
      // initEntryPoints();
      // initTypes();
    }
  }

  public String getID() { return id; }
  public String getDescription() { return description; }
  public String getInfoUrl() { return info_url; }
  public String getTaxon() { return taxon; }

  public Das2ServerInfo getDas2ServerInfo() { return server; }

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
    if (! versions_initialized) {
      // initVersions();
    }
    return versions;
  }

  public void addVersion(Das2VersionedSource version) {
    versions.put(version.getID(), version);
  }

  /*
  public Map getEntryPoints() {
    if (! entries_initialized)  {
      initEntryPoints();
    }
    return entry_points;
  }
  public Map getTypes() {
    if (! types_initialized) {
      initTypes();
    }
    return types;
  }
  void addEntryPoint(DasEntryPoint entry_point) {
    entry_points.put(entry_point.getID(), entry_point);
  }
  void addType(DasType type) {
    types.put(type.getID(), type);
  }
  */


  /** Get entry points from das server. */
  /*
  protected void initEntryPoints() {
    String entry_request = getDas2ServerInfo().getRootUrl() + "/" + getID() + "/entry_points";
    try {
      System.out.println("Das Entry Request: " + entry_request);
      Document doc = DasLoader.getDocument(entry_request);
      Element top_element = doc.getDocumentElement();
      NodeList segments = doc.getElementsByTagName("SEGMENT");
      System.out.println("segments: " + segments.getLength());
      for (int i=0; i< segments.getLength(); i++)  {
	Element seg = (Element)segments.item(i);
        String segid = seg.getAttribute("id");
	String startstr = seg.getAttribute("start");
	String stopstr = seg.getAttribute("stop");
	String sizestr = seg.getAttribute("size");  // can optionally use "size" instead of "start" and "stop"
	String seqtype = seg.getAttribute("type");  // optional
	String orient = seg.getAttribute("orientation");  // optional if using "size" attribute
	String subpart_str = seg.getAttribute("subparts");

	String description = null;
        Text desctext = (Text)seg.getFirstChild();
	if (desctext != null) { description = desctext.getData(); }
	//	System.out.println("segment id: " + segid);
	int start = 1;
	int stop = 1;
	boolean forward = true;
	if (orient != null) {
	  forward = (! orient.equals("-"));  // anything other than "-" is considered forward
	}
	if (startstr != null && stopstr != null) {
	  start = Integer.parseInt(startstr);
	  stop = Integer.parseInt(stopstr);
	}
	else if (sizestr != null) {
	  stop = Integer.parseInt(sizestr);
	}
	boolean has_subparts = false;
	if (subpart_str != null) {
	  has_subparts = (subpart_str.equalsIgnoreCase("yes") || subpart_str.equalsIgnoreCase("true"));
	}
	//	DasEntryPoint entry_point =
	//	  new DasEntryPoint(this, segid, seqtype, desc, has_subparts, start, stop, forward);
	DasEntryPoint entry_point = new DasEntryPoint(this, segid);
	entry_point.setSeqType(seqtype);
	entry_point.setDescription(description);
	entry_point.setInterval(start, stop, forward);
	entry_point.setSubParts(has_subparts);
	this.addEntryPoint(entry_point);
      }
    }
    catch (Exception ex) {
      ErrorHandler.errorPanel("Error initializing DAS entry points for\n"+entry_request, ex);
    }
    //TODO should entries_initialized be true if an exception occured?
    entries_initialized = true;
  }
  */

  // get annotation types from das server
  /*
  protected void initTypes() {
    String types_request = getDas2ServerInfo().getRootUrl() + "/" + getID() + "/types";
    try {
      System.out.println("Das Types Request: " + types_request);
      Document doc = DasLoader.getDocument(types_request);
      Element top_element = doc.getDocumentElement();
      NodeList typelist = doc.getElementsByTagName("TYPE");
      System.out.println("types: " + typelist.getLength());
      for (int i=0; i< typelist.getLength(); i++)  {
	Element typenode = (Element)typelist.item(i);
        String typeid = typenode.getAttribute("id");
	String method = typenode.getAttribute("method");
	String category = typenode.getAttribute("category");

	String countstr = null;
	Text count_text = (Text)typenode.getFirstChild();
	if (count_text != null) { countstr = count_text.getData(); }

	//	System.out.println("type id: " + typeid);
	DasType type = new DasType(this, typeid);
	this.addType(type);
      }
    }
    catch (Exception ex) {
      ErrorHandler.errorPanel("Error initializing DAS types for\n"+types_request, ex);
    }
    //TODO should types_initialized be true after an exception?
    types_initialized = true;
  }
  */
}
