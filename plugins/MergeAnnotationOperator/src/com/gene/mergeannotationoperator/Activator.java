package com.gene.mergeannotationoperator;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genometryImpl.operator.Operator;

public class Activator implements BundleActivator {
	private ServiceRegistration<Operator> mergeAnnotationOperatorRegistration;

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		mergeAnnotationOperatorRegistration = bundleContext.registerService(Operator.class, new MergeAnnotationOperator(), null);
    }

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		mergeAnnotationOperatorRegistration.unregister();
	}
}
