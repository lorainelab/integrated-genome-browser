package com.affymetrix.igb.general;

import com.affymetrix.igb.das.DasServerInfo;
import com.affymetrix.igb.das2.Das2ServerInfo;

/**
 * A class that's useful for visualizing a generic server.
 */
public final class GenericServer {
	public static enum ServerType { DAS, DAS2, QuickLoad, Unknown };
	
	public final String serverName;   // name of the server.
	public final String URL;          // URL/file that points to the server.
	public final ServerType serverType;
	public final Object serverObj;    // Das2ServerInfo, DasServerInfo, ..., QuickLoad?

	/**
	 * @param serverName
	 * @param URL
	 * @param serverType
	 * @param serverObj
	 */
	public GenericServer(String serverName, String URL, ServerType serverType, Object serverObj) {
		this.serverName = serverName;
		this.URL = URL;
		this.serverType = serverType;
		this.serverObj = serverObj;
	}

	/**
	 * @param serverName
	 * @param URL
	 * @param Class c -- converted to serverType
	 * @param serverObj
	 */
	public GenericServer(String serverName, String URL, Class c, Object serverObj) {
		this.serverName = serverName;
		this.URL = URL;
		this.serverObj = serverObj;

		if (c == DasServerInfo.class) {
			this.serverType = ServerType.DAS;
		} else if (c == Das2ServerInfo.class) {
			this.serverType = ServerType.DAS2;
		} else {
			this.serverType = ServerType.QuickLoad;
		}
	}

	@Override
	public String toString() {
		return this.serverName;
	}
}
