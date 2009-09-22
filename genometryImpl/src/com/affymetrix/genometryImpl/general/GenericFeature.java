package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStatus;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	private boolean visible;							// indicates whether this feature should be visible or not (used in FeatureTreeView/GeneralLoadView interaction).
	public LoadStrategy loadStrategy;  // range chosen by the user, defaults to NO_LOAD.
	public Map<MutableAnnotatedBioSeq, LoadStatus> LoadStatusMap; // each chromosome maps to a feature loading status.
	public URL friendlyURL = null;			// friendly URL that users may look at.
	
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
			this.loadStrategy = LoadStrategy.WHOLE;
			this.setVisible();
		} else {
			this.loadStrategy = LoadStrategy.NO_LOAD;
			this.visible = false;
			
		}
		this.setFriendlyURL();
		this.LoadStatusMap = new HashMap<MutableAnnotatedBioSeq, LoadStatus>();
	}

	public void setVisible() {
		this.visible = true;
		if (this.loadStrategy != LoadStrategy.NO_LOAD) {
			return;
		}
		if ((gVersion != null && gVersion.gServer != null && gVersion.gServer.serverType == ServerType.DAS2)) {
			this.loadStrategy = LoadStrategy.VISIBLE;  // DAS/2 server should default to "Region in View"
			} else {
			this.loadStrategy = LoadStrategy.NO_LOAD;
		}
	}

	public boolean isVisible() {
		return this.visible;
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
	private void setFriendlyURL() {
		if (this.featureProps == null || !this.featureProps.containsKey("url") || this.featureProps.get("url").length() == 0) {
			return;
		}
		try {
			this.friendlyURL = new URL(this.featureProps.get("url"));
		} catch (MalformedURLException ex) {
			Logger.getLogger(GenericFeature.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public String description() {
		if (this.featureProps != null && this.featureProps.containsKey("description")) {
			return this.featureProps.get("description");
		}
		return "";
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
