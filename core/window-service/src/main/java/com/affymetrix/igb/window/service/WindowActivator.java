package com.affymetrix.igb.window.service;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.igb.service.api.IGBService;
import com.affymetrix.igb.service.api.IgbTabPanel;
import com.affymetrix.igb.service.api.IgbTabPanelI;
import com.affymetrix.igb.service.api.XServiceRegistrar;

/**
 * This is the main Activator for all tab panel bundles. Those bundles have an
 * Activator that extends this class and they only need to implement the
 * getPage() method
 */
public abstract class WindowActivator extends XServiceRegistrar<IGBService> implements BundleActivator {

    public WindowActivator() {
        super(IGBService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IGBService igbService) throws Exception {
        return new ServiceRegistration[]{
            bundleContext.registerService(IgbTabPanelI.class, getPage(bundleContext, igbService), null),};
    }

    /**
     * get the tab panel for the bundle
     *
     * @param igbService the IGBService implementation
     * @return the tab panel
     */
    protected abstract IgbTabPanel getPage(BundleContext bundleContext, IGBService igbService);

}
