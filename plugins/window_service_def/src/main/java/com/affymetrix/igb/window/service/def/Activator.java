package com.affymetrix.igb.window.service.def;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.common.ExtensionPointListener;
import com.affymetrix.igb.service.api.IGBTabPanel;
import com.affymetrix.igb.service.api.IWindowRoutine;
import com.affymetrix.igb.window.service.IWindowService;

public class Activator implements BundleActivator {

    private static final String SERVICE_FILTER = "(objectClass=" + IGBTabPanel.class.getName() + ")";
    private static final String TAB_PANEL_CATEGORY = "IGBTabPanel-";

    private void addTab(BundleContext bundleContext, ServiceReference<IGBTabPanel> serviceReference, WindowServiceDefaultImpl windowServiceDefaultImpl) {
        IGBTabPanel panel = bundleContext.getService(serviceReference);
        windowServiceDefaultImpl.addTab(panel);
    }

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        if (CommonUtils.getInstance().isExit(bundleContext)) {
            return;
        }
        // Adding it first should be ok for now. But what if service was added 
        // after start routine was already called? 
        final WindowServiceDefaultImpl windowServiceDefaultImpl = new WindowServiceDefaultImpl();
        ExtensionPointHandler<IWindowRoutine> stopRoutineExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, IWindowRoutine.class);
        stopRoutineExtensionPoint.addListener(
                new ExtensionPointListener<IWindowRoutine>() {
                    @Override
                    public void addService(IWindowRoutine routine) {
                        windowServiceDefaultImpl.addStopRoutine(routine);
                    }

                    @Override
                    public void removeService(IWindowRoutine routine) {
                        windowServiceDefaultImpl.removeStopRoutine(routine);
                    }
                }
        );
        bundleContext.registerService(IWindowService.class.getName(), windowServiceDefaultImpl, null);
        @SuppressWarnings("unchecked")
        ServiceReference<IGBTabPanel>[] serviceReferences = (ServiceReference<IGBTabPanel>[]) bundleContext.getAllServiceReferences(IGBTabPanel.class.getName(), null);
        if (serviceReferences != null) {
            for (ServiceReference<IGBTabPanel> serviceReference : serviceReferences) {
                addTab(bundleContext, serviceReference, windowServiceDefaultImpl);
            }            
            windowServiceDefaultImpl.showTabs();
        }
        try {
            bundleContext.addServiceListener(
                    event -> {
                        @SuppressWarnings("unchecked")
                        ServiceReference<IGBTabPanel> serviceReference = (ServiceReference<IGBTabPanel>) event.getServiceReference();
                        if (event.getType() == ServiceEvent.UNREGISTERING || event.getType() == ServiceEvent.MODIFIED || event.getType() == ServiceEvent.MODIFIED_ENDMATCH) {
                            windowServiceDefaultImpl.removeTab(bundleContext.getService(serviceReference));
                        }
                        if (event.getType() == ServiceEvent.REGISTERED || event.getType() == ServiceEvent.MODIFIED) {
                            addTab(bundleContext, serviceReference, windowServiceDefaultImpl);
                        }
                    }, SERVICE_FILTER);
        } catch (InvalidSyntaxException x) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, WindowServiceDefaultImpl.BUNDLE.getString("loadError"), x.getMessage());
        }
//		bundleContext.registerService(IStopRoutine.class, 
//			new IStopRoutine() {
//				@Override
//				public void stop() {
//					windowServiceDefaultImpl.shutdown();
//				}
//			},
//			null
//		);
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {

    }
}
