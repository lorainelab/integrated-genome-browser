package com.affymetrix.igb.general;

import com.affymetrix.genometryImpl.GenometryConstants;
import com.affymetrix.genometryImpl.event.GenericServerInitEvent;
import com.affymetrix.genometryImpl.event.GenericServerInitListener;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.quickload.QuickLoadServerModel;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.genometryImpl.util.ServerUtils;
import com.affymetrix.igb.parsers.XmlPrefsParser;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.w3c.dom.Element;

/**
 *
 * @version $Id: ServerList.java 11173 2012-04-19 13:52:00Z imnick $
 */
public final class ServerList {
	private static final boolean DEBUG = false;
	private final Map<String, GenericServer> url2server = new LinkedHashMap<String, GenericServer>();
	private final Set<GenericServerInitListener> server_init_listeners = new CopyOnWriteArraySet<GenericServerInitListener>();
	private final GenericServer localFilesServer = new GenericServer("Local Files", "", ServerTypeI.LocalFiles, true, null, false, null); //qlmirror
	private final GenericServer igbFilesServer = new GenericServer("IGB Tracks", "", ServerTypeI.LocalFiles, true, null, false, null); //qlmirror
	private static ServerList serverInstance = new ServerList("server");
	private static ServerList repositoryInstance = new ServerList("repository");
	private final String textName;
	private final Comparator<GenericServer> serverOrderComparator = new Comparator<GenericServer>() {
			@Override
			public int compare(GenericServer o1, GenericServer o2) {
				return getServerOrder(o1) - getServerOrder(o2);
			}
	};
	
	private ServerList(String textName) {
		this.textName = textName;
	}

	public static ServerList getServerInstance() {
		return serverInstance;
	}

	public static ServerList getRepositoryInstance() {
		return repositoryInstance;
	}

	public String getTextName() {
		return textName;
	}

	public boolean hasTypes() {
		return this == serverInstance;
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

	public Comparator<GenericServer> getServerOrderComparator() {
		return serverOrderComparator;
	}
	
	public GenericServer getLocalFilesServer() {
		return localFilesServer;
	}

	public GenericServer getIGBFilesServer() {
		return igbFilesServer;
	}

	public boolean areAllServersInited() {
		for (GenericServer gServer : getAllServers()) {
			if (!gServer.isEnabled() || gServer.isPrimary()) {
				continue;
			}
			if (gServer.getServerStatus() == ServerStatus.NotInitialized) {
				return false;
			}
		}
		return true;
	}

	public synchronized Collection<GenericServer> getAllServers() {
		ArrayList<GenericServer> allServers = new ArrayList<GenericServer>(url2server.values());
		Collections.sort(allServers, serverOrderComparator);
		return allServers;
	}

	public synchronized Collection<GenericServer> getAllServersExceptCached() {
		Collection<GenericServer> servers = getAllServers();
		GenericServer server = getPrimaryServer();
		if (server != null) {
			servers.remove(server);
		}
		return servers;
	}

	/**
	 * Given an URLorName string which should be the resolvable root SERVER_URL (but
	 * may optionally be the server name) Return the GenericServer object. (This
	 * could be non-unique if passed a name.)
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

	public GenericServer addServer(ServerTypeI serverType, String name, String url,
			boolean enabled, boolean primary, int order, boolean isDefault, String mirrorURL) { //qlmirror
		url = ServerUtils.formatURL(url, serverType);
		GenericServer server = url2server.get(url);
		Object info;

		if (server == null) {
			info = serverType == null ? url : serverType.getServerInfo(url, name);

			if (info != null) {
				if (serverType == null || serverType.isSaveServersInPrefs()) {
					Preferences node = getPreferencesNode().node(GenericServer.getHash(url));
					if (node.get(GenericServer.NAME, null) != null) {
						name = node.get(GenericServer.NAME, null); //Apply changes users may have made to server name
					}
				}
				server = new GenericServer(name, url, serverType, enabled, info, primary, isDefault, mirrorURL);

				if (server != null) {
					url2server.put(url, server);
					addServerToPrefs(server, order, isDefault);
				}
			}
		}

		return server;
	}

	private ServerTypeI getServerType(String name) {
		if (name == null) {
			return null;
		}
		for (ServerTypeI serverType : ServerUtils.getServerTypes()) {
			if (name.equalsIgnoreCase(serverType.getName())) {
				return serverType;
			}
		}
		return null;
	}

	public GenericServer addServer(Preferences node) {
		GenericServer server = url2server.get(GeneralUtils.URLDecode(node.get(GenericServer.SERVER_URL, "")));
		String url;
		String name;
		ServerTypeI serverType;
		Object info;
		
		if (server == null) {
			url = GeneralUtils.URLDecode(node.get(GenericServer.SERVER_URL, ""));
			name = node.get(GenericServer.NAME, "Unknown");
			String type = node.get(GenericServer.TYPE, hasTypes() ? ServerTypeI.DEFAULT.getName() : null);
			serverType = getServerType(type);
			url = ServerUtils.formatURL(url, serverType);
			info = (serverType == null) ? url : serverType.getServerInfo(url, name);
		
			if (info != null) {
				server = new GenericServer(node, info, serverType, false, null); //qlmirror

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
		if (server != null) {
			server.setEnabled(false);
			if (server.serverType == ServerTypeI.QuickLoad) {
				QuickLoadServerModel.removeQLModelForURL(url);
			}
			fireServerInitEvent(server, ServerStatus.NotResponding, true); // remove it from our lists.
		}
	}

	/**
	 * Load server preferences from the Java preferences subsystem.
	 */
	public void loadServerPrefs() {
		ServerTypeI serverType;
		Preferences node;

		try {
			//serverURL not an actual url now, it is a long hash instead.
			for (String serverURL : getPreferencesNode().childrenNames()) {
				node = getPreferencesNode().node(serverURL);
				//this check for the old preference format which used the url as the key
				//the new one uses a long integer hash, so if the key is not a long
				//we have the old format.  We can convert the old format to the new one
				//without loss of data.
				if (!isLong(serverURL)) {

					String url = GeneralUtils.URLDecode(node.name());
					System.out.println("Converting old standard server preferences to new standard (" + url + ").");
					Preferences n_node = getPreferencesNode().node(GenericServer.getHash(url));
					n_node.put(GenericServer.SERVER_URL, node.name());
					n_node.put(GenericServer.LOGIN, node.get(GenericServer.LOGIN, ""));
					n_node.put(GenericServer.PASSWORD, node.get(GenericServer.PASSWORD, ""));
					n_node.put(GenericServer.NAME, node.get(GenericServer.NAME, ""));
					n_node.putInt(GenericServer.ORDER, node.getInt(GenericServer.ORDER, 0));
					if (node.get(GenericServer.TYPE, null) != null) {
						n_node.put(GenericServer.TYPE, node.get(GenericServer.TYPE, null));
					}
					n_node.putBoolean(GenericServer.ENABLED, node.getBoolean(GenericServer.ENABLED, true));
					
					node.removeNode();
					node = n_node;
				}

				serverType = null;
				if (node.get(GenericServer.TYPE, null) != null) {
					serverType = getServerType(node.get(GenericServer.TYPE, ServerTypeI.DEFAULT.getName()));
				}

				if (serverType == ServerTypeI.LocalFiles) {
					continue;
				}

				addServer(node);
			}
		} catch (BackingStoreException ex) {
			Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public boolean isLong(String input) {
		try {
			Long.parseLong(input);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Update the old-style preference nodes to the newer format. This is now
	 * called by the PrefsLoader when checking/updating the preferences version.
	 */
	public void updateServerPrefs() {
		GenericServer server;

		for (ServerTypeI type : ServerUtils.getServerTypes()) {
			try {
				if (getPreferencesNode().nodeExists(type.toString())) {
					Preferences prefServers = getPreferencesNode().node(type.toString());
					String name, login, password, real_url;
					boolean enabled, isDefault;
					//in here, again, the url is actually a hash of type long
					for (String url : prefServers.keys()) {
						name = prefServers.node(GenericServer.NAME).get(url, "Unknown");
						login = prefServers.node(GenericServer.LOGIN).get(url, "");
						password = prefServers.node(GenericServer.PASSWORD).get(url, "");
						enabled = prefServers.node(GenericServer.ENABLED).getBoolean(url, true);
						real_url = prefServers.node(GenericServer.SERVER_URL).get(url, "");
						
						server = addServerToPrefs(GeneralUtils.URLDecode(real_url), name, type, -1, false);
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

	private Preferences getPreferencesNode() {
		return hasTypes() ? PreferenceUtils.getServersNode() : PreferenceUtils.getRepositoriesNode();
	}

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

					normalizedURL = ServerUtils.formatURL(decodedURL, getServerType(serverType));

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
	 * Add or update a server in the preferences subsystem. This only modifies
	 * the preferences nodes, it does not affect any other part of the
	 * application.
	 *
	 * @param url SERVER_URL of this server.
	 * @param name name of this server.
	 * @param type type of this server.
	 * @return an anemic GenericServer object whose sole purpose is to aid in
	 * setting of additional preferences
	 */
	private GenericServer addServerToPrefs(String url, String name,
			ServerTypeI type, int order, boolean isDefault) {
		url = ServerUtils.formatURL(url, type);
		Preferences node = getPreferencesNode().node(GenericServer.getHash(url));
		if (node.get(GenericServer.NAME, null) == null) {
			node.put(GenericServer.NAME, name);
			node.put(GenericServer.TYPE, type.getName());
			node.putInt(GenericServer.ORDER, order);
			//Added url to preferences.
			//long url was bugging the node name since it only accepts 80 char names
			node.put(GenericServer.SERVER_URL, GeneralUtils.URLEncode(url));

		}
		return new GenericServer(node, null, 
				getServerType(node.get(GenericServer.TYPE, ServerTypeI.DEFAULT.getName())),
				isDefault, null); //qlmirror
	}

	/**
	 * Add or update a repository in the preferences subsystem. This only
	 * modifies the preferences nodes, it does not affect any other part of the
	 * application.
	 *
	 * @param url SERVER_URL of this server.
	 * @param name name of this server.
	 * @return an anemic GenericServer object whose sole purpose is to aid in
	 * setting of additional preferences
	 */
	private GenericServer addRepositoryToPrefs(String url, String name, boolean isDefault) {
		Preferences node = PreferenceUtils.getRepositoriesNode().node(GenericServer.getHash(url));

		node.put(GenericServer.NAME, name);
		node.put(GenericServer.SERVER_URL, GeneralUtils.URLEncode(url));
		
		return new GenericServer(node, null, null, isDefault, null); //qlmirror
	}

	/**
	 * Add or update a server in the preferences subsystem. This only modifies
	 * the preferences nodes, it does not affect any other part of the
	 * application.
	 *
	 * @param server GenericServer object of the server to add or update.
	 */
	public void addServerToPrefs(GenericServer server, int order, boolean isDefault) {
		if (server.serverType == null) {
			addRepositoryToPrefs(server.URL, server.serverName, isDefault);
		} else if (server.serverType.isSaveServersInPrefs()) {
			addServerToPrefs(server.URL, server.serverName,
					server.serverType, order, server.isDefault());
		}
	}

	/**
	 * Remove a server from the preferences subsystem. This only modifies the
	 * preference nodes, it does not affect any other part of the application.
	 *
	 * @param url SERVER_URL of the server to remove
	 */
	public void removeServerFromPrefs(String url) {
		try {
			getPreferencesNode().node(GenericServer.getHash(url)).removeNode();
		} catch (BackingStoreException ex) {
			Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void setServerOrder(String url, int order) {
		getPreferencesNode().node(GenericServer.getHash(url)).putInt(GenericServer.ORDER, order);
	}

	private int getServerOrder(GenericServer server) {
		String url = ServerUtils.formatURL(server.URL, server.serverType);
		return PreferenceUtils.getServersNode().node(GenericServer.getHash(url)).getInt(GenericServer.ORDER, 0);
	}

	/**
	 * Get server from ServerList that matches the SERVER_URL.
	 *
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

	public void fireServerInitEvent(GenericServer server, ServerStatus status, boolean removedManually) {
		if (status == ServerStatus.NotResponding) {
			if (server.serverType != null && !server.serverType.isSaveServersInPrefs()) {
				removeServer(server.URL);
			}

			if (!removedManually) {
				String errorText;
				if (server.serverType != null && server.serverType == ServerTypeI.QuickLoad) {
					boolean siteOK = LocalUrlCacher.isValidURL(server.URL);
					errorText = siteOK
							? MessageFormat.format(GenometryConstants.BUNDLE.getString("quickloadContentError"), server.serverName)
							: MessageFormat.format(GenometryConstants.BUNDLE.getString("quickloadConnectError"), server.serverName);
						ErrorHandler.errorPanelWithReportBug(server.serverName, errorText, Level.SEVERE);
				} else {
					String superType = textName.substring(0, 1).toUpperCase() + textName.substring(1);
					errorText = MessageFormat.format(GenometryConstants.BUNDLE.getString("connectError"), superType, server.serverName);
					if (server.serverType != null && server.serverType.isSaveServersInPrefs()) {
						ErrorHandler.errorPanel(server.serverName, errorText, Level.SEVERE);
					}
					else {
						Logger.getLogger(this.getClass().getPackage().getName()).log(Level.SEVERE, errorText);
					}
				}
			}

//			if (server.serverType == null) {
//				Application.getSingleton().removeNotLockedUpMsg("Loading " + textName + " " + server);
//			} else if (server.serverType != ServerTypeI.LocalFiles) {
//				Application.getSingleton().removeNotLockedUpMsg("Loading " + textName + " " + server + " (" + server.serverType.toString() + ")");
//			}
		}

		// Fire event whenever server status in set to initialized 
		// or server status does not match previous status
		if (status == ServerStatus.Initialized || server.getServerStatus() != status) {
			server.setServerStatus(status);
			GenericServerInitEvent evt = new GenericServerInitEvent(server);
			for (GenericServerInitListener listener : server_init_listeners) {
				listener.genericServerInit(evt);
			}
		}
	}

	/**
	 * Gets the primary server if present else returns null.
	 */
	public GenericServer getPrimaryServer() {
		for (GenericServer server : getEnabledServers()) {
			if (server.isPrimary()) {
				return server;
			}
		}
		return null;
	}
	
	private static void processServer(Element el, ServerList serverList, ServerTypeI server_type) {
		String server_name = el.getAttribute("name");
		String server_url = el.getAttribute("url");
		String mirror_url = el.getAttribute("mirror"); //qlmirror
		String en = el.getAttribute("enabled");
		String orderString = el.getAttribute("order");
		Integer order = orderString == null || orderString.isEmpty() ? 0 : Integer.valueOf(orderString);
		Boolean enabled = en == null || en.isEmpty() ? true : Boolean.valueOf(en);
		String pr = el.getAttribute("primary");
		Boolean primary = pr == null || pr.isEmpty() ? false : Boolean.valueOf(pr);
		String d = el.getAttribute("default");
		Boolean isDefault = d == null || d.isEmpty() ? false : Boolean.valueOf(d);
		
		if (DEBUG) {
			System.out.println("XmlPrefsParser adding " + server_type 
					+ " server: " + server_name + ",  " + server_url + " mirror: " + mirror_url
					+ ", enabled: " + enabled + "default: " + isDefault);
		}
		serverList.addServer(server_type, server_name, server_url, 
				enabled, primary, order.intValue(), isDefault, mirror_url); //qlmirror
	}

	
	public static class ServerElementHandler implements XmlPrefsParser.ElementHandler {

		@Override
		public void processElement(Element el) {
			processServer(el, ServerList.getServerInstance(), getServerType(el.getAttribute("type")));
		}

		@Override
		public String getElementTag() {
			return ServerList.getServerInstance().getTextName();
		}
	
		private static ServerTypeI getServerType(String type) {
			for (ServerTypeI t : ServerUtils.getServerTypes()) {
				if (type.equalsIgnoreCase(t.getName())) {
					return t;
				}
			}
			return ServerTypeI.DEFAULT;
		}
	}
	
	public static class RepositoryElementHandler implements XmlPrefsParser.ElementHandler {

		@Override
		public void processElement(Element el) {
			processServer(el, ServerList.getRepositoryInstance(), null);
		}

		@Override
		public String getElementTag() {
			return ServerList.getRepositoryInstance().getTextName();
		}
		
	}
}
