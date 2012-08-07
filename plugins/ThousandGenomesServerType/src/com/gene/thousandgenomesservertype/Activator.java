package com.gene.thousandgenomesservertype;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.igb.osgi.service.IGBService;

public class Activator implements BundleActivator {
//	private static final String _1000_GENOMES_US = "ftp://ftp-trace.ncbi.nih.gov/1000genomes/ftp/";
	static final String _1000_GENOMES_US = "ftp://ftp-trace.ncbi.nih.gov/1000genomes/ftp/phase1/";
//	private static final String _1000_GENOMES_EUROPE = "ftp://ftp.1000genomes.ebi.ac.uk/vol1/ftp/";
	private BundleContext bundleContext;

	private void registerServers(final IGBService igbService) {
		ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, ServerTypeI.class);
		bundleContext.registerService(ServerTypeI.class, ThousandGenomesServerType.getInstance(), null);
		igbService.addServer(ThousandGenomesServerType.getInstance(), "1000 Genomes", _1000_GENOMES_US, Integer.MAX_VALUE);
	}

	@Override
	public void start(BundleContext bundleContext_) throws Exception {
		this.bundleContext = bundleContext_;
    	ServiceReference<IGBService> igbServiceReference = bundleContext.getServiceReference(IGBService.class);

        if (igbServiceReference != null) {
        	IGBService igbService = bundleContext.getService(igbServiceReference);
        	registerServers(igbService);
        }
        else {
        	ServiceTracker<IGBService,Object> serviceTracker = new ServiceTracker<IGBService,Object>(bundleContext, IGBService.class, null) {
        	    public Object addingService(ServiceReference<IGBService> igbServiceReference) {
                	IGBService igbService = bundleContext.getService(igbServiceReference);
                   	registerServers(igbService);
                    return super.addingService(igbServiceReference);
        	    }
        	};
        	serviceTracker.open();
        }
    }

	public void stop(BundleContext bundleContext) throws Exception {
		this.bundleContext = null;
	}
}
