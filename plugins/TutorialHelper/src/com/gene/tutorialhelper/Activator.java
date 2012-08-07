package com.gene.tutorialhelper;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.affymetrix.genoviz.swing.recordplayback.JRPWidgetDecorator;

public class Activator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		bundleContext.registerService(JRPWidgetDecorator.class, new WidgetIdTooltip(), null);
	}

	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}
}
