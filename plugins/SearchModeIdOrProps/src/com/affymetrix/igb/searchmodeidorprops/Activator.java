package com.affymetrix.igb.searchmodeidorprops;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.ISearchMode;

public class Activator implements BundleActivator {
	private BundleContext bundleContext;
	private ServiceRegistration<?> searchModeIDRegistration;
	private ServiceRegistration<?> searchModePropsRegistration;

	private void registerService(ServiceReference<?> igbServiceReference) {
        try
        {
        	IGBService igbService = (IGBService) bundleContext.getService(igbServiceReference);
    		searchModeIDRegistration = bundleContext.registerService(ISearchMode.class.getName(), new SearchModeID(igbService), null);
    		searchModePropsRegistration = bundleContext.registerService(ISearchMode.class.getName(), new SearchModeProps(igbService), null);
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
    	ServiceReference<?> igbServiceReference = bundleContext.getServiceReference(IGBService.class.getName());

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
	}
}
