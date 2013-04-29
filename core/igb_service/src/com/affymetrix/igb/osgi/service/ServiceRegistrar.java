package com.affymetrix.igb.osgi.service;

import com.affymetrix.common.CommonUtils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This is the main Activator for all bundles.
 * Those bundles have an Activator that extends this class
 * and they only need to implement the registerService() method
 */
public abstract class ServiceRegistrar implements BundleActivator {
	protected BundleContext bundleContext;
	protected ServiceRegistration<?> registrations[];
	
	protected abstract ServiceRegistration<?>[] registerService(IGBService igbService) throws Exception;
	
	/**
	 * once the IGBService is available, we can register service with OSGi
	 * @param igbServiceReference the ServiceReference for the IGBService
	 */
	private void registerService(ServiceReference<IGBService> igbServiceReference) {
        try
        {
        	IGBService igbService = bundleContext.getService(igbServiceReference);
			registrations = registerService(igbService);
			bundleContext.ungetService(igbServiceReference);
        }
        catch (Exception ex) {
            System.out.println(this.getClass().getName() + " - Exception in Activator.registerService() -> " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
	}

	/**
	 * waits (if necessary) for the igbService, and then calls createPage 
	 * @throws Exception
	 */
	private void registerService() throws Exception {
		ServiceReference<IGBService> igbServiceReference = bundleContext.getServiceReference(IGBService.class);

		if (igbServiceReference != null) {
			registerService(igbServiceReference);
		} else {
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
	public void start(BundleContext bundleContext) throws Exception {
		this.bundleContext = bundleContext;
		if (CommonUtils.getInstance().isExit(bundleContext)) {
    		return;
    	}

    	registerService();
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

