package com.affymetrix.igb.restrictions;

import javax.swing.JComponent;

import org.osgi.framework.BundleActivator;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	@Override
	protected JComponent getPage(IGBService igbService) {
		return new RestrictionControlView(igbService);
	}

	@Override
	protected String getName() {
        return RestrictionControlView.BUNDLE.getString("restrictionSitesTab");
	}

	@Override
	protected String getTitle() {
        return RestrictionControlView.BUNDLE.getString("restrictionSitesTab");
	}
}
