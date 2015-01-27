package com.gene.sampleselection;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.igb.service.api.IGBService;
import com.affymetrix.igb.service.api.IgbTabPanel;
import com.affymetrix.igb.service.api.XServiceRegistrar;
import com.affymetrix.igb.shared.TrackClickListener;

public class Activator extends XServiceRegistrar<IGBService> implements BundleActivator {

    public Activator() {
        super(IGBService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IGBService igbService) throws Exception {
        return new ServiceRegistration[]{
            bundleContext.registerService(IgbTabPanel.class, new SampleSelectionView(igbService), null),
            bundleContext.registerService(TrackClickListener.class, new VCFListener(igbService), null)
        };
    }
}
