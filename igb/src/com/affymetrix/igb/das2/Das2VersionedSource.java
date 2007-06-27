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
import java.lang.Object.*;
import java.net.URI.*;
import java.util.regex.*;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.das.DasLoader;
import com.affymetrix.igb.parsers.Das2FeatureSaxParser;

/**
 *
 *  started with com.affymetrix.igb.das.DasSource and modified
 */
public class Das2VersionedSource  {
  public static String SEGMENTS_CAP_QUERY = "segments";
  public static String TYPES_CAP_QUERY = "types";
  public static String FEATURES_CAP_QUERY = "features";

  static boolean DEBUG_TYPES_QUERY = false;
  static boolean DEBUG_SEGMENTS_QUERY = false;

  static String ID = Das2FeatureSaxParser.ID;
  static String URID = Das2FeatureSaxParser.URID;
  static String SEGMENT = Das2FeatureSaxParser.SEGMENT;
  static String NAME = Das2FeatureSaxParser.NAME;
  static String TITLE = Das2FeatureSaxParser.TITLE;


  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  URI version_uri;
  URI coords_uri;
  Das2Source source;
  String name;
  String description;
  String info_url;
  Date creation_date;
  Date modified_date;
  Map capabilities = new HashMap();
  Map namespaces;
  Map regions = new LinkedHashMap();
  Map properties;
  java.util.List assembly;

  AnnotatedSeqGroup genome = null;
  protected Map types = new LinkedHashMap();
  protected Map name2types = new LinkedHashMap();
  protected boolean regions_initialized = false;
  protected boolean types_initialized = false;
  protected String types_filter = null;

  LinkedList platforms = new LinkedList();

  /**
   *  To maintain backward compatibility, keeping constuctor with no coords_uri argument,
   *   but it just acts as a pass-through to the constructor that takes a coords_uri argument
   */
  public Das2VersionedSource(Das2Source das_source, URI vers_uri, String name,
			     String href, String description, boolean init) {
    this(das_source, vers_uri, null, name, href, description, init);
  }

  public Das2VersionedSource(Das2Source das_source, URI vers_uri, URI coords_uri, String name,
			     String href, String description, boolean init) {
    this.name = name;
    this.coords_uri = coords_uri;
    version_uri = vers_uri;
    source = das_source;
    if (init) {
      initSegments();
      initTypes(null, false);
    }
  }

  public URI getURI() { return version_uri; }
  public String getID() { return version_uri.toString(); }
  public String getName() { return name; }
  public String toString() { return getName(); }
  public String getDescription() { return description; }
  public String getInfoUrl() { return info_url; }
  public Date getCreationDate() { return creation_date; }
  public Date getLastModifiedDate() { return modified_date; }
  public Das2Source getSource() { return source; }
  public URI getCoordinatesURI() { return coords_uri; }

  /** NOT YET IMPLEMENTED */
  //  public List getAssembly()   { return assembly; }
  /** NOT YET IMPLEMENTED */
  //  public Map getProperties()  { return properties; }
  /** NOT YET IMPLEMENTED */
  //  public Map getNamespaces()  { return namespaces; }
  /** NOT YET IMPLEMENTED */
  //  public Map getCapabilities()  { return capabilities; }

  public void addCapability(Das2Capability cap)  {
    capabilities.put(cap.getType(), cap);
  }

  public Das2Capability getCapability(String type) {
    return (Das2Capability)capabilities.get(type);
  }

  public AnnotatedSeqGroup getGenome() {
    if (genome == null) {
      // trying to use name for groupd id first, if no name then use full URI
      // This won't work in every situation!  Really need to resolve issues between VersionedSource URI ids and group ids
      String groupid = this.getName();
      if (groupid == null) { groupid = this.getID(); }
      //      genome = gmodel.addSeqGroup(groupid);  // gets existing seq group if possible, otherwise adds new one
      genome = gmodel.getSeqGroup(groupid);  // gets existing seq group if possible
      if (genome == null && coords_uri != null) { // try coordinates
	//	System.out.println("tring to match up coordinates: " + coords_uri);
	genome = gmodel.getSeqGroup(coords_uri.toString());
	//	if (genome != null)  { System.out.println("  found match: " + genome.getID()); }
      }
      if (genome == null) {
	// add new seq group -- if has global coordinates uri, then use that
	//   otherwise, use groupid (version source name or URI)
	if (coords_uri == null) {
	  System.out.println("@@@@  Adding genome: " + groupid);
	  //	  genome = gmodel.addSeqGroup(groupid); 
	  genome = new Das2SeqGroup(this, groupid);
	}
	else {
	  System.out.println("@@@@  Adding genome: " + coords_uri);
	  // genome = gmodel.addSeqGroup(coords_uri.toString());
	  genome = new Das2SeqGroup(this, coords_uri.toString());
	}
	gmodel.addSeqGroup(genome);
      }
    }
    return genome;
  }

  //  void setID(String id)  { this.id = id; }
  void setDescription(String desc) { this.description = desc; }
  void setInfoUrl(String url) { this.info_url = url; }

  public synchronized Map getSegments() {
    if (! regions_initialized)  {
      initSegments();
    }
    return regions;
  }

  /**
   *  assumes there is only one region for each seq
   *    may want to change this to return a list of regions instead
   **/
  public Das2Region getSegment(BioSeq seq) {
    // should probably make a region2seq hash, but for now can just iterate through regions
    Das2Region result = null;
    Iterator iter = getSegments().values().iterator();
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

  public synchronized void addRegion(Das2Region region) {
    regions.put(region.getID(), region);
  }

  public synchronized void addType(Das2Type type) {
    types.put(type.getID(), type);
    String name = type.getName();
    List prevlist = (List)name2types.get(name);
    if (prevlist == null) {
      prevlist = new ArrayList();
      name2types.put(name, prevlist);
    }
    prevlist.add(type);
  }

  public synchronized Map getTypes() {
    if (! types_initialized || types_filter != null) {
      initTypes(null, false);
    }
    return types;
  }

  public List getTypesByName(String name) {
    if (! types_initialized || types_filter != null) {
      initTypes(null, false);
    }
    return (List)name2types.get(name);
  }

  /*
  public Map getTypes(String filter) {
     if (! types_initialized || !filter.equals(types_filter)) {
       initTypes(filter, true);
    }
    return types;
  }
  */

  public void clearTypes() {
      this.types = new LinkedHashMap();
  }

  /** Get regions from das server. */
  protected synchronized void initSegments() {
    String region_request;
    Das2Capability segcap = (Das2Capability)getCapability(SEGMENTS_CAP_QUERY);
    region_request = segcap.getRootURI().toString();
    try {
      System.out.println("Das Segments Request: " + region_request);
      Document doc = DasLoader.getDocument(region_request);
      Element top_element = doc.getDocumentElement();
      NodeList regionlist = doc.getElementsByTagName("SEGMENT");
      System.out.println("segments: " + regionlist.getLength());
      for (int i=0; i< regionlist.getLength(); i++)  {
	Element reg = (Element)regionlist.item(i);
        String region_id = reg.getAttribute(URID);
	if (region_id.length() == 0) { region_id = reg.getAttribute(ID); }
	URI region_uri = Das2ServerInfo.getBaseURI(region_request, reg).resolve(region_id);

	// GAH _TEMPORARY_ hack to strip down region_id
	// Need to move to full URI resolution very soon!
	if (Das2FeatureSaxParser.DO_SEQID_HACK) {
	  region_id = Das2FeatureSaxParser.doSeqIdHack(region_id);
	}
	String lengthstr = reg.getAttribute("length");
	String region_name = reg.getAttribute(NAME);
	if (region_name.length() == 0) { region_name = reg.getAttribute(TITLE); }
	String region_info_url = reg.getAttribute("doc_href");

	String description = null;
	int length = Integer.parseInt(lengthstr);
	Das2Region region = new Das2Region(this, region_uri, region_name, region_info_url, length);
	if (DEBUG_SEGMENTS_QUERY) {
	  System.out.println("segment: " + region_uri.toString() + ", length = " + lengthstr + ", name = " + region_name);
	}
	this.addRegion(region);
      }
    }
    catch (Exception ex) {
      ErrorHandler.errorPanel("Error initializing DAS region points for\n"+region_request, ex);
    }
    //TODO should regions_initialized be true if an exception occured?
    regions_initialized = true;
  }


  // get annotation types from das server
  /**
   *  loading of parents disabled, getParents currently does nothing
   */
  protected synchronized void initTypes(String filter, boolean getParents) {
    this.types_filter = filter;
    this.clearTypes();

    // how should xml:base be handled?
    //example of type request:  http://das.biopackages.net/das/assay/mouse/6/type?ontology=MA
    //    String types_request = this.getRootUrl() + "/" + TYPES_QUERY;
    Das2Capability typecap = this.getCapability(TYPES_CAP_QUERY);
    String types_request = typecap.getRootURI().toString();

    //    if (filter != null) { types_request = types_request+"?ontology="+filter; }
    try {
      System.out.println("Das Types Request: " + types_request);
      Document doc = DasLoader.getDocument(types_request);
      Element top_element = doc.getDocumentElement();
      NodeList typelist = doc.getElementsByTagName("TYPE");
      System.out.println("types: " + typelist.getLength());
      int typeCounter = 0;

      //      ontologyStuff1();
      for (int i=0; i< typelist.getLength(); i++) {
	Element typenode = (Element)typelist.item(i);

	String typeid = typenode.getAttribute(URID); // Gets the ID value
	if (typeid.length() == 0)  { typeid = typenode.getAttribute(ID); }

	// GAH Temporary hack to deal with typeids that are not legal URIs
	//    unfortunately this can mess up XML Base resolution when the id is an absolute URI
	//    (because URI-encoding will replace any colons, but those are used by URI resolution...)
	//    real fix needs to be on server(s), not client!!

	//	typeid = URLEncoder.encode(typeid, "UTF-8");

	//	typeid = "./" + typeid;
	//        String typeid = typenode.getAttribute("ontology");                            // Gets the ID value
        //FIXME: quick hack to get the type IDs to be kind of right (for now)

        // temporary workaround for getting type ending, rather than full URI
	//	if (typeid.startsWith("./")) { typeid = typeid.substring(2); }          //if these characters are one the beginning, take off the 1st 2 characters...
	//FIXME: quick hack to get the type IDs to be kind of right (for now)

        String ontid = typenode.getAttribute("ontology");
	String type_source = typenode.getAttribute("source");
	String href = typenode.getAttribute("doc_href");
	String type_name = typenode.getAttribute(NAME);
	if (type_name.length() == 0) { type_name = typenode.getAttribute(TITLE); }

	NodeList flist = typenode.getElementsByTagName("FORMAT");               //FIXME: I don't even know if these are in the XML yet.
	LinkedHashMap formats = new LinkedHashMap();                            //I don't think that this has ever been used yet.
	HashMap props = new HashMap();
	for (int k=0; k<flist.getLength(); k++) {
	  Element fnode = (Element)flist.item(k);
	  String formatid = fnode.getAttribute(NAME);
	  if (formatid == null) { formatid = fnode.getAttribute(ID); }
	  String mimetype = fnode.getAttribute("mimetype");
	  if (mimetype == null || mimetype.equals("")) { mimetype = "unknown"; }
          //	  System.out.println("alternative format for annot type " + typeid +
          //": format = " + formatid + ", mimetype = " + mimetype);
          formats.put(formatid, mimetype);
	}

	NodeList plist = typenode.getElementsByTagName("PROP");                 //What IS this?  I am not sure if this is used either.
	for (int k=0; k<plist.getLength(); k++) {
	  Element pnode = (Element)plist.item(k);
	  String key = pnode.getAttribute("key");
	  String val = pnode.getAttribute("value");
	  props.put(key, val);
	}

	//	ontologyStuff2();
	// System.out.println("type id att: " + typeid);
	// System.out.println("base_uri: " + Das2ServerInfo.getBaseURI(types_request, typenode));

        // If one of the typeid's is not a valid URI, then skip it, but allow
        // other typeid's to get through.
        URI type_uri = null;
        try {
	  type_uri = Das2ServerInfo.getBaseURI(types_request, typenode).resolve(typeid);
        } catch (Exception e) {
          System.out.println("Error in typeid, skipping: " + typeid +
              "\nUsually caused by an improper character in the URI.");
        }

        if (type_uri != null) {
          // System.out.println("type URI: " + type_uri.toString());
          Das2Type type = new Das2Type(this, type_uri, type_name, ontid, type_source, href, formats, props, null);   // parents field is null for now -- remove at some point?
          //	Das2Type type = new Das2Type(this, typeid, ontid, type_source, href, formats, props, null);  // parents field is null for now -- remove at some point?
          //	Das2Type type = new Das2Type(this, typeid, ontid, type_source, href, formats, props);
          this.addType(type);
        }
      }
    }
    catch (Exception ex) {
      ErrorHandler.errorPanel("Error initializing DAS types for\n"+types_request, ex);
    }
    //TODO should types_initialized be true after an exception?
    types_initialized = true;
  }


  /**
   *  Use the name feature filter in DAS/2 to retrieve features by name or id (maybe alias).
   *  this method should also add any feature retrieved to the appropriate seq(s) in the AnnotatedSeqGroup
   *      (should they be added directly or indirectly?  For range-based queries the returned features are
   *       wrapped in a Das2FeatureRequestSym -- should there be a wrapper sym for name-based queries also,
   *       or expand/refactor/subclass Das2FeatureRequestSym to serve as wrapper for name-based queries?
   *       For now, trying to just add features directly to seq...)
   *   For now, not allowing combination with any other filters
   */
  public synchronized List getFeaturesByName(String name) {
    List feats = null;
    try {
      Das2Capability featcap = getCapability(FEATURES_CAP_QUERY);
      String request_root = featcap.getRootURI().toString();
      String nameglob = name;
      if (Das2Region.URL_ENCODE_QUERY) {
	nameglob = URLEncoder.encode(nameglob, "UTF-8");
      }
      String feature_query = request_root + "?name=" + nameglob;
      System.out.println("feature query: " + feature_query);

      Das2FeatureSaxParser parser = new Das2FeatureSaxParser();
      URL query_url = new URL(feature_query);
      URLConnection query_con = query_url.openConnection();
      InputStream istr = query_con.getInputStream();
      BufferedInputStream bis = new BufferedInputStream(istr);
      //      feats = parser.parse(new InputSource(bis), feature_query, this.getGenome(), false);
      feats = parser.parse(new InputSource(bis), feature_query, this.getGenome(), true);
      int feat_count = feats.size();
      System.out.println("parsed query results, annot count = " + feat_count);
      /*
      for (int k=0; k<feat_count; k++) {
	SeqSymmetry feat = (SeqSymmetry)feats.get(k);
	//	request_sym.addChild(feat);
      }
      */
      bis.close();
      istr.close();
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    return feats;
  }



}
