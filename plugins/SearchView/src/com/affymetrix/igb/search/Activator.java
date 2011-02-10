package com.affymetrix.igb.search;

import javax.swing.JComponent;

import org.osgi.framework.BundleActivator;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	@Override
	protected JComponent getPage(IGBService igbService) {
		return new SearchView(igbService);
	}

	@Override
	protected String getName() {
        return SearchView.BUNDLE.getString("searchTab");
	}

	@Override
	protected String getTitle() {
        return SearchView.BUNDLE.getString("searchTab");
	}
}
