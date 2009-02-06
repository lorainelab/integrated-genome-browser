package com.affymetrix.igb.general;

/**
 * A class that's useful for visualizing a generic server.
 */
public final class genericServer {

    public final String serverName;   // name of the server.
    public final String URL;          // URL/file that points to the server.
    public final Class serverClass;   // Das2ServerInfo, DasServerInfo, QuickLoadServerModel?
    public final Object serverObj;    // Das2ServerInfo, DasServerInfo, ..., QuickLoad?

    /**
     * @param serverName
     * @param serverClass
     * @param serverObj
     */
    public genericServer(String serverName, String URL, Class serverClass, Object serverObj) {
        this.serverName = serverName;
        this.URL = URL;
        this.serverClass = serverClass;
        this.serverObj = serverObj;
    }
}
