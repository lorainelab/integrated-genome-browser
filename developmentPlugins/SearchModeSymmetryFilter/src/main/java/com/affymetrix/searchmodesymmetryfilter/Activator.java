package com.affymetrix.searchmodesymmetryfilter;

import com.affymetrix.genometry.filter.SymmetryFilterProps;
import org.lorainelab.igb.igb.services.search.ISearchMode;
import org.lorainelab.igb.igb.services.IgbService;
import org.lorainelab.igb.igb.services.XServiceRegistrar;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator extends XServiceRegistrar<IgbService> implements BundleActivator {

    public Activator() {
        super(IgbService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IgbService igbService) throws Exception {
        return new ServiceRegistration[]{
            bundleContext.registerService(ISearchMode.class, new SearchModeSymmetryFilter(igbService, new SymmetryFilterProps(), 2000), null)
        };
    }
}
