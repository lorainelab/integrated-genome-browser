package com.affymetrix.igb.thresholding;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.swing.AMenuItem;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.XServiceRegistrar;
import com.affymetrix.igb.shared.Selections;
import com.affymetrix.igb.thresholding.action.ThresholdingAction;

public class Activator extends XServiceRegistrar<IGBService> implements BundleActivator {

    private SelectionListener selectionListener;

    public Activator() {
        super(IGBService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IGBService igbService) throws Exception {
        ThresholdingAction.createAction(igbService);

        JRPMenuItem thresholdingMenuItem = new JRPMenuItem("Thresholding_thresholding", ThresholdingAction.getAction());
        thresholdingMenuItem.setEnabled(false);
        selectionListener = new SelectionListener(thresholdingMenuItem);
        Selections.addRefreshSelectionListener(selectionListener);

        return new ServiceRegistration[]{
            bundleContext.registerService(GenericAction.class, ThresholdingAction.getAction(), null),
            bundleContext.registerService(AMenuItem.class, new AMenuItem(thresholdingMenuItem, "tools"), null)
        };
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        super.stop(bundleContext);
        selectionListener = null;
    }
}
