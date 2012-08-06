package com.gene.dasregistryservertype;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.igb.osgi.service.IGBService;

public class Activator implements BundleActivator {

	private BundleContext bundleContext;

	private void registerServerType(final IGBService igbService) {
		ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ServerTypeI.class);
		DASRegistryServerType drst = new DASRegistryServerType(igbService);
		bundleContext.registerService(ServerTypeI.class, drst, null);
		if (GenometryModel.getGenometryModel().getSelectedSeqGroup() != null) {
			drst.setGroup(GenometryModel.getGenometryModel().getSelectedSeqGroup());
		}
	}

	@Override
	public void start(BundleContext bundleContext_) throws Exception {
		this.bundleContext = bundleContext_;
    	ServiceReference<IGBService> igbServiceReference = bundleContext.getServiceReference(IGBService.class);

        if (igbServiceReference != null) {
        	IGBService igbService = bundleContext.getService(igbServiceReference);
        	registerServerType(igbService);
        }
        else {
        	ServiceTracker<IGBService,Object> serviceTracker = new ServiceTracker<IGBService,Object>(bundleContext, IGBService.class, null) {
        	    public Object addingService(ServiceReference<IGBService> igbServiceReference) {
                	IGBService igbService = bundleContext.getService(igbServiceReference);
                	registerServerType(igbService);
                    return super.addingService(igbServiceReference);
        	    }
        	};
        	serviceTracker.open();
        }
    }

	public void stop(BundleContext bundleContext) throws Exception {
		bundleContext = null;
	}
}
