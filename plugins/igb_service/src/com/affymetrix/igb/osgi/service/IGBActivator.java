package com.affymetrix.igb.osgi.service;

import javax.swing.JComponent;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.igb.osgi.service.IGBService;

public abstract class IGBActivator implements BundleActivator {

	protected BundleContext bundleContext;
	protected JComponent page;

	protected BundleContext getContext() {
		return bundleContext;
	}

	protected abstract void process(IGBService igbService);

	private void createPage(ServiceReference igbServiceReference) {
        try
        {
            IGBService igbService = (IGBService) bundleContext.getService(igbServiceReference);
            process(igbService);
            bundleContext.ungetService(igbServiceReference);
        } catch (Exception ex) {
            System.out.println(this.getClass().getName() + " - Exception in Activator.createPage() -> " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
	}

	protected void processCreate() throws Exception
	{
    	ServiceReference igbServiceReference = bundleContext.getServiceReference(IGBService.class.getName());

        if (igbServiceReference != null)
        {
        	createPage(igbServiceReference);
        }
        else
        {
        	ServiceTracker serviceTracker = new ServiceTracker(bundleContext, IGBService.class.getName(), null) {
        	    public Object addingService(ServiceReference igbServiceReference) {
        	    	createPage(igbServiceReference);
        	        return super.addingService(igbServiceReference);
        	    }
        	};
        	serviceTracker.open();
        }
	}

	public void start(BundleContext _bundleContext) throws Exception
	{
    	bundleContext = _bundleContext;
    	processCreate();
	}

	public void stop(BundleContext _bundleContext) throws Exception
	{
		bundleContext = null;
	}
}

