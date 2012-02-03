package com.affymetrix.searchmodesymmetryfilter;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

//import com.affymetrix.genometryImpl.filter.SymmetryFilterSearchId;
import com.affymetrix.genometryImpl.filter.SymmetryFilterProps;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.ISearchModeSym;

public class Activator implements BundleActivator {
	private BundleContext bundleContext;
	private List<ServiceRegistration<ISearchModeSym>> searchModeSymmetryFilterRegistrations = new ArrayList<ServiceRegistration<ISearchModeSym>>();

	private void registerService(ServiceReference<IGBService> igbServiceReference) {
        try
        {
        	IGBService igbService = bundleContext.getService(igbServiceReference);
			System.out.println("searchModeSymmetryFilterRegistrations.registerService");
        	searchModeSymmetryFilterRegistrations.add(bundleContext.registerService(ISearchModeSym.class, new SearchModeSymmetryFilter(igbService, new SymmetryFilterProps(), 2000), null));
//        	searchModeSymmetryFilterRegistrations.add(bundleContext.registerService(ISearchModeSym.class, new SearchModeSymmetryFilter(igbService, new SymmetryFilterSearchId(), 4000), null));
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
        	ServiceTracker<IGBService,Object> serviceTracker = new ServiceTracker<IGBService,Object>(bundleContext, IGBService.class.getName(), null) {
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
		for (ServiceRegistration<ISearchModeSym> searchModeSymmetryFilterRegistration : searchModeSymmetryFilterRegistrations) {
			searchModeSymmetryFilterRegistration.unregister();
		}
	}
}
