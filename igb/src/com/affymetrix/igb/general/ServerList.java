package com.affymetrix.igb.general;

import com.affymetrix.genometryImpl.event.GenericServerInitEvent;
import com.affymetrix.genometryImpl.event.GenericServerInitListener;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericServerPref;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.ServerUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.view.load.GeneralLoadUtils;

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

import javax.swing.JFrame;

/**
 *
 * @version $Id$
 */
public final class ServerList {
	private final Map<String, GenericServer> url2server = new LinkedHashMap<String, GenericServer>();
	private final Set<GenericServerInitListener> server_init_listeners = new CopyOnWriteArraySet<GenericServerInitListener>();
	private final GenericServer localFilesServer = new GenericServer("Local Files","",ServerType.LocalFiles,true,null);

	private static ServerList serverInstance = new ServerList("server");
	private static ServerList repositoryInstance = new ServerList("repository");
	private final String textName;
	private ServerList(String textName) {
		this.textName = textName;
	}
	public static final ServerList getServerInstance() {
		return serverInstance;
	}
	public static final ServerList getRepositoryInstance() {
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

	private int getServerOrder(GenericServer server) {
		String url = GeneralUtils.URLEncode(ServerUtils.formatURL(server.URL, server.serverType));
		return Integer.parseInt(PreferenceUtils.getServersNode().node(GenericServer.getHash(url)).get(GenericServerPref.ORDER, "0"));
	}

	public synchronized Collection<GenericServer> getAllServers() {
		ArrayList<GenericServer> allServers = new ArrayList<GenericServer>(url2server.values());
		Collections.sort(allServers,
			new Comparator<GenericServer>() {
				@Override
				public int compare(GenericServer o1, GenericServer o2) {
					return getServerOrder(o1) - getServerOrder(o2);
				}
			}
		);
		return allServers;
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
				server = new GenericServer(name, url, serverType, enabled, info, primary);

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
		GenericServer server = url2server.get(GeneralUtils.URLDecode( node.get(GenericServerPref.URL, "" ) ) );
		String url;
		String name;
		ServerType serverType;
		Object info;

		if (server == null) {
			url = GeneralUtils.URLDecode(node.get( GenericServerPref.URL, "" ));
			name = node.get(GenericServerPref.NAME, "Unknown");
			String type = node.get(GenericServerPref.TYPE, hasTypes() ? ServerType.LocalFiles.name() : null);
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
			//serverURL not an actual url now, it is a long hash instead.
			for (String serverURL : getPreferencesNode().childrenNames()) {
				node = getPreferencesNode().node(serverURL);
				//this check for the old preference format which used the url as the key
				//the new one uses a long integer hash, so if the key is not a long
				//we have the old format.  We can convert the old format to the new one
				//without loss of data.
				if( !isLong( serverURL ) ){
					
					String url = GeneralUtils.URLDecode( node.name() );
					System.out.println("Converting old standard server preferences to new standard ("+ url +").");
					Preferences n_node = getPreferencesNode().node( GenericServer.getHash( url ));
					n_node.put( GenericServerPref.URL, node.name() );
					n_node.put( GenericServerPref.LOGIN, node.get(GenericServerPref.LOGIN, ""));
					n_node.put( GenericServerPref.PASSWORD, node.get(GenericServerPref.PASSWORD, ""));
					n_node.put( GenericServerPref.NAME, node.get(GenericServerPref.NAME, ""));
					n_node.put( GenericServerPref.ORDER, node.get(GenericServerPref.ORDER, ""));
					if( node.get(GenericServerPref.TYPE, null) != null){
						n_node.put( GenericServerPref.TYPE, node.get(GenericServerPref.TYPE, null));
					}
					n_node.put( GenericServerPref.ENABLED, node.get(GenericServerPref.ENABLED, "true"));
					node.removeNode();
					node = n_node;
				}
				
				serverType = null;
				if (node.get(GenericServerPref.TYPE, null) != null) {
					serverType = ServerType.valueOf(node.get(GenericServerPref.TYPE, ServerType.LocalFiles.name()));
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

	public boolean isLong( String input ){  
	   try{  
		  Long.parseLong( input );  
		  return true;  
	   }catch( NumberFormatException e )  
	   {  
		  return false;  
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
					String name, login, password, real_url;
					boolean enabled;
					//in here, again, the url is actually a hash of type long
					for (String url : prefServers.keys()) {
						name        = prefServers.node(GenericServerPref.NAME).get(url, "Unknown");
						login       = prefServers.node(GenericServerPref.LOGIN).get(url, "");
						password    = prefServers.node(GenericServerPref.PASSWORD).get(url, "");
						enabled     = Boolean.parseBoolean(prefServers.node(GenericServerPref.ENABLED).get(url, "true"));
						real_url	= prefServers.node(GenericServerPref.URL).get(url, ""); 

						server = addServerToPrefs(GeneralUtils.URLDecode(real_url), name, type);
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
		Preferences node = getPreferencesNode().node(GenericServer.getHash(url));
		
		node.put(GenericServerPref.NAME,  name);
		node.put(GenericServerPref.TYPE, type.toString());
		//Added url to preferences.
		//long url was bugging the node name since it only accepts 80 char names
		node.put(GenericServerPref.URL, GeneralUtils.URLEncode(url) );

		return new GenericServer(node, null, ServerType.valueOf(node.get(GenericServerPref.TYPE, ServerType.LocalFiles.name())));
	}

	/**
	 * Add or update a repository in the preferences subsystem.  This only modifies
	 * the preferences nodes, it does not affect any other part of the application.
	 *
	 * @param url URL of this server.
	 * @param name name of this server.
	 * @return an anemic GenericServer object whose sole purpose is to aid in setting of additional preferences
	 */
	private GenericServer addRepositoryToPrefs(String url, String name) {
		Preferences node = PreferenceUtils.getRepositoriesNode().node( GenericServer.getHash( url ) );

		node.put( GenericServerPref.NAME, name);
		node.put( GenericServerPref.URL, GeneralUtils.URLEncode(url));

		return new GenericServer(node, null, null);
	}

	/**
	 * Add or update a server in the preferences subsystem.  This only modifies
	 * the preferences nodes, it does not affect any other part of the application.
	 *
	 * @param server GenericServer object of the server to add or update.
	 */
	public void addServerToPrefs(GenericServer server) {
		if (server.serverType == null) {
			addRepositoryToPrefs(server.URL, server.serverName);
		}
		else {
			addServerToPrefs(server.URL, server.serverName, server.serverType);
		}
	}

	/**
	 * Remove a server from the preferences subsystem.  This only modifies the
	 * preference nodes, it does not affect any other part of the application.
	 *
	 * @param url  URL of the server to remove
	 */
	public void removeServerFromPrefs(String url) {
		try {
			getPreferencesNode().node( GenericServer.getHash(url) ).removeNode();
		} catch (BackingStoreException ex) {
			Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void setServerOrder(String url, int order) {
		getPreferencesNode().node(GenericServer.getHash(url)).put( GenericServerPref.ORDER, "" + order);
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
			GeneralLoadUtils.removeServer(server);

			if(!removedManually) {
				String errorText;
				if (server.serverType == ServerType.QuickLoad) {
					boolean siteOK = LocalUrlCacher.isValidURL(server.URL);
					errorText = siteOK ?
						MessageFormat.format(IGBConstants.BUNDLE.getString("quickloadContentError"), server.serverName) :
						MessageFormat.format(IGBConstants.BUNDLE.getString("quickloadConnectError"), server.serverName);
					ErrorHandler.errorPanelWithReportBug(server.serverName, errorText);
				}
				else {
					String superType = textName.substring(0, 1).toUpperCase() + textName.substring(1);
					errorText = MessageFormat.format(IGBConstants.BUNDLE.getString("connectError"), superType, server.serverName);
					ErrorHandler.errorPanel((JFrame) null, server.serverName, errorText, null);
				}
			}
			if (server.serverType != ServerType.LocalFiles) {
				if (server.serverType == null) {
					Application.getSingleton().removeNotLockedUpMsg("Loading " + textName + " " + server);
				}
				else {
					Application.getSingleton().removeNotLockedUpMsg("Loading " + textName + " " + server + " (" + server.serverType.toString() + ")");
				}
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
}
