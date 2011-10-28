package com.affymetrix.igb.search;

import org.osgi.framework.BundleActivator;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.common.ExtensionPointListener;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.shared.ISearchMode;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	@Override
	protected IGBTabPanel getPage(IGBService igbService) {
		ExtensionPointHandler<ISearchMode> extensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ISearchMode.class);
		final SearchView searchView = new SearchView(igbService);
		extensionPoint.addListener(new ExtensionPointListener<ISearchMode>() {
			
			@Override
			public void removeService(ISearchMode searchMode) {
				searchView.initSearchCB();
			}
			
			@Override
			public void addService(ISearchMode searchMode) {
				searchView.initSearchCB();
			}
		});
		return searchView;
	}
}
