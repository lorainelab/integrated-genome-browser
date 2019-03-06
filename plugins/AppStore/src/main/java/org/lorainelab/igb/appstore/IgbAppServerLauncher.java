/**
 * Copyright (c) 2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package org.lorainelab.igb.appstore;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.SwingUtilities;
import java.io.IOException;
import org.osgi.framework.BundleContext;

/**
 * Launches IgbAppServer in a new thread to avoid blocking GUI. 
 * 
 * @author Kiran Korey, Ann Loraine, others TBN
 * 
 * To test:
 * 
 * Start IGB. Use Web browser or Postman to hit these endpoints:
 * 
 * Install an App:
 * localhost:7090/installApp?symbolicName=org.lorainelab.igb.protannot
 * 
 * Check for IGB running:
 * localhost:7090
 * 
 * Question: Do we really need to pass App version to REST endpoint for
 * everything to work properly? Maybe not.
 */
@Component(immediate=true)
public final class IgbAppServerLauncher {

    private static final Logger logger = LoggerFactory.getLogger(IgbAppServerLauncher.class);

    // injected by service component run-time (SCR)
    WebAppManager webAppManager;
    
    /**
     * Lets the SCR know that this Component requires one of these.
     * @param webAppManager 
     */
    @Reference
    private void setWebAppManager(WebAppManager webAppManager) {
        this.webAppManager = webAppManager;
    }
    
    
    /**
     * Invoked when the SCR container starts the bundle. The above Component
     * informs the SCR that this activate method should be called right away
     * after the containing bundle is loaded and its dependencies are satisfied.
     * @param context 
     */
    @Activate
    public void activate(BundleContext context) {
        //TODO: Get the port from config file
        logger.info("activate START");
        //IgbAppServerLauncher.setServerPort("7090");
        logger.info(webAppManager.toString());
        startServer(webAppManager);
        logger.info("activate DONE");
    }
    
    /**
     * Start the App management REST endpoint in a new thread to avoid blocking the
     * GUI. 
     * Pass the WebAppManager object which provides access to methods in
     * Plugin Manager bundle. 
     * Next:
     * Re-design so that those methods will be provided by a service owned
     * and implemented by Plugin Manager bundle. We only want to import
     * an interface, not implementation. If we do this properly, we can easily
     * test just the methods of this bundle. 
     */
    protected static void startServer(WebAppManager webAppManager) {
        Runnable r;
        r = () -> {
            IgbAppServer server = new IgbAppServer();
            server.setWebAppManager(webAppManager);
            //server.setWebApp(webApp);
            try {
                server.start();
                logger.info("Started REST endpoint.");
            } catch (IOException ex) {
                logger.error("Could not start REST endpoint.");
            }
        };
        final Thread t = new Thread(r);
        SwingUtilities.invokeLater(t::start);
    }

}
