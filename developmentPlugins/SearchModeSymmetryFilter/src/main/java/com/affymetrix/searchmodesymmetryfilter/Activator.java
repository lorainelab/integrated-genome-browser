package com.affymetrix.searchmodesymmetryfilter;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genometry.filter.SymmetryFilterProps;
import com.affymetrix.igb.service.api.IgbService;
import com.affymetrix.igb.service.api.XServiceRegistrar;
import com.affymetrix.igb.shared.ISearchModeSym;
import org.osgi.framework.BundleContext;

public class Activator extends XServiceRegistrar<IgbService> implements BundleActivator {

    public Activator() {
        super(IgbService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IgbService igbService) throws Exception {
        return new ServiceRegistration[]{
            bundleContext.registerService(ISearchModeSym.class, new SearchModeSymmetryFilter(igbService, new SymmetryFilterProps(), 2000), null)
        };
    }
}
