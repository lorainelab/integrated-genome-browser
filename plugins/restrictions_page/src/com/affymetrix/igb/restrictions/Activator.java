package com.affymetrix.igb.restrictions;

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

	private void loadRestrictionPage(ServiceReference igbServiceReference) {
        try
        {
            IGBService igbService = (IGBService) bundleContext.getService(igbServiceReference);
            RestrictionControlView restrictionControlView = new RestrictionControlView(igbService);
            igbService.addPlugIn(restrictionControlView, RestrictionControlView.BUNDLE.getString("restrictionSitesTab"));
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
        	loadRestrictionPage(igbServiceReference);
        }
        else
        {
        	ServiceTracker serviceTracker = new ServiceTracker(bundleContext, IGBService.class.getName(), null) {
        	    public Object addingService(ServiceReference igbServiceReference) {
        	    	loadRestrictionPage(igbServiceReference);
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
                igbService.removePlugIn(RestrictionControlView.BUNDLE.getString("restrictionSitesTab"));
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
