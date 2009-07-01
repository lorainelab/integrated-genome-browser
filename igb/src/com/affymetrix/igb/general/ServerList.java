package com.affymetrix.igb.general;

import com.affymetrix.genometry.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.igb.das.DasServerInfo;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ServerList {

    // Currently just has list of Quickload servers.  Eventually will add DAS and DAS/2 servers here.
    static Map<String, String> name2url = new LinkedHashMap<String, String>();
    static Map<String, GenericServer> name2server = new LinkedHashMap<String, GenericServer>();
    //static Map<String, GenericServer> url2server = new LinkedHashMap<String, GenericServer>();

    /**
     *  Map is from Strings (server names) to generic servers.
     */
    public static Map<String, GenericServer> getServers() {
        return name2server;
    }

    public static Map<String, String> getUrls() {
        return name2url;
    }

    /**
     *  Given an id string which should be the resolvable root URL
     *     (but may optionally be the server name)
     *  Return the GenericServer object
     */
    /*public static GenericServer getServer(String id) {
        GenericServer server = url2server.get(id);
        if (server == null) {
            server = name2server.get(id);
        }
        return server;
    }*/

    /**
     * 
     * @param serverType
     * @param name
     * @param url
     * @return
     */
    public static GenericServer addServer(ServerType serverType, String name, String url) {
        if (name2url.get(name) == null) {
            name2url.put(url, name);
            return initServer(serverType, url, name);
        } else {
            return null;
        }
    }

    /**
     * Initialize the server.  Currently only supports Quickload.
     * @param serverType
     * @param url
     * @param name
     * @return
     */
    protected static GenericServer initServer(ServerType serverType, String url, String name) {
        GenericServer server = null;
        try {
            String root_url = url;
            if (! root_url.endsWith("/")) {
                root_url = root_url + "/";
            }
            if (serverType == ServerType.QuickLoad) {
                server = new GenericServer(name, root_url, serverType, root_url);
                name2server.put(name, server);
                //url2server.put(url, server);
                return server;
            }
						/*if (serverType == ServerType.DAS) {
							DasServerInfo info = new DasServerInfo(url, name);
							server = new GenericServer(name, root_url, serverType, info);
							name2server.put(name, server);
							return server;
						}*/
        } catch (Exception e) {
            System.out.println("WARNING: Could not initialize " + serverType + " server with address: " + url);
            e.printStackTrace(System.out);
        }
        return server;
    }
}
