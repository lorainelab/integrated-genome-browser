package com.affymetrix.igb.window.service;

import javax.swing.JComponent;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.igb.osgi.service.ExtensionFactory;
import com.affymetrix.igb.osgi.service.ExtensionPoint;
import com.affymetrix.igb.osgi.service.ExtensionPointRegistry;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;

public abstract class WindowActivator implements BundleActivator {

	protected BundleContext bundleContext;
	private ExtensionFactory<IGBTabPanel> tabFactory;
	protected JComponent page;

	protected BundleContext getContext() {
		return bundleContext;
	}

	protected abstract IGBTabPanel getPage(IGBService igbService);

	private void createPage(ServiceReference igbServiceReference) {
        try
        {
            final IGBService igbService = (IGBService) bundleContext.getService(igbServiceReference);
            ExtensionPointRegistry registry = igbService.getExtensionPointRegistry();
            @SuppressWarnings("unchecked")
			ExtensionPoint<IGBTabPanel> tabPanelExtensionPoint = (ExtensionPoint<IGBTabPanel>)registry.getExtensionPoint(IGBService.TAB_PANELS);
            if (tabPanelExtensionPoint != null) {
            	tabFactory = 
	            	new ExtensionFactory<IGBTabPanel>() {
						@Override
						public IGBTabPanel createInstance() {
							return getPage(igbService);
						}
	            		
	            	};
	            tabPanelExtensionPoint.registerExtension(tabFactory);
            }
            bundleContext.ungetService(igbServiceReference);
        } catch (Exception ex) {
            System.out.println(this.getClass().getName() + " - Exception in Activator.createPage() -> " + ex.getMessage());
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

	public void start(BundleContext _bundleContext) throws Exception
	{
    	bundleContext = _bundleContext;
    	processCreatePage();
	}

	public void stop(BundleContext _bundleContext) throws Exception
	{
    	ServiceReference igbServiceReference = bundleContext.getServiceReference(IGBService.class.getName());
    	if (igbServiceReference != null) {
	        IGBService igbService = (IGBService) bundleContext.getService(igbServiceReference);
	        bundleContext.ungetService(igbServiceReference);
	        ExtensionPointRegistry registry = igbService.getExtensionPointRegistry();
			@SuppressWarnings("unchecked")
			ExtensionPoint<IGBTabPanel> tabPanelExtensionPoint = (ExtensionPoint<IGBTabPanel>)registry.getExtensionPoint(IGBService.TAB_PANELS);
            if (tabPanelExtensionPoint != null) {
            	if (tabFactory != null) {
            		tabPanelExtensionPoint.removeExtension(tabFactory);
            	}
            }
    	}
		bundleContext = null;
	}

}
