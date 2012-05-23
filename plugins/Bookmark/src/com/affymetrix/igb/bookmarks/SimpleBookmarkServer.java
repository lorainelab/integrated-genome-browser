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

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.IGBServerSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  A very simple servlet that listens for bookmark commands as http GET requests.
 *  The server reads only the single "GET" line from the header,
 *  ignores all other input, returns no output, and closes the connection.
 */
public final class SimpleBookmarkServer {
	final static String http_response = IGBServerSocket.http_response;
	final static String SERVLET_NAME_OLD = IGBServerSocket.SERVLET_NAME_OLD;
	final static String SERVLET_NAME = IGBServerSocket.SERVLET_NAME;
	final static String DEFAULT_SERVLET_URL = IGBServerSocket.DEFAULT_SERVLET_URL;
	
	public SimpleBookmarkServer(IGBService igbService) {
		try {

			if (IGBServerSocket.getServerSocket() == null) {
				Logger.getLogger(SimpleBookmarkServer.class.getName()).log(Level.SEVERE,
						"Couldn't find an available port for IGB to listen to control requests!\n"
						+ "Turning off IGB's URL-based control features");
			} else {
				ServerSocket server = IGBServerSocket.getServerSocket();

				while (true) {
					Socket socket = server.accept();
					Logger.getLogger(SimpleBookmarkServer.class.getName()).log(Level.FINE, "Connection accepted {0}:{1}",
							new Object[]{socket.getInetAddress(), socket.getPort()});
					BookmarkHttpRequestHandler request = new BookmarkHttpRequestHandler(igbService, socket);
					Thread thread = new Thread(request);
					thread.start();
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
