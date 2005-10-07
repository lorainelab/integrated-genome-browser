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

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.util.DasUtils;
import com.affymetrix.igb.util.SynonymLookup;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.das.DasLoader;

/**
 *
 *  started with com.affymetrix.igb.das.DasSource and modified
 */
public class Das2VersionedSource  {
  static boolean DO_FILE_TEST = false;
  static String test_file = "file:/C:/data/das2_responses/alan_server/regions.xml";

  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  Das2Source source;
  String id;
  String description;
  String info_url;
  Date creation_date;
  Date modified_date;
  Map capabilities;
  Map namespaces;
  Map regions = new LinkedHashMap();
  Map properties;
  java.util.List assembly;

  AnnotatedSeqGroup genome = null;
  Map types = new LinkedHashMap();
  Map assays = new LinkedHashMap();
  Map materials = new LinkedHashMap();
  Map results = new LinkedHashMap();
  boolean regions_initialized = false;
  boolean types_initialized = false;
  boolean assays_initialized = false;
  boolean materials_initialized = false;
  boolean results_initialized = false;
  String types_filter = null;
  
  public Das2VersionedSource(Das2Source das_source, String version_id, boolean init) {
    id = version_id;
    source = das_source;
    if (init) {
      initRegions();
      initTypes(null);
    }
  }

  public String getID() { return id; }
  public String getDescription() { return description; }
  public String getInfoUrl() { return info_url; }
  public Date getCreationDate() { return creation_date; }
  public Date getLastModifiedDate() { return modified_date; }
  public Das2Source getSource() { return source; }

  /** NOT YET IMPLEMENTED */
  public List getAssembly()   { return assembly; }
  /** NOT YET IMPLEMENTED */
  public Map getProperties()  { return properties; }
  /** NOT YET IMPLEMENTED */
  public Map getNamespaces()  { return namespaces; }
  /** NOT YET IMPLEMENTED */
  public Map getCapabilities()  { return capabilities; }

  public AnnotatedSeqGroup getGenome() {
    if (genome == null) {
      genome = gmodel.addSeqGroup(id);  // gets existing seq group if possible, otherwise adds new one
    }
    return genome;
  }

  void setID(String id)  { this.id = id; }
  void setDescription(String desc) { this.description = desc; }
  void setInfoUrl(String url) { this.info_url = url; }


  public Map getRegions() {
    if (! regions_initialized)  {
      initRegions();
    }
    return regions;
  }

  /**
   *  assumes there is only one region for each seq
   *    may want to change this to return a list of regions instead
   **/
  public Das2Region getRegion(BioSeq seq) {
    // should probably make a region2seq hash, but for now can just iterate through regions
    Das2Region result = null;
    Iterator iter = getRegions().values().iterator();
    while (iter.hasNext()) {
      Das2Region region = (Das2Region)iter.next();
      BioSeq region_seq = region.getAnnotatedSeq();
      if (region_seq == seq) {
	result = region;
	break;
      }
    }
    return result;
  }

  public void addRegion(Das2Region region) {
    regions.put(region.getID(), region);
  }

  public Map getTypes() {
    if (! types_initialized || types_filter != null) {
      initTypes(null);
    }
    return types;
  }
  
  public Map getTypes(String filter) {
     if (! types_initialized || !filter.equals(types_filter)) {
       initTypes(filter);
    }
    return types;
  }

  public Map getAssays() {
      if(! assays_initialized) {
          initAssays();
      }
      return(assays);
  }
  
  public Map getResults() {
      if(! results_initialized) {
          initResults();
      }
      return(results);
  }
  
  public Map getMaterials() {
      if(! materials_initialized) {
          initMaterials();
      }
      return(materials);
  }

  public void clearTypes() {
      this.types = new LinkedHashMap();
  }
  
  public void clearAssays() {
      this.assays = new LinkedHashMap();
  }
  
  public void clearResults() {
      this.results = new LinkedHashMap();
  }
  
  public void clearMaterials() {
      this.materials = new LinkedHashMap();
  }  
  
  public void addType(Das2Type type) {
    types.put(type.getID(), type);
  }

  public void addAssay(Das2Assay assay) {
      assays.put(assay.getID(), assay);
  }
  
  public void addResult(Das2Result result) {
      results.put(result.getID(), result);
  }  
  
  public void addMaterial(Das2Material material) {
      materials.put(material.getID(), material);
  }


  /** Get regions from das server. */
  protected void initRegions() {
    String region_request;
    if (DO_FILE_TEST)  {
      region_request = test_file;
    }
    else {
      region_request = getSource().getServerInfo().getRootUrl() + "/" +
          this.getID() + "/region";
    }
    try {
      System.out.println("Das Region Request: " + region_request);
      Document doc = DasLoader.getDocument(region_request);
      Element top_element = doc.getDocumentElement();
      NodeList regionlist = doc.getElementsByTagName("REGION");
      System.out.println("regions: " + regionlist.getLength());
      for (int i=0; i< regionlist.getLength(); i++)  {
	Element reg = (Element)regionlist.item(i);
        String region_id = reg.getAttribute("id");
	String startstr = reg.getAttribute("start");
	String endstr = reg.getAttribute("end");
	String region_name = reg.getAttribute("name");
	String region_info_url = reg.getAttribute("doc_href");

	String description = null;
	int start = 0;
	int end = 1;
	if (startstr != null && endstr != null) {
	  start = Integer.parseInt(startstr);
	  end = Integer.parseInt(endstr);
	}
	/*
         System.out.println("  region id = " + region_id +
			   ", start = " + start + ", end = " + end +
			   ", name = " + region_name +
			   ", info url = " + region_info_url);
        */
	Das2Region region = new Das2Region(this, region_id, start, end, true);
	//	region.setInterval(start, end, true);
	this.addRegion(region);
      }
    }
    catch (Exception ex) {
      ErrorHandler.errorPanel("Error initializing DAS region points for\n"+region_request, ex);
    }
    //TODO should regions_initialized be true if an exception occured?
    regions_initialized = true;
  }

  protected void initMaterials() {
    this.clearMaterials();
    String materials_request = getSource().getServerInfo().getRootUrl() +
      "/" + this.getID() + "/material";
    try {
      System.out.println("Das Materials Request: " + materials_request);
      Document doc = DasLoader.getDocument(materials_request);
      Element top_element = doc.getDocumentElement();
      NodeList materialList = doc.getElementsByTagName("BioSource");
      System.out.println("materials: " + materialList.getLength());
      for (int i=0; i< materialList.getLength(); i++)  {
	Element materialnode = (Element)materialList.item(i);
        String materialid = materialnode.getAttribute("identifier");
        String name = materialnode.getAttribute("name");
        
        // types
	NodeList tlist = materialnode.getElementsByTagName("DatabaseEntry");
	HashMap types = new HashMap();
	for (int k=0; k<tlist.getLength(); k++) {
	  Element inode = (Element)tlist.item(k);
	  String uri = inode.getAttribute("URI");
          types.put(uri.substring(8), uri);
        }

        // contacts
        HashMap contacts = new HashMap();
	NodeList clist = materialnode.getElementsByTagName("Organization_ref");
	for (int k=0; k<clist.getLength(); k++) {
	  Element pnode = (Element)clist.item(k);
	  String id = pnode.getAttribute("identifier");
	  contacts.put(id.substring(11), id);
	}
        
	this.addMaterial(new Das2Material(this, materialid, name, types, contacts));
      }
    } catch (Exception ex) {
      ErrorHandler.errorPanel("Error initializing DAS materials for\n"+materials_request, ex);
    }
    //TODO should types_initialized be true after an exception?
    materials_initialized = true;
  }  

  protected void initResults() {
    this.clearResults();
    String results_request = getSource().getServerInfo().getRootUrl() +
      "/" + this.getID() + "/result";
    try {
      System.out.println("Das Results Request: " + results_request);
      Document doc = DasLoader.getDocument(results_request);
      Element top_element = doc.getDocumentElement();
      NodeList resultlist = doc.getElementsByTagName("RESULT");
      System.out.println("results: " + resultlist.getLength());
      for (int i=0; i< resultlist.getLength(); i++)  {
	Element resultnode = (Element)resultlist.item(i);
        String resultid = resultnode.getAttribute("id");
        String assayId = resultnode.getAttribute("assay");
        assayId = assayId.substring(9);
        String imageId = resultnode.getAttribute("image");
        imageId = imageId.substring(9);
        String protocolId = resultnode.getAttribute("protocol");
        protocolId = protocolId.substring(12);
        
	this.addResult(new Das2Result(this, resultid, assayId, imageId, protocolId));
      }
    } catch (Exception ex) {
      ErrorHandler.errorPanel("Error initializing DAS types for\n"+results_request, ex);
    }
    //TODO should types_initialized be true after an exception?
    results_initialized = true;
  }  
  
  protected void initAssays() {
    this.clearAssays();
    String assays_request = getSource().getServerInfo().getRootUrl() +
      "/" + this.getID() + "/assay";
    try {
      System.out.println("Das Assays Request: " + assays_request);
      Document doc = DasLoader.getDocument(assays_request);
      Element top_element = doc.getDocumentElement();
      NodeList assaylist = doc.getElementsByTagName("PhysicalBioAssay");
      System.out.println("assays: " + assaylist.getLength());
      for (int i=0; i< assaylist.getLength(); i++)  {
	Element assaynode = (Element)assaylist.item(i);
        String assayid = assaynode.getAttribute("identifier");

        // images
	NodeList ilist = assaynode.getElementsByTagName("Image");
	HashMap images = new HashMap();
	for (int k=0; k<ilist.getLength(); k++) {
	  Element inode = (Element)ilist.item(k);
	  String uri = inode.getAttribute("URI");
          images.put(uri.substring(9), uri);
	}

        // biomaterials
        HashMap biomat = new HashMap();
	NodeList bmlist = assaynode.getElementsByTagName("BioMaterial_ref");
	for (int k=0; k<bmlist.getLength(); k++) {
	  Element pnode = (Element)bmlist.item(k);
	  String id = pnode.getAttribute("identifier");
	  biomat.put(id.substring(12), id);
	}
        
        // array platform
        HashMap platform = new HashMap();
        NodeList plist = assaynode.getElementsByTagName("Array_ref");
        for (int l=0; l<plist.getLength(); l++) {
           Element pnode = (Element)plist.item(l);
           String id = pnode.getAttribute("identifier");
           platform.put(id.substring(12), id);
        }
        
	this.addAssay(new Das2Assay(this, assayid, images, biomat, platform));
      }
    } catch (Exception ex) {
      ErrorHandler.errorPanel("Error initializing DAS types for\n"+assays_request, ex);
    }
    //TODO should types_initialized be true after an exception?
    assays_initialized = true;
  }

  // get annotation types from das server
  protected void initTypes(String filter) {
    this.types_filter = filter;
    this.clearTypes();
    // how should xml:base be handled?
    String types_request = getSource().getServerInfo().getRootUrl() +
        "/" + this.getID() + "/type";
    if (filter != null) {
      types_request = types_request+"?ontology="+filter;
    } 
    //    String types_request = "file:/C:/data/das2_responses/alan_server/types_short.xml";
    try {
      System.out.println("Das Types Request: " + types_request);
      Document doc = DasLoader.getDocument(types_request);
      Element top_element = doc.getDocumentElement();
      NodeList typelist = doc.getElementsByTagName("TYPE");
      System.out.println("types: " + typelist.getLength());
      for (int i=0; i< typelist.getLength(); i++)  {
	Element typenode = (Element)typelist.item(i);
        String typeid = typenode.getAttribute("id");
	// temporary workaround for getting type ending, rather thatn full URI
	if (typeid.startsWith("./")) { typeid = typeid.substring(2); }
	String ontid = typenode.getAttribute("ontology");
	String type_source = typenode.getAttribute("source");
	String href = typenode.getAttribute("doc_href");
	NodeList flist = typenode.getElementsByTagName("FORMAT");
	LinkedHashMap formats = new LinkedHashMap();
	HashMap props = new HashMap();
	for (int k=0; k<flist.getLength(); k++) {
	  Element fnode = (Element)flist.item(k);
	  String formatid = fnode.getAttribute("id");
	  String mimetype = fnode.getAttribute("mimetype");
	  if (mimetype == null || mimetype.equals("")) { mimetype = "unknown"; }
          //	  System.out.println("alternative format for annot type " + typeid +
          //": format = " + formatid + ", mimetype = " + mimetype);
          formats.put(formatid, mimetype);
	}

	NodeList plist = typenode.getElementsByTagName("PROP");
	for (int k=0; k<plist.getLength(); k++) {
	  Element pnode = (Element)plist.item(k);
	  String key = pnode.getAttribute("key");
	  String val = pnode.getAttribute("value");
	  props.put(key, val);
	}

        HashMap parents = new HashMap();
        NodeList parentsList = typenode.getElementsByTagName("PARENT");
        for (int l=0; l<parentsList.getLength(); l++) {
           Element pnode = (Element)parentsList.item(l);
           String key = pnode.getAttribute("id");
           String val = pnode.getAttribute("id");
           parents.put(key, val);
        }


	Das2Type type = new Das2Type(this, typeid, ontid, type_source, href, formats, props, parents);
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
