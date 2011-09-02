package com.affymetrix.igb.searchmoderesidue;

import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.ISearchMode;

public class Activator implements BundleActivator {
	private BundleContext bundleContext;
	private ServiceRegistration searchModeResidueRegistration;

	private void registerService(ServiceReference igbServiceReference) {
        try
        {
        	IGBService igbService = (IGBService) bundleContext.getService(igbServiceReference);
        	searchModeResidueRegistration = bundleContext.registerService(ISearchMode.class.getName(), new SearchModeResidue(igbService), new Properties());
        }
        catch (Exception ex) {
            System.out.println(this.getClass().getName() + " - Exception in Activator.createPage() -> " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		this.bundleContext = bundleContext;
    	ServiceReference igbServiceReference = bundleContext.getServiceReference(IGBService.class.getName());

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
		searchModeResidueRegistration.unregister();
	}
}
