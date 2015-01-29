package com.affymetrix.igb.search;

import org.osgi.framework.BundleActivator;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.common.ExtensionPointListener;
import com.affymetrix.genometry.event.GenericServerInitListener;
import com.affymetrix.igb.service.api.IgbService;
import com.affymetrix.igb.service.api.IgbTabPanelI;
import com.affymetrix.igb.service.api.XServiceRegistrar;
import com.affymetrix.igb.shared.ISearchModeSym;
import com.affymetrix.igb.shared.SearchListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator extends XServiceRegistrar<IgbService> implements BundleActivator {

    public Activator() {
        super(IgbService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IgbService igbService) throws Exception {
        final SearchView searchView = new SearchView(igbService);
        ExtensionPointHandler<ISearchModeSym> extensionPointSym = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ISearchModeSym.class);
        extensionPointSym.addListener(new ExtensionPointListener<ISearchModeSym>() {
            @Override
            public void removeService(ISearchModeSym searchMode) {
                searchView.initSearchCB();
            }

            @Override
            public void addService(ISearchModeSym searchMode) {
                searchView.initSearchCB();
            }
        });

        return new ServiceRegistration[]{
            bundleContext.registerService(IgbTabPanelI.class, searchView, null),
            bundleContext.registerService(SearchListener.class, searchView, null),
            bundleContext.registerService(GenericServerInitListener.class, searchView, null),};
    }
}
