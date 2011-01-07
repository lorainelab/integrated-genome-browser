package com.affymetrix.igb.external;

import javax.swing.JComponent;

import org.osgi.framework.BundleActivator;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	@Override
	protected JComponent getPage(IGBService igbService) {
		return new ExternalViewer(igbService);
	}

	@Override
	protected String getName() {
        return ExternalViewer.BUNDLE.getString("externalViewTab");
	}

	@Override
	protected String getTitle() {
        return ExternalViewer.BUNDLE.getString("externalViewTab");
	}
}
