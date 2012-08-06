package com.gene.overlapannotationoperator;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genometryImpl.operator.Operator;

public class Activator implements BundleActivator {
	private ServiceRegistration<Operator> overlapAnnotationOperatorRegistration;

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		overlapAnnotationOperatorRegistration = bundleContext.registerService(Operator.class, new OverlapAnnotationOperator(), null);
    }

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		overlapAnnotationOperatorRegistration.unregister();
	}
}
