package com.affymetrix.igb.searchmodeidorprops;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.igb.service.api.IGBService;
import com.affymetrix.igb.service.api.XServiceRegistrar;
import com.affymetrix.igb.shared.IKeyWordSearch;
import com.affymetrix.igb.shared.ISearchHints;
import com.affymetrix.igb.shared.ISearchModeSym;
import org.osgi.framework.BundleContext;

public class Activator extends XServiceRegistrar<IGBService> implements BundleActivator {

    public Activator() {
        super(IGBService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IGBService igbService) throws Exception {
        SearchModeID smID = new SearchModeID(igbService);
        return new ServiceRegistration[]{
            bundleContext.registerService(ISearchModeSym.class, smID, null),
            bundleContext.registerService(IKeyWordSearch.class, new SearchModeProps(igbService), null),
            bundleContext.registerService(ISearchHints.class, new PropSearchHints(), null)
        };
    }
}
