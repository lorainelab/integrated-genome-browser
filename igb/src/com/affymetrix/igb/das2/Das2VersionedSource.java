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
import java.util.List;
import org.w3c.dom.*;
import java.lang.Object.*;
import java.net.URI.*;

import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.util.LocalUrlCacher;
import com.affymetrix.igb.das.DasLoader;
import com.affymetrix.genometryImpl.parsers.Das2FeatureSaxParser;
import com.affymetrix.genometryImpl.util.GeneralUtils;

import org.xml.sax.InputSource;
import static com.affymetrix.igb.IGBConstants.UTF8;

public final class Das2VersionedSource {
    private static final boolean URL_ENCODE_QUERY = true;
    public static String SEGMENTS_CAP_QUERY = "segments";
    public static String TYPES_CAP_QUERY = "types";
    public static String FEATURES_CAP_QUERY = "features";
    private static final boolean DEBUG = false;
    static String ID = Das2FeatureSaxParser.ID;
    static String URID = Das2FeatureSaxParser.URID;
    static String SEGMENT = Das2FeatureSaxParser.SEGMENT;
    static String NAME = Das2FeatureSaxParser.NAME;
    static String TITLE = Das2FeatureSaxParser.TITLE;
    static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
    private URI version_uri;
    private URI coords_uri;
    private Das2Source source;
    private String name;
    private final Map<String,Das2Capability> capabilities = new HashMap<String,Das2Capability>();
    private final Map<String,Das2Region> regions = new LinkedHashMap<String,Das2Region>();
    private AnnotatedSeqGroup genome = null;
    private final Map<String,Das2Type> types = new LinkedHashMap<String,Das2Type>();
    private final Map<String,List<Das2Type>> name2types = new LinkedHashMap<String,List<Das2Type>>();
    private boolean regions_initialized = false;
    private boolean types_initialized = false;
    private String types_filter = null;


    public Das2VersionedSource(Das2Source das_source, URI vers_uri, URI coords_uri, String name,
            String href, String description, boolean init) {
        this.name = name;
        this.coords_uri = coords_uri;
        version_uri = vers_uri;
        source = das_source;
        if (init) {
            initSegments();
            initTypes(null);
        }
    }

    public String getID() {
        return version_uri.toString();
    }

    public String getName() {
        return name;
    }

		@Override
    public String toString() {
        return getName();
    }

    public Das2Source getSource() {
        return source;
    }

    public void addCapability(Das2Capability cap) {
        capabilities.put(cap.getType(), cap);
		Das2Capability.getCapabilityMap().put(cap.getRootURI().toString(), this);
    }

    public Das2Capability getCapability(String type) {
        return capabilities.get(type);
    }

	public AnnotatedSeqGroup getGenome() {
		if (genome != null) {
			return genome;
		}
		// trying to use name for group id first, if no name then use full URI
		// This won't work in every situation!  Really need to resolve issues between VersionedSource URI ids and group ids
		String groupid = this.getName();
		if (groupid == null) {
			groupid = this.getID();
		}
		genome = gmodel.getSeqGroup(groupid);  // gets existing seq group if possible
		if (genome == null && coords_uri != null) { // try coordinates
			genome = gmodel.getSeqGroup(coords_uri.toString());
		}
		if (genome == null) {
			// add new seq group -- if has global coordinates uri, then use that
			//   otherwise, use groupid (version source name or URI)
			if (coords_uri == null) {
				genome = new Das2SeqGroup(this, groupid);
			} else {
				// for now only use coords URI for group if version has no name (just ID), otherwise use name
				if (this.getName() == null) {
					genome = new Das2SeqGroup(this, coords_uri.toString());
				} else {
					genome = new Das2SeqGroup(this, groupid);
				}
			}
			gmodel.addSeqGroup(genome);
		}
		return genome;
	}

    public synchronized Map<String,Das2Region> getSegments() {
        if (!regions_initialized) {
            initSegments();
        }
        return regions;
    }

    /**
     *  assumes there is only one region for each seq
     *    may want to change this to return a list of regions instead
     **/
    public Das2Region getSegment(MutableAnnotatedBioSeq seq) {
		for (Das2Region region : getSegments().values()) {
            MutableAnnotatedBioSeq region_seq = region.getAnnotatedSeq();
            if (region_seq == seq) {
                return region;
            }
        }
        return null;
    }

    private synchronized void addType(Das2Type type) {
        types.put(type.getID(), type);
        String name = type.getName();
        List<Das2Type> prevlist = name2types.get(name);
        if (prevlist == null) {
            prevlist = new ArrayList<Das2Type>();
            name2types.put(name, prevlist);
        }
        prevlist.add(type);
    }

    public synchronized Map<String,Das2Type> getTypes() {
        if (!types_initialized || types_filter != null) {
            initTypes(null);
        }
        return types;
    }

    public synchronized List<Das2Type> getTypesByName(String name) {
        if (!types_initialized || types_filter != null) {
            initTypes(null);
        }
        return name2types.get(name);
    }

    /** Get regions from das server. */
	private synchronized void initSegments() {
		String region_request;
		Das2Capability segcap = getCapability(SEGMENTS_CAP_QUERY);
		region_request = segcap.getRootURI().toString();
		try {
			if (DEBUG) {
				System.out.println("Das2 Segments Request: " + region_request);
			}
			// don't cache this!  If the file is corrupted, this can hose the IGB instance until the cache and preferences are cleared.
			InputStream response = LocalUrlCacher.getInputStream(region_request, false);

			Document doc = DasLoader.getDocument(response);
			Element top_element = doc.getDocumentElement();
			NodeList regionlist = doc.getElementsByTagName("SEGMENT");
			if (DEBUG) {
				System.out.println("segments: " + regionlist.getLength());
			}
			getRegionList(regionlist, region_request);
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Error initializing DAS2 region points for\n" + region_request, ex);
		}
		//TODO should regions_initialized be true if an exception occurred?
		regions_initialized = true;
	}


	private void getRegionList(NodeList regionlist, String region_request) throws NumberFormatException {
		for (int i = 0; i < regionlist.getLength(); i++) {
			Element reg = (Element) regionlist.item(i);
			String region_id = reg.getAttribute(URID);
			if (region_id.length() == 0) {
				region_id = reg.getAttribute(ID);
			}
			// GAH 10-24-2007  temporary hack to weed out bad seqs that are somehow
			//   getting added to segments response from Affy DAS/2 server
			if ((region_id.indexOf("|") >= 0) || (region_id.charAt(region_id.length() - 1) == '.')) {
				System.out.println("@@@@@@@@@@@@@ caught bad seq id: " + region_id);
				continue;
			}
			URI region_uri = Das2ServerInfo.getBaseURI(region_request, reg).resolve(region_id);
			// GAH _TEMPORARY_ hack to strip down region_id
			// Need to move to full URI resolution very soon!
			if (Das2FeatureSaxParser.DO_SEQID_HACK) {
				region_id = Das2FeatureSaxParser.doSeqIdHack(region_id);
			}
			String lengthstr = reg.getAttribute("length");
			String region_name = reg.getAttribute(NAME);
			if (region_name.length() == 0) {
				region_name = reg.getAttribute(TITLE);
			}
			String region_info_url = reg.getAttribute("doc_href");
			//String description = null;
			int length = Integer.parseInt(lengthstr);
			Das2Region region = new Das2Region(this, region_uri, region_name, region_info_url, length);
			if (DEBUG) {
				System.out.println("segment: " + region_uri.toString() + ", length = " + lengthstr + ", name = " + region_name);
			}
			regions.put(region.getID(), region);
		}
	}


    // get annotation types from das2 server
    /**
     *  loading of parents disabled, getParents currently does nothing
     */
    private synchronized void initTypes(String filter) {
        this.types_filter = filter;
		this.types.clear();

        // how should xml:base be handled?
        //example of type request:  http://das.biopackages.net/das/assay/mouse/6/type?ontology=MA
        Das2Capability typecap = this.getCapability(TYPES_CAP_QUERY);
        String types_request = typecap.getRootURI().toString();

        try {
					if (DEBUG) {
						System.out.println("Das2 Types Request: " + types_request);
					}
					Map<String, String> headers = new LinkedHashMap<String, String>();
					InputStream response;
					//set in header a sessionId for types authentication?
					//Also, if there is a sessionId then should ignore cache so user can get hidden types
					String sessionId = source.getServerInfo().getSessionId();
					if (sessionId != null) {
						headers.put("sessionId", sessionId);
						//if sessionID then connected so ignore cache
						response = LocalUrlCacher.getInputStream(types_request, LocalUrlCacher.IGNORE_CACHE, false, headers);
					} //get input stream
					else {
						// don't cache this!  If the file is corrupted, this can hose the IGB instance until the cache and preferences are cleared.
						response = LocalUrlCacher.getInputStream(types_request, false, headers);           
					}
					if (response == null) {
						System.out.println("Types request " + types_request + " was not reachable.");
						return;
					}
					Document doc = DasLoader.getDocument(response);
					Element top_element = doc.getDocumentElement();
					NodeList typelist = doc.getElementsByTagName("TYPE");

					getTypeList(typelist, types_request);

					if (DEBUG) {
						System.out.println("Out of Das2 Types Request: " + types_request);
					}
				} catch (Exception ex) {
            ErrorHandler.errorPanel("Error initializing DAS2 types for\n" + types_request, ex);
        }
        //TODO should types_initialized be true after an exception?
        types_initialized = true;
    }


	private void getTypeList(NodeList typelist, String types_request) {
		if (DEBUG) {
		System.out.println("Das2 Type Length: " + typelist.getLength());
		if (typelist.getLength() == 1) {
			System.out.println("Das2 Types: " + typelist.item(0));
		}
		}
		for (int i = 0; i < typelist.getLength(); i++) {
			Element typenode = (Element) typelist.item(i);
			String typeid = typenode.getAttribute(URID); // Gets the ID value
			if (typeid.length() == 0) {
				typeid = typenode.getAttribute(ID);
			}
			// GAH Temporary hack to deal with typeids that are not legal URIs
			//    unfortunately this can mess up XML Base resolution when the id is an absolute URI
			//    (because URI-encoding will replace any colons, but those are used by URI resolution...)
			//    real fix needs to be on server(s), not client!!
			//	typeid = URLEncoder.encode(typeid, "UTF-8");
			//	typeid = "./" + typeid;
			//        String typeid = typenode.getAttribute("ontology");                            // Gets the ID value
			//FIXME: quick hack to get the type IDs to be kind of right (for now)
			// temporary workaround for getting type ending, rather than full URI
			//	if (typeid.startsWith("./")) { typeid = typeid.substring(2); }
			// if these characters are one the beginning, take off the 1st 2 characters...
			//FIXME: quick hack to get the type IDs to be kind of right (for now)

			String type_name = typenode.getAttribute(NAME);
			if (type_name.length() == 0) {
				type_name = typenode.getAttribute(TITLE);
			}
			NodeList flist = typenode.getElementsByTagName("FORMAT");
			LinkedHashMap<String,String> formats = new LinkedHashMap<String,String>();
			HashMap<String,String> props = new HashMap<String,String>();
			for (int k = 0; k < flist.getLength(); k++) {
				Element fnode = (Element) flist.item(k);
				String formatid = fnode.getAttribute(NAME);
				if (formatid == null) {
					formatid = fnode.getAttribute(ID);
				}
				String mimetype = fnode.getAttribute("mimetype");
				if (mimetype == null || mimetype.equals("")) {
					mimetype = "unknown";
				}
				formats.put(formatid, mimetype);
			}
			NodeList plist = typenode.getElementsByTagName("PROP");
			for (int k = 0; k < plist.getLength(); k++) {
				Element pnode = (Element) plist.item(k);
				String key = pnode.getAttribute("key");
				String val = pnode.getAttribute("value");
				props.put(key, val);
			}
			// If one of the typeid's is not a valid URI, then skip it, but allow
			// other typeid's to get through.
			URI type_uri = null;
			try {
				type_uri = Das2ServerInfo.getBaseURI(types_request, typenode).resolve(typeid);
			} catch (Exception e) {
				System.out.println("Error in typeid, skipping: " + typeid + "\nUsually caused by an improper character in the URI.");
			}
			if (type_uri != null) {
				Das2Type type = new Das2Type(this, type_uri, type_name, formats, props);
				// parents field is null for now -- remove at some point?
				this.addType(type);
			}
		}
	}


    /**
     *  Use the name feature filter in DAS/2 to retrieve features by name or id.
     */
    public synchronized List<SeqSymmetry> getFeaturesByName(String name, AnnotatedSeqGroup group, BioSeq chrFilter) {
		InputStream istr = null;
		BufferedInputStream bis = null;
		try {
			Das2Capability featcap = getCapability(FEATURES_CAP_QUERY);
			String request_root = featcap.getRootURI().toString();
			String nameglob = name;
			if (URL_ENCODE_QUERY) {
				nameglob = URLEncoder.encode(nameglob, UTF8);
			}
			String chrFilterStr = (chrFilter == null ? "?" : "?segment=" + URLEncoder.encode(chrFilter.getID(), UTF8) + ";");
			String feature_query = request_root +
					chrFilterStr + "name=" + nameglob +";format=das2xml";
			if (DEBUG) {
				System.out.println("feature query: " + feature_query);
			}
			Das2FeatureSaxParser parser = new Das2FeatureSaxParser();
			URL query_url = new URL(feature_query);
			URLConnection query_con = query_url.openConnection();
			query_con.setConnectTimeout(LocalUrlCacher.CONNECT_TIMEOUT);
			query_con.setReadTimeout(LocalUrlCacher.READ_TIMEOUT);
			istr = query_con.getInputStream();
			bis = new BufferedInputStream(istr);

			// temporary group needed to avoid side effects (remote SeqSymmetries added to the genome)
			AnnotatedSeqGroup tempGroup = AnnotatedSeqGroup.tempGenome(group);
			List<SeqSymmetry> feats = parser.parse(new InputSource(bis), feature_query, tempGroup, false);
			if (DEBUG) {
				int feat_count = feats.size();
				System.out.println("parsed query results, annot count = " + feat_count);
			}
			return feats;
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(bis);
			GeneralUtils.safeClose(istr);
		}
		return null;
	}
}
