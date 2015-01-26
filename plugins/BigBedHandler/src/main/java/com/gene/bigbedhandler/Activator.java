package com.gene.bigbedhandler;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genometry.parsers.FileTypeHandler;
import com.affymetrix.igb.service.api.SimpleServiceRegistrar;
import org.osgi.framework.BundleContext;

public class Activator extends SimpleServiceRegistrar implements BundleActivator {
	
	@Override
	protected ServiceRegistration<?>[] getServices(BundleContext bundleContext) throws Exception {
		return new ServiceRegistration[] {
			bundleContext.registerService(FileTypeHandler.class, new BigBedHandler(), null)
		};
	}
}
