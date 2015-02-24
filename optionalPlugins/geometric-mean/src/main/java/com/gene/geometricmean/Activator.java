package com.gene.geometricmean;

import com.affymetrix.genometry.operator.Operator;
import com.lorainelab.igb.services.SimpleServiceRegistrar;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator extends SimpleServiceRegistrar implements BundleActivator {

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext) throws Exception {
        return new ServiceRegistration[]{
            bundleContext.registerService(Operator.class, new GeometricMeanOperator(), null)
        };
    }
}
