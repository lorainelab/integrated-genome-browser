package com.affymetrix.igb.osgi.service;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public abstract class ExtensionPointHandler {
	private Class<?> clazz;
	public ExtensionPointHandler(Class<?> clazz) {
		super();
		this.clazz = clazz;
	}
	public String getClassName() {
		return clazz.getName();
	}
	public abstract void addService(Object o);
	public abstract void removeService(Object o);
	public static void addExtensionPoint(final BundleContext bundleContext, final ExtensionPointHandler serviceHandler) {
		// register service - an extension point
		try {
			ServiceReference[] serviceReferences = bundleContext.getAllServiceReferences(serviceHandler.getClassName(), null);
			if (serviceReferences != null) {
				for (ServiceReference serviceReference : serviceReferences) {
					serviceHandler.addService(bundleContext.getService(serviceReference));
				}
			}
			bundleContext.addServiceListener(
				new ServiceListener() {
					@Override
					public void serviceChanged(ServiceEvent event) {
						ServiceReference serviceReference = event.getServiceReference();
						if (event.getType() == ServiceEvent.UNREGISTERING || event.getType() == ServiceEvent.MODIFIED || event.getType() == ServiceEvent.MODIFIED_ENDMATCH) {
							serviceHandler.removeService(bundleContext.getService(serviceReference));
						}
						if (event.getType() == ServiceEvent.REGISTERED || event.getType() == ServiceEvent.MODIFIED) {
							serviceHandler.addService(bundleContext.getService(serviceReference));
						}
					}
				}
			, "(objectClass=" + serviceHandler.getClassName() + ")");
		}
		catch (InvalidSyntaxException x) {
			Logger.getLogger(ExtensionPointHandler.class.getName()).log(Level.WARNING, "error loading/unloading " + serviceHandler.getClassName(), x.getMessage());
		}
	}
}
