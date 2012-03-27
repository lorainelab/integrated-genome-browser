package com.gene.bigwighandler;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;

public class Activator implements BundleActivator {
	private BundleContext bundleContext;
	private ServiceRegistration<FileTypeHandler> bigwigHandlerRegistration;
	private ServiceRegistration<MapViewGlyphFactoryI> annotationBigwigSemanticZoomGlyphFactoryRegistration;
	private ServiceRegistration<MapViewGlyphFactoryI> alignmentBigwigSemanticZoomGlyphFactoryRegistration;
	private ServiceRegistration<MapViewGlyphFactoryI> graphBigwigSemanticZoomGlyphFactoryRegistration;
	private MapViewGlyphFactoryI annotationGlyphFactory = null;
	private MapViewGlyphFactoryI alignmentGlyphFactory = null;
	private MapViewGlyphFactoryI graphGlyphFactory = null;
    private ServiceTracker<MapViewGlyphFactoryI,Object> serviceTrackerMapViewGlyphFactoryI = null;
	private boolean factoryCreated = false;

	private void checkReference(ServiceReference<MapViewGlyphFactoryI> reference) {
    	MapViewGlyphFactoryI factory = bundleContext.getService(reference);
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
			for (ServiceReference<MapViewGlyphFactoryI> reference : bundleContext.getServiceReferences(MapViewGlyphFactoryI.class, null)) {
				checkReference(reference);
			}
		}
		catch (InvalidSyntaxException x) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "could not get MapViewGlyphFactoryI service references", x);
		}
    	serviceTrackerMapViewGlyphFactoryI = new ServiceTracker<MapViewGlyphFactoryI,Object>(bundleContext, MapViewGlyphFactoryI.class, null) {
    	    public synchronized Object addingService(ServiceReference<MapViewGlyphFactoryI> mapViewGlyphFactoryIServiceReference) {
    	    	if (!factoryCreated) {
    	    		checkReference(mapViewGlyphFactoryIServiceReference);
					synchronized (this) {
						if (annotationGlyphFactory != null && alignmentGlyphFactory != null && graphGlyphFactory != null) {
							serviceTrackerMapViewGlyphFactoryI.close();
							factoryCreated = true;
							BigWigSemanticZoomGlyphFactory annotationBigWigSemanticZoomGlyphFactory = new BigWigSemanticZoomGlyphFactory(annotationGlyphFactory, graphGlyphFactory);
//							annotationBigWigSemanticZoomGlyphFactory.setIgbService(igbService);
							annotationBigwigSemanticZoomGlyphFactoryRegistration = bundleContext.registerService(MapViewGlyphFactoryI.class, annotationBigWigSemanticZoomGlyphFactory, null);
							BigWigSemanticZoomGlyphFactory alignmentBigWigSemanticZoomGlyphFactory = new BigWigSemanticZoomGlyphFactory(alignmentGlyphFactory, graphGlyphFactory);
//							alignmentBigWigSemanticZoomGlyphFactory.setIgbService(igbService);
							alignmentBigwigSemanticZoomGlyphFactoryRegistration = bundleContext.registerService(MapViewGlyphFactoryI.class, alignmentBigWigSemanticZoomGlyphFactory, null);
							BigWigSemanticZoomGlyphFactory graphBigWigSemanticZoomGlyphFactory = new BigWigSemanticZoomGlyphFactory(graphGlyphFactory, graphGlyphFactory);
//							graphBigWigSemanticZoomGlyphFactory.setIgbService(igbService);
							graphBigwigSemanticZoomGlyphFactoryRegistration = bundleContext.registerService(MapViewGlyphFactoryI.class, graphBigWigSemanticZoomGlyphFactory, null);
						}
					}
    	    	}
    	    	return super.addingService(mapViewGlyphFactoryIServiceReference);
    	    }
    	};
    	serviceTrackerMapViewGlyphFactoryI.open();
	}

	@Override
	public void start(BundleContext bundleContext_) throws Exception {
		this.bundleContext = bundleContext_;
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
