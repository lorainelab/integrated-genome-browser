package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometry.util.LoadUtils.ServerType;
import java.net.URL;
import javax.swing.ImageIcon;

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
	public final URL friendlyURL;			// friendly URL that users may look at.
	public ImageIcon friendlyIcon;		// friendly icon that users may look at.

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

		this.friendlyIcon = determineFriendlyIcon(this.friendlyURL);

//    try {
//      // to be used by DAS/2 authentication
//      friendlyURL = new java.net.URL("http://www.life.com");
//      java.net.URL imgURL = new java.net.URL("http://www.lawhelp.org/content/images/icons/g-life.gif");
//      if (imgURL != null) {
//        friendlyIcon = new ImageIcon(imgURL);
//      } else {
//        System.err.println("Couldn't find file: server.ico");
//      }
//    } catch (MalformedURLException ex) {
//      friendlyURL = null;
//      ex.printStackTrace();
//    }
	}

	@Override
	public String toString() {
		return serverName;
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

	private static ImageIcon determineFriendlyIcon(URL friendlyURL) {
		if (friendlyURL == null) {
			return null;
		}

		URL iconURL = null;
		try {
			String iconString = friendlyURL.toString() + "/favicon.ico";
			iconURL = new URL(iconString);
		} catch (Exception ex) {
			// Ignore an exception here, since this is only for making a pretty UI.
		}
		if (iconURL == null) {
			return null;
		}

		ImageIcon friendlyIcon = null;
		try {
			friendlyIcon = new ImageIcon(iconURL);
		}
		catch (Exception ex) {
			// Ignore an exception here, since this is only for making a pretty UI.
		}

		return friendlyIcon;
	}
	
}
