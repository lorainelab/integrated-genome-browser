package com.affymetrix.igb.search;

import org.osgi.framework.BundleActivator;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.common.ExtensionPointListener;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.shared.ISearchModeGlyph;
import com.affymetrix.igb.shared.ISearchModeSym;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	@Override
	protected IGBTabPanel getPage(IGBService igbService) {
		ExtensionPointHandler<ISearchModeSym> extensionPointSym = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ISearchModeSym.class);
		ExtensionPointHandler<ISearchModeGlyph> extensionPointGlyph = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ISearchModeGlyph.class);
		final SearchView searchView = new SearchView(igbService);
		extensionPointSym.addListener(new ExtensionPointListener<ISearchModeSym>() {
			@Override
			public void removeService(ISearchModeSym searchMode) {
				searchView.initSearchCB();
			}
			@Override
			public void addService(ISearchModeSym searchMode) {
				searchView.initSearchCB();
			}
		});
		extensionPointGlyph.addListener(new ExtensionPointListener<ISearchModeGlyph>() {
			@Override
			public void removeService(ISearchModeGlyph searchMode) {
				searchView.initSearchCB();
			}
			@Override
			public void addService(ISearchModeGlyph searchMode) {
				searchView.initSearchCB();
			}
		});
		return searchView;
	}
}
