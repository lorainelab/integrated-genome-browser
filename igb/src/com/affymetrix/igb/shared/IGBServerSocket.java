package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hiralv
 */
public class IGBServerSocket {
	// Declare constants
	public static final int default_server_port = 7085;
	
	/** The OLD name of the IGB servlet, "UnibrowControl". */
	public final static String SERVLET_NAME_OLD = "UnibrowControl";
	
	/** The current name of the IGB servlet, "IGBControl". Current versions of
	*  IGB will respond to both this and {@link #SERVLET_NAME_OLD}, but versions
	*  up to and including 4.56 will respond ONLY to the old name.
	*/
	public static final String SERVLET_NAME = "IGBControl";
	
	public final static byte[] prompt = "igb >>> ".getBytes();
	public final static String http_response = "HTTP/1.1 204 No Content\n\n";
	
	/** The basic localhost URL that starts a call to IGB; for backwards-compatibility
	*  with versions of IGB 4.56 and earlier, the old name {@link #SERVLET_NAME_OLD}
	*  is used.
	*/
	public static final String DEFAULT_SERVLET_URL = "http://localhost:"
      + default_server_port + "/" + SERVLET_NAME_OLD;
	
	
	
	private static final int ports_to_try = 5;
	private static boolean tried = false;
	private static ServerSocket singleton;

	public static ServerSocket getServerSocket() {
		if (singleton == null && !tried) {
			try {
				tried = true;
				int server_port = findAvailablePort();
				
				if (server_port != -1) {
					singleton = new ServerSocket(server_port);
				}
				
			} catch (IOException ex) {
				Logger.getLogger(IGBServerSocket.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		return singleton;
	}

	/**
	 * find an available port, starting with the default_server_point and
	 * incrementing up from there...
	 * @return found port
	 */
	private static int findAvailablePort() {
		// 
		int ports_tried = 0;
		int server_port = default_server_port - 1;
		boolean available_port_found = false;
		while ((!available_port_found) && (ports_tried < ports_to_try)) {
			server_port++;
			URL test_url;
			try {
				test_url = new URL("http://localhost:" + server_port
						+ "/" + SERVLET_NAME + "?ping=yes");
			} catch (MalformedURLException mfe) {
				return -1;
			}

			try {
				// try and find an open port...
				URLConnection conn = test_url.openConnection();
				conn.setConnectTimeout(LocalUrlCacher.CONNECT_TIMEOUT);
				conn.setReadTimeout(LocalUrlCacher.READ_TIMEOUT);
				conn.connect();
				// if connection is successful, that means we cannot use that port
				// and must try another one.
				ports_tried++;
			} catch (IOException ex) {
				Logger.getLogger(IGBServerSocket.class.getName()).log(Level.INFO,
						"Found available port for bookmark server: " + server_port);
				available_port_found = true;
			}
		}

		if (available_port_found) {
			return server_port;
		} else {
			return -1;
		}
	}
}
