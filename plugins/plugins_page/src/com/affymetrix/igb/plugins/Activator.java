package com.affymetrix.igb.plugins;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.igb.osgi.service.IGBService;

public class Activator implements BundleActivator {

	private static BundleContext bundleContext;
	private PluginsView pluginsView;

	static BundleContext getContext() {
		return bundleContext;
	}

	private void loadPluginsPage(ServiceReference igbServiceReference) {
        try
        {
            IGBService igbService = (IGBService) bundleContext.getService(igbServiceReference);
            pluginsView = new PluginsView(igbService);
            pluginsView.setBundleContext(bundleContext);
            igbService.addPlugIn(pluginsView, PluginsView.BUNDLE.getString("viewTab"));
            bundleContext.ungetService(igbServiceReference);
        } catch (Exception ex) {
            System.out.println(this.getClass().getName() + " - Exception in Activator.loadPluginsPage() -> " + ex.getMessage());
        }
	}

    public void start(BundleContext bundleContext) throws Exception
    {
    	Activator.bundleContext = bundleContext;
    	ServiceReference igbServiceReference = bundleContext.getServiceReference(IGBService.class.getName());

        if (igbServiceReference != null)
        {
        	loadPluginsPage(igbServiceReference);
        }
        else
        {
        	ServiceTracker serviceTracker = new ServiceTracker(bundleContext, IGBService.class.getName(), null) {
        	    public Object addingService(ServiceReference igbServiceReference) {
        	    	loadPluginsPage(igbServiceReference);
        	        return super.addingService(igbServiceReference);
        	    }
        	};
        	serviceTracker.open();
        }
    }

    public void stop(BundleContext bundleContext)
    {
    	if (pluginsView != null) {
    		pluginsView.deactivate();
    	}
    	ServiceReference igbRef = bundleContext.getServiceReference(IGBService.class.getName());

        if (igbRef != null)
        {
            try
            {
                IGBService igbService = (IGBService) bundleContext.getService(igbRef);
                igbService.removePlugIn(PluginsView.BUNDLE.getString("viewTab"));
                bundleContext.ungetService(igbRef);
                pluginsView = null;
            }
            catch (Exception ex) {
            	ex.printStackTrace(System.out);
            }
        }
        else
        {
            // System.out.println(this.getClass().getName() + " - Couldn't find any igb service...");
        }
		Activator.bundleContext = null;
    }
}
