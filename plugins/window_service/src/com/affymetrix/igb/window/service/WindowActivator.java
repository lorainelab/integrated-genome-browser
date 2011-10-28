package com.affymetrix.igb.window.service;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;

/**
 * This is the main Activator for all tab panel bundles.
 * Those bundles have an Activator that extends this class
 * and they only need to implement the getPage() method
 */
public abstract class WindowActivator implements BundleActivator {

	protected BundleContext bundleContext;
	protected ServiceRegistration<IGBTabPanel> serviceRegistration;

	/**
	 * standard getter
	 * @return the bundle context
	 */
	protected BundleContext getContext() {
		return bundleContext;
	}

	/**
	 * get the tab panel for the bundle
	 * @param igbService the IGBService implementation
	 * @return the tab panel
	 */
	protected abstract IGBTabPanel getPage(IGBService igbService);

	/**
	 * once the IGBService is available, we can create the page, and
	 * register it with OSGi, so that the tab can be added.
	 * @param igbServiceReference the ServiceReference for the IGBService
	 */
	@SuppressWarnings("unchecked")
	private void createPage(ServiceReference<IGBService> igbServiceReference) {
        try
        {
        	final IGBService igbService = bundleContext.getService(igbServiceReference);
        	serviceRegistration = (ServiceRegistration<IGBTabPanel>) bundleContext.registerService(IGBTabPanel.class.getName(), getPage(igbService), null);
        	bundleContext.ungetService(igbServiceReference);
        } catch (Exception ex) {
            System.out.println(this.getClass().getName() + " - Exception in Activator.createPage() -> " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
	}

	/**
	 * waits (if necessary) for the igbService, and then calls createPage 
	 * @throws Exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void processCreatePage() throws Exception
	{
    	ServiceReference<IGBService> igbServiceReference = (ServiceReference<IGBService>) bundleContext.getServiceReference(IGBService.class.getName());

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

	@Override
	public void start(BundleContext _bundleContext) throws Exception
	{
    	bundleContext = _bundleContext;
    	processCreatePage();
	}

	@Override
	public void stop(BundleContext _bundleContext) throws Exception
	{
		if (serviceRegistration != null) {
			serviceRegistration.unregister();
			serviceRegistration = null;
		}
		bundleContext = null;
	}

}
