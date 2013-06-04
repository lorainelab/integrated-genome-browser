package com.affymetrix.igb.keywordsearch;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.common.ExtensionPointListener;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.ServiceRegistrar;
import com.affymetrix.igb.shared.IKeyWordSearch;
import com.affymetrix.igb.shared.ISearchModeSym;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 *
 * @author hiralv
 */
public class Activator  extends ServiceRegistrar implements BundleActivator {
	
	@Override
	protected ServiceRegistration<?>[] registerService(BundleContext bundleContext, IGBService igbService) throws Exception {
		ExtensionPointHandler<IKeyWordSearch> extensionPointKWS = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, IKeyWordSearch.class);
		final KeyWordSearch keyWordSearch = new KeyWordSearch();

		extensionPointKWS.addListener(new ExtensionPointListener<IKeyWordSearch>() {
			@Override
			public void removeService(IKeyWordSearch searchMode) {
				keyWordSearch.initSearchModes();
			}

			@Override
			public void addService(IKeyWordSearch searchMode) {
				keyWordSearch.initSearchModes();
			}
		});

		return new ServiceRegistration[] {
				bundleContext.registerService(ISearchModeSym.class, keyWordSearch, null)
			};
	}
}
