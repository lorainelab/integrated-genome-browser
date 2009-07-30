package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometryImpl.comparator.StringVersionDateComparator;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that's useful for visualizing a generic version.
 *
 * @other $Id$
 */
public final class GenericVersion implements Comparable<GenericVersion> {

	/** Display name of this version */
	public final String versionName;          // name of the other.
	/** ID of this version on this server */
	public final String versionID;
	public final GenericServer gServer; // generic Server object.
	public final Object versionSourceObj;     // Das2VersionedSource, DasVersionedSource, ..., QuickLoad?
	public final List<GenericFeature> features;

	/**
	 * @param versionID 
	 * @param versionName
	 * @param gServer
	 * @param versionSourceObj
	 */
	public GenericVersion(String versionID, String versionName, GenericServer gServer, Object versionSourceObj) {
		this.versionID = versionID;
		this.versionName = versionName;
		this.gServer = gServer;
		this.versionSourceObj = versionSourceObj;
		this.features = new ArrayList<GenericFeature>();
	}

	@Override
	public String toString() {
		return this.versionName;
	}

	public int compareTo(GenericVersion other) {
		return new StringVersionDateComparator().compare(this.versionName, other.versionName);
	}
}
