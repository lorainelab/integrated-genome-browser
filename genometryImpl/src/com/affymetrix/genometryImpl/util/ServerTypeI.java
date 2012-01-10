package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.das.DasServerType;
import com.affymetrix.genometryImpl.das2.Das2ServerType;
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
}