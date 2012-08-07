package com.gene.searchmodelength;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.ISearchModeSym;
import com.affymetrix.searchmodesymmetryfilter.SearchModeSymmetryFilter;

public class Activator implements BundleActivator {
	private BundleContext bundleContext;
	private ServiceRegistration<ISearchModeSym> searchModeLengthRegistration;

	private void registerService(ServiceReference<IGBService> igbServiceReference) {
        try
        {
        	IGBService igbService = bundleContext.getService(igbServiceReference);
    		searchModeLengthRegistration = bundleContext.registerService(ISearchModeSym.class, new SearchModeSymmetryFilter(igbService, new SymmetryFilterLength(), 9999999), null);
        }
        catch (Exception ex) {
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
        	ServiceTracker<IGBService, Object> serviceTracker = new ServiceTracker<IGBService, Object>(bundleContext, IGBService.class, null) {
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
		searchModeLengthRegistration.unregister();
	}
}
