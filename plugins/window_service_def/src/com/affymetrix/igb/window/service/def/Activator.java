package com.affymetrix.igb.window.service.def;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.IStopRoutine;
import com.affymetrix.igb.window.service.IWindowService;

public class Activator implements BundleActivator {
	private static final String SERVICE_FILTER = "(objectClass=" + IGBTabPanel.class.getName() + ")";
	private static final String TAB_PANEL_CATEGORY = "IGBTabPanel-";
	private static BundleContext bundleContext;

	/**
	 * standard getter
	 * @return the bundle context
	 */
	static BundleContext getContext() {
		return bundleContext;
	}

	private void addTab(ServiceReference<?> serviceReference, WindowServiceDefaultImpl windowServiceDefaultImpl, List<String> tabPanels) {
		IGBTabPanel panel = (IGBTabPanel)bundleContext.getService(serviceReference);
		windowServiceDefaultImpl.addTab(panel);
		tabPanels.remove(panel.getName());
		if (tabPanels.isEmpty()) {
			windowServiceDefaultImpl.showTabs();
		}
	}

	@Override
	public void start(BundleContext _bundleContext) throws Exception {
		Activator.bundleContext = _bundleContext;
		final List<String> tabPanels = new ArrayList<String>();
		Enumeration<String> bundleKeys = WindowServiceDefaultImpl.BUNDLE.getKeys();
		while (bundleKeys.hasMoreElements()) {
			String key = bundleKeys.nextElement();
			String value = WindowServiceDefaultImpl.BUNDLE.getString(key);
			if (value.contains(TAB_PANEL_CATEGORY)) {
				for (String part : value.split(";")) {
					if (part.startsWith(TAB_PANEL_CATEGORY)) {
						for (String className : part.substring(TAB_PANEL_CATEGORY.length()).split(",")) {
							tabPanels.add(className);
						}
					}
				}
			}
		}
		final WindowServiceDefaultImpl windowServiceDefaultImpl = new WindowServiceDefaultImpl();
		bundleContext.registerService(IWindowService.class.getName(), windowServiceDefaultImpl, null);
		ServiceReference<?>[] serviceReferences = bundleContext.getAllServiceReferences(IGBTabPanel.class.getName(), null);
		if (serviceReferences != null) {
			for (ServiceReference<?> serviceReference : serviceReferences) {
				addTab(serviceReference, windowServiceDefaultImpl, tabPanels);
			}
		}
		try {
			bundleContext.addServiceListener(
				new ServiceListener() {
					@Override
					public void serviceChanged(ServiceEvent event) {
						ServiceReference<?> serviceReference = event.getServiceReference();
						if (event.getType() == ServiceEvent.UNREGISTERING || event.getType() == ServiceEvent.MODIFIED || event.getType() == ServiceEvent.MODIFIED_ENDMATCH) {
							windowServiceDefaultImpl.removeTab((IGBTabPanel)bundleContext.getService(serviceReference));
						}
						if (event.getType() == ServiceEvent.REGISTERED || event.getType() == ServiceEvent.MODIFIED) {
							addTab(serviceReference, windowServiceDefaultImpl, tabPanels);
						}
					}
				}
			, SERVICE_FILTER);
		}
		catch (InvalidSyntaxException x) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, WindowServiceDefaultImpl.BUNDLE.getString("loadError"), x.getMessage());
		}
		bundleContext.registerService(IStopRoutine.class.getName(), 
			new IStopRoutine() {
				@Override
				public void stop() {
					windowServiceDefaultImpl.shutdown();
				}
			},
			null
		);
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.bundleContext = null;
	}
}
