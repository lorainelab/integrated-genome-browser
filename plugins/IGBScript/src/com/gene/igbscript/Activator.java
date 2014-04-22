package com.gene.igbscript;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.igb.swing.ScriptProcessor;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.XServiceRegistrar;

public class Activator extends XServiceRegistrar<IGBService> implements BundleActivator {
	
	public Activator(){
		super(IGBService.class);
	}
	
	@Override
	protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IGBService igbService) throws Exception {
		return new ServiceRegistration[]{
			bundleContext.registerService(ScriptProcessor.class, new IGBScriptProcessor(igbService), null)
		};
	}
}
