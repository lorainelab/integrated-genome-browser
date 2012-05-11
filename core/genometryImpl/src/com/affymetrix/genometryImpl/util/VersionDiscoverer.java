package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;

public interface VersionDiscoverer {
	public GenericVersion discoverVersion(String versionID, String versionName, GenericServer gServer, Object versionSourceObj, String speciesName);
	public String versionName2Species(String versionName);
}
