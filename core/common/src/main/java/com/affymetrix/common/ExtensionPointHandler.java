package com.affymetrix.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;

public class ExtensionPointHandler<S> {

    private static final Map<Class<?>, ExtensionPointHandler<?>> handlerInstances = new HashMap<>();
    private final List<ExtensionPointListener<S>> listeners = new ArrayList<>();
    private final List<S> extensionPointImpls = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public static <Z> ExtensionPointHandler<Z> getExtensionPoint(final Class<Z> clazz) {
        return (ExtensionPointHandler<Z>) handlerInstances.get(clazz);
    }

    @SuppressWarnings("unchecked")
    public static <Z> ExtensionPointHandler<Z> getOrCreateExtensionPoint(final BundleContext bundleContext, final Class<Z> clazz) {
        ExtensionPointHandler<Z> existingExtensionPointHandler = (ExtensionPointHandler<Z>) handlerInstances.get(clazz);
        if (existingExtensionPointHandler != null) { // alreay created, return existing
            return existingExtensionPointHandler;
        }
        final ExtensionPointHandler<Z> extensionPointHandler = new ExtensionPointHandler<>();
        handlerInstances.put(clazz, extensionPointHandler);
        // register service - an extension point
        try {
            ServiceReference<Z>[] serviceReferences = (ServiceReference<Z>[]) bundleContext.getAllServiceReferences(clazz.getName(), null);
            if (serviceReferences != null) {
                for (ServiceReference<Z> serviceReference : serviceReferences) {
                    Z extensionPointImpl = bundleContext.getService(serviceReference);
                    extensionPointHandler.addExtensionPointImpl(extensionPointImpl);
                }
            }
            bundleContext.addServiceListener(
                    event -> {
                        ServiceReference<Z> serviceReference = (ServiceReference<Z>) event.getServiceReference();
                        Z extensionPointImpl = bundleContext.getService(serviceReference);
                        if (event.getType() == ServiceEvent.UNREGISTERING || event.getType() == ServiceEvent.MODIFIED || event.getType() == ServiceEvent.MODIFIED_ENDMATCH) {
                            extensionPointHandler.removeExtensionPointImpl(extensionPointImpl);
                            for (ExtensionPointListener<Z> listener : extensionPointHandler.listeners) {
                                listener.removeService(extensionPointImpl);
                            }
                        }
                        if (event.getType() == ServiceEvent.REGISTERED || event.getType() == ServiceEvent.MODIFIED) {
                            extensionPointHandler.addExtensionPointImpl(extensionPointImpl);
                            for (ExtensionPointListener<Z> listener : extensionPointHandler.listeners) {
                                listener.addService(extensionPointImpl);
                            }
                        }
                    }, "(objectClass=" + clazz.getName() + ")");
        } catch (InvalidSyntaxException x) {
            Logger.getLogger(ExtensionPointHandler.class.getName()).log(Level.WARNING, "error loading/unloading " + clazz.getName(), x.getMessage());
        }
        return extensionPointHandler;
    }

    public void addListener(ExtensionPointListener<S> listener) {
        getExtensionPointImpls().forEach(listener::addService);
        listeners.add(listener);
    }

    public void removeListener(ExtensionPointListener<S> listener) {
        listeners.remove(listener);
    }

    public List<S> getExtensionPointImpls() {
        return extensionPointImpls;
    }

    private void addExtensionPointImpl(S s) {
        extensionPointImpls.add(s);
    }

    private void removeExtensionPointImpl(S s) {
        extensionPointImpls.remove(s);
    }
}
