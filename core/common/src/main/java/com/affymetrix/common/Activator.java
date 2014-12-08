package com.affymetrix.common;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

public class Activator implements BundleActivator {

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        if (CommonUtils.getInstance().isExit(bundleContext)) {
            bundleContext.addBundleListener(
                    new BundleListener() {
                        @Override
                        public void bundleChanged(BundleEvent evt) {
                            checkAllStarted(bundleContext);
                        }
                    }
            );
        }
    }

    private void checkAllStarted(BundleContext bundleContext) {
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
