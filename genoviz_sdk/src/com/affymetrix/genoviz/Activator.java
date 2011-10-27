package com.affymetrix.genoviz;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genoviz.swing.recordplayback.JRPWidgetDecorator;
import com.affymetrix.genoviz.swing.recordplayback.RecordPlaybackHolder;

/**
 * OSGi Activator for genoviz bundle
 */
public class Activator implements BundleActivator {
	protected BundleContext bundleContext;

	@Override
	public void start(BundleContext _bundleContext) throws Exception {
		bundleContext = _bundleContext;
		ExtensionPointHandler.addExtensionPoint(bundleContext,
			new ExtensionPointHandler<JRPWidgetDecorator>() {
				@Override
				public void addService(JRPWidgetDecorator decorator) {
					RecordPlaybackHolder.getInstance().addDecorator(decorator);
				}
				@Override
				public void removeService(JRPWidgetDecorator decorator) {}
			}
		);
		bundleContext.registerService(RecordPlaybackHolder.class.getName(), RecordPlaybackHolder.getInstance(), null);
	}

	@Override
	public void stop(BundleContext _bundleContext) throws Exception {}
}
