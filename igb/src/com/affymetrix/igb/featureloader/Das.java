package com.affymetrix.igb.featureloader;

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
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
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
public final class Das {
	/** Private constructor to prevent instantiation. */
	private Das() { }

	/**
	 * Load annotations from a DAS server.
	 *
	 * @param gFeature the generic feature that is to be loaded from the server.
	 * @param spans List of spans containing the ranges for which you want annotations.
	 * @return true if data was loaded
	 */
	public static boolean loadFeatures(List<SeqSpan> spans, GenericFeature gFeature) {
		//DasType feature = (DasType)gFeature.typeObj;
		//URL serverURL = feature.getServerURL();
		BioSeq current_seq = spans.get(0).getBioSeq();
		List<URL> urls = new ArrayList<URL>();
		Set<String> segments = ((DasSource)gFeature.gVersion.versionSourceObj).getEntryPoints();
		String segment = SynonymLookup.getDefaultLookup().findMatchingSynonym(segments, current_seq.getID());

		try {
			QueryBuilder builder = new QueryBuilder(gFeature.typeObj.toString());
			builder.add("segment", segment);
			for(SeqSpan span : spans) {
				builder.add("segment", segment + ":" + (span.getMin() + 1) + "," + span.getMax());
				urls.add(builder.build());
			}
			loadOptimizedSym(urls, gFeature);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}


	private static void loadOptimizedSym(List<URL> urls, GenericFeature gFeature) throws MalformedURLException, UnsupportedEncodingException {
		// initialize styles
		for (int i = 0; i < urls.size(); i++) {
			// TODO: temp hack.  The style should be determined by the URI, not the feature name.
			ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(gFeature.featureName, gFeature.featureName);
			//ITrackStyleExtended style =
			//	DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(urls.get(i).toString(), gFeature.featureName);
			style.setFeature(gFeature);
		}
		String[] tier_names = new String[urls.size()];
		Arrays.fill(tier_names, gFeature.featureName);
		UrlLoaderThread loader = new UrlLoaderThread(Application.getSingleton().getMapView(), urls.toArray(new URL[urls.size()]), null, tier_names);
		loader.runEventually();
	}

}
