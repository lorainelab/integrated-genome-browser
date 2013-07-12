/**
*   Copyright (c) 2007 Affymetrix, Inc.
*
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*   Distributions from Affymetrix, Inc., place this in the
*   IGB_LICENSE.html file.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/

package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.igb.osgi.service.IGBService;
import java.io.IOException;
import java.net.*;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 *  A very simple servlet that listens for bookmark commands as http GET requests.
 *  The server reads only the single "GET" line from the header,
 *  ignores all other input, returns no output, and closes the connection.
 */
public final class SimpleBookmarkServer {
	public static final String http_response ="\nHTTP/1.1 204 No Content\nAccess-Control-Allow-Origin: *\n";
	private static final int NO_PORT = -1;
	public static final int default_server_port = 7085;
	private static final int ports_to_try = 5;
	/** The OLD name of the IGB servlet, "UnibrowControl". */
	public final static String SERVLET_NAME_OLD = "UnibrowControl";
	/** The current name of the IGB servlet, "IGBControl". Current versions of
	*  IGB will respond to both this and {@link #SERVLET_NAME_OLD}, but versions
	*  up to and including 4.56 will respond ONLY to the old name.
	*/
	public static final String SERVLET_NAME = "IGBControl";
	/** The basic localhost URL that starts a call to IGB; for backwards-compatibility
	*  with versions of IGB 4.56 and earlier, the old name {@link #SERVLET_NAME_OLD}
	*  is used.
	*/
	public static final String DEFAULT_SERVLET_URL = "http://localhost:"
      + default_server_port + "/" + SERVLET_NAME_OLD;
	private static int server_port = NO_PORT;
	private static final Logger ourLogger
			= Logger.getLogger(SimpleBookmarkServer.class.getPackage().getName());

	static {
		try {
			setServerPort(ResourceBundle.getBundle("sockets").getString("server_port"));
		}
		catch (MissingResourceException x) {}
	}
	
	public static void setServerPort(String portString) {
		try {
			int port = Integer.parseInt(portString);
			if (port > 0 && port <= 65535) {
				server_port = port;
			}
			else {
				ourLogger.log(Level.SEVERE,
						"Invalid port number {0}, must be between 0 and 65535", port);
			}
		}
		catch (NumberFormatException x) {
			ourLogger.log(Level.SEVERE, "Invalid number {0} for server_port", portString);
		}
	}
	
	public SimpleBookmarkServer(IGBService igbService, ServerSocket server) {
		try {
			while (true) {
				Socket socket = server.accept();
				ourLogger.log(Level.FINE, "Connection accepted {0}:{1}",
						new Object[]{socket.getInetAddress(), socket.getPort()});
				BookmarkHttpRequestHandler request = new BookmarkHttpRequestHandler(igbService, socket);
				Thread thread = new Thread(request);
				thread.start();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	// Use the Swing Thread to start a non-Swing thread
	// that will start the control server.
	// Thus the control server will be started only after current GUI stuff is finished,
	// but starting it won't cause the GUI to hang.
	public static void init(final IGBService igbService) {
		startServerSocket(igbService, default_server_port);
		if (server_port != NO_PORT) {
			startServerSocket(igbService, server_port);
		}
	}

	public static void startServerSocket(final IGBService igbService, int startPort) {
		try {
			int serverPort = findAvailablePort(startPort);
			
			if (serverPort == NO_PORT) {
				ourLogger.log(Level.SEVERE,
						"Couldn't find an available port for IGB to listen to control requests on port {0}!\nTurning off IGB's URL-based control features", startPort);
			}
			else {
				final ServerSocket serverSocket = new ServerSocket(serverPort);
				Runnable r = new Runnable() {
					
					@Override
					public void run() {
						new SimpleBookmarkServer(igbService, serverSocket);
					}
				};
		
				final Thread t = new Thread(r);
		
				SwingUtilities.invokeLater(new Runnable() {
		
					@Override
					public void run() {
						t.start();
					}
				});
			}
			
		} catch (IOException ex) {
			ourLogger.log(Level.SEVERE, "I/O Problem", ex);
		}
	}

	/**
	 * Find an available port.
	 * Start with the default_server_point and incrementing up from there.
	 * @return port found
	 */
	private static int findAvailablePort(int startPort) {
		// 
		int ports_tried = 0;
		int serverPort = startPort - 1;
		boolean available_port_found = false;
		while ((!available_port_found) && (ports_tried < ports_to_try)) {
			serverPort++;
			URL test_url;
			try {
				test_url = new URL("http://localhost:" + serverPort
						+ "/" + SERVLET_NAME + "?ping=yes");
			} catch (MalformedURLException mfe) {
				return SimpleBookmarkServer.NO_PORT;
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
				ourLogger.log(Level.INFO,
						"Found available port for bookmark server: {0}", serverPort);
				available_port_found = true;
			}
		}

		if (available_port_found) {
			return serverPort;
		} else {
			return SimpleBookmarkServer.NO_PORT;
		}
	}
}
