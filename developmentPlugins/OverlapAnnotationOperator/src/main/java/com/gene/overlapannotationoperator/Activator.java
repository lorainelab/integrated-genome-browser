package com.gene.overlapannotationoperator;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.igb.service.api.SimpleServiceRegistrar;
import org.osgi.framework.BundleContext;

public class Activator extends SimpleServiceRegistrar implements BundleActivator {

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext) throws Exception {
        return new ServiceRegistration[]{
            bundleContext.registerService(Operator.class, new OverlapAnnotationOperator(FileTypeCategory.Alignment), null),
            bundleContext.registerService(Operator.class, new OverlapAnnotationOperator(FileTypeCategory.Annotation), null),
            bundleContext.registerService(Operator.class, new OverlapAnnotationOperator(FileTypeCategory.ProbeSet), null)
        };
    }
}
