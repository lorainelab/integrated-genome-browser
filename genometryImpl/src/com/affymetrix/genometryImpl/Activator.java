package com.affymetrix.genometryImpl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.common.ExtensionPointListener;
import com.affymetrix.genometryImpl.filter.SymmetryFilterProps;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.operator.annotation.*;
import com.affymetrix.genometryImpl.operator.graph.*;
import com.affymetrix.genometryImpl.operator.transform.*;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.util.ServerTypeI;

/**
 * OSGi Activator for genometry bundle
 */
public class Activator implements BundleActivator {
	protected BundleContext bundleContext;

	@Override
	public void start(BundleContext _bundleContext) throws Exception {
		bundleContext = _bundleContext;
		initFileTypeHandlers();
		initServerTypes();
		initTransforms();
		initGraphOperators();
		initAnnotationOperators();
		initOperators();
	}

	@Override
	public void stop(BundleContext _bundleContext) throws Exception {}

	private void initFileTypeHandlers() {
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
	}

	private void initServerTypes() {
		ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ServerTypeI.class);
		bundleContext.registerService(ServerTypeI.class, ServerTypeI.LocalFiles, null);
		bundleContext.registerService(ServerTypeI.class, ServerTypeI.QuickLoad, null);
		bundleContext.registerService(ServerTypeI.class, ServerTypeI.DAS, null);
		bundleContext.registerService(ServerTypeI.class, ServerTypeI.DAS2, null);
	}

	private void initTransforms() {
		bundleContext.registerService(FloatTransformer.class, new IdentityTransform(), null);
		bundleContext.registerService(FloatTransformer.class, new LogTransform(2.0), null);
		bundleContext.registerService(FloatTransformer.class, new LogTransform(10.0), null);
		bundleContext.registerService(FloatTransformer.class, new LogTransform(Math.E), null);
		bundleContext.registerService(FloatTransformer.class, new LogTransform(), null);
		bundleContext.registerService(FloatTransformer.class, new InverseLogTransform(2.0), null);
		bundleContext.registerService(FloatTransformer.class, new InverseLogTransform(10.0), null);
		bundleContext.registerService(FloatTransformer.class, new InverseLogTransform(Math.E), null);
		bundleContext.registerService(FloatTransformer.class, new InverseLogTransform(), null);
	}

	private void initGraphOperators() {
		bundleContext.registerService(GraphOperator.class, new DiffOperator(), null);
		bundleContext.registerService(GraphOperator.class, new ProductOperator(), null);
		bundleContext.registerService(GraphOperator.class, new RatioOperator(), null);
		bundleContext.registerService(GraphOperator.class, new SumOperator(), null);
		bundleContext.registerService(GraphOperator.class, new MinOperator(), null);
		bundleContext.registerService(GraphOperator.class, new MaxOperator(), null);
		bundleContext.registerService(GraphOperator.class, new MeanOperator(), null);
		bundleContext.registerService(GraphOperator.class, new MedianOperator(), null);
	}

	private void initAnnotationOperators() {
		bundleContext.registerService(AnnotationOperator.class, new ExclusiveAAnnotationOperator(), null);
		bundleContext.registerService(AnnotationOperator.class, new ExclusiveBAnnotationOperator(), null);
		bundleContext.registerService(AnnotationOperator.class, new IntersectionAnnotationOperator(), null);
		bundleContext.registerService(AnnotationOperator.class, new NotAnnotationOperator(), null);
		bundleContext.registerService(AnnotationOperator.class, new UnionAnnotationOperator(), null);
		bundleContext.registerService(AnnotationOperator.class, new XorAnnotationOperator(), null);
		bundleContext.registerService(AnnotationOperator.class, new CopyAnnotationOperator(), null);
	}

	private void initOperators() {
		ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, Operator.class);
		bundleContext.registerService(Operator.class, new com.affymetrix.genometryImpl.operator.CopyAnnotationOperator(), null);
		bundleContext.registerService(Operator.class, new com.affymetrix.genometryImpl.operator.CopyGraphOperator(), null);
		bundleContext.registerService(Operator.class, new com.affymetrix.genometryImpl.operator.FilterOperator(new SymmetryFilterProps()), null);
		bundleContext.registerService(Operator.class, new com.affymetrix.genometryImpl.operator.LogTransform(), null);
		bundleContext.registerService(Operator.class, new com.affymetrix.genometryImpl.operator.DepthOperator(), null);
		bundleContext.registerService(Operator.class, new com.affymetrix.genometryImpl.operator.NotOperator(), null);
		bundleContext.registerService(Operator.class, new com.affymetrix.genometryImpl.operator.SumOperator(), null);
	}
}
