package com.affymetrix.igb.general;

import com.affymetrix.igb.view.QuickLoadServerModel;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ServerList {

    // Currently just has list of Quickload servers.  Eventually will add DAS and DAS/2 servers here.
    static Map<String, String> name2url = new LinkedHashMap<String, String>();
    static Map<String, genericServer> name2server = new LinkedHashMap<String, genericServer>();
    static Map<String, genericServer> url2server = new LinkedHashMap<String, genericServer>();

    /**
     *  Map is from Strings (server names) to generic servers.
     */
    public static Map<String, genericServer> getServers() {
        return name2server;
    }

    public static Map<String, String> getUrls() {
        return name2url;
    }

    /**
     *  Given an id string which should be the resolvable root URL
     *     (but may optionally be the server name)
     *  Return the genericServer object
     */
    public static genericServer getServer(String id) {
        genericServer server = url2server.get(id);
        if (server == null) {
            server = name2server.get(id);
        }
        return server;
    }

    /**
     * 
     * @param serverClass
     * @param name
     * @param url
     * @return
     */
    public static genericServer addServer(Class serverClass, String name, String url) {
        if (name2url.get(name) == null) {
            name2url.put(url, name);
            return initServer(serverClass, url, name);
        } else {
            return null;
        }
    }

    /**
     * Initialize the server.  Currently only supports Quickload.
     * @param serverClass
     * @param url
     * @param name
     * @return
     */
    protected static genericServer initServer(Class serverClass, String url, String name) {
        genericServer server = null;
        try {
            String root_url = url;
            if (! root_url.endsWith("/")) {
                root_url = root_url + "/";
            }
            if (serverClass == QuickLoadServerModel.class) {
                server = new genericServer(name, root_url, serverClass, root_url);
                name2server.put(name, server);
                url2server.put(url, server);
                return server;
            }
        /*server = new genericServer(name, serverClass, url);
        name2server.put(name, server);
        url2server.put(url, server);*/
        } catch (Exception e) {
            System.out.println("WARNING: Could not initialize " + serverClass + " server with address: " + url);
            e.printStackTrace(System.out);
        }
        return server;
    }
}
