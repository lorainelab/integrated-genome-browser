package com.affymetrix.igb.restrictions;

import org.osgi.framework.BundleActivator;

import com.affymetrix.igb.service.api.IGBService;
import com.affymetrix.igb.service.api.IGBTabPanel;
import com.affymetrix.igb.window.service.WindowActivator;
import org.osgi.framework.BundleContext;

public class Activator extends WindowActivator implements BundleActivator {

    @Override
    protected IGBTabPanel getPage(BundleContext bundleContext, IGBService igbService) {
        return new RestrictionControlView(igbService);
    }
}
