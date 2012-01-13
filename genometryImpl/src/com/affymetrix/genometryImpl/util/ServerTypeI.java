package com.affymetrix.genometryImpl.util;

import java.io.IOException;
import java.net.URL;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.das.DasServerType;
import com.affymetrix.genometryImpl.das2.Das2ServerType;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.quickload.QuickloadServerType;

public interface ServerTypeI extends Comparable<ServerTypeI> {
	ServerTypeI QuickLoad = QuickloadServerType.getInstance();
	ServerTypeI DAS = DasServerType.getInstance();
	ServerTypeI DAS2 = Das2ServerType.getInstance();
	ServerTypeI LocalFiles = LocalFilesServerType.getInstance();
	ServerTypeI DEFAULT = LocalFiles;

	public String getName();
	public int getOrdinal();
	public boolean processServer(GenericServer gServer, String path);
	public String formatURL(String url);
	/**
	 * Initialize the server.
	 *
	 * @param url
	 * @param name
	 * @return initialized server
	 */
	public Object getServerInfo(String url, String name);
	public String adjustURL(String url);
	public boolean loadStrategyVisibleOnly();
	public void discoverFeatures(GenericVersion gVersion, boolean autoload);
	public void discoverChromosomes(Object versionSourceObj);
	public boolean hasFriendlyURL();
	public boolean canHandleFeature();
	public boolean getSpeciesAndVersions(GenericServer gServer, GenericServer primaryServer, URL primaryURL, VersionDiscoverer versionDiscoverer);
	public boolean loadFeatures(SeqSpan span, GenericFeature feature) throws IOException;
	public boolean isAuthOptional();
}