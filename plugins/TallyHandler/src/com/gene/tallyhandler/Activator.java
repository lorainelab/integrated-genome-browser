package com.gene.tallyhandler;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.igb.osgi.service.IGBService;

public class Activator implements BundleActivator {
	private BundleContext bundleContext;
	private ServiceRegistration<FileTypeHandler> tallyHandlerRegistration;

	private void registerServices(IGBService igbService) {
		TallyHandler tallyHandler = new TallyHandler();
		tallyHandlerRegistration = bundleContext.registerService(FileTypeHandler.class, tallyHandler, null);
	}

	@Override
	public void start(BundleContext bundleContext_) throws Exception {
		this.bundleContext = bundleContext_;
 		ServiceReference<IGBService> igbServiceReference = bundleContext.getServiceReference(IGBService.class);

        if (igbServiceReference != null) {
        	IGBService igbService = bundleContext.getService(igbServiceReference);
        	registerServices(igbService);
        }
        else {
        	ServiceTracker<IGBService, ServiceReference<IGBService>> serviceTracker = new ServiceTracker<IGBService, ServiceReference<IGBService>>(bundleContext, IGBService.class.getName(), null) {
        	    public ServiceReference<IGBService> addingService(ServiceReference<IGBService> igbServiceReference) {
                	IGBService igbService = bundleContext.getService(igbServiceReference);
                   	registerServices(igbService);
                    return super.addingService(igbServiceReference);
        	    }
        	};
        	serviceTracker.open();
        }
    }

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		tallyHandlerRegistration.unregister();
	}
}
