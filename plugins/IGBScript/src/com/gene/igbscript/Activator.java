package com.gene.igbscript;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genoviz.swing.recordplayback.ScriptProcessor;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.ServiceRegistrar;

public class Activator extends ServiceRegistrar implements BundleActivator {
	
	@Override
	protected ServiceRegistration<?>[] registerService(BundleContext bundleContext, IGBService igbService) throws Exception {
		return new ServiceRegistration[]{
			bundleContext.registerService(ScriptProcessor.class, new IGBScriptProcessor(igbService), null)
		};
	}
}
