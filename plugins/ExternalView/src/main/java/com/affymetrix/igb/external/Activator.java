package com.affymetrix.igb.external;

import org.osgi.framework.BundleActivator;

import com.affymetrix.igb.service.api.IGBService;
import com.affymetrix.igb.service.api.IGBTabPanel;
import com.affymetrix.genoviz.swing.AMenuItem;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.affymetrix.igb.window.service.WindowActivator;
import org.osgi.framework.BundleContext;

public class Activator extends WindowActivator implements BundleActivator {

    private static final int VIEW_MENU_POS = 3;

    @Override
    protected IGBTabPanel getPage(BundleContext bundleContext, IGBService igbService) {
        UCSCViewAction ucscViewAction = new UCSCViewAction(igbService);
        JRPMenuItem menuItem = new JRPMenuItem("ExternalViewer_ucscView", ucscViewAction);
        bundleContext.registerService(AMenuItem.class, new AMenuItem(menuItem, "view", VIEW_MENU_POS), null);
        return new ExternalViewer(igbService, ucscViewAction);
    }
}
