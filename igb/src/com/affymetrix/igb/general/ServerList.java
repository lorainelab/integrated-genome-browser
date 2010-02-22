package com.affymetrix.igb.general;

import com.affymetrix.genometryImpl.event.GenericServerInitEvent;
import com.affymetrix.genometryImpl.event.GenericServerInitListener;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.igb.das.DasServerInfo;
import com.affymetrix.igb.das2.Das2ServerInfo;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 *
 * @version $Id$
 */
public final class ServerList {
	private final static Map<String, GenericServer> url2server = new LinkedHashMap<String, GenericServer>();
	private final static Set<GenericServerInitListener> server_init_listeners = new CopyOnWriteArraySet<GenericServerInitListener>();

	public static Set<GenericServer> getEnabledServers() {
		Set<GenericServer> serverList = new HashSet<GenericServer>();
		for (GenericServer gServer : getAllServers()) {
			if (gServer.isEnabled() && gServer.getServerStatus() != ServerStatus.NotResponding) {
				serverList.add(gServer);
			}
		}
		return serverList;
	}

	public static Set<GenericServer> getInitializedServers() {
		Set<GenericServer> serverList = new HashSet<GenericServer>();
		for (GenericServer gServer : ServerList.getEnabledServers()) {
			if (gServer.getServerStatus() == ServerStatus.Initialized) {
				serverList.add(gServer);
			}
		}
		return serverList;
	}

	public static Collection<GenericServer> getAllServers() {
		return url2server.values();
	}

	/*public static Map<String, String> getUrls() {
	return url2Name;
	}*/
	/**
	 *  Given an URLorName string which should be the resolvable root URL
	 *  (but may optionally be the server name)
	 *  Return the GenericServer object.  (This could be non-unique if passed a name.)
	 *
	 * @param URLorName
	 * @return gserver or server
	 */
	public static GenericServer getServer(String URLorName) {
		GenericServer server = url2server.get(URLorName);
		if (server == null) {
			for (GenericServer gServer : getAllServers()) {
				if (gServer.serverName.equals(URLorName)) {
					return gServer;
				}
			}
		}
		return server;
	}

	/**
	 *
	 * @param serverType
	 * @param name
	 * @param url
	 * @return GenericServer
	 */
	public static GenericServer addServer(ServerType serverType, String name, String url) {
		GenericServer server = url2server.get(url);
		Object info;

		if (server == null) {
			info = getServerInfo(serverType, url, name);

			if (info != null) {
				server = new GenericServer(name, url, serverType, info);

				if (server != null) {
					url2server.put(url, server);
				}
			}
		}
		
		return server;
	}

	public static GenericServer addServer(Preferences node) {
		GenericServer server = url2server.get(GeneralUtils.URLDecode(node.name()));
		String url;
		String name;
		ServerType serverType;
		Object info;

		if (server == null) {
			url = GeneralUtils.URLDecode(node.name());
			name = node.get("name", "Unknown");
			serverType = ServerType.valueOf(node.get("type", ServerType.Unknown.name()));
			info = getServerInfo(serverType, url, name);

			if (info != null) {
				server = new GenericServer(node, info);

				if (server != null) {
					url2server.put(url, server);
				}
			}
		}
		
		return server;
	}

	/**
	 * Remove a server.
	 *
	 * @param url
	 */
	public static void removeServer(String url) {
		GenericServer server = url2server.get(url);
		url2server.remove(url);
		server.setEnabled(false);
		fireServerInitEvent(server, ServerStatus.NotResponding);	// remove it from our lists.
	}

	/**
	 * Initialize the server.
	 *
	 * @param serverType
	 * @param url
	 * @param name
	 * @return initialized server
	 */
	private static Object getServerInfo(ServerType serverType, String url, String name) {
		Object info = null;

		try {
			if (serverType == ServerType.QuickLoad) {
				info = url.endsWith("/") ? url : url + "/";
			} else if (serverType == ServerType.DAS) {
				info = new DasServerInfo(url);
			} else if (serverType == ServerType.DAS2) {
				info = new Das2ServerInfo(url, name, false);
			}			
		} catch (URISyntaxException e) {
			System.out.println("WARNING: Could not initialize " + serverType + " server with address: " + url);
			e.printStackTrace(System.out);
		}
		return info;
	}

	/**
	 * Load server preferences from the Java preferences subsystem.
	 */
	public static void loadServerPrefs() {
		ServerType serverType;
		Preferences node;

		try {
			for (String serverURL : PreferenceUtils.getServersNode().childrenNames()) {
				node = PreferenceUtils.getServersNode().node(serverURL);
				serverType = ServerType.valueOf(node.get("type", ServerType.Unknown.name()));

				if (serverType == ServerType.Unknown) {
					continue;
				}

				addServer(node);
			}
		} catch (BackingStoreException ex) {
			Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Update the old-style preference nodes to the newer format.  This is now
	 * called by the PrefsLoader when checking/updating the preferences version.
	 */
	public static void updateServerPrefs() {
		GenericServer server;

		for (ServerType type : ServerType.values()) {
			try {
				if (PreferenceUtils.getServersNode().nodeExists(type.toString())) {
					Preferences prefServers = PreferenceUtils.getServersNode().node(type.toString());
					String name, login, password;
					boolean enabled;
					for (String url : prefServers.keys()) {
						name        = prefServers.get(url, "Unknown");
						login       = prefServers.node("login").get(url, "");
						password    = prefServers.node("password").get(url, "");
						enabled     = Boolean.parseBoolean(prefServers.node("enabled").get(url, "true"));


						server = addServerToPrefs(GeneralUtils.URLDecode(url), name, type);
						server.setLogin(login);
						server.setEncryptedPassword(password);
						server.setEnabled(enabled);
					}
					prefServers.removeNode();
				}
			} catch (BackingStoreException ex) {
				Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	public static void updateServerURLsInPrefs() {
		Preferences servers = PreferenceUtils.getServersNode();
		Preferences currentServer;
		String normalizedURL;
		String decodedURL;
		
		try {
			for (String encodedURL : servers.childrenNames()) {
				currentServer = servers.node(encodedURL);
				decodedURL = GeneralUtils.URLDecode(encodedURL);
				normalizedURL = formatURL(decodedURL, ServerType.valueOf(currentServer.get("type", "Unknown")));

				if (!decodedURL.equals(normalizedURL)) {
					Logger.getLogger(ServerList.class.getName()).log(Level.FINE, "upgrading server URL: '" + decodedURL + "' in preferences");
					Preferences normalizedServer = servers.node(GeneralUtils.URLEncode(normalizedURL));
					for (String key : currentServer.keys()) {
						normalizedServer.put(key, currentServer.get(key, ""));
					}
					currentServer.removeNode();
				}
			}
		} catch (BackingStoreException ex) {
			Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Add or update a server in the preferences subsystem.  This only modifies
	 * the preferences nodes, it does not affect any other part of the application.
	 *
	 * @param url URL of this server.
	 * @param name name of this server.
	 * @param type type of this server.
	 * @return an anemic GenericServer object whose sole purpose is to aid in setting of additional preferences
	 */
	private static GenericServer addServerToPrefs(String url, String name, ServerType type) {
		url = formatURL(url, type);
		Preferences node = PreferenceUtils.getServersNode().node(GeneralUtils.URLEncode(formatURL(url, type)));

		node.put("name",  name);
		node.put("type", type.toString());

		return new GenericServer(node, null);
	}

	/**
	 * Add or update a server in the preferences subsystem.  This only modifies
	 * the preferences nodes, it does not affect any other part of the application.
	 *
	 * @param server GenericServer object of the server to add or update.
	 */
	public static void addServerToPrefs(GenericServer server) {
		addServerToPrefs(server.URL, server.serverName, server.serverType);
	}

	/**
	 * Remove a server from the preferences subsystem.  This only modifies the
	 * preference nodes, it does not affect any other part of the application.
	 *
	 * @param url  URL of the server to remove
	 */
	public static void removeServerFromPrefs(String url) {
		try {
			PreferenceUtils.getServersNode().node(GeneralUtils.URLEncode(url)).removeNode();
		} catch (BackingStoreException ex) {
			Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Check if a server exists in the preferences subsystem.  This method is
	 * used to determine if a server was added from the preferences subsystem
	 * or an external source.
	 *
	 * @param url URL of the server to check
	 * @return true if the url is in the preferences subsystem
	 */
	public static boolean inServerPrefs(String url) {
		try {
			return PreferenceUtils.getServersNode().nodeExists(GeneralUtils.URLEncode(url));
		} catch (BackingStoreException ex) {
			Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
			throw new IllegalArgumentException(ex);
		}
	}

	/**
	 * Format a URL based on the ServerType's requirements.
	 *
	 * @param url URL to format
	 * @param type type of server the URL represents
	 * @return formatted URL
	 */
	private static String formatURL(String url, ServerType type) {
		try {
			/* remove .. and // from URL */
			url = new URI(url).normalize().toASCIIString();
		} catch (URISyntaxException ex) {
			String message = "Unable to parse URL: '" + url + "'";
			Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, message, ex);
			throw new IllegalArgumentException(message, ex);
		}
		switch (type) {
			case DAS:
			case DAS2:
				return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
			case QuickLoad:
				return url.endsWith("/") ? url : url + "/";
			default:
				return url;
		}
	}

	/**
	 * Get server from ServerList that matches the URL.
	 * @param u
	 * @return server
	 * @throws URISyntaxException
	 */
	public static GenericServer getServer(URL u) throws URISyntaxException {
		URI a = u.toURI();
		URI b;
		for (String url : url2server.keySet()) {
			try {
				b = new URI(url);
				if (!b.relativize(a).equals(a)) {
					return url2server.get(url);
				}
			} catch (URISyntaxException ex) {
				Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		throw new IllegalArgumentException("URL " + u.toString() + " is not a valid server.");
	}

	public static void addServerInitListener(GenericServerInitListener listener) {
		server_init_listeners.add(listener);
	}

	public static void fireServerInitEvent(GenericServer server, ServerStatus status) {
		if (server.getServerStatus() != status) {
			server.setServerStatus(status);
			GenericServerInitEvent evt = new GenericServerInitEvent(server);
			for (GenericServerInitListener listener : server_init_listeners) {
				listener.genericServerInit(evt);
			}
		}
	}

}
