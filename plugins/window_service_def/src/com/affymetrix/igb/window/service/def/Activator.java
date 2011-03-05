package com.affymetrix.igb.window.service.def;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.window.service.IWindowService;

public class Activator implements BundleActivator {
	private static final String SERVICE_FILTER = "(objectClass=" + IGBTabPanel.class.getName() + ")";

	private static BundleContext bundleContext;

	/**
	 * standard getter
	 * @return the bundle context
	 */
	static BundleContext getContext() {
		return bundleContext;
	}

	@Override
	public void start(BundleContext _bundleContext) throws Exception {
		Activator.bundleContext = _bundleContext;
		final WindowServiceDefaultImpl windowServiceDefaultImpl = new WindowServiceDefaultImpl();
		bundleContext.registerService(IWindowService.class.getName(), windowServiceDefaultImpl, new Properties());
		ServiceReference[] serviceReferences = bundleContext.getAllServiceReferences(IGBTabPanel.class.getName(), null);
		if (serviceReferences != null) {
			for (ServiceReference serviceReference : serviceReferences) {
				windowServiceDefaultImpl.addTab((IGBTabPanel)bundleContext.getService(serviceReference));
			}
		}
		try {
			bundleContext.addServiceListener(
				new ServiceListener() {
					@Override
					public void serviceChanged(ServiceEvent event) {
						ServiceReference serviceReference = event.getServiceReference();
						if (event.getType() == ServiceEvent.UNREGISTERING || event.getType() == ServiceEvent.MODIFIED || event.getType() == ServiceEvent.MODIFIED_ENDMATCH) {
							windowServiceDefaultImpl.removeTab((IGBTabPanel)bundleContext.getService(serviceReference));
						}
						if (event.getType() == ServiceEvent.REGISTERED || event.getType() == ServiceEvent.MODIFIED) {
							windowServiceDefaultImpl.addTab((IGBTabPanel)bundleContext.getService(serviceReference));
						}
					}
				}
			, SERVICE_FILTER);
		}
		catch (InvalidSyntaxException x) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "error loading IGBTabPanels", x.getMessage());
		}
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.bundleContext = null;
	}
}
