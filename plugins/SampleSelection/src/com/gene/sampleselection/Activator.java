package com.gene.sampleselection;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.shared.TrackClickListener;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	private ServiceRegistration<TrackClickListener> trackClickListenerRegistration;

	@Override
	protected IGBTabPanel getPage(BundleContext bundleContext, IGBService igbService) {
		trackClickListenerRegistration = bundleContext.registerService(TrackClickListener.class, new VCFListener(igbService), null);
        return new SampleSelectionView(igbService);
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		trackClickListenerRegistration.unregister();
	}
}
