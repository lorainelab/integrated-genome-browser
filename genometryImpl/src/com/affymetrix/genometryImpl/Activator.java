package com.affymetrix.genometryImpl;

import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometryImpl.operator.annotation.*;
import com.affymetrix.genometryImpl.operator.graph.*;
import com.affymetrix.genometryImpl.operator.transform.*;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;

/**
 * OSGi Activator for genometry bundle
 */
public class Activator implements BundleActivator {
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
		ExtensionPointHandler.addExtensionPoint(bundleContext,
			new ExtensionPointHandler(FileTypeHandler.class) {
				@Override
				public void addService(Object o) {
					FileTypeHolder.getInstance().addFileTypeHandler((FileTypeHandler)o);
				}
				@Override
				public void removeService(Object o) {
					FileTypeHolder.getInstance().removeFileTypeHandler((FileTypeHandler)o);
				}
			}
		);
		initTransforms();
		initGraphOperators();
		initAnnotationOperators();
	}

	@Override
	public void stop(BundleContext _bundleContext) throws Exception {}

	private void initTransforms() {
		bundleContext.registerService(FloatTransformer.class.getName(), new IdentityTransform(), new Properties());
		bundleContext.registerService(FloatTransformer.class.getName(), new LogTransform(2.0), new Properties());
		bundleContext.registerService(FloatTransformer.class.getName(), new LogTransform(10.0), new Properties());
		bundleContext.registerService(FloatTransformer.class.getName(), new LogTransform(Math.E), new Properties());
		bundleContext.registerService(FloatTransformer.class.getName(), new LogTransform(), new Properties());
		bundleContext.registerService(FloatTransformer.class.getName(), new InverseLogTransform(2.0), new Properties());
		bundleContext.registerService(FloatTransformer.class.getName(), new InverseLogTransform(10.0), new Properties());
		bundleContext.registerService(FloatTransformer.class.getName(), new InverseLogTransform(Math.E), new Properties());
		bundleContext.registerService(FloatTransformer.class.getName(), new InverseLogTransform(), new Properties());
	}

	private void initGraphOperators() {
		bundleContext.registerService(GraphOperator.class.getName(), new DiffOperator(), new Properties());
		bundleContext.registerService(GraphOperator.class.getName(), new ProductOperator(), new Properties());
		bundleContext.registerService(GraphOperator.class.getName(), new RatioOperator(), new Properties());
		bundleContext.registerService(GraphOperator.class.getName(), new SumOperator(), new Properties());
		bundleContext.registerService(GraphOperator.class.getName(), new MinOperator(), new Properties());
		bundleContext.registerService(GraphOperator.class.getName(), new MaxOperator(), new Properties());
		bundleContext.registerService(GraphOperator.class.getName(), new MeanOperator(), new Properties());
		bundleContext.registerService(GraphOperator.class.getName(), new MedianOperator(), new Properties());
	}

	private void initAnnotationOperators() {
		bundleContext.registerService(AnnotationOperator.class.getName(), new ExclusiveAAnnotationOperator(), new Properties());
		bundleContext.registerService(AnnotationOperator.class.getName(), new ExclusiveBAnnotationOperator(), new Properties());
		bundleContext.registerService(AnnotationOperator.class.getName(), new IntersectionAnnotationOperator(), new Properties());
		bundleContext.registerService(AnnotationOperator.class.getName(), new NotAnnotationOperator(), new Properties());
		bundleContext.registerService(AnnotationOperator.class.getName(), new UnionAnnotationOperator(), new Properties());
		bundleContext.registerService(AnnotationOperator.class.getName(), new XorAnnotationOperator(), new Properties());
	}
}
