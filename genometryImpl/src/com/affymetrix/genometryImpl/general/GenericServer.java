package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometry.util.LoadUtils.ServerType;

/**
 * A class that's useful for visualizing a generic server.
 */
public final class GenericServer {

    public final String serverName;   // name of the server.
    public final String URL;          // URL/file that points to the server.
    public final ServerType serverType;
    public final Object serverObj;    // Das2ServerInfo, DasServerInfo, ..., QuickLoad?

    /**
     * @param serverName
     * @param URL
     * @param serverType
     * @param serverObj
     */
    public GenericServer(String serverName, String URL, ServerType serverType, Object serverObj) {
        this.serverName = serverName;
        this.URL = URL;
        this.serverType = serverType;
        this.serverObj = serverObj;
    }

    @Override
    public String toString() {
			return this.serverName + "(" + this.serverType.toString() + ")";
    }
}
