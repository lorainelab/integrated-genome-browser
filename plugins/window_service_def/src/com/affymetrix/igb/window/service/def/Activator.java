package com.affymetrix.igb.window.service.def;

import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.affymetrix.igb.window.service.IWindowService;

public class Activator implements BundleActivator {

	private static BundleContext bundleContext;

	static BundleContext getContext() {
		return bundleContext;
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		Activator.bundleContext = bundleContext;
		WindowServiceDefaultImpl windowServiceDefaultImpl = new WindowServiceDefaultImpl();
		bundleContext.registerService(IWindowService.class.getName(), windowServiceDefaultImpl, new Properties());
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.bundleContext = null;
	}
}
