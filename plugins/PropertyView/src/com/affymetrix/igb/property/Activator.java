package com.affymetrix.igb.property;

import javax.swing.JComponent;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {
	@Override
	protected JComponent getPage(IGBService igbService) {
		PropertyView propertyView = new PropertyView(igbService);
        igbService.setPropertyHandler(propertyView);
		return propertyView;
	}

	@Override
	protected String getName() {
        return PropertyView.BUNDLE.getString("selectionInfoTab");
	}

	@Override
	protected String getTitle() {
        return PropertyView.BUNDLE.getString("selectionInfoTab");
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
