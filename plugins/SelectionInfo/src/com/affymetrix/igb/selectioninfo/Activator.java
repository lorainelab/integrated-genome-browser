package com.affymetrix.igb.selectioninfo;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	@Override
	protected IGBTabPanel getPage(IGBService igbService) {
		SelectionInfo selectionInfo = new SelectionInfo(igbService);
        igbService.setPropertyHandler(selectionInfo);
		return selectionInfo;
	}

	@Override
	public void stop(BundleContext _bundleContext) throws Exception
	{
    	ServiceReference igbServiceReference = bundleContext.getServiceReference(IGBService.class.getName());
        if (igbServiceReference != null) {
            IGBService igbService = (IGBService) bundleContext.getService(igbServiceReference);
            igbService.setPropertyHandler(null);
        }
		super.stop(_bundleContext);
	}
}
