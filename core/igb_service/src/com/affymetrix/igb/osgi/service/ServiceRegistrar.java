package com.affymetrix.igb.osgi.service;

import com.affymetrix.common.CommonUtils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 *
 * @author hiralv
 */
public abstract class ServiceRegistrar implements BundleActivator {
	protected BundleContext bundleContext;
	protected ServiceRegistration<?> registrations[];
	
	private void registerService(ServiceReference<IGBService> igbServiceReference) {
        try
        {
        	IGBService igbService = bundleContext.getService(igbServiceReference);
			registrations = registerService(igbService);
        }
        catch (Exception ex) {
            System.out.println(this.getClass().getName() + " - Exception in Activator.registerService() -> " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
	}

	protected abstract ServiceRegistration<?>[] registerService(IGBService igbService) throws Exception;
	
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		this.bundleContext = bundleContext;
		if (CommonUtils.getInstance().isExit(bundleContext)) {
    		return;
    	}
		
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
		if(registrations != null){
			for(ServiceRegistration registration : registrations){
				registration.unregister();
			}
			registrations = null;
		}
		bundleContext = null;
	}
}

