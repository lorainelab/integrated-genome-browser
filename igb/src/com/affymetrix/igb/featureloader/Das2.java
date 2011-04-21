package com.affymetrix.igb.featureloader;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.das2.Das2Capability;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.das2.Das2Region;
import com.affymetrix.genometryImpl.das2.Das2Type;
import com.affymetrix.genometryImpl.das2.Das2VersionedSource;
import com.affymetrix.genometryImpl.das2.FormatPriorities;
import com.affymetrix.genometryImpl.parsers.Das2FeatureSaxParser;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genometryImpl.symloader.BAM;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.util.Constants;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.TrackView;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

/**
 *
 * @author jnicol
 */
public class Das2 {
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
	public static boolean loadFeatures(final SeqSpan span, final GenericFeature feature) {
		final Das2Type dtype = (Das2Type) feature.typeObj;
		final Das2Region region = ((Das2VersionedSource) feature.gVersion.versionSourceObj).getSegment(span.getBioSeq());

		if (dtype == null || region == null) {
			return true;
		}
		Application.getSingleton().addNotLockedUpMsg("Loading feature " + feature.featureName + " on sequence " + span.getBioSeq().getID());
		
		final SeqMapView gviewer = Application.getSingleton().getMapView();
	
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

			public Void doInBackground() {
				try {
					loadSpan(feature, span, region, dtype);
					TrackView.updateDependentData();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				return null;
			}

			@Override
			public void done() {
				try {
					gviewer.setAnnotatedSeq(GenometryModel.getGenometryModel().getSelectedSeq(), true, true);
				} catch (Exception ex) {
					Logger.getLogger(Das2.class.getName()).log(Level.SEVERE, null, ex);
				} finally {
					Application.getSingleton().removeNotLockedUpMsg("Loading feature " + feature.featureName + " on sequence " + span.getBioSeq().getID());
				}
			}
		};

		ThreadUtils.getPrimaryExecutor(feature).execute(worker);
		return true;
	}


    private static void loadSpan(GenericFeature feature, SeqSpan span, Das2Region region, Das2Type type) {
		// Create an AnnotStyle so that we can automatically set the
		// human-readable name to the DAS2 name, rather than the ID, which is a URI
		ITrackStyle ts = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(type.getURI().toString(), type.getName());
		ts.setFeature(feature);

		//TODO: Probably not necessary.
		ts = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(feature.featureName, feature.featureName);
		ts.setFeature(feature);

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
			LoadFeaturesFromQuery(feature, span, feature_query, format, type.getURI(), type.getName());
		} catch (Exception ex) {
			Logger.getLogger(Das2.class.getName()).log(Level.SEVERE, null, ex);
		}
    }

   private static String DetermineQueryPart(Das2Region region, String overlap_filter, URI typeURI, String format) throws UnsupportedEncodingException {
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
 public static Map<String, List<SeqSymmetry>> splitResultsByTracks(List<? extends SeqSymmetry> results) {
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

	private static void filterAndAddAnnotations(
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


	private static List<? extends SeqSymmetry> filterOutExistingSymmetries(SeqSymmetry original_sym, List<? extends SeqSymmetry> syms, BioSeq seq) {
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

    private static boolean LoadFeaturesFromQuery(
            GenericFeature feature, SeqSpan span, String feature_query, String format, URI typeURI, String typeName) {

        /**
         *  Need to look at content-type of server response
         */
        BufferedInputStream bis = null;
        InputStream istr = null;
        String content_subtype = null;

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

            Logger.getLogger(Das2.class.getName()).log(Level.INFO,
					"Parsing {0} format for DAS2 feature response", content_subtype.toUpperCase());
			String extension = "." + content_subtype;	// We add a ".", since this is expected to be a file extension
			List<? extends SeqSymmetry> feats = null;
			FileTypeHandler fileTypeHandler = FileTypeHolder.getInstance().getFileTypeHandler(content_subtype);
			if (fileTypeHandler == null) {
				Logger.getLogger(SymLoader.class.getName()).log(
					Level.WARNING, "ABORTING FEATURE LOADING, FORMAT NOT RECOGNIZED: {0}", content_subtype);
				return false;
			}
			else {
				SymLoader symL = fileTypeHandler.createSymLoader(new URI(extension), typeName, aseq.getSeqGroup());
				if (symL instanceof BAM) {
					File bamfile = GeneralUtils.convertStreamToFile(istr, typeName);
					bamfile.deleteOnExit();
					BAM bam = new BAM(bamfile.toURI(),typeName, aseq.getSeqGroup());
					//for DAS/2 responses, the bam data is already trimmed so should just load it and not build an index, note bam files loaded from a url are not parsed here but elsewhere so the only http inputs are from DAS
					if (typeURI.getScheme().equals("http")) {
						feats = bam.parseAll(span.getBioSeq());
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

			/*
			 TODO: This no longer applies.  Whatever this is doing needs to be done somewhere else.
			 //watch out for useq format, this can contain stranded graph data from a single DAS/2 response, modify the name so it can be caught while making graphs
			String name = type.getName();
			if (format.equals(USeqUtilities.USEQ_EXTENSION_NO_PERIOD)) {
				name += USeqUtilities.USEQ_EXTENSION_WITH_PERIOD;
			}*/

			for (Map.Entry<String, List<SeqSymmetry>> entry : splitResultsByTracks(feats).entrySet()) {
				if (entry.getValue().isEmpty()) {
					continue;
				}
				filterAndAddAnnotations(entry.getValue(), span, feature.getURI(), feature);

				// Some format do not annotate. So it might not have method name. e.g bgn
				if(entry.getKey() != null)
					feature.addMethod(entry.getKey());
			}
			
			feature.addLoadedSpanRequest(span);	// this span is now considered loaded.

            return (feats != null);
        } catch (Exception ex) {
			Logger.getLogger(Das2.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		} finally {
            GeneralUtils.safeClose(bis);
            GeneralUtils.safeClose(istr);
        }
    }

}
