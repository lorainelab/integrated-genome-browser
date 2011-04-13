package com.affymetrix.genometryImpl;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;

/**
 * OSGi Activator for genometry bundle
 */
public class Activator implements BundleActivator {
	private static final String SERVICE_FILTER = "(objectClass=" + FileTypeHandler.class.getName() + ")";
	protected BundleContext bundleContext;

	@Override
	public void start(BundleContext _bundleContext) throws Exception {
		bundleContext = _bundleContext;
		// add all Parser implementations to ParserHolder
		ServiceReference[] serviceReferences = bundleContext.getAllServiceReferences(FileTypeHandler.class.getName(), null);
		if (serviceReferences != null) {
			for (ServiceReference serviceReference : serviceReferences) {
				FileTypeHolder.getInstance().addFileTypeHandler((FileTypeHandler)bundleContext.getService(serviceReference));
			}
		}
		try {
			bundleContext.addServiceListener(
				// add / remove any Parser implementations to ParserHolder dynamically
				new ServiceListener() {
					@Override
					public void serviceChanged(ServiceEvent event) {
						ServiceReference serviceReference = event.getServiceReference();
						if (event.getType() == ServiceEvent.UNREGISTERING || event.getType() == ServiceEvent.MODIFIED || event.getType() == ServiceEvent.MODIFIED_ENDMATCH) {
							FileTypeHolder.getInstance().removeFileTypeHandler((FileTypeHandler)bundleContext.getService(serviceReference));
						}
						if (event.getType() == ServiceEvent.REGISTERED || event.getType() == ServiceEvent.MODIFIED) {
							FileTypeHolder.getInstance().addFileTypeHandler((FileTypeHandler)bundleContext.getService(serviceReference));
						}
					}
				}
			, SERVICE_FILTER);
		}
		catch (InvalidSyntaxException x) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "error loading Parsers", x.getMessage());
		}
	}

	@Override
	public void stop(BundleContext _bundleContext) throws Exception {}
}
