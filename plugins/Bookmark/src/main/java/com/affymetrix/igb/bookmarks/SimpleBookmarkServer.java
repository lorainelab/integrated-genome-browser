/**
 * Copyright (c) 2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.bookmarks;

import aQute.bnd.annotation.component.Component;
import static com.affymetrix.igb.bookmarks.BookmarkConstants.DEFAULT_SERVER_PORT;
import java.io.IOException;
import java.net.Socket;
import javax.swing.SwingUtilities;
import org.lorainelab.igb.services.IgbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A very simple servlet that listens for bookmark commands as http GET
 * requests. The server reads only the single "GET" line from the header,
 * ignores all other input, returns no output, and closes the connection.
 */
@Component(immediate = true)
public final class SimpleBookmarkServer {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleBookmarkServer.class);
    private static final int NO_PORT = -1;
    private static final int PORTS_TO_TRY = 1;
    private static int SERVER_PORT = NO_PORT;

    public static void setServerPort(String portString) {
        try {
            int port = Integer.parseInt(portString);
            if (port > 0 && port <= 65535) {
                SERVER_PORT = port;
            } else {
                LOG.error("Invalid port number {}, must be between 0 and 65535", Integer.toString(port));
            }
        } catch (NumberFormatException x) {
            LOG.error("Invalid number {} for server_port", portString);
        }
    }

    // Use the Swing Thread to start a non-Swing thread
    // that will start the control server.
    // Thus the control server will be started only after current GUI stuff is finished,
    // but starting it won't cause the GUI to hang.
    public static void init(final IgbService igbService) {
        startServerSocket(igbService, DEFAULT_SERVER_PORT);
        if (SERVER_PORT != NO_PORT) {
            startServerSocket(igbService, SERVER_PORT);
        }
    }

    public static void startServerSocket(final IgbService igbService, int startPort) {
        try {
            final int serverPort = findAvailablePort(startPort);

            if (serverPort == NO_PORT) {
                LOG.error(
                        "Couldn't find an available port for IGB to listen to control requests on port {}!\nTurning off IGB's URL-based control features", Integer.toString(startPort));
            } else {

                Runnable r = () -> {
                    BookmarkHttpRequestHandler handler = new BookmarkHttpRequestHandler(igbService, serverPort);
                    try {
                        handler.start();
                    } catch (IOException ex) {
                        LOG.error("Could not start bookmark server, turning off IGB's URL-based control features.");
                    }
                };

                final Thread t = new Thread(r);

                SwingUtilities.invokeLater(t::start);
            }

        } catch (Exception ex) {
            LOG.error("I/O Problem", ex);
        }
    }

    /**
     * Find an available port. Start with the default_server_point and
     * incrementing up from there.
     *
     * @return port found
     */
    private static int findAvailablePort(int startPort) {
        for (int i = 0; i < PORTS_TO_TRY; i++) {
            if (isPortAvailable(startPort)) {
                return startPort;
            }
            startPort++;
        }
        return NO_PORT;
    }

    /**
     * Returns the availability of a given port
     *
     * @param port
     * @return boolean
     */
    private static boolean isPortAvailable(int port) {
        LOG.debug("Testing port {}", Integer.toString(port));
        try (Socket s = new Socket("localhost", port);) {
            LOG.debug("Port {} is not available", Integer.toString(port));
            return false;
        } catch (IOException e) {
            LOG.debug("Port {} is available", Integer.toString(port));
            return true;
        }
    }

}
