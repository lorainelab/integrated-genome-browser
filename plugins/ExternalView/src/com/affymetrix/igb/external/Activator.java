package com.affymetrix.igb.external;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	private ExternalViewer externalViewer;
	@Override
	protected IGBTabPanel getPage(IGBService igbService) {
		externalViewer = new ExternalViewer(igbService);
		return externalViewer;
	}

	@Override
	public void stop(BundleContext _bundleContext) throws Exception
	{
		externalViewer.removeViewer();
		super.stop(_bundleContext);
	}	
}
