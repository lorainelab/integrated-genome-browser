package com.gene.igb.window.service.dockingframes;

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
		WindowServiceDockingFramesImpl windowServiceDockingFramesImpl = new WindowServiceDockingFramesImpl();
		bundleContext.registerService(IWindowService.class.getName(), windowServiceDockingFramesImpl, new Properties());
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.bundleContext = null;
	}
}
