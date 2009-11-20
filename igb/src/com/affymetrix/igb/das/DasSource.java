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

//import java.io.*;
//import java.net.*;
import java.util.*;
//import org.xml.sax.*;
import org.w3c.dom.*;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.util.XMLUtils;

public final class DasSource {

  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  String id;
  String version;
  String name;
  String description;
  String info_url;
  String mapmaster;
  DasServerInfo server;
  AnnotatedSeqGroup genome = null;
  Map<String,DasEntryPoint> entry_points = new LinkedHashMap<String,DasEntryPoint>();
  Map<String,DasType> types = new LinkedHashMap<String,DasType>();
  boolean entries_initialized = false;
  boolean types_initialized = false;

  public DasSource(DasServerInfo source_server, String source_id, boolean init) {
    id = source_id;
    server = source_server;
    if (init) {
      initEntryPoints();
      initTypes();
    }
  }

  public String getID() { return id; }
  public String getVersion() { return version; }
  public String getName() { return name; }
  public String getDescription() { return description; }
  public String getInfoUrl() { return info_url; }

  public String getMapMaster() { return mapmaster; }  // or should this return URL?
  public DasServerInfo getDasServerInfo() { return server; }

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

  public synchronized Map<String,DasEntryPoint> getEntryPoints() {
    if (! entries_initialized)  {
      initEntryPoints();
    }
    return entry_points;
  }

  public synchronized Map<String,DasType> getTypes() {
    if (! types_initialized) {
      initTypes();
    }
    return types;
  }

  void setVersion(String version) { this.version = version; }
  void setName(String name) { this.name = name; }
  void setDescription(String desc) { this.description = desc; }
  void setInfoUrl(String url) { this.info_url = url; }
  void setMapMaster(String master) { this.mapmaster = master; }

  void addEntryPoint(DasEntryPoint entry_point) {
    entry_points.put(entry_point.getID(), entry_point);
  }

  void addType(DasType type) {
    types.put(type.getID(), type);
  }

  /** Get entry points from das server. */
  protected synchronized void initEntryPoints() {
    String entry_request;
	if (mapmaster != null && !mapmaster.isEmpty()) {
		entry_request = mapmaster + "/entry_points";
	} else {
		entry_request = getDasServerInfo().getRootUrl() + "/" + getID() + "/entry_points";
	}
	
    try {
      System.out.println("Das Entry Request: " + entry_request);
      Document doc = XMLUtils.getDocument(entry_request);
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
	if (startstr != null && startstr.length() > 0
            && stopstr != null && stopstr.length() > 0) {
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
    //TODO should entries_initialized be true if an exception occurred?
    entries_initialized = true;
  }

  // get annotation types from das server
  protected synchronized void initTypes() {
    String types_request = getDasServerInfo().getRootUrl() + "/" + getID() + "/types";
    try {
      System.out.println("Das Types Request: " + types_request);
      Document doc = XMLUtils.getDocument(types_request);
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
	DasType type = new DasType(this, typeid, method, category);
	this.addType(type);
      }
    }
    catch (Exception ex) {
      ErrorHandler.errorPanel("Error initializing DAS types for\n"+types_request, ex);
    }
    //TODO should types_initialized be true after an exception?
    types_initialized = true;
  }
}
