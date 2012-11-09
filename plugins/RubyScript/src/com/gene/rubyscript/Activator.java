package com.gene.rubyscript;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genoviz.swing.recordplayback.ScriptProcessor;

public class Activator implements BundleActivator {
	protected ServiceRegistration<ScriptProcessor> scriptProcessorRegistration;
	
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		scriptProcessorRegistration = bundleContext.registerService(ScriptProcessor.class, new RubyScriptProcessor(), null);
    }

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		if(scriptProcessorRegistration != null){
			scriptProcessorRegistration.unregister();
			scriptProcessorRegistration = null;
		}
	}
}
