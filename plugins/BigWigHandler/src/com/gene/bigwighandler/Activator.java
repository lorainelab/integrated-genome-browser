package com.gene.bigwighandler;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.igb.osgi.service.IGBService;

public class Activator implements BundleActivator {
	private BundleContext bundleContext;
	private ServiceRegistration<FileTypeHandler> bigwigHandlerRegistration;

	private void registerServices(final IGBService igbService) {
		bigwigHandlerRegistration = bundleContext.registerService(FileTypeHandler.class, new BigWigHandler(), null);
	}

	@Override
	public void start(BundleContext bundleContext_) throws Exception {
		this.bundleContext = bundleContext_;
    	if (CommonUtils.getInstance().isExit(bundleContext)) {
    		return;
    	}
    	ServiceReference<IGBService> igbServiceReference = bundleContext.getServiceReference(IGBService.class);

        if (igbServiceReference != null)
        {
        	IGBService igbService = bundleContext.getService(igbServiceReference);
        	registerServices(igbService);
        }
        else
        {
        	ServiceTracker<IGBService,Object> serviceTrackerIGBService = new ServiceTracker<IGBService,Object>(bundleContext, IGBService.class, null) {
        	    public Object addingService(ServiceReference<IGBService> igbServiceReference) {
                	IGBService igbService = bundleContext.getService(igbServiceReference);
                   	registerServices(igbService);
                    return super.addingService(igbServiceReference);
        	    }
        	};
        	serviceTrackerIGBService.open();
        }
    }

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		bigwigHandlerRegistration.unregister();
	}
}
