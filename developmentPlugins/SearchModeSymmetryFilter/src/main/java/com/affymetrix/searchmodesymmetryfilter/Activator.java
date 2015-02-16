package com.affymetrix.searchmodesymmetryfilter;

import com.affymetrix.genometry.filter.SymmetryFilterProps;
import com.affymetrix.igb.shared.ISearchMode;
import com.lorainelab.igb.service.api.IgbService;
import com.lorainelab.igb.service.api.XServiceRegistrar;
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
