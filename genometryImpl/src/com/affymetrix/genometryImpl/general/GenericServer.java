package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometry.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.net.URL;
import javax.swing.ImageIcon;

/**
 * A class that's useful for visualizing a generic server.
 */
public final class GenericServer implements Comparable<GenericServer> {

	public String serverName;   // name of the server.
	public String URL;          // URL/file that points to the server.
	public ServerType serverType;
	public String login;				// Defaults to ""
	public String password;			// Defaults to ""
	public boolean enabled;			// Is this server enabled?
	public final Object serverObj;    // Das2ServerInfo, DasServerInfo, ..., QuickLoad?
	public final URL friendlyURL;			// friendly URL that users may look at.
	public ImageIcon friendlyIcon = null;		// friendly icon that users may look at.
	public int  loginAttempts = 0;

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

		this.friendlyURL = determineFriendlyURL(URL, serverType);
		if (this.friendlyURL != null) {
			this.friendlyIcon = GeneralUtils.determineFriendlyIcon(
				this.friendlyURL.toString() + "/favicon.ico");
		}
	}

	private static URL determineFriendlyURL(String URL, ServerType serverType) {
		if (URL == null) {
			return null;
		}
		String tempURL = URL;
		URL tempFriendlyURL = null;
		if (tempURL.endsWith("/")) {
			tempURL = tempURL.substring(0, tempURL.length() - 1);
		}
		if (serverType.equals(ServerType.DAS)) {
			if (tempURL.endsWith("/dsn")) {
				tempURL = tempURL.substring(0, tempURL.length() - 4);
			}
		} else if (serverType.equals(ServerType.DAS2)) {
			// Remove the last section (e.g., "/genome" or "/das2")
			tempURL = tempURL.substring(0, tempURL.lastIndexOf("/"));
		}
		try {
			tempFriendlyURL = new URL(tempURL);
		} catch (Exception ex) {
			// Ignore an exception here, since this is only for making a pretty UI.
		}
		return tempFriendlyURL;
	}

	@Override
	public String toString() {
		return serverName;
	}

	/**
	 * Order by:
	 * enabled/disabled,
	 * then server name,
	 * then DAS2, DAS, Quickload.
	 * @param gServer
	 * @return
	 */
	public int compareTo(GenericServer gServer) {
		if (this.enabled != gServer.enabled) {
			return ((Boolean)this.enabled).compareTo(gServer.enabled);
		}
		if (!(this.serverName.equals(gServer.serverName))) {
			return this.serverName.compareTo(gServer.serverName);
		}
		return this.serverType.compareTo(gServer.serverType);		
	}
}
