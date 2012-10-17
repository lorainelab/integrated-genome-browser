package com.affymetrix.igb.thresholding;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenuItem;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.Selections;
import com.affymetrix.igb.thresholding.action.ThresholdingAction;

public class Activator implements BundleActivator {
	private BundleContext bundleContext;
	private ServiceRegistration<GenericAction> thresholdingActionRegistration;
	private SelectionListener selectionListener;

	private void registerServices(IGBService igbService) {
		ThresholdingAction.createAction(igbService);
		thresholdingActionRegistration = bundleContext.registerService(GenericAction.class, ThresholdingAction.getAction(), null);
		JRPMenuItem thresholdingMenuItem = new JRPMenuItem("Thresholding_thresholding",  ThresholdingAction.getAction());
		MenuUtil.addToMenu(igbService.getMenu("tools"), thresholdingMenuItem);
		thresholdingMenuItem.setEnabled(false);
		selectionListener = new SelectionListener(thresholdingMenuItem);
		Selections.addRefreshSelectionListener(selectionListener);
	}

	@Override
	public void start(BundleContext bundleContext_) throws Exception {
		this.bundleContext = bundleContext_;
    	if (CommonUtils.getInstance().isExit(bundleContext)) {
    		return;
    	}
    	ServiceReference<IGBService> igbServiceReference = bundleContext.getServiceReference(IGBService.class);

        if (igbServiceReference != null)
        {
        	IGBService igbService = bundleContext.getService(igbServiceReference);
        	registerServices(igbService);
        }
        else
        {
        	ServiceTracker<IGBService,Object> serviceTracker = new ServiceTracker<IGBService,Object>(bundleContext, IGBService.class, null) {
        	    public Object addingService(ServiceReference<IGBService> igbServiceReference) {
                	IGBService igbService = bundleContext.getService(igbServiceReference);
                   	registerServices(igbService);
                    return super.addingService(igbServiceReference);
        	    }
        	};
        	serviceTracker.open();
        }
    }

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		thresholdingActionRegistration.unregister();
		selectionListener = null;
	}
}
