package com.affymetrix.genometryImpl.das2;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.parsers.Das2FeatureSaxParser;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symloader.BAM;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.util.Constants;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.genometryImpl.util.ServerUtils;
import com.affymetrix.genometryImpl.util.SpeciesLookup;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genometryImpl.util.VersionDiscoverer;

public class Das2ServerType implements ServerTypeI {
	private static final String name = "DAS2";
	public static final int ordinal = 10;
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	/**
	 * Private copy of the default Synonym lookup
	 * @see SynonymLookup#getDefaultLookup()
	 */
	private static final SynonymLookup LOOKUP = SynonymLookup.getDefaultLookup();
	/** For files too be looked up on server. **/
	private static final Set<String> das2Files = new HashSet<String>();

	/** Add files to be looked up. **/
	static{
		das2Files.add(Das2VersionedSource.TYPES_CAP_QUERY);
		das2Files.add(Das2VersionedSource.SEGMENTS_CAP_QUERY);
	}
	private static final Das2ServerType instance = new Das2ServerType();
	public static Das2ServerType getInstance() {
		return instance;
	}

	private Das2ServerType() {
		super();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int compareTo(ServerTypeI o) {
		return ordinal - o.getOrdinal();
	}

	@Override
	public int getOrdinal() {
		return ordinal;
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * Returns true if file may not exist else false.
	 * @param fileName
	 * @return
	 */
	private boolean getFileAvailability(String fileName){
		if(fileName.equals(Constants.annotsTxt) || fileName.equals(Constants.annotsXml) || fileName.equals(Constants.liftAllLft))
			return true;

		return false;
	}

	/**
	 * Gets files for a genome and copies it to it's directory.
	 * @param servertype	Server type to determine which set of files to be used.
	 * @param server_path	Server path from where mapping is to be copied.
	 * @param local_path	Local path from where mapping is to saved.
	 */
	private boolean getAllFiles(GenericServer gServer, String genome_name, String local_path){
		File file;
		Set<String> files = das2Files;

		String server_path = gServer.URL + "/" + genome_name;
		local_path += "/" + genome_name;
		GeneralUtils.makeDir(local_path);
		boolean fileMayNotExist;
		for(String fileName : files){
			fileMayNotExist = getFileAvailability(fileName);

			file = GeneralUtils.getFile(server_path+"/"+fileName, fileMayNotExist);

			fileName += Constants.xml_ext;

			if((file == null && !fileMayNotExist))
				return false;

			if(!GeneralUtils.moveFileTo(file,fileName,local_path))
				return false;
		}

		return true;
	}

	@Override
	public boolean processServer(GenericServer gServer, String path) {
		File file = GeneralUtils.getFile(gServer.URL, false);
		if(!GeneralUtils.moveFileTo(file, Constants.GENOME_SEQ_ID+ Constants.xml_ext, path))
			return false;
		
		Das2ServerInfo serverInfo = (Das2ServerInfo) gServer.serverObj;
		Map<String,Das2Source> sources = serverInfo.getSources();
		
		if (sources == null || sources.values() == null || sources.values().isEmpty()) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"Couldn't find species for server: ",gServer);
			return false;
		}

		for (Das2Source source : sources.values()) {
			// Das/2 has versioned sources.  Get each version.
			for (Das2VersionedSource versionSource : source.getVersions().values()) {
				if(!getAllFiles(gServer,versionSource.getName(),path)){
					Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Could not find all files for {0} !!!", gServer.serverName);
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public String formatURL(String url) {
		while (url.endsWith("/")) {
			url = url.substring(0, url.length()-1);
		}
		return url;
	}

	@Override
	public Object getServerInfo(String url, String name) {
		Object info = null;

		try {
			info = new Das2ServerInfo(url, name, false);
		} catch (URISyntaxException e) {
			Logger.getLogger(ServerUtils.class.getName()).log(Level.WARNING,
					"Could not initialize {0} server with address: {1}", new Object[]{name, url});
			e.printStackTrace(System.out);
		}
		return info;
	}

	@Override
	public String adjustURL(String url) {
		String tempURL = url;
		if (tempURL.endsWith("/genome")) {
			tempURL = tempURL.substring(0, tempURL.length() - 7);
		} 
		return tempURL;
	}

	@Override
	public boolean loadStrategyVisibleOnly() {
		return true;
	}

	@Override
	public void discoverFeatures(GenericVersion gVersion, boolean autoload) {
		Das2VersionedSource version = (Das2VersionedSource) gVersion.versionSourceObj;
		for (Das2Type type : version.getTypes().values()) {
			String type_name = type.getName();
			if (type_name == null || type_name.length() == 0) {
				System.out.println("WARNING: Found empty feature name in " + gVersion.versionName + ", " + gVersion.gServer.serverName);
				continue;
			}
			Map<String, String> type_props = type.getProps();
			gVersion.addFeature(new GenericFeature(type_name, type_props, gVersion, null, type, autoload));
		}
	}

	@Override
	public void discoverChromosomes(Object versionSourceObj) {
		Das2VersionedSource version = (Das2VersionedSource) versionSourceObj;
		
		version.getGenome();  // adds genome to singleton genometry model if not already present
		// Calling version.getSegments() to ensure that Das2VersionedSource is populated with Das2Region segments,
		//    which in turn ensures that AnnotatedSeqGroup is populated with SmartAnnotBioSeqs
		version.getSegments();
	}

	@Override
	public boolean hasFriendlyURL() {
		return true;
	}

	@Override
	public boolean canHandleFeature() {
		return true;
	}

	/**
	 * Discover genomes from DAS/2
	 * @param gServer
	 * @return false if there's an obvious problem
	 */
	@Override
	public boolean getSpeciesAndVersions(GenericServer gServer, GenericServer primaryServer, URL primaryURL, VersionDiscoverer versionDiscoverer) {
		Das2ServerInfo server = (Das2ServerInfo) gServer.serverObj;
		Map<String, Das2Source> sources = server.getSources(primaryURL, primaryServer);
		if (sources == null || sources.values() == null || sources.values().isEmpty()) {
			System.out.println("WARNING: Couldn't find species for server: " + gServer);
			return false;
		}
		for (Das2Source source : sources.values()) {
			String speciesName = SpeciesLookup.getSpeciesName(source.getName());

			// Das/2 has versioned sources.  Get each version.
			for (Das2VersionedSource versionSource : source.getVersions().values()) {
				String versionName = LOOKUP.findMatchingSynonym(gmodel.getSeqGroupNames(), versionSource.getName());
				String versionID = versionSource.getName();
				versionDiscoverer.discoverVersion(versionID, versionName, gServer, versionSource, speciesName);
			}
		}
		return true;
	}

	private static final String default_format = "das2feature";

	/**
	 * Loads (and displays) DAS/2 annotations.
	 * This is done in a multi-threaded fashion so that the UI doesn't lock up.
	 * @param selected_seq
	 * @param gFeature
	 * @param gviewer
	 * @param overlap
	 * @return true or false
	 */
	@Override
	public boolean loadFeatures(SeqSpan span, GenericFeature feature) {
		final Das2Type dtype = (Das2Type) feature.typeObj;
		final Das2Region region = ((Das2VersionedSource) feature.gVersion.versionSourceObj).getSegment(span.getBioSeq());

		if (dtype == null || region == null) {
			return false;
		}
		
		return loadSpan(feature, span, region, dtype);
	}


    private boolean loadSpan(GenericFeature feature, SeqSpan span, Das2Region region, Das2Type type) {

        String overlap_filter = Das2FeatureSaxParser.getRangeString(span, false);

		String format = FormatPriorities.getFormat(type);
		if (format == null) {
			format = default_format;
		}

        Das2VersionedSource versioned_source = region.getVersionedSource();
        Das2Capability featcap = versioned_source.getCapability(Das2VersionedSource.FEATURES_CAP_QUERY);
        String request_root = featcap.getRootURI().toString();

        try {
            String query_part = DetermineQueryPart(region, overlap_filter, type.getURI(), format);
            String feature_query = request_root + "?" + query_part;
			return LoadFeaturesFromQuery(feature, span, feature_query, format, type.getURI(), type.getName());
		} catch (Exception ex) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
		}
		
		return false;
    }

   private String DetermineQueryPart(Das2Region region, String overlap_filter, URI typeURI, String format) throws UnsupportedEncodingException {
		StringBuilder buf = new StringBuilder(200);
		buf.append("segment=");
		buf.append(URLEncoder.encode(region.getID(), Constants.UTF8));
		buf.append(";");
		buf.append("overlaps=");
		buf.append(URLEncoder.encode(overlap_filter, Constants.UTF8));
		buf.append(";");
		buf.append("type=");
		buf.append(URLEncoder.encode(typeURI.toString(), Constants.UTF8));
		if (format != null) {
			buf.append(";");
			buf.append("format=");
			buf.append(URLEncoder.encode(format, Constants.UTF8));
		}
		return buf.toString();
	}

 	/**
  * Split list of symmetries by track.
  * @param results - list of symmetries
  * @return - Map<String trackName,List<SeqSymmetry>>
  */
 public Map<String, List<SeqSymmetry>> splitResultsByTracks(List<? extends SeqSymmetry> results) {
		Map<String, List<SeqSymmetry>> track2Results = new HashMap<String, List<SeqSymmetry>>();
		List<SeqSymmetry> resultList = null;
		String method = null;
		for (SeqSymmetry result : results) {
			method = BioSeq.determineMethod(result);
			if (track2Results.containsKey(method)) {
				resultList = track2Results.get(method);
			} else {
				resultList = new ArrayList<SeqSymmetry>();
				track2Results.put(method, resultList);
			}
			resultList.add(result);
		}

	  return track2Results;
 }

	private void filterAndAddAnnotations(
			List<? extends SeqSymmetry> feats, SeqSpan span, URI uri, GenericFeature feature) {
		if (feats == null || feats.isEmpty()) {
			return;
		}
		SeqSymmetry originalRequestSym = feature.getRequestSym();
		List<? extends SeqSymmetry> filteredFeats = filterOutExistingSymmetries(originalRequestSym, feats, span.getBioSeq());	
		if (filteredFeats.isEmpty()) {
			return;
		}
		if (filteredFeats.get(0) instanceof GraphSym) {
			// We assume that if there are any GraphSyms, then we're dealing with a list of GraphSyms.
			for(SeqSymmetry feat : filteredFeats) {
				//grafs.add((GraphSym)feat);
				if (feat instanceof GraphSym) {
					GraphSymUtils.addChildGraph((GraphSym) feat, ((GraphSym) feat).getID(), ((GraphSym) feat).getGraphName(), uri.toString(), span);
				}
			}

			return;
		}

		BioSeq seq = span.getBioSeq();
		for (SeqSymmetry feat : filteredFeats) {
			seq.addAnnotation(feat);
		}
	}


	private List<? extends SeqSymmetry> filterOutExistingSymmetries(SeqSymmetry original_sym, List<? extends SeqSymmetry> syms, BioSeq seq) {
		List<SeqSymmetry> newSyms = new ArrayList<SeqSymmetry>(syms.size());	// roughly this size
		MutableSeqSymmetry dummySym = new SimpleMutableSeqSymmetry();
		for (SeqSymmetry sym : syms) {

			/**
			 * Since GraphSym is only SeqSymmetry containing all points.
			 * The intersection may find some points intersecting and
			 * thus not add whole GraphSym at all. So if GraphSym is encountered
			 * the it's not checked if it is intersecting. 
			 */
			if (sym instanceof GraphSym) {
				// if graphs, then adding to annotation BioSeq is handled by addChildGraph() method
				return syms;
			}

			dummySym.clear();
			if (SeqUtils.intersection(sym, original_sym, dummySym, seq)) {
				// There is an intersection with previous requests.  Ignore this symmetry
				continue;
			}
			newSyms.add(sym);
		}
		return newSyms;
	}

    private boolean LoadFeaturesFromQuery(
            GenericFeature feature, SeqSpan span, String feature_query, String format, URI typeURI, String typeName) {

        /**
         *  Need to look at content-type of server response
         */
        BufferedInputStream bis = null;
        InputStream istr = null;
        String content_subtype = null;

		Thread thread = Thread.currentThread();
		
		if(thread.isInterrupted()){
			return false;
		}
		
        try {
			BioSeq aseq = span.getBioSeq();
            // if overlap_span is entire length of sequence, then check for caching
            if ((span.getMin() == 0) && (span.getMax() == aseq.getLength())) {
                istr = LocalUrlCacher.getInputStream(feature_query);
                if (istr == null) {
                    System.out.println("Server couldn't be accessed with query " + feature_query);
                    return false;
                }
                // for now, assume that when caching, content type returned is same as content type requested
                content_subtype = format;
            } else {
                URL query_url = new URL(feature_query);

                // casting to HttpURLConnection, since Das2 servers should be either accessed via either HTTP or HTTPS
                HttpURLConnection query_con = (HttpURLConnection) query_url.openConnection();
				query_con.setConnectTimeout(LocalUrlCacher.CONNECT_TIMEOUT);
				query_con.setReadTimeout(LocalUrlCacher.READ_TIMEOUT);
                int response_code = query_con.getResponseCode();
                String response_message = query_con.getResponseMessage();

                if (response_code != 200) {
                    System.out.println("WARNING, HTTP response code not 200/OK: " + response_code + ", " + response_message);
                }

                if (response_code >= 400 && response_code < 600) {
                    System.out.println("Server returned error code, aborting response parsing!");
                    return false;
                }
                String content_type = query_con.getContentType();
				istr = query_con.getInputStream();

				content_subtype = content_type.substring(content_type.indexOf("/") + 1);
				int sindex = content_subtype.indexOf(';');
				if (sindex >= 0) {
					content_subtype = content_subtype.substring(0, sindex);
					content_subtype = content_subtype.trim();
				}
				if (content_subtype == null || content_type.equals("unknown") || content_subtype.equals("unknown") || content_subtype.equals("xml") || content_subtype.equals("plain")) {
					// if content type is not descriptive enough, go by what was requested
					content_subtype = format;
				}
            }

            Logger.getLogger(this.getClass().getName()).log(Level.INFO,
					"Parsing {0} format for DAS2 feature response", content_subtype.toUpperCase());
					
			List<? extends SeqSymmetry> feats = null;
			FileTypeHandler fileTypeHandler = FileTypeHolder.getInstance().getFileTypeHandler(content_subtype.toLowerCase());
			if (fileTypeHandler == null) {
				Logger.getLogger(SymLoader.class.getName()).log(
					Level.WARNING, "ABORTING FEATURE LOADING, FORMAT NOT RECOGNIZED: {0}", content_subtype);
				return false;
			}
			else {
				// Create an AnnotStyle so that we can automatically set the
				// human-readable name to the DAS2 name, rather than the ID, which is a URI
				ITrackStyleExtended ts = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(typeURI.toString(), typeName, format);
				ts.setFeature(feature);

				//TODO: Probably not necessary.
				//ts = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(feature.featureName, feature.featureName, format);
				//ts.setFeature(feature);

				SymLoader symL = fileTypeHandler.createSymLoader(typeURI, typeName, aseq.getSeqGroup());
				symL.setExtension(content_subtype);
				if (symL instanceof BAM) {
					File bamfile = GeneralUtils.convertStreamToFile(istr, typeName);
					bamfile.deleteOnExit();
					BAM bam = new BAM(bamfile.toURI(),typeName, aseq.getSeqGroup());
					//for DAS/2 responses, the bam data is already trimmed so should just load it and not build an index, note bam files loaded from a url are not parsed here but elsewhere so the only http inputs are from DAS
					if (typeURI.getScheme().equals("http")) {
						feats = bam.parseAll(span.getBioSeq(), typeURI.toString());
					}
					else {
						feats = bam.getRegion(span);
					}
				}
				else {
					feats = symL.parse(istr, false);
				}
//				feats = SymProcessor.getInstance().parse(extension, typeURI, istr, aseq.getSeqGroup(), typeName, span);
			}

			if(thread.isInterrupted()){
				feats = null;
				return false;
			}
			
			for (Map.Entry<String, List<SeqSymmetry>> entry : splitResultsByTracks(feats).entrySet()) {
				if (entry.getValue().isEmpty()) {
					continue;
				}
				filterAndAddAnnotations(entry.getValue(), span, feature.getURI(), feature);

				// Some format do not annotate. So it might not have method name. e.g bgn
				if(entry.getKey() != null)
					feature.addMethod(entry.getKey());
			}
			
            return (feats != null && !feats.isEmpty());
			
        } catch (Exception ex) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
			return false;
		} finally {
            GeneralUtils.safeClose(bis);
            GeneralUtils.safeClose(istr);
        }
    }

	@Override
	public boolean isAuthOptional() {
		return true;
	}
}
