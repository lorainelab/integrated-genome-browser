package com.affymetrix.genometryImpl;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.affymetrix.genometryImpl.operator.*;
import com.affymetrix.genometryImpl.operator.annotation.AnnotationOperator;
import com.affymetrix.genometryImpl.operator.annotation.ExclusiveAAnnotationOperator;
import com.affymetrix.genometryImpl.operator.annotation.ExclusiveBAnnotationOperator;
import com.affymetrix.genometryImpl.operator.annotation.IntersectionAnnotationOperator;
import com.affymetrix.genometryImpl.operator.annotation.NotAnnotationOperator;
import com.affymetrix.genometryImpl.operator.annotation.UnionAnnotationOperator;
import com.affymetrix.genometryImpl.operator.annotation.XorAnnotationOperator;
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
		// add all FileTypeHandler implementations to FileTypeHolder
		ServiceReference[] serviceReferences = bundleContext.getAllServiceReferences(FileTypeHandler.class.getName(), null);
		if (serviceReferences != null) {
			for (ServiceReference serviceReference : serviceReferences) {
				FileTypeHolder.getInstance().addFileTypeHandler((FileTypeHandler)bundleContext.getService(serviceReference));
			}
		}
		try {
			bundleContext.addServiceListener(
				// add / remove any FileTypeHandler implementations to FileTypeHolder dynamically
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
		initAnnotationOperators();
	}

	@Override
	public void stop(BundleContext _bundleContext) throws Exception {}


	private void initAnnotationOperators() {
		bundleContext.registerService(AnnotationOperator.class.getName(), new ExclusiveAAnnotationOperator(), new Properties());
		bundleContext.registerService(AnnotationOperator.class.getName(), new ExclusiveBAnnotationOperator(), new Properties());
		bundleContext.registerService(AnnotationOperator.class.getName(), new IntersectionAnnotationOperator(), new Properties());
		bundleContext.registerService(AnnotationOperator.class.getName(), new NotAnnotationOperator(), new Properties());
		bundleContext.registerService(AnnotationOperator.class.getName(), new UnionAnnotationOperator(), new Properties());
		bundleContext.registerService(AnnotationOperator.class.getName(), new XorAnnotationOperator(), new Properties());
	}
}
