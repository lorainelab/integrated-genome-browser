package com.affymetrix.igb.keywordsearch;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.common.ExtensionPointListener;
import com.affymetrix.igb.osgi.service.IGBService;
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
public class Activator implements BundleActivator {
	private BundleContext bundleContext;
	private ServiceRegistration<ISearchModeSym> kwSearch;
	
	private void registerService(ServiceReference<IGBService> igbServiceReference) {
		try {
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
			kwSearch = bundleContext.registerService(ISearchModeSym.class, keyWordSearch, null);

		} catch (Exception ex) {
			System.out.println(this.getClass().getName() + " - Exception in Activator.createPage() -> " + ex.getMessage());
			ex.printStackTrace(System.out);
		}
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		this.bundleContext = bundleContext;
    	ServiceReference<IGBService> igbServiceReference = bundleContext.getServiceReference(IGBService.class);
		
        if (igbServiceReference != null)
        {
        	registerService(igbServiceReference);
        }
        else
        {
        	ServiceTracker<IGBService, Object> serviceTracker = new ServiceTracker<IGBService, Object>(bundleContext, IGBService.class.getName(), null) {
				@Override
        	    public Object addingService(ServiceReference<IGBService> igbServiceReference) {
        	    	registerService(igbServiceReference);
        	        return super.addingService(igbServiceReference);
        	    }
        	};
        	serviceTracker.open();
        }
    }

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		kwSearch.unregister();
	}
}
