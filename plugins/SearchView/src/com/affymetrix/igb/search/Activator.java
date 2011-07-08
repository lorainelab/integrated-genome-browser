package com.affymetrix.igb.search;

import java.util.Properties;

import org.osgi.framework.BundleActivator;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.search.mode.ISearchMode;
import com.affymetrix.igb.search.mode.SearchModeHolder;
import com.affymetrix.igb.search.mode.SearchModeID;
import com.affymetrix.igb.search.mode.SearchModeProps;
import com.affymetrix.igb.search.mode.SearchModeResidue;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	@Override
	protected IGBTabPanel getPage(IGBService igbService) {
		final SearchView searchView = new SearchView(igbService);
		ExtensionPointHandler.addExtensionPoint(bundleContext,
			new ExtensionPointHandler(ISearchMode.class) {
				@Override
				public void addService(Object o) {
					SearchModeHolder.getInstance().addSearchMode((ISearchMode)o);
					searchView.initSearchCB();
				}
				@Override
				public void removeService(Object o) {
					SearchModeHolder.getInstance().removeSearchMode((ISearchMode)o);
					searchView.initSearchCB();
				}
			}
		);
		initSearchModes(igbService);
		return searchView;
	}

	private void initSearchModes(IGBService igbService) {
		bundleContext.registerService(ISearchMode.class.getName(), new SearchModeID(igbService), new Properties());
		bundleContext.registerService(ISearchMode.class.getName(), new SearchModeProps(igbService), new Properties());
		bundleContext.registerService(ISearchMode.class.getName(), new SearchModeResidue(igbService), new Properties());
	}
}
