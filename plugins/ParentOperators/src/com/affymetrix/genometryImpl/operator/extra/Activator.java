package com.affymetrix.genometryImpl.operator.extra;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.ServiceRegistrar;

public class Activator extends ServiceRegistrar implements BundleActivator {
	
	@Override
	protected ServiceRegistration<?>[] registerService(IGBService igbService) throws Exception {
		return new ServiceRegistration[] {
			bundleContext.registerService(Operator.class, new ParentNotOperator(FileTypeCategory.Annotation), null),
			bundleContext.registerService(Operator.class, new ParentNotOperator(FileTypeCategory.Alignment), null),
			bundleContext.registerService(Operator.class, new ParentNotOperator(FileTypeCategory.ProbeSet), null),
			bundleContext.registerService(Operator.class, new ParentIntersectionOperator(FileTypeCategory.Annotation), null),
			bundleContext.registerService(Operator.class, new ParentIntersectionOperator(FileTypeCategory.Alignment), null),
			bundleContext.registerService(Operator.class, new ParentIntersectionOperator(FileTypeCategory.ProbeSet), null),
			bundleContext.registerService(Operator.class, new ParentUnionOperator(FileTypeCategory.Annotation), null),
			bundleContext.registerService(Operator.class, new ParentUnionOperator(FileTypeCategory.Alignment), null),
			bundleContext.registerService(Operator.class, new ParentUnionOperator(FileTypeCategory.ProbeSet), null)
		};
    }

}
