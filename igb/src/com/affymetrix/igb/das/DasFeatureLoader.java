package com.affymetrix.igb.das;

import com.affymetrix.genometryImpl.das.DasSource;
import com.affymetrix.genometryImpl.das.DasType;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.util.QueryBuilder;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.event.UrlLoaderThread;


/**
 * Class to aid in loading features from DAS servers.
 * <p />
 * This class will load features for requested regions, skipping any sub-regions
 * which have already been loaded.
 *
 * @author sgblanch
 */
public final class DasFeatureLoader {
	/** Private constructor to prevent instantiation. */
	private DasFeatureLoader() { }

	/**
	 * Load annotations from a DAS server.
	 * 
	 * @param gFeature the generic feature that is to be loaded from the server.
	 * @param query_span SeqSpan containing the range for which you want annotations.
	 * @return true if data was loaded
	 */
	public static boolean loadFeatures( SeqSpan query_span, GenericFeature gFeature) {
		DasType feature = (DasType)gFeature.typeObj;
		URL serverURL = feature.getServerURL();
		BioSeq current_seq = query_span.getBioSeq();
		List<URL> urls = new ArrayList<URL>();
		Set<String> segments = ((DasSource)gFeature.gVersion.versionSourceObj).getEntryPoints();
		String segment = SynonymLookup.getDefaultLookup().findMatchingSynonym(segments, current_seq.getID());

		try {
			QueryBuilder builder = new QueryBuilder(new URL(serverURL, feature.getSource() + "/features"));
			builder.add("segment", segment);
			builder.add("type", feature.getID());

			SeqSymmetry optimized_sym = gFeature.optimizeRequest(query_span);
			if (optimized_sym != null) {
				convertSymToDasURLs(optimized_sym, builder, segment, urls);

				String[] tier_names = new String[urls.size()];
				Arrays.fill(tier_names, gFeature.featureName);

				UrlLoaderThread loader = new UrlLoaderThread(
						Application.getSingleton().getMapView(), urls.toArray(new URL[urls.size()]), null, tier_names);
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
	 * @param sym the SeqSymmetry to walk.
	 * @param query_root the base URL used to build all URLs.
	 * @param encoded_type the type fragment which will be appended to all URLs.
	 * @param urls the List which complete DAS URLs will be added to.
	 * @throws java.io.UnsupportedEncodingException
	 * @throws java.net.MalformedURLException
	 */
	private static void convertSymToDasURLs(SeqSymmetry sym, QueryBuilder builder, String segment, List<URL> urls)
			throws UnsupportedEncodingException, MalformedURLException {
		int childCount = sym.getChildCount();
		if (childCount > 0) {
			for (int i = 0; i < childCount; i++) {
				convertSymToDasURLs(sym.getChild(i), builder, segment, urls);
			}
		} else {
			SeqSpan span;
			int spanCount = sym.getSpanCount();
			for (int i = 0; i < spanCount; i++) {
				span = sym.getSpan(i);
				builder.add("segment", segment + ":" + (span.getMin() + 1) + "," + span.getMax());
				urls.add(builder.build());
			}
		}
	}
}
