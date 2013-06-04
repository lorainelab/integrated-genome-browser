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
public abstract class XServiceRegistrar<Z> implements BundleActivator {
	private final Class<Z> clazz;
	protected ServiceRegistration<?>[] registrations;
	
	protected XServiceRegistrar(Class<Z> clazz){
		this.clazz = clazz;
	}
	
	protected abstract ServiceRegistration<?>[] registerService(BundleContext bundleContext, Z zService) throws Exception;

	/**
	 * once the Z is available, we can register service with OSGi
	 * @param zServiceReference the ServiceReference for the IGBService
	 */
	protected void registerService(BundleContext bundleContext, ServiceReference<Z> zServiceReference) {
		try {
			Z igbService = bundleContext.getService(zServiceReference);
			registrations = registerService(bundleContext, igbService);
			bundleContext.ungetService(zServiceReference);
		} catch (Exception ex) {
			System.out.println(this.getClass().getName() + " - Exception in Activator.registerService() -> " + ex.getMessage());
			ex.printStackTrace(System.out);
		}
	}

	/**
	 * waits (if necessary) for the zService, and then calls createPage
	 * @throws Exception
	 */
	protected void registerService(final BundleContext bundleContext) throws Exception {
		ServiceReference<Z> zServiceReference = bundleContext.getServiceReference(clazz);
		if (zServiceReference != null) {
			registerService(bundleContext, zServiceReference);
		} else {
			ServiceTracker<Z, Object> serviceTracker = new ServiceTracker<Z, Object>(bundleContext, clazz.getName(), null) {
				@Override
				public Object addingService(ServiceReference<Z> zServiceReference) {
					registerService(bundleContext, zServiceReference);
					return super.addingService(zServiceReference);
				}
			};
			serviceTracker.open();
		}
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		if (CommonUtils.getInstance().isExit(bundleContext)) {
			return;
		}
		registerService(bundleContext);
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		if (registrations != null) {
			for (ServiceRegistration registration : registrations) {
				registration.unregister();
			}
			registrations = null;
		}
	}
    
}
