package com.affymetrix.igb.window.service;

import javax.swing.JComponent;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.window.service.IWindowService;

public abstract class WindowActivator implements BundleActivator {

	protected BundleContext bundleContext;
	protected JComponent page;

	protected BundleContext getContext() {
		return bundleContext;
	}

	protected abstract JComponent getPage(IGBService igbService);
	protected abstract String getName();
	protected abstract String getTitle();

	private void createPage(ServiceReference igbServiceReference) {
        try
        {
            IGBService igbService = (IGBService) bundleContext.getService(igbServiceReference);
            page = getPage(igbService);
            bundleContext.ungetService(igbServiceReference);
            processLoadPage();
        } catch (Exception ex) {
            System.out.println(this.getClass().getName() + " - Exception in Activator.createPage() -> " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
	}


	private void loadPage(ServiceReference windowServiceReference) {
        try
        {
            IWindowService windowService = (IWindowService) bundleContext.getService(windowServiceReference);
            windowService.addPlugIn(page, getName(), getTitle(), -1);
            bundleContext.ungetService(windowServiceReference);
        } catch (Exception ex) {
            System.out.println(this.getClass().getName() + " - Exception in Activator.loadPage() -> " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
	}

	protected void processCreatePage() throws Exception
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

	private void processLoadPage() throws Exception
	{
    	ServiceReference windowServiceReference = bundleContext.getServiceReference(IWindowService.class.getName());

        if (windowServiceReference != null)
        {
        	loadPage(windowServiceReference);
        }
        else
        {
        	ServiceTracker serviceTracker = new ServiceTracker(bundleContext, IWindowService.class.getName(), null) {
        	    public Object addingService(ServiceReference windowServiceReference) {
        	    	loadPage(windowServiceReference);
        	        return super.addingService(windowServiceReference);
        	    }
        	};
        	serviceTracker.open();
        }
	}

	public void start(BundleContext _bundleContext) throws Exception
	{
    	bundleContext = _bundleContext;
    	processCreatePage();
	}

	public void stop(BundleContext _bundleContext) throws Exception
	{
    	ServiceReference windowServiceReference = bundleContext.getServiceReference(IWindowService.class.getName());

        if (windowServiceReference != null)
        {
            try
            {
                IWindowService windowService = (IWindowService) bundleContext.getService(windowServiceReference);
                windowService.removePlugIn(getName());
                bundleContext.ungetService(windowServiceReference);
            }
            catch (Exception ex) {
            	ex.printStackTrace(System.out);
			}
        }
        else
        {
            //System.out.println(this.getClass().getName() + " - Couldn't find any igb service...");
        }
		bundleContext = null;
	}

}
