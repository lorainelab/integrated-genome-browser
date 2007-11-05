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

import com.affymetrix.igb.Application;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 *  A very simple servlet that listens for bookmark commands as http GET requests.
 *  The server reads only the single "GET" line from the header,
 *  ignores all other input, returns no output, and closes the connection.
 */
public class SimpleBookmarkServer {
  public static final int default_server_port = 7085;

  /** The OLD name of the IGB servlet, "UnibrowControl". */
  public final static String SERVLET_NAME_OLD = "UnibrowControl";

  /** The current name of the IGB servlet, "IGBControl". Current versions of
   *  IGB will respond to both this and {@link #SERVLET_NAME_OLD}, but versions
   *  up to and including 4.56 will respond ONLY to the old name.
   */
  public final static String SERVLET_NAME = "IGBControl";
  static int ports_to_try = 5;
  int server_port;

  /** The basic localhost URL that starts a call to IGB; for backwards-compatibility
   *  with versions of IGB 4.56 and earlier, the old name {@link #SERVLET_NAME_OLD}
   *  is used.
   */
  public static final String DEFAULT_SERVLET_URL = "http://localhost:"
      + default_server_port + "/" + SERVLET_NAME_OLD;

  public static final String NO_CONTENT = "HTTP/1.x 204 No Content\r\n\r\n";

  public SimpleBookmarkServer(Application app) {
    try {

      server_port = findAvailablePort();

      if (server_port == -1) {
        Application.logError(
            "Couldn't find an available port for IGB to listen to control requests!\n"
        + "Turning off IGB's URL-based control features");
      }
      else {
        ServerSocket server = new ServerSocket(server_port);


        while (true) {
          Socket socket = server.accept();
          Application.logDebug("Connection accepted " +
                  socket.getInetAddress() +
                  ":" + socket.getPort());

          BookmarkHttpRequestHandler request = new BookmarkHttpRequestHandler(app, socket);
          Thread thread = new Thread(request);
          thread.start();
        }
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public int getAssignedPort() {
    return server_port;
  }

  int findAvailablePort() {
    // find an available port, starting with the default_server_point and
      //   incrementing up from there...
    int ports_tried = 0;
    server_port = default_server_port - 1;
    boolean available_port_found = false;
    while ((!available_port_found) && (ports_tried < ports_to_try)) {
      server_port++;
      URL test_url;
      try {
        test_url = new URL("http://localhost:" + server_port +
                "/" + SERVLET_NAME + "?ping=yes");
      } catch (MalformedURLException mfe) {
        return -1;
      }


      try {
        // try and find an open port...
        URLConnection conn = test_url.openConnection();
        conn.connect();
        // if connection is successful, that means we cannot use that port
        // and must try another one.
        ports_tried++;
      } catch (IOException ex) {
        Application.logInfo("Found available port for bookmark server: " +
                server_port);
        available_port_found = true;
      }
    }

    if (available_port_found) {
      return server_port;
    } else {
      return -1;
    }
  }

  /**
   *  Look for another instance of IGB listening on another localhost port.
   */
  public int findDifferentUnibrowPort() {
    int this_port = getAssignedPort();
    int min_port = default_server_port;
    int max_port = min_port + ports_to_try;
    int other_igb_port = -1;
    for (int current_port=min_port; current_port<max_port; current_port++) {
      if (current_port != this_port) {
        try {
          URL test_url = new URL("http://localhost:" + current_port + "/"+SERVLET_NAME+"?ping=yes");
          URLConnection conn = test_url.openConnection();
          conn.connect();
          Application.logInfo("Found another igb listening at port: " + current_port);
          other_igb_port = current_port;
          break;
        }
        catch (Exception ex) {
          Application.logError("No igb found at port: " + current_port);
        }
      }
    }
    return other_igb_port;
  }
}
