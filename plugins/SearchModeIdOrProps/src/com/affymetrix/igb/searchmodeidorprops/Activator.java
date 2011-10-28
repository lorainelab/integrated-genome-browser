package com.affymetrix.igb.searchmodeidorprops;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.ISearchMode;

public class Activator implements BundleActivator {
	private BundleContext bundleContext;
	private ServiceRegistration<ISearchMode> searchModeIDRegistration;
	private ServiceRegistration<ISearchMode> searchModePropsRegistration;
	private ServiceRegistration<RemoteSearchI> remoteSearchDAS2Registration;

	@SuppressWarnings("unchecked")
	private void registerService(ServiceReference<IGBService> igbServiceReference) {
        try
        {
        	IGBService igbService = bundleContext.getService(igbServiceReference);
    		searchModeIDRegistration = (ServiceRegistration<ISearchMode>) bundleContext.registerService(ISearchMode.class.getName(), new SearchModeID(igbService), null);
    		searchModePropsRegistration = (ServiceRegistration<ISearchMode>) bundleContext.registerService(ISearchMode.class.getName(), new SearchModeProps(igbService), null);
    		remoteSearchDAS2Registration = (ServiceRegistration<RemoteSearchI>) bundleContext.registerService(RemoteSearchI.class.getName(), new RemoteSearchDAS2(), null);
        }
        catch (Exception ex) {
            System.out.println(this.getClass().getName() + " - Exception in Activator.createPage() -> " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		this.bundleContext = bundleContext;
		ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, RemoteSearchI.class);
    	ServiceReference<IGBService> igbServiceReference = (ServiceReference<IGBService>) bundleContext.getServiceReference(IGBService.class.getName());

        if (igbServiceReference != null)
        {
        	registerService(igbServiceReference);
        }
        else
        {
        	ServiceTracker serviceTracker = new ServiceTracker(bundleContext, IGBService.class.getName(), null) {
        	    public Object addingService(ServiceReference igbServiceReference) {
        	    	registerService(igbServiceReference);
        	        return super.addingService(igbServiceReference);
        	    }
        	};
        	serviceTracker.open();
        }
    }

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		searchModeIDRegistration.unregister();
		searchModePropsRegistration.unregister();
		remoteSearchDAS2Registration.unregister();
	}
}
