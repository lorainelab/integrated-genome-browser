package com.affymetrix.igb.external;

import org.osgi.framework.BundleActivator;

import com.affymetrix.igb.service.api.IgbService;
import com.affymetrix.igb.service.api.IgbTabPanel;
import com.affymetrix.genoviz.swing.AMenuItem;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.affymetrix.igb.window.service.WindowActivator;
import org.osgi.framework.BundleContext;

public class Activator extends WindowActivator implements BundleActivator {

    private static final int VIEW_MENU_POS = 3;

    @Override
    protected IgbTabPanel getPage(BundleContext bundleContext, IgbService igbService) {
        UCSCViewAction ucscViewAction = new UCSCViewAction(igbService);
        JRPMenuItem menuItem = new JRPMenuItem("ExternalViewer_ucscView", ucscViewAction);
        bundleContext.registerService(AMenuItem.class, new AMenuItem(menuItem, "view", VIEW_MENU_POS), null);
        return new ExternalViewer(igbService, ucscViewAction);
    }
}
