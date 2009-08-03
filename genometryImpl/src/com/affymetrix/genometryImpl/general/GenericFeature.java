package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometry.util.LoadUtils.LoadStatus;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A class that's useful for visualizing a generic feature.
 * A feature is unique to a genome version/species/server.
 * (Even if the feature names and version names match, but the servers don't,
 * we can't guarantee that they would contain the same information.)
 */
public final class GenericFeature {

	public final String featureName;      // friendly name of the feature.
	public final Map<String, String> featureProps;
	public final GenericVersion gVersion;        // Points to the version that uses this feature.
	public boolean visible;							// indicates whether this feature should be visible or not (used in FeatureTreeView/GeneralLoadView interaction).
	public LoadStrategy loadStrategy;  // range chosen by the user, defaults to NO_LOAD.
	public Map<MutableAnnotatedBioSeq, LoadStatus> LoadStatusMap; // each chromosome maps to a feature loading status.
	public URL friendlyURL = null;			// friendly URL that users may look at.

	/**
	 * @param featureName
	 * @param gVersion
	 */
	public GenericFeature(String featureName, GenericVersion gVersion) {
		this(featureName, null, gVersion);
	}
	
	/**
	 * @param featureName
	 * @param featureProps
	 * @param gVersion
	 */
	public GenericFeature(String featureName, Map<String, String> featureProps, GenericVersion gVersion) {
		this.featureName = featureName;
		this.featureProps = featureProps;
		this.gVersion = gVersion;
		if (shouldAutoLoad(featureProps)) {
			this.visible = true;
			this.loadStrategy = LoadStrategy.WHOLE;
		} else {
			this.visible = false;
			this.loadStrategy = LoadStrategy.NO_LOAD;
		}
		this.LoadStatusMap = new HashMap<MutableAnnotatedBioSeq, LoadStatus>();
	}

	/**
	 * @param featureProps feature properties
	 * @return true if feature should be loaded automatically
	 */
	private static boolean shouldAutoLoad(Map<String,String> featureProps) {
		return (featureProps != null &&
				featureProps.containsKey("load_hint") &&
				featureProps.get("load_hint").equals("Whole Sequence"));
	}

	@Override
	public String toString() {
		// remove all but the last "/", since these will be represented in a friendly tree view.
		if (!this.featureName.contains("/"))
			return this.featureName;

		int lastSlash = this.featureName.lastIndexOf("/");
		return this.featureName.substring(lastSlash + 1,featureName.length());
	}	
}
