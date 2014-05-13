package com.gene.sampleselection;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.XServiceRegistrar;
import com.affymetrix.igb.shared.TrackClickListener;

public class Activator extends XServiceRegistrar<IGBService> implements BundleActivator {

    public Activator() {
        super(IGBService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IGBService igbService) throws Exception {
        return new ServiceRegistration[]{
            bundleContext.registerService(IGBTabPanel.class, new SampleSelectionView(igbService), null),
            bundleContext.registerService(TrackClickListener.class, new VCFListener(igbService), null)
        };
    }
}
