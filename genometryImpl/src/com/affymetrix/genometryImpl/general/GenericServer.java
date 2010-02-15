package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.swing.ImageIcon;

/**
 * A class that's useful for visualizing a generic server.
 */
public final class GenericServer implements Comparable<GenericServer> {

	public String serverName;							// name of the server.
	public String URL;									// URL/file that points to the server.
	public ServerType serverType;						// DAS, DAS2, QuickLoad, Unknown (local file)
	public final boolean hardcodedPreferences;			// Was this server added via the hardcoded preferences file?
	public String login = "";						// to be used by DAS/2 authentication
	public String password = "";						// to be used by DAS/2 authentication
	public int loginAttempts = 0;						// to be used by DAS/2 authentication
	public boolean enabled = true;								// Is this server enabled?
	public final Object serverObj;						// Das2ServerInfo, DasServerInfo, ..., QuickLoad?
	public final URL friendlyURL;						// friendly URL that users may look at.
	private ImageIcon friendlyIcon = null;				// friendly icon that users may look at.
	private boolean friendlyIconAttempted = false;		// Don't keep on searching for friendlyIcon
	private ServerStatus serverStatus = 
			ServerStatus.NotInitialized;				// Is this server initialized?
	private final Set<GenericVersion>versions =
			new CopyOnWriteArraySet<GenericVersion>();	// list of versions associated with this server

	public GenericServer(String serverName, String URL, ServerType serverType, boolean hardcodedPrefs, Object serverObj) {
		this.serverName = serverName;
		this.URL = URL;
		this.serverType = serverType;
		if (serverType == ServerType.Unknown) {
			this.enabled = false;
		}
		this.serverObj = serverObj;
		this.hardcodedPreferences = hardcodedPrefs;
		this.friendlyURL = determineFriendlyURL(URL, serverType);

	}
	
	public ImageIcon getFriendlyIcon() {
		if (friendlyIcon == null && !friendlyIconAttempted) {
			if (this.friendlyURL != null) {
				friendlyIconAttempted = true;
				this.friendlyIcon = GeneralUtils.determineFriendlyIcon(
							this.friendlyURL.toString() + "/favicon.ico");
			}		
		}
		return friendlyIcon;
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
			if (tempURL.endsWith("/genome")) {
				tempURL = tempURL.substring(0, tempURL.length() - 7);
			} 
		}
		try {
			tempFriendlyURL = new URL(tempURL);
		} catch (Exception ex) {
			// Ignore an exception here, since this is only for making a pretty UI.
		}
		return tempFriendlyURL;
	}

	public void addVersion(GenericVersion v) {
		versions.add(v);
	}

	/**
	 * Return versions, but don't allow them to be modified.
	 * @return
	 */
	public Set<GenericVersion> getVersions() {
		return Collections.<GenericVersion>unmodifiableSet(versions);
	}

	public void setServerStatus(ServerStatus serverStatus) {
		this.serverStatus = serverStatus;
	}

	public ServerStatus getServerStatus() {
		return this.serverStatus;
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
	 * @return comparison integer
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
