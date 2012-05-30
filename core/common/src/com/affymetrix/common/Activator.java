package com.affymetrix.common;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

public class Activator implements BundleActivator {
	private BundleContext bundleContext;

	@Override
	public void start(BundleContext bundleContext_) throws Exception {
		this.bundleContext = bundleContext_;
        if (CommonUtils.getInstance().isExit(bundleContext_)) {
			bundleContext.addBundleListener(
				new BundleListener() {
					@Override
					public void bundleChanged(BundleEvent evt) {
						checkAllStarted();
					}
				}
			);
        }
    }

	private void checkAllStarted() {
		boolean allStarted = true;
		for (Bundle bundle : bundleContext.getBundles()) {
			if (bundle.getState() != Bundle.ACTIVE) {
				allStarted = false;
			}
		}
		if (allStarted) {
			System.exit(0);
		}
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
	}
}
