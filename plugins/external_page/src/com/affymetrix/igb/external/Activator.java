package com.affymetrix.igb.external;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.igb.osgi.service.IGBService;

public class Activator implements BundleActivator {

	private static BundleContext bundleContext;

	static BundleContext getContext() {
		return bundleContext;
	}

	private void loadExternalPage(ServiceReference igbServiceReference) {
        try
        {
            IGBService igbService = (IGBService) bundleContext.getService(igbServiceReference);
            ExternalViewer externalViewer = new ExternalViewer(igbService);
            igbService.addPlugIn(externalViewer, ExternalViewer.BUNDLE.getString("externalViewTab"));
            bundleContext.ungetService(igbServiceReference);
        } catch (Exception ex) {
            System.out.println(this.getClass().getName() + " - Exception in Activator.start() -> " + ex.getMessage());
        }
	}

	public void start(BundleContext bundleContext) throws Exception
	{
    	Activator.bundleContext = bundleContext;
    	ServiceReference igbServiceReference = bundleContext.getServiceReference(IGBService.class.getName());

        if (igbServiceReference != null)
        {
        	loadExternalPage(igbServiceReference);
        }
        else
        {
        	ServiceTracker serviceTracker = new ServiceTracker(bundleContext, IGBService.class.getName(), null) {
        	    public Object addingService(ServiceReference igbServiceReference) {
        	    	loadExternalPage(igbServiceReference);
        	        return super.addingService(igbServiceReference);
        	    }
        	};
        	serviceTracker.open();
        }
	}

	public void stop(BundleContext bundleContext) throws Exception
	{
        ServiceReference igbRef = bundleContext.getServiceReference(IGBService.class.getName());

        if (igbRef != null)
        {
            try
            {
                IGBService igbService = (IGBService) bundleContext.getService(igbRef);
                igbService.removePlugIn(ExternalViewer.BUNDLE.getString("externalViewTab"));
                bundleContext.ungetService(igbRef);
            }
            catch (Exception ex) {
            	ex.printStackTrace(System.out);
			}
        }
        else
        {
            //System.out.println(this.getClass().getName() + " - Couldn't find any igb service...");
        }
		Activator.bundleContext = null;
	}

}
