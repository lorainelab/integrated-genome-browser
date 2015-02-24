package com.gene.sampleselection;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.lorainelab.igb.services.IgbService;
import com.lorainelab.igb.services.window.tabs.IgbTabPanel;
import com.lorainelab.igb.services.XServiceRegistrar;
import com.affymetrix.igb.shared.TrackClickListener;

public class Activator extends XServiceRegistrar<IgbService> implements BundleActivator {

    public Activator() {
        super(IgbService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IgbService igbService) throws Exception {
        return new ServiceRegistration[]{
            bundleContext.registerService(IgbTabPanel.class, new SampleSelectionView(igbService), null),
            bundleContext.registerService(TrackClickListener.class, new VCFListener(igbService), null)
        };
    }
}
