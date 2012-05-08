package com.gene.igbscript;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.genoviz.swing.recordplayback.ScriptProcessorHolder;
import com.affymetrix.igb.osgi.service.IGBService;

public class Activator implements BundleActivator {
	protected BundleContext bundleContext;

	/**
	 * once the IGBService is available, we can create the page, and
	 * register it with OSGi, so that the tab can be added.
	 * @param igbServiceReference the ServiceReference for the IGBService
	 */
	private void createFactory(ServiceReference<IGBService> igbServiceReference) {
        try {
        	IGBService igbService = bundleContext.getService(igbServiceReference);
    		List<String> extensions = new ArrayList<String>();
    		extensions.add("igb");
    		ScriptProcessorHolder.getInstance().addScriptProcessor(new IGBScriptProcessor(igbService));
       } catch (Exception ex) {
            System.out.println(this.getClass().getName() + " - Exception in Activator.createPage() -> " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
	}
	/**
	 * waits (if necessary) for the igbService, and then calls createPage 
	 * @throws Exception
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {
    	this.bundleContext = bundleContext;
    	ServiceReference<IGBService> igbServiceReference = bundleContext.getServiceReference(IGBService.class);

        if (igbServiceReference != null)
        {
        	createFactory(igbServiceReference);
        }
        else
        {
        	ServiceTracker<IGBService,Object> serviceTracker = new ServiceTracker<IGBService,Object>(bundleContext, IGBService.class.getName(), null) {
        	    public Object addingService(ServiceReference<IGBService> igbServiceReference) {
        	    	createFactory(igbServiceReference);
        	        return super.addingService(igbServiceReference);
        	    }
        	};
        	serviceTracker.open();
        }
    }

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
	}
}
