package com.affymetrix.igb.window.service.def;

import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.affymetrix.igb.window.service.IGetDefaultWindowService;
import com.affymetrix.igb.window.service.IWindowService;

public class Activator implements BundleActivator {

	private static BundleContext bundleContext;

	static BundleContext getContext() {
		return bundleContext;
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		Activator.bundleContext = bundleContext;
		final WindowServiceDefaultImpl windowServiceDefaultImpl = new WindowServiceDefaultImpl();
		bundleContext.registerService(IGetDefaultWindowService.class.getName(), 
			new IGetDefaultWindowService() {
				@Override
				public IWindowService getWindowService() {
					return windowServiceDefaultImpl;
				}
			},
			new Properties()
		);
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.bundleContext = null;
	}
}
