package com.affymetrix.igb.external;

import javax.swing.JComponent;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.window.service.IWindowService;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	private ExternalViewer externalViewer;
	@Override
	protected JComponent getPage(IGBService igbService) {
		externalViewer = new ExternalViewer(igbService);
		return externalViewer;
	}

	@Override
	protected String getName() {
        return ExternalViewer.BUNDLE.getString("externalViewTab");
	}

	@Override
	protected String getTitle() {
        return ExternalViewer.BUNDLE.getString("externalViewTab");
	}

	@Override
	protected void useWindowService(IWindowService windowService) {
		super.useWindowService(windowService);
	}

	@Override
	public void stop(BundleContext _bundleContext) throws Exception
	{
		externalViewer.removeViewer();
		super.stop(_bundleContext);
	}	
}
