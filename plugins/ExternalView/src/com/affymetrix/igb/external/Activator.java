package com.affymetrix.igb.external;

import org.osgi.framework.BundleActivator;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.genoviz.swing.AMenuItem;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenuItem;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	private static final int VIEW_MENU_POS = 2;
	
	@Override
	protected IGBTabPanel getPage(IGBService igbService) {
		UCSCViewAction ucscViewAction = new UCSCViewAction(igbService);
		JRPMenuItem menuItem = new JRPMenuItem("ExternalViewer_ucscView", ucscViewAction);
		bundleContext.registerService(AMenuItem.class, new AMenuItem(menuItem, "view" , VIEW_MENU_POS), null);
		return new ExternalViewer(igbService, ucscViewAction);
	}
}
