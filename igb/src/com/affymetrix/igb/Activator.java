package com.affymetrix.igb;

import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.window.service.IWindowService;

public class Activator implements BundleActivator {
	protected BundleContext bundleContext;

	@Override
	public void start(BundleContext _bundleContext) throws Exception {
		this.bundleContext = _bundleContext;
		// Verify jidesoft license.
		com.jidesoft.utils.Lm.verifyLicense("Dept. of Bioinformatics and Genomics, UNCC",
			"Integrated Genome Browser", ".HAkVzUi29bDFq2wQ6vt2Rb4bqcMi8i1");
    	ServiceReference windowServiceReference = bundleContext.getServiceReference(IWindowService.class.getName());

        if (windowServiceReference != null)
        {
        	run(windowServiceReference);
        }
        else
        {
        	ServiceTracker serviceTracker = new ServiceTracker(bundleContext, IWindowService.class.getName(), null) {
        	    public Object addingService(ServiceReference windowServiceReference) {
        	    	run(windowServiceReference);
        	        return super.addingService(windowServiceReference);
        	    }
        	};
        	serviceTracker.open();
        }
	}
	@Override
	public void stop(BundleContext _bundleContext) throws Exception {
	}

	protected void run(ServiceReference windowServiceReference) {
        IWindowService windowService = (IWindowService) bundleContext.getService(windowServiceReference);
        IGB igb = new IGB();
        igb.init(bundleContext.getProperty("args").split(","));
        igb.setWindowService(windowService);
		bundleContext.registerService(IGBService.class.getName(), IGBServiceImpl.getInstance(), new Properties());
	}
}
