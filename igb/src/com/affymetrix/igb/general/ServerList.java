package com.affymetrix.igb.general;

import com.affymetrix.genometry.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.das.DasServerInfo;
import com.affymetrix.igb.das2.Das2ServerInfo;
import com.affymetrix.igb.util.StringEncrypter;
import com.affymetrix.igb.util.StringEncrypter.EncryptionException;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class ServerList {
	static Map<String, String> url2Name = new LinkedHashMap<String, String>();
	static Map<GenericServer, String> server2Name = new LinkedHashMap<GenericServer, String>();
	static Map<String, GenericServer> url2server = new LinkedHashMap<String, GenericServer>();

	public static Set<GenericServer> getEnabledServers() {
		Set<GenericServer> serverList = new HashSet<GenericServer>();
		for (GenericServer gServer : server2Name.keySet()) {
			if (gServer.enabled) {
				serverList.add(gServer);
			}
		}
		return serverList;
	}

	public static Set<GenericServer> getAllServers() {
		return server2Name.keySet();
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
	 * @return 
	 */
	public static GenericServer getServer(String URLorName) {
		GenericServer server = url2server.get(URLorName);
		if (server == null) {
			for (GenericServer gServer : server2Name.keySet()) {
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
	 * @return
	 */
	public static GenericServer addServer(ServerType serverType, String name, String url) {

		return addServer(serverType, name, url, null, null);
	}
	
	/**
	 *
	 * @param serverType
	 * @param name
	 * @param url
	 * @param username
	 * @param password
	 * @return
	 */
	public static GenericServer addServer(ServerType serverType, String name, String url, String username, String password) {

		if (url2Name.get(url) == null) {
			url2Name.put(url, name);
			return initServer(serverType, url, name, username, password);
		}
		return null;
	}
	
	/**
	 * Remove a server.
	 * @param url
	 */
	public static void removeServer(String url) {
		GenericServer server = url2server.get(url);
		url2Name.remove(url);
		server2Name.remove(server);
		url2server.remove(url);
	}

	/**
	 * Initialize the server.
	 * @param serverType
	 * @param url
	 * @param name
	 * @return
	 */
	private static GenericServer initServer(ServerType serverType, String url, String name, String username, String password) {
		GenericServer server = null;
		try {
			if (serverType == ServerType.Unknown) {
				return null;
			}

			
			if (serverType == ServerType.QuickLoad) {
				String root_url = url;
				if (!root_url.endsWith("/")) {
					root_url = root_url + "/";
				}
				server = new GenericServer(name, root_url, serverType, root_url);
			}
			if (serverType == ServerType.DAS) {
				DasServerInfo info = new DasServerInfo(url, name);
				server = new GenericServer(name, info.getRootUrl(), serverType, info);
			}
			if (serverType == ServerType.DAS2) {
				Das2ServerInfo info = new Das2ServerInfo(url, name, false);
				server = new GenericServer(name, info.getURI().toString(), serverType, true, username, password, info);
			}
			server2Name.put(server, name);
			url2server.put(url, server);
			return server;

		} catch (Exception e) {
			System.out.println("WARNING: Could not initialize " + serverType + " server with address: " + url);
			e.printStackTrace(System.out);
		}
		return server;
	}

	/**
	 * Load server preferences from the Java preferences subsystem.
	 */
	public static void LoadServerPrefs() {
		/* Look for old-style preferences to update */
		for (ServerType type : ServerType.values()) {
			UpdateServerPrefs(type);
		}

		String server_name, login, password;
		ServerType serverType;
		Boolean enabled;
		try {
			for (String serverURL : UnibrowPrefsUtil.getServersNode().childrenNames()) {
				Preferences node = UnibrowPrefsUtil.getServersNode().node(serverURL);

				serverURL = GeneralUtils.URLDecode(serverURL);
				server_name = node.get("name", "Unknown");
				serverType = ServerType.valueOf(node.get("type", "Unknown"));

				login = node.get("login", "");
				password = decrypt(node.get("password", ""));

				enabled = node.getBoolean("enabled", true);

				System.out.println("Adding " + server_name + ":" + serverURL + " " + serverType);
				
				if (serverType == ServerType.Unknown) {
					System.out.println("WARNING: this server has an unknown type.  Skipping");
					continue;
				}

				// Add the server
				GenericServer server = addServer(serverType, server_name, serverURL, login, password);

				// Now set the enabled flag on the server
				if (server != null) {
					server.enabled = enabled;
				}
			}
		} catch (BackingStoreException ex) {
			Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Update the old-style preference nodes to the newer format.
	 *
	 * @param type the type of server to update
	 */
	private static void UpdateServerPrefs(ServerType type) {
		try {
			if (UnibrowPrefsUtil.getServersNode().nodeExists(type.toString())) {
				Preferences prefServers = UnibrowPrefsUtil.getServersNode().node(type.toString());
				String name, login, password;
				boolean authEnabled, enabled;
				for (String url : prefServers.keys()) {
					name        = prefServers.get(url, "Unknown");
					login       = prefServers.node("login").get(url, "");
					password    = decrypt(prefServers.node("password").get(url, ""));
					authEnabled = !(login.isEmpty() || password.isEmpty());
					enabled     = Boolean.parseBoolean(prefServers.node("enabled").get(url, "true"));
					
					addServerToPrefs(GeneralUtils.URLDecode(url), name, type, authEnabled, login, password, enabled);
				}
				prefServers.removeNode();
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
	 * @param authEnabled boolean noting if client should authenticate to this server
	 * @param login account to use if attemting to authenticate to this server
	 * @param password password to use if attempting to authenticate to this server
	 * @param enabled boolean indicating whether this server is enabled.
	 */
	public static void addServerToPrefs(String url, String name, ServerType type, boolean authEnabled, String login, String password, boolean enabled) {
		Preferences node = UnibrowPrefsUtil.getServersNode().node(GeneralUtils.URLEncode(formatURL(url, type)));

		node.put("name",  name);
		node.put("type", type.toString());

		if (authEnabled) {
			node.put("login", login);
			node.put("password", encrypt(password));
		}

		node.putBoolean("enabled", enabled);
	}

	/**
	 * Add or update a server in the preferences subsystem.  This only modifies
	 * the preferences nodes, it does not affect any other part of the application.
	 *
	 * @param server GenericServer object of the server to add or update.
	 */
	public static void addServerToPrefs(GenericServer server) {
		boolean authEnabled = (server.login != null && server.password != null) && !(server.login.isEmpty() || server.password.isEmpty());
		addServerToPrefs(server.URL, server.serverName, server.serverType, authEnabled, server.login, server.password, server.enabled);
	}

	/**
	 * Remove a server from the preferences subsystem.  This only modifies the
	 * preference nodes, it does not affect any other part of the application.
	 *
	 * @param url  URL of the server to remove
	 */
	public static void removeServerFromPrefs(String url) {
		try {
			UnibrowPrefsUtil.getServersNode().node(GeneralUtils.URLEncode(url)).removeNode();
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
			return UnibrowPrefsUtil.getServersNode().nodeExists(GeneralUtils.URLEncode(url));
		} catch (BackingStoreException ex) {
			Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
			throw new IllegalArgumentException(ex);
		}
	}

	/**
	 * Decrypt the given password.
	 *
	 * @param encrypted encrypted representation of the password
	 * @return string representation of the password
	 */
	private static String decrypt(String encrypted) {
		if (!encrypted.isEmpty()) {
			try {
				StringEncrypter encrypter = new StringEncrypter(StringEncrypter.DESEDE_ENCRYPTION_SCHEME);
				return encrypter.decrypt(encrypted);
			} catch (EncryptionException ex) {
				Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
				throw new IllegalArgumentException(ex);
			}
		}
		return "";
	}

	/**
	 * Encrypt the given password.
	 * 
	 * @param password unencrypted password string
	 * @return the encrypted representation of the password
	 */
	private static String encrypt(String password) {
		if (!password.isEmpty()) {
			try {
				StringEncrypter encrypter = new StringEncrypter(StringEncrypter.DESEDE_ENCRYPTION_SCHEME);
				return encrypter.encrypt(password);
			} catch (Exception ex) {
				Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
				throw new IllegalArgumentException(ex);
			}
		}
		return "";
	}

	/**
	 * Format a URL based on the ServerType's requirements.
	 *
	 * @param url URL to format
	 * @param type type of server the URL represents
	 * @return formatted URL
	 */
	private static String formatURL(String url, ServerType type) {
		switch (type) {
			case DAS:
				return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
			case QuickLoad:
				return url.endsWith("/") ? url : url + "/";
			default:
				return url;
		}
	}
}
