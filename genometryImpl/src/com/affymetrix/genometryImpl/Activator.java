package com.affymetrix.genometryImpl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.common.ExtensionPointListener;
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
		ExtensionPointHandler<FileTypeHandler> extensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, FileTypeHandler.class);
		extensionPoint.addListener(new ExtensionPointListener<FileTypeHandler>() {
			
			@Override
			public void removeService(FileTypeHandler fileTypeHandler) {
				FileTypeHolder.getInstance().removeFileTypeHandler(fileTypeHandler);
			}
			
			@Override
			public void addService(FileTypeHandler fileTypeHandler) {
				FileTypeHolder.getInstance().addFileTypeHandler(fileTypeHandler);
			}
		});
		initTransforms();
		initGraphOperators();
		initAnnotationOperators();
	}

	@Override
	public void stop(BundleContext _bundleContext) throws Exception {}

	private void initTransforms() {
		bundleContext.registerService(FloatTransformer.class.getName(), new IdentityTransform(), null);
		bundleContext.registerService(FloatTransformer.class.getName(), new LogTransform(2.0), null);
		bundleContext.registerService(FloatTransformer.class.getName(), new LogTransform(10.0), null);
		bundleContext.registerService(FloatTransformer.class.getName(), new LogTransform(Math.E), null);
		bundleContext.registerService(FloatTransformer.class.getName(), new LogTransform(), null);
		bundleContext.registerService(FloatTransformer.class.getName(), new InverseLogTransform(2.0), null);
		bundleContext.registerService(FloatTransformer.class.getName(), new InverseLogTransform(10.0), null);
		bundleContext.registerService(FloatTransformer.class.getName(), new InverseLogTransform(Math.E), null);
		bundleContext.registerService(FloatTransformer.class.getName(), new InverseLogTransform(), null);
	}

	private void initGraphOperators() {
		bundleContext.registerService(GraphOperator.class.getName(), new DiffOperator(), null);
		bundleContext.registerService(GraphOperator.class.getName(), new ProductOperator(), null);
		bundleContext.registerService(GraphOperator.class.getName(), new RatioOperator(), null);
		bundleContext.registerService(GraphOperator.class.getName(), new SumOperator(), null);
		bundleContext.registerService(GraphOperator.class.getName(), new MinOperator(), null);
		bundleContext.registerService(GraphOperator.class.getName(), new MaxOperator(), null);
		bundleContext.registerService(GraphOperator.class.getName(), new MeanOperator(), null);
		bundleContext.registerService(GraphOperator.class.getName(), new MedianOperator(), null);
	}

	private void initAnnotationOperators() {
		bundleContext.registerService(AnnotationOperator.class.getName(), new ExclusiveAAnnotationOperator(), null);
		bundleContext.registerService(AnnotationOperator.class.getName(), new ExclusiveBAnnotationOperator(), null);
		bundleContext.registerService(AnnotationOperator.class.getName(), new IntersectionAnnotationOperator(), null);
		bundleContext.registerService(AnnotationOperator.class.getName(), new NotAnnotationOperator(), null);
		bundleContext.registerService(AnnotationOperator.class.getName(), new UnionAnnotationOperator(), null);
		bundleContext.registerService(AnnotationOperator.class.getName(), new XorAnnotationOperator(), null);
		bundleContext.registerService(AnnotationOperator.class.getName(), new CopyAnnotationOperator(), null);
	}
}
