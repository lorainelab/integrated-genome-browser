package com.affymetrix.genometry.util;

import com.affymetrix.genometry.general.GenericServer;
import com.affymetrix.genometry.general.GenericVersion;

public interface VersionDiscoverer {
	public GenericVersion discoverVersion(String versionID, String versionName, GenericServer gServer, Object versionSourceObj, String speciesName);
	public String versionName2Species(String versionName);
}
