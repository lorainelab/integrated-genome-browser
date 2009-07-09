package com.affymetrix.igb.general;

import com.affymetrix.genometry.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.igb.das.DasServerInfo;
import com.affymetrix.igb.das2.Das2ServerInfo;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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

		if (url2Name.get(url) == null) {
			url2Name.put(url, name);
			return initServer(serverType, url, name);
		}
		return null;
	}

	/**
	 * Initialize the server.
	 * @param serverType
	 * @param url
	 * @param name
	 * @return
	 */
	private static GenericServer initServer(ServerType serverType, String url, String name) {
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
				server = new GenericServer(name, info.getURI().toString(), serverType, info);
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
}
