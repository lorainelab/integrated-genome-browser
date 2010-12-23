package com.affymetrix.igb.plugins;

import javax.swing.JComponent;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	private PluginsView pluginsView;

	@Override
	protected JComponent getPage(IGBService igbService) {
        pluginsView = new PluginsView(igbService);
        pluginsView.setBundleContext(bundleContext);
		return pluginsView;
	}

	@Override
	protected String getName() {
        return PluginsView.BUNDLE.getString("pluginsViewTab");
	}

	@Override
	protected String getTitle() {
        return PluginsView.BUNDLE.getString("pluginsViewTab");
	}

    public void stop(BundleContext bundleContext) throws Exception
    {
    	if (pluginsView != null) {
    		pluginsView.deactivate();
    	}
    	super.stop(bundleContext);
    }
}
