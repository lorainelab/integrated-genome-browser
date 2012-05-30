package com.affymetrix.genoviz;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.common.ExtensionPointListener;
import com.affymetrix.genoviz.swing.recordplayback.JRPWidgetDecorator;
import com.affymetrix.genoviz.swing.recordplayback.ScriptManager;

/**
 * OSGi Activator for genoviz bundle
 */
public class Activator implements BundleActivator {
	protected BundleContext bundleContext;

	@Override
	public void start(BundleContext _bundleContext) throws Exception {
		bundleContext = _bundleContext;
    	if (CommonUtils.getInstance().isExit(bundleContext)) {
    		return;
    	}
		ExtensionPointHandler<JRPWidgetDecorator> extensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, JRPWidgetDecorator.class);
		extensionPoint.addListener(new ExtensionPointListener<JRPWidgetDecorator>() {
			
			@Override
			public void removeService(JRPWidgetDecorator decorator) {}
			
			@Override
			public void addService(JRPWidgetDecorator decorator) {
				ScriptManager.getInstance().addDecorator(decorator);
			}
		});
		bundleContext.registerService(ScriptManager.class, ScriptManager.getInstance(), null);
	}

	@Override
	public void stop(BundleContext _bundleContext) throws Exception {}
}
