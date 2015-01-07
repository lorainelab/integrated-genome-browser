package com.affymetrix.igb.osgi.service;

import com.affymetrix.common.CommonUtils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 *
 * @author hiralv
 */
public abstract class SimpleServiceRegistrar implements BundleActivator {
	private ServiceRegistration<?>[] registrations;
		
	protected abstract ServiceRegistration<?>[] getServices(BundleContext bundleContext) throws Exception;
	
	protected void registerService(final BundleContext bundleContext){
		try {
			registrations = getServices(bundleContext);
		} catch (Exception ex) {
			System.out.println(this.getClass().getName() + " - Exception in Activator.registerService() -> " + ex.getMessage());
			ex.printStackTrace(System.out);
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
