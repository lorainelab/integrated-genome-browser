package com.affymetrix.common;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * OSGi Activator for common bundle
 */
public class Activator implements BundleActivator {
	protected BundleContext bundleContext;

	@Override
	public void start(BundleContext _bundleContext) throws Exception {
		this.bundleContext = _bundleContext;
		CommonUtils.getInstance().setBundleContext(_bundleContext);
	}

	@Override
	public void stop(BundleContext _bundleContext) throws Exception {}
}
