package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometry.util.LoadUtils.ServerType;
import java.awt.Image;
import java.net.URL;

/**
 * A class that's useful for visualizing a generic server.
 */
public final class GenericServer {

	public final String serverName;   // name of the server.
	public final String URL;          // URL/file that points to the server.
	public final ServerType serverType;
	public final Object serverObj;    // Das2ServerInfo, DasServerInfo, ..., QuickLoad?

	public final boolean enabled;			// Is this server enabled?

	public final String login;				// Defaults to ""
	public final String password;			// Defaults to ""
	
	public final URL friendlyURL = null;			// friendly URL that users may look at.
	public final Image friendlyIcon = null;		// friendly icon that users may look at.


	/**
	 * @param serverName
	 * @param URL
	 * @param serverType
	 * @param serverObj
	 */
	public GenericServer(String serverName, String URL, ServerType serverType, Object serverObj) {
		this(serverName, URL, serverType, true, "", "", serverObj);
	}

	public GenericServer(String serverName, String URL, ServerType serverType, boolean enabled, String login, String password, Object serverObj) {
		this.serverName = serverName;
		this.URL = URL;
		this.serverType = serverType;
		this.serverObj = serverObj;
		this.enabled = enabled;
		this.login = login;				// to be used by DAS/2 authentication
		this.password = password;			// to be used by DAS/2 authentication
	}

	@Override
	public String toString() {
		return this.serverName + "(" + this.serverType.toString() + ")";
	}
}
