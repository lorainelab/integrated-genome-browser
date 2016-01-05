package com.gene.overlapannotationoperator;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genometry.operator.Operator;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import org.lorainelab.igb.services.SimpleServiceRegistrar;
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
