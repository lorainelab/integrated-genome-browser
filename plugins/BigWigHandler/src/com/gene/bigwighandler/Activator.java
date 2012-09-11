package com.gene.bigwighandler;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.MapTierGlyphFactoryI;

public class Activator implements BundleActivator {
	private BundleContext bundleContext;
	private ServiceRegistration<FileTypeHandler> bigwigHandlerRegistration;
	private ServiceRegistration<MapTierGlyphFactoryI> annotationBigwigSemanticZoomGlyphFactoryRegistration;
	private ServiceRegistration<MapTierGlyphFactoryI> alignmentBigwigSemanticZoomGlyphFactoryRegistration;
	private ServiceRegistration<MapTierGlyphFactoryI> graphBigwigSemanticZoomGlyphFactoryRegistration;
	private MapTierGlyphFactoryI annotationGlyphFactory = null;
	private MapTierGlyphFactoryI alignmentGlyphFactory = null;
	private MapTierGlyphFactoryI graphGlyphFactory = null;
    private ServiceTracker<MapTierGlyphFactoryI,Object> serviceTrackerMapViewGlyphFactoryI = null;
	private boolean factoryCreated = false;

	private void checkReference(ServiceReference<MapTierGlyphFactoryI> reference) {
    	MapTierGlyphFactoryI factory = bundleContext.getService(reference);
		if ("annotation".equals(factory.getName())) {
			annotationGlyphFactory = factory;
		}
		else if ("alignment".equals(factory.getName())) {
			alignmentGlyphFactory = factory;
		}
		else if ("stairstepgraph".equals(factory.getName())) {
			graphGlyphFactory = factory;
		}
	}

	private void registerServices(final IGBService igbService) {
		bigwigHandlerRegistration = bundleContext.registerService(FileTypeHandler.class, new BigWigHandler(), null);
		try {
			for (ServiceReference<MapTierGlyphFactoryI> reference : bundleContext.getServiceReferences(MapTierGlyphFactoryI.class, null)) {
				checkReference(reference);
			}
		}
		catch (InvalidSyntaxException x) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "could not get MapViewGlyphFactoryI service references", x);
		}
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
		if (annotationBigwigSemanticZoomGlyphFactoryRegistration != null) {
			annotationBigwigSemanticZoomGlyphFactoryRegistration.unregister();
		}
		if (alignmentBigwigSemanticZoomGlyphFactoryRegistration != null) {
			alignmentBigwigSemanticZoomGlyphFactoryRegistration.unregister();
		}
		if (graphBigwigSemanticZoomGlyphFactoryRegistration != null) {
			graphBigwigSemanticZoomGlyphFactoryRegistration.unregister();
		}
	}
}
