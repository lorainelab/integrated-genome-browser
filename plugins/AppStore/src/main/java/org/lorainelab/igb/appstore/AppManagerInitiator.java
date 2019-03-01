package org.lorainelab.igb.appstore;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import org.lorainelab.igb.services.IgbService;
import org.osgi.framework.BundleContext;

/**
 *
 * @author kkorey
 */
@Component(immediate = true, provide = {AppManagerInitiator.class})
public class AppManagerInitiator {

    private IgbService igbService;
    private BundleContext bundleContext;
    private WebAppManager webAppManager;

    @Reference
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    @Reference
    public void setWebAppManager(WebAppManager webAppManager) {
        this.webAppManager = webAppManager;
    }

    @Activate
    public void activate(BundleContext context) {
        this.bundleContext = context;
        //TODO: Get the port from config file
        SimpleAppMangerServer.setServerPort("7086");
        SimpleAppMangerServer.init(igbService,webAppManager);
    }


}
