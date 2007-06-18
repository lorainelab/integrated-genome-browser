/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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

package com.affymetrix.igb.servlets;

import java.net.*;
import org.mortbay.http.*;
import org.mortbay.jetty.servlet.*;

import com.affymetrix.igb.Application;

/**
 *  A tiny server that's integrated into IGB, to listen to
 *     a particular port for IGB control requests to UnibrowControlServlet.
 */
public class UnibrowControlServer {
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
      + UnibrowControlServer.default_server_port +
      "/" + UnibrowControlServer.SERVLET_NAME_OLD;

  public UnibrowControlServer(Application app) {
    try {

      // find an available port, starting with the default_server_point and
      //   incrementing up from there...
      int ports_tried = 0;
      server_port = default_server_port - 1;
      boolean available_port_found = false;
      while ( (!available_port_found) && (ports_tried < ports_to_try)) {
        server_port++;
        try {
          // try and find an open port...
          //      System.out.println("trying port: " + server_port);
          URL test_url = new URL("http://localhost:" + server_port +
                                 "/"+SERVLET_NAME+"?ping=yes");
          URLConnection conn = test_url.openConnection();
          conn.connect();
          //      System.out.println("port not available: " + server_port);
          available_port_found = false;
          ports_tried++;
        }
        catch (Exception ex) {
          System.out.println("found available port for UnibrowControl: " +
                             server_port);
          available_port_found = true;
        }
      }
      if (!available_port_found) {
        System.err.println(
            "couldn't find an available port for IGB to listen to control requests!");
        System.err.println("turning off IGB's URL-based control features");
        server_port = -1;
      }
      else {
        final HttpServer server = new HttpServer();
        // Create a port listener
        SocketListener listener = new SocketListener();
        listener.setPort(server_port);
        server.addListener(listener);
        // Create a context
        HttpContext context = new HttpContext();
        context.setContextPath("/");
        // Create a servlet container
        ServletHandler servlets = new ServletHandler();
        context.addHandler(servlets);
        // Map a servlet onto the container
        ServletHolder sholder = servlets.addServlet(SERVLET_NAME, "/"+SERVLET_NAME+"/*",
                                                    "com.affymetrix.igb.servlets.UnibrowControlServlet");
        sholder.setInitOrder(1);
        ServletHolder sholder_old = servlets.addServlet(SERVLET_NAME, "/"+SERVLET_NAME_OLD+"/*",
                                                    "com.affymetrix.igb.servlets.UnibrowControlServlet");

        ServletHolder writeback_test_servlet = servlets.addServlet("Das2WritebackTester", "/Das2WritebackTester/*",
                                                    "com.affymetrix.igb.servlets.Das2WritebackDevel");

        server.addContext(context);

        // Start the http server
        server.start();

        // set the form content size limit high to allow for long urls
        System.setProperty("org.mortbay.http.HttpRequest.maxFormContentSize", "100000000");
        UnibrowControlServlet igb_controller = (UnibrowControlServlet)sholder.getServlet();
        igb_controller.setUnibrowInstance(app);
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public int getAssignedPort() {
    return server_port;
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
          //      System.out.println("port not available: " + server_port);
          System.out.println("found another igb listening at port: " + current_port);
          other_igb_port = current_port;
          break;
        }
        catch (Exception ex) {
          System.out.println("no igb found at port: " + current_port);
        }
      }
    }
    return other_igb_port;
  }

}
