package com.affymetrix.genometry;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.common.ExtensionPointListener;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.operator.Operator;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.parsers.FileTypeHandler;
import com.affymetrix.genometry.parsers.FileTypeHolder;
import com.affymetrix.genometry.util.ServerTypeI;

/**
 * OSGi Activator for genometry bundle
 */
public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        if (CommonUtils.getInstance().isExit(bundleContext)) {
            return;
        }
        //bundleContext.registerService(WaitHelperI.class, CThreadHolder.getInstance(), null);
        initFileTypeHandlers(bundleContext);
        initGenericActions(bundleContext);
        initServerTypes(bundleContext);
        initOperators(bundleContext);
    }

    @Override
    public void stop(BundleContext _bundleContext) throws Exception {
    }

    private void initFileTypeHandlers(BundleContext bundleContext) {
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

    private void initGenericActions(BundleContext bundleContext) {
        // add all GenericAction implementations to GenericActionHolder
        ExtensionPointHandler<GenericAction> extensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, GenericAction.class);
        extensionPoint.addListener(new ExtensionPointListener<GenericAction>() {
            @Override
            public void addService(GenericAction genericAction) {
                GenericActionHolder.getInstance().addGenericAction(genericAction);
            }

            @Override
            public void removeService(GenericAction genericAction) {
                GenericActionHolder.getInstance().removeGenericAction(genericAction);
            }
        });
    }

    private void initServerTypes(BundleContext bundleContext) {
        ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ServerTypeI.class);
        bundleContext.registerService(ServerTypeI.class, ServerTypeI.LocalFiles, null);
        bundleContext.registerService(ServerTypeI.class, ServerTypeI.QuickLoad, null);
        bundleContext.registerService(ServerTypeI.class, ServerTypeI.DAS, null);
        bundleContext.registerService(ServerTypeI.class, ServerTypeI.DAS2, null);
    }

    private void initOperators(BundleContext bundleContext) {
        ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, Operator.class);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.ComplementSequenceOperator(), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.CopyGraphOperator(), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.CopyMismatchOperator(), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.CopySequenceOperator(), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.CopyXOperator(FileTypeCategory.Alignment), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.CopyXOperator(FileTypeCategory.Annotation), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.CopyXOperator(FileTypeCategory.ProbeSet), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.DepthOperator(FileTypeCategory.Alignment), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.DepthOperator(FileTypeCategory.Annotation), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.DepthOperator(FileTypeCategory.ProbeSet), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.StartDepthOperator(FileTypeCategory.Alignment), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.StartDepthOperator(FileTypeCategory.Annotation), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.StartDepthOperator(FileTypeCategory.ProbeSet), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.SummaryOperator(FileTypeCategory.Annotation), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.SummaryOperator(FileTypeCategory.Alignment), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.SummaryOperator(FileTypeCategory.ProbeSet), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.NotOperator(FileTypeCategory.Annotation), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.NotOperator(FileTypeCategory.Alignment), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.NotOperator(FileTypeCategory.ProbeSet), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.DiffOperator(), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.ExclusiveAOperator(FileTypeCategory.Annotation), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.ExclusiveAOperator(FileTypeCategory.Alignment), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.ExclusiveAOperator(FileTypeCategory.ProbeSet), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.ExclusiveBOperator(FileTypeCategory.Annotation), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.ExclusiveBOperator(FileTypeCategory.Alignment), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.ExclusiveBOperator(FileTypeCategory.ProbeSet), null);
		//bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.FilterOperator(FileTypeCategory.Annotation, new SymmetryFilterProps()), null);
        //bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.IdentityTransform(), null); //Same as copy graph operator
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.IntersectionOperator(FileTypeCategory.Annotation), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.IntersectionOperator(FileTypeCategory.Alignment), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.IntersectionOperator(FileTypeCategory.ProbeSet), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.InverseTransformer(), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.InverseLogTransform(), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.InverseLogTransform(Math.E), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.InverseLogTransform(2.0), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.InverseLogTransform(10.0), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.LogTransform(), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.LogTransform(Math.E), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.LogTransform(2.0), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.LogTransform(10.0), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.PowerTransformer(), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.PowerTransformer(0.5), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.MaxOperator(), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.MeanOperator(), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.MedianOperator(), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.MinOperator(), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.ProductOperator(), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.RatioOperator(), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.SumOperator(), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.UnionOperator(FileTypeCategory.Alignment), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.UnionOperator(FileTypeCategory.Annotation), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.UnionOperator(FileTypeCategory.ProbeSet), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.XorOperator(FileTypeCategory.Alignment), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.XorOperator(FileTypeCategory.Annotation), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.XorOperator(FileTypeCategory.ProbeSet), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.GraphMultiplexer(), null);
//		bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.FindJunctionOperator(false), null);
//		bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.FindJunctionOperator(true), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.AddMathTransform(), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.DivideMathTransform(), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.MultiplyMathTransform(), null);
        bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.SubtractMathTransform(), null);
//	bundleContext.registerService(Operator.class, new com.affymetrix.genometry.operator.FindMateOperator(), null);
    }
}
