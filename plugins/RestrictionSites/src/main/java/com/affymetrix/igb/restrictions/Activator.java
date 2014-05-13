package com.affymetrix.igb.restrictions;

import org.osgi.framework.BundleActivator;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.window.service.WindowActivator;
import org.osgi.framework.BundleContext;

public class Activator extends WindowActivator implements BundleActivator {

    @Override
    protected IGBTabPanel getPage(BundleContext bundleContext, IGBService igbService) {
        return new RestrictionControlView(igbService);
    }
}
