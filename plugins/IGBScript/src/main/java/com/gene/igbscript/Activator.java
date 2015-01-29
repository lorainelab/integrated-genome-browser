package com.gene.igbscript;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.igb.swing.ScriptProcessor;
import com.affymetrix.igb.service.api.IgbService;
import com.affymetrix.igb.service.api.XServiceRegistrar;

public class Activator extends XServiceRegistrar<IgbService> implements BundleActivator {

    public Activator() {
        super(IgbService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IgbService igbService) throws Exception {
        return new ServiceRegistration[]{
            bundleContext.registerService(ScriptProcessor.class, new IGBScriptProcessor(igbService), null)
        };
    }
}
