package com.gene.bigwighandler;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.igb.osgi.service.ServiceRegistrar;
import com.affymetrix.igb.osgi.service.IGBService;

public class Activator extends ServiceRegistrar implements BundleActivator {
	
	@Override
	protected ServiceRegistration<?>[] registerService(IGBService igbService) throws Exception {
		return new ServiceRegistration[] {
			bundleContext.registerService(FileTypeHandler.class, new BigWigHandler(), null)
		};
	}	

}
