package com.affymetrix.genometry;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.common.ExtensionPointListener;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * OSGi Activator for genometry bundle
 */
public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        setupGenometryServiceDependencyTracker(bundleContext);
    }

    @Override
    public void stop(BundleContext _bundleContext) throws Exception {
    }

    private void setupGenometryServiceDependencyTracker(final BundleContext bundleContext) {
        ServiceTracker<GenometryServiceDependencyManager, Object> dependencyTracker;
        dependencyTracker = new ServiceTracker<GenometryServiceDependencyManager, Object>(bundleContext, GenometryServiceDependencyManager.class, null) {

            @Override
            public Object addingService(ServiceReference<GenometryServiceDependencyManager> reference) {
                initGenericActions(bundleContext);
                return super.addingService(reference);
            }

        };
        dependencyTracker.open();
    }

    private void initGenericActions(BundleContext bundleContext) {
        // add all GenericAction implementations to GenericActionHolder
        ExtensionPointHandler<GenericAction> extensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, GenericAction.class);
        extensionPoint.addListener(new ExtensionPointListener<GenericAction>() {
            @Override
            public void addService(GenericAction genericAction) {
                GenericActionHolder.getInstance().addGenericAction(genericAction);
            }

            @Override
            public void removeService(GenericAction genericAction) {
                GenericActionHolder.getInstance().removeGenericAction(genericAction);
            }
        });
    }

}
