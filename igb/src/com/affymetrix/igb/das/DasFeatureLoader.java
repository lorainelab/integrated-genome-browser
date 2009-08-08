package com.affymetrix.igb.das;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometry.MutableSeqSymmetry;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.util.SeqUtils;

import com.affymetrix.genometryImpl.SimpleSymWithProps;

import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.event.UrlLoaderThread;
import com.affymetrix.igb.view.SeqMapView;

/**
 * Class to aid in loading features from DAS servers.
 * <p />
 * This class will load features for requested regions, skipping any sub-regions
 * which have already been loaded.
 *
 * @author sgblanch
 */
public final class DasFeatureLoader {
	/** Encoding used for {@link java.net.URLEncoder#encode(String,String)}.  Default value is {@value} */
	private static final String ENCODING = "UTF-8";

	/** A private copy of IGB's Map view */
	private static final SeqMapView gviewer = Application.getSingleton().getMapView();
	
	/**
	 * Map of annotation IDs to SeqSymmetry.
	 * 
	 * Each SeqSymmetry tracks what portions of a given annotation set have been loaded.
	 * <p />
	 * We should not have to track this: there must be a way to get this information from elsewhere in the code.
	 */
	private static final Map<String,MutableSeqSymmetry> loadMap = new HashMap<String,MutableSeqSymmetry>();

	/** Private constructor to prevent instantiation. */
	private DasFeatureLoader() { }

	/**
	 * Load annotations from a DAS server.
	 * 
	 * @param gFeature the generic feature that is to be loaded from the server.
	 * @param query_span SeqSpan containing the range for which you want annotations.
	 * @return true if data was loaded
	 */
	public static boolean loadFeatures(GenericFeature gFeature, SeqSpan query_span) {
		String das_root = gFeature.gVersion.gServer.URL;
		MutableAnnotatedBioSeq current_seq = gviewer.getViewSeq();
		List<URL> urls = new ArrayList<URL>();

		try {
			String query_root = das_root.endsWith("/") ? das_root : das_root.concat("/")
						+ URLEncoder.encode(gFeature.gVersion.versionID, ENCODING) + "/features?"
						+ "segment=" + URLEncoder.encode(current_seq.getID(), ENCODING);
			String encoded_type = ";type=" + URLEncoder.encode(gFeature.featureName, ENCODING);
			String id = query_root + encoded_type;

			SimpleSymWithProps query_sym = new SimpleSymWithProps();
			query_sym.setProperty("method", id);
			query_sym.addSpan(query_span);

			MutableSeqSymmetry seen = loadMap.containsKey(id) ? loadMap.get(id) : new SimpleSymWithProps();
			loadMap.put(id, seen);

			SeqSymmetry optimized_sym = SeqUtils.exclusive(query_sym, seen, current_seq);
			walksym(optimized_sym, query_root, encoded_type, urls);

			if (!urls.isEmpty()) {
				seen.addChild(optimized_sym);
			
				String[] tier_names = new String[urls.size()];
				Arrays.fill(tier_names, gFeature.featureName);

				Application.getSingleton().setNotLockedUpStatus("loading " + gFeature.featureName);

				UrlLoaderThread loader = new UrlLoaderThread(gviewer, urls.toArray(new URL[urls.size()]), null, tier_names);
				loader.runEventually();
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Walk the SeqSymmetry, converting all of its children into DAS URLs.
	 *
	 * @param sym the SeqSymmetry to walk.
	 * @param query_root the base URL used to build all URLs.
	 * @param type the type fragment which will be appended to all URLs.
	 * @param urls the List which complete DAS URLs will be added to.
	 * @throws java.io.UnsupportedEncodingException
	 * @throws java.net.MalformedURLException
	 */
	private static void walksym(SeqSymmetry sym, String query_root, String encoded_type, List<URL> urls) throws UnsupportedEncodingException, MalformedURLException {
		for (int i=0; i< sym.getChildCount(); i++) {
			walksym(sym.getChild(i), query_root, encoded_type, urls);
		}
		if (sym.getChildCount() == 0) {
			SeqSpan span;
			String query;
			for (int i = 0; i < sym.getSpanCount(); i++) {
				span = sym.getSpan(i);
				query = query_root + URLEncoder.encode(":" + (span.getMin() + 1) + "," + span.getMax(), ENCODING) + encoded_type;
				urls.add(new URL(query));
			}
		}
	}
}
