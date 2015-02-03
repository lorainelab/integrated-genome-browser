package com.affymetrix.genometry.operator.service;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import com.affymetrix.genometry.operator.AddMathTransform;
import com.affymetrix.genometry.operator.ComplementSequenceOperator;
import com.affymetrix.genometry.operator.CopyGraphOperator;
import com.affymetrix.genometry.operator.CopyMismatchOperator;
import com.affymetrix.genometry.operator.CopySequenceOperator;
import com.affymetrix.genometry.operator.CopyXOperator;
import com.affymetrix.genometry.operator.DepthOperator;
import com.affymetrix.genometry.operator.DiffOperator;
import com.affymetrix.genometry.operator.DivideMathTransform;
import com.affymetrix.genometry.operator.ExclusiveAOperator;
import com.affymetrix.genometry.operator.ExclusiveBOperator;
import com.affymetrix.genometry.operator.GraphMultiplexer;
import com.affymetrix.genometry.operator.IntersectionOperator;
import com.affymetrix.genometry.operator.InverseLogTransform;
import com.affymetrix.genometry.operator.InverseTransformer;
import com.affymetrix.genometry.operator.LogTransform;
import com.affymetrix.genometry.operator.MaxOperator;
import com.affymetrix.genometry.operator.MeanOperator;
import com.affymetrix.genometry.operator.MedianOperator;
import com.affymetrix.genometry.operator.MinOperator;
import com.affymetrix.genometry.operator.MultiplyMathTransform;
import com.affymetrix.genometry.operator.NotOperator;
import com.affymetrix.genometry.operator.Operator;
import com.affymetrix.genometry.operator.PowerTransformer;
import com.affymetrix.genometry.operator.ProductOperator;
import com.affymetrix.genometry.operator.RatioOperator;
import com.affymetrix.genometry.operator.StartDepthOperator;
import com.affymetrix.genometry.operator.SubtractMathTransform;
import com.affymetrix.genometry.operator.SumOperator;
import com.affymetrix.genometry.operator.SummaryOperator;
import com.affymetrix.genometry.operator.UnionOperator;
import com.affymetrix.genometry.operator.XorOperator;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import java.util.ArrayList;
import java.util.List;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author dcnorris
 */
@Component(name = OperatorServiceFactory.COMPONENT_NAME, immediate = true)
public class OperatorServiceFactory {

    public static final String COMPONENT_NAME = "OperatorServiceFactory";
    private BundleContext bundleContext;
    private final List<ServiceReference<Operator>> serviceReferences;

    public OperatorServiceFactory() {
        serviceReferences = new ArrayList<>();
    }

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        bundleContext.registerService(Operator.class, new ComplementSequenceOperator(), null);
        bundleContext.registerService(Operator.class, new CopyGraphOperator(), null);
        bundleContext.registerService(Operator.class, new CopyMismatchOperator(), null);
        bundleContext.registerService(Operator.class, new CopySequenceOperator(), null);
        bundleContext.registerService(Operator.class, new CopyXOperator(FileTypeCategory.Alignment), null);
        bundleContext.registerService(Operator.class, new CopyXOperator(FileTypeCategory.Annotation), null);
        bundleContext.registerService(Operator.class, new CopyXOperator(FileTypeCategory.ProbeSet), null);
        bundleContext.registerService(Operator.class, new DepthOperator(FileTypeCategory.Alignment), null);
        bundleContext.registerService(Operator.class, new DepthOperator(FileTypeCategory.Annotation), null);
        bundleContext.registerService(Operator.class, new DepthOperator(FileTypeCategory.ProbeSet), null);
        bundleContext.registerService(Operator.class, new StartDepthOperator(FileTypeCategory.Alignment), null);
        bundleContext.registerService(Operator.class, new StartDepthOperator(FileTypeCategory.Annotation), null);
        bundleContext.registerService(Operator.class, new StartDepthOperator(FileTypeCategory.ProbeSet), null);
        bundleContext.registerService(Operator.class, new SummaryOperator(FileTypeCategory.Annotation), null);
        bundleContext.registerService(Operator.class, new SummaryOperator(FileTypeCategory.Alignment), null);
        bundleContext.registerService(Operator.class, new SummaryOperator(FileTypeCategory.ProbeSet), null);
        bundleContext.registerService(Operator.class, new NotOperator(FileTypeCategory.Annotation), null);
        bundleContext.registerService(Operator.class, new NotOperator(FileTypeCategory.Alignment), null);
        bundleContext.registerService(Operator.class, new NotOperator(FileTypeCategory.ProbeSet), null);
        bundleContext.registerService(Operator.class, new DiffOperator(), null);
        bundleContext.registerService(Operator.class, new ExclusiveAOperator(FileTypeCategory.Annotation), null);
        bundleContext.registerService(Operator.class, new ExclusiveAOperator(FileTypeCategory.Alignment), null);
        bundleContext.registerService(Operator.class, new ExclusiveAOperator(FileTypeCategory.ProbeSet), null);
        bundleContext.registerService(Operator.class, new ExclusiveBOperator(FileTypeCategory.Annotation), null);
        bundleContext.registerService(Operator.class, new ExclusiveBOperator(FileTypeCategory.Alignment), null);
        bundleContext.registerService(Operator.class, new ExclusiveBOperator(FileTypeCategory.ProbeSet), null);
        bundleContext.registerService(Operator.class, new IntersectionOperator(FileTypeCategory.Annotation), null);
        bundleContext.registerService(Operator.class, new IntersectionOperator(FileTypeCategory.Alignment), null);
        bundleContext.registerService(Operator.class, new IntersectionOperator(FileTypeCategory.ProbeSet), null);
        bundleContext.registerService(Operator.class, new InverseTransformer(), null);
        bundleContext.registerService(Operator.class, new InverseLogTransform(), null);
        bundleContext.registerService(Operator.class, new InverseLogTransform(Math.E), null);
        bundleContext.registerService(Operator.class, new InverseLogTransform(2.0), null);
        bundleContext.registerService(Operator.class, new InverseLogTransform(10.0), null);
        bundleContext.registerService(Operator.class, new LogTransform(), null);
        bundleContext.registerService(Operator.class, new LogTransform(Math.E), null);
        bundleContext.registerService(Operator.class, new LogTransform(2.0), null);
        bundleContext.registerService(Operator.class, new LogTransform(10.0), null);
        bundleContext.registerService(Operator.class, new PowerTransformer(), null);
        bundleContext.registerService(Operator.class, new PowerTransformer(0.5), null);
        bundleContext.registerService(Operator.class, new MaxOperator(), null);
        bundleContext.registerService(Operator.class, new MeanOperator(), null);
        bundleContext.registerService(Operator.class, new MedianOperator(), null);
        bundleContext.registerService(Operator.class, new MinOperator(), null);
        bundleContext.registerService(Operator.class, new ProductOperator(), null);
        bundleContext.registerService(Operator.class, new RatioOperator(), null);
        bundleContext.registerService(Operator.class, new SumOperator(), null);
        bundleContext.registerService(Operator.class, new UnionOperator(FileTypeCategory.Alignment), null);
        bundleContext.registerService(Operator.class, new UnionOperator(FileTypeCategory.Annotation), null);
        bundleContext.registerService(Operator.class, new UnionOperator(FileTypeCategory.ProbeSet), null);
        bundleContext.registerService(Operator.class, new XorOperator(FileTypeCategory.Alignment), null);
        bundleContext.registerService(Operator.class, new XorOperator(FileTypeCategory.Annotation), null);
        bundleContext.registerService(Operator.class, new XorOperator(FileTypeCategory.ProbeSet), null);
        bundleContext.registerService(Operator.class, new GraphMultiplexer(), null);
        bundleContext.registerService(Operator.class, new AddMathTransform(), null);
        bundleContext.registerService(Operator.class, new DivideMathTransform(), null);
        bundleContext.registerService(Operator.class, new MultiplyMathTransform(), null);
        bundleContext.registerService(Operator.class, new SubtractMathTransform(), null);
    }

    @Deactivate
    public void deactivate() {
        serviceReferences.forEach(sr -> bundleContext.ungetService(sr));
    }
}
