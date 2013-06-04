package com.affymetrix.genometryImpl.operator.extra;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.ServiceRegistrar;
import org.osgi.framework.BundleContext;

public class Activator extends ServiceRegistrar implements BundleActivator {
	
	@Override
	protected ServiceRegistration<?>[] registerService(BundleContext bundleContext, IGBService igbService) throws Exception {
		return new ServiceRegistration[] {
			bundleContext.registerService(Operator.class, new ParentDepthOperator(FileTypeCategory.Annotation), null),
			bundleContext.registerService(Operator.class, new ParentDepthOperator(FileTypeCategory.Alignment), null),
			bundleContext.registerService(Operator.class, new ParentDepthOperator(FileTypeCategory.ProbeSet), null),
			bundleContext.registerService(Operator.class, new ParentNotOperator(FileTypeCategory.Annotation), null),
			bundleContext.registerService(Operator.class, new ParentNotOperator(FileTypeCategory.Alignment), null),
			bundleContext.registerService(Operator.class, new ParentNotOperator(FileTypeCategory.ProbeSet), null),
			bundleContext.registerService(Operator.class, new ParentSummaryOperator(FileTypeCategory.Annotation), null),
			bundleContext.registerService(Operator.class, new ParentSummaryOperator(FileTypeCategory.Alignment), null),
			bundleContext.registerService(Operator.class, new ParentSummaryOperator(FileTypeCategory.ProbeSet), null),
			bundleContext.registerService(Operator.class, new ParentIntersectionOperator(FileTypeCategory.Annotation), null),
			bundleContext.registerService(Operator.class, new ParentIntersectionOperator(FileTypeCategory.Alignment), null),
			bundleContext.registerService(Operator.class, new ParentIntersectionOperator(FileTypeCategory.ProbeSet), null),
			bundleContext.registerService(Operator.class, new ParentUnionOperator(FileTypeCategory.Annotation), null),
			bundleContext.registerService(Operator.class, new ParentUnionOperator(FileTypeCategory.Alignment), null),
			bundleContext.registerService(Operator.class, new ParentUnionOperator(FileTypeCategory.ProbeSet), null),
			bundleContext.registerService(Operator.class, new ParentXorOperator(FileTypeCategory.Annotation), null),
			bundleContext.registerService(Operator.class, new ParentXorOperator(FileTypeCategory.Alignment), null),
			bundleContext.registerService(Operator.class, new ParentXorOperator(FileTypeCategory.ProbeSet), null),
			bundleContext.registerService(Operator.class, new ParentExclusiveAOperator(FileTypeCategory.Annotation), null),
			bundleContext.registerService(Operator.class, new ParentExclusiveAOperator(FileTypeCategory.Alignment), null),
			bundleContext.registerService(Operator.class, new ParentExclusiveAOperator(FileTypeCategory.ProbeSet), null),
			bundleContext.registerService(Operator.class, new ParentExclusiveBOperator(FileTypeCategory.Annotation), null),
			bundleContext.registerService(Operator.class, new ParentExclusiveBOperator(FileTypeCategory.Alignment), null),
			bundleContext.registerService(Operator.class, new ParentExclusiveBOperator(FileTypeCategory.ProbeSet), null)
		};
    }

}
