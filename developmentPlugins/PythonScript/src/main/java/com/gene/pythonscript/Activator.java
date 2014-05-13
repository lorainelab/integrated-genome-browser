package com.gene.pythonscript;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.igb.swing.ScriptProcessor;
import com.affymetrix.igb.osgi.service.SimpleServiceRegistrar;

public class Activator extends SimpleServiceRegistrar implements BundleActivator {

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext) throws Exception {
        return new ServiceRegistration[]{
            bundleContext.registerService(ScriptProcessor.class, new PythonScriptProcessor(), null)
        };
    }
}
