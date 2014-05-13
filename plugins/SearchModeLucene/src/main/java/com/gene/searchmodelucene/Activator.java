package com.gene.searchmodelucene;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.XServiceRegistrar;
import com.affymetrix.igb.shared.IKeyWordSearch;
import org.osgi.framework.BundleContext;

public class Activator extends XServiceRegistrar<IGBService> implements BundleActivator {

    public Activator() {
        super(IGBService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IGBService igbService) throws Exception {
        return new ServiceRegistration[]{
            bundleContext.registerService(IKeyWordSearch.class, new SearchModeLucene(igbService), null)
        };
    }
}
