package com.affymetrix.igb.osgi.service;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 *
 * @author hiralv
 */
public abstract class XServiceRegistrar<Z> extends SimpleServiceRegistrar implements BundleActivator {

    private final Class<Z> clazz;

    protected XServiceRegistrar(Class<Z> clazz) {
        this.clazz = clazz;
    }

    protected abstract ServiceRegistration<?>[] getServices(BundleContext bundleContext, Z zService) throws Exception;

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext) {
        try {
            return getServices(bundleContext, getService(bundleContext));
        } catch (Exception ex) {
            System.out.println(this.getClass().getName() + " - Exception in Activator.registerService() -> " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
        return null;
    }

    /**
     * waits (if necessary) for the zService, and then calls createPage
     *
     * @throws Exception
     */
    @Override
    protected void registerService(final BundleContext bundleContext) {
        if (bundleContext.getServiceReference(clazz) != null) {
            super.registerService(bundleContext);
        } else {
            ServiceTracker<Z, Object> serviceTracker = new ServiceTracker<Z, Object>(bundleContext, clazz.getName(), null) {
                @Override
                public Object addingService(ServiceReference<Z> zServiceReference) {
                    XServiceRegistrar.super.registerService(bundleContext);
                    return super.addingService(zServiceReference);
                }
            };
            serviceTracker.open();
        }
    }

    private Z getService(BundleContext bundleContext) throws Exception {
        ServiceReference<Z> zServiceReference = bundleContext.getServiceReference(clazz);
        Z zService = bundleContext.getService(zServiceReference);
        bundleContext.ungetService(zServiceReference);
        return zService;
    }
}
