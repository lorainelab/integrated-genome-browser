package com.gene.geometricmean;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genometryImpl.operator.Operator;

public class Activator implements BundleActivator {
	private ServiceRegistration geometricMeanRegistration;

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		geometricMeanRegistration = bundleContext.registerService(Operator.class, new GeometricMeanOperator(), null);
    }

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		geometricMeanRegistration.unregister();
	}
}
