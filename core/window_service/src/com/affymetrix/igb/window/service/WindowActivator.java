package com.affymetrix.igb.window.service;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.common.CommonUtils;
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
	private void createPage(ServiceReference<IGBService> igbServiceReference) {
        try
        {
        	IGBService igbService = bundleContext.getService(igbServiceReference);
        	serviceRegistration = bundleContext.registerService(IGBTabPanel.class, getPage(igbService), null);
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
	protected void processCreatePage() throws Exception
	{
    	ServiceReference<IGBService> igbServiceReference = bundleContext.getServiceReference(IGBService.class);

        if (igbServiceReference != null)
        {
        	createPage(igbServiceReference);
        }
        else
        {
        	ServiceTracker<IGBService,Object> serviceTracker = new ServiceTracker<IGBService,Object>(bundleContext, IGBService.class.getName(), null) {
        	    public Object addingService(ServiceReference<IGBService> igbServiceReference) {
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
    	if (CommonUtils.getInstance().isExit(bundleContext)) {
    		return;
    	}
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
