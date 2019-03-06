package org.lorainelab.igb.appstore;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import org.osgi.framework.BundleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kkorey
 */
//@Component(immediate = true, provide = {AppManagerInitiator.class})
public class AppManagerInitiator {
/**
    
    private WebAppManager webAppManager;
    private static final Logger logger = LoggerFactory.getLogger(AppManagerInitiator.class);

    @Reference
    public void setWebAppManager(WebAppManager webAppManager) {
        this.webAppManager = webAppManager;
    }

    @Activate
    public void activate(BundleContext context) {
        //TODO: Get the port from config file
        logger.info("activate START");
        //IgbAppServerLauncher.setServerPort("7090");
        logger.info(webAppManager.toString());
        IgbAppServerLauncher.startServer(webAppManager);
        logger.info("activate DONE");
    }
*/
}
