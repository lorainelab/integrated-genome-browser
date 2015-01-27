package com.affymetrix.igb.plugins;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.affymetrix.igb.service.api.IGBService;
import com.affymetrix.igb.service.api.IgbTabPanel;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {

    private PluginsView pluginsView;

    @Override
    protected IgbTabPanel getPage(BundleContext bundleContext, IGBService igbService) {
        pluginsView = new PluginsView(igbService);
        pluginsView.setBundleContext(bundleContext);
        return pluginsView;
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        if (pluginsView != null) {
            pluginsView.deactivate();
        }
        super.stop(bundleContext);
    }
}
