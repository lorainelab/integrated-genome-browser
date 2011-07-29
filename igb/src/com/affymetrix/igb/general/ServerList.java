package com.affymetrix.igb.general;

import com.affymetrix.genometryImpl.event.GenericServerInitEvent;
import com.affymetrix.genometryImpl.event.GenericServerInitListener;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.ServerUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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
public abstract class ServerList {
	protected final Map<String, GenericServer> url2server = new LinkedHashMap<String, GenericServer>();
	private final Set<GenericServerInitListener> server_init_listeners = new CopyOnWriteArraySet<GenericServerInitListener>();

	private static Map<String, ServerList> serverListMap = new HashMap<String, ServerList>();
	private static ServerList serverInstance = new DataSourceList();
	private static ServerList repositoryInstance = new RepositoryList();
	private static ServerList sequenceServerInstance = new SequenceServerList();

	private final GenericServer localFilesServer = new GenericServer("Local Files","",ServerType.LocalFiles,true,null,null);
	private final String textName;
	protected ServerList(String textName) {
		this.textName = textName;
		serverListMap.put(textName, this);
	}
	public static final ServerList getServerInstance() {
		return serverInstance;
	}
	public static final ServerList getRepositoryInstance() {
		return repositoryInstance;
	}
	public static final ServerList getSequenceServerInstance() {
		return sequenceServerInstance;
	}
	public static ServerList getServerList(String name) {
		return serverListMap.get(name);
	}
	public static List<ServerList> getServerLists() {
		return new ArrayList<ServerList>(serverListMap.values());
	}
	public String getTextName() {
		return textName;
	}

	public boolean hasTypes() {
		return true;
	}

	public Set<GenericServer> getEnabledServers() {
		Set<GenericServer> serverList = new HashSet<GenericServer>();
		for (GenericServer gServer : getAllServers()) {
			if (gServer.isEnabled() && gServer.getServerStatus() != ServerStatus.NotResponding) {
				serverList.add(gServer);
			}
		}
		return serverList;
	}

	public Set<GenericServer> getInitializedServers() {
		Set<GenericServer> serverList = new HashSet<GenericServer>();
		for (GenericServer gServer : getEnabledServers()) {
			if (gServer.getServerStatus() == ServerStatus.Initialized) {
				serverList.add(gServer);
			}
		}
		return serverList;
	}

	public GenericServer getLocalFilesServer() {
		return localFilesServer;
	}

	public boolean areAllServersInited() {
		for (GenericServer gServer : getAllServers()) {
			if (!gServer.isEnabled()) {
				continue;
			}
			if (gServer.getServerStatus() == ServerStatus.NotInitialized) {
				return false;
			}
		}
		return true;
	}

	public synchronized Collection<GenericServer> getAllServers() {
		return url2server.values();
	}

	public synchronized Collection<GenericServer> getAllServersExceptCached(){
		Collection<GenericServer> servers = getAllServers();
		GenericServer server = getPrimaryServer();
		if(server != null)
			servers.remove(server);
		return servers;
	}

	/**
	 *  Given an URLorName string which should be the resolvable root URL
	 *  (but may optionally be the server name)
	 *  Return the GenericServer object.  (This could be non-unique if passed a name.)
	 *
	 * @param URLorName
	 * @return gserver or server
	 */
	public GenericServer getServer(String URLorName) {
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
	 * @param enabled
	 * @param isPrimary
	 * @return GenericServer
	 */
	public GenericServer addServer(ServerType serverType, String name, String url, boolean enabled, boolean primary) {
		url = ServerUtils.formatURL(url, serverType);
		GenericServer server = url2server.get(url);
		Object info;

		if (server == null) {
			info = ServerUtils.getServerInfo(serverType, url, name);

			if (info != null) {
				server = new GenericServer(name, url, serverType, enabled, info, getPreferencesNode().node(GeneralUtils.URLEncode(url)), primary);

				if (server != null) {
					url2server.put(url, server);
					addServerToPrefs(server);
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
	 * @param enabled
	 * @return GenericServer
	 */
	public GenericServer addServer(ServerType serverType, String name, String url, boolean enabled) {
		return addServer(serverType, name, url, enabled, false);
	}

	public GenericServer addServer(Preferences node) {
		GenericServer server = url2server.get(GeneralUtils.URLDecode(node.name()));
		String url;
		String name;
		ServerType serverType;
		Object info;

		if (server == null) {
			url = GeneralUtils.URLDecode(node.name());
			name = node.get("name", "Unknown");
			String type = node.get("type", hasTypes() ? ServerType.LocalFiles.name() : null);
			serverType = type == null ? null : ServerType.valueOf(type);
			url = ServerUtils.formatURL(url, serverType);
			info = ServerUtils.getServerInfo(serverType, url, name);

			if (info != null) {
				server = new GenericServer(node, info, serverType);

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
	public void removeServer(String url) {
		GenericServer server = url2server.get(url);
		url2server.remove(url);
		server.setEnabled(false);
		fireServerInitEvent(server, ServerStatus.NotResponding);	// remove it from our lists.
	}

	/**
	 * Load server preferences from the Java preferences subsystem.
	 */
	public void loadServerPrefs() {
		ServerType serverType;
		Preferences node;

		try {
			for (String serverURL : getPreferencesNode().childrenNames()) {
				node = getPreferencesNode().node(serverURL);
				serverType = null;
				if (node.get("type", null) != null) {
					serverType = ServerType.valueOf(node.get("type", ServerType.LocalFiles.name()));
				}

				if (serverType == ServerType.LocalFiles) {
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
	public void updateServerPrefs() {
		GenericServer server;

		for (ServerType type : ServerType.values()) {
			try {
				if (getPreferencesNode().nodeExists(type.toString())) {
					Preferences prefServers = getPreferencesNode().node(type.toString());
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

	protected abstract Preferences getPreferencesNode();

	public void updateServerURLsInPrefs() {
		Preferences servers = getPreferencesNode();
		Preferences currentServer;
		String normalizedURL;
		String decodedURL;
		
		try {
			for (String encodedURL : servers.childrenNames()) {
				try {
				currentServer = servers.node(encodedURL);
				decodedURL = GeneralUtils.URLDecode(encodedURL);
				String serverType = currentServer.get("type", "Unknown");
				if (serverType.equals("Unknown")) {
					Logger.getLogger(ServerList.class.getName()).log(
							Level.WARNING, "server URL: {0} could not be determined; ignoring.\nPreferences may be corrupted; clear preferences.", decodedURL);
					continue;
				}
				
				normalizedURL = ServerUtils.formatURL(decodedURL, ServerType.valueOf(serverType));

				if (!decodedURL.equals(normalizedURL)) {
					Logger.getLogger(ServerList.class.getName()).log(Level.FINE, "upgrading " + textName + " URL: ''{0}'' in preferences", decodedURL);
					Preferences normalizedServer = servers.node(GeneralUtils.URLEncode(normalizedURL));
					for (String key : currentServer.keys()) {
						normalizedServer.put(key, currentServer.get(key, ""));
					}
					currentServer.removeNode();
				}
				} catch (Exception ex) {
					// Allow preferences loading to continue if an exception is encountered.
					Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
					continue;
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
	private GenericServer addServerToPrefs(String url, String name, ServerType type) {
		url = ServerUtils.formatURL(url, type);
		Preferences node = getPreferencesNode().node(GeneralUtils.URLEncode(ServerUtils.formatURL(url, type)));

		node.put("name",  name);
		if (type != null) {
			node.put("type", type.toString());
		}
		ServerType useType = (type == null) ? null : ServerType.valueOf(node.get("type", ServerType.LocalFiles.name()));
		return new GenericServer(node, null, useType);
	}

	/**
	 * Add or update a server in the preferences subsystem.  This only modifies
	 * the preferences nodes, it does not affect any other part of the application.
	 *
	 * @param server GenericServer object of the server to add or update.
	 */
	public void addServerToPrefs(GenericServer server) {
		addServerToPrefs(server.URL, server.serverName, server.serverType);
	}

	/**
	 * Remove a server from the preferences subsystem.  This only modifies the
	 * preference nodes, it does not affect any other part of the application.
	 *
	 * @param url  URL of the server to remove
	 */
	public void removeServerFromPrefs(String url) {
		try {
			getPreferencesNode().node(GeneralUtils.URLEncode(url)).removeNode();
		} catch (BackingStoreException ex) {
			Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void setServerOrder(String url, int order) {
		getPreferencesNode().node(GeneralUtils.URLEncode(url)).put("order", "" + order);
	}

	/**
	 * Get server from ServerList that matches the URL.
	 * @param u
	 * @return server
	 * @throws URISyntaxException
	 */
	public GenericServer getServer(URL u) throws URISyntaxException {
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
		throw new IllegalArgumentException("URL " + u.toString() + " is not a valid " + textName + ".");
	}

	public void addServerInitListener(GenericServerInitListener listener) {
		server_init_listeners.add(listener);
	}

	public void removeServerInitListener(GenericServerInitListener listener) {
		server_init_listeners.remove(listener);
	}

	public void fireServerInitEvent(GenericServer server, ServerStatus status) {
		fireServerInitEvent(server, status, false, true);
	}

	public void fireServerInitEvent(GenericServer server, ServerStatus status, boolean removedManually) {
		fireServerInitEvent(server, status, false, removedManually);
	}

	public void fireServerInitEvent(GenericServer server, ServerStatus status, boolean forceUpdate, boolean removedManually) {
		if (status == ServerStatus.NotResponding) {
			server.setEnabled(false);
			if (!removedManually) {
				ErrorHandler.errorPanel(server.serverName, textName.substring(0, 1).toUpperCase() + textName.substring(1) + " " + server.serverName + " is not responding. Disabling it for this session.");
			}
		}

		if (forceUpdate || server.getServerStatus() != status) {
			server.setServerStatus(status);
			GenericServerInitEvent evt = new GenericServerInitEvent(server);
			for (GenericServerInitListener listener : server_init_listeners) {
				listener.genericServerInit(evt);
			}
		}
	}

	/**
	 * Gets the primary server if present else returns null.
	 * @return
	 */
	public  GenericServer getPrimaryServer(){
		for(GenericServer server : getEnabledServers()){
			if(server.isPrimary())
				return server;
		}
		return null;
	}

	public abstract boolean discoverServer(GenericServer gServer);
	public abstract GenericVersion discoverVersion(String versionID, String versionName, GenericServer gServer, Object versionSourceObj, String speciesName);
}
