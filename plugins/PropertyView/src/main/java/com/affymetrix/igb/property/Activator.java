package com.affymetrix.igb.property;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.window.service.WindowActivator;

public class Activator extends WindowActivator implements BundleActivator {

    @Override
    protected IGBTabPanel getPage(BundleContext bundleContext, IGBService igbService) {
        PropertyView propertyView = new PropertyView(igbService);
        igbService.getSeqMapView().setPropertyHandler(propertyView);
        return propertyView;
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        ServiceReference<?> igbServiceReference = bundleContext.getServiceReference(IGBService.class.getName());
        if (igbServiceReference != null) {
            IGBService igbService = (IGBService) bundleContext.getService(igbServiceReference);
            igbService.getSeqMapView().setPropertyHandler(null);
        }
        super.stop(bundleContext);
    }
}
