package com.affymetrix.igb;

import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.genometryImpl.event.TierMaintenanceListener;
import com.affymetrix.genometryImpl.event.TierMaintenanceListenerHolder;
import com.affymetrix.genometryImpl.operator.annotation.AnnotationOperator;
import com.affymetrix.genometryImpl.operator.annotation.AnnotationOperatorHolder;
import com.affymetrix.genometryImpl.parsers.NibbleResiduesParser;
import com.affymetrix.genoviz.glyph.GlyphProcessorHolder;
import com.affymetrix.genoviz.glyph.GlyphProcessorHolder.GlyphProcessor;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.tiers.TierLabelManager;
import com.affymetrix.igb.window.service.IWindowService;

/**
 * OSGi Activator for igb bundle
 */
public class Activator implements BundleActivator {
	protected BundleContext bundleContext;
    String[] args;

	@Override
	public void start(BundleContext _bundleContext) throws Exception {
		this.bundleContext = _bundleContext;
        args = new String[]{};
        if (bundleContext.getProperty("args") != null) {
        	args = bundleContext.getProperty("args").split("[ ]*,[ ]*");
			if ("-convert".equals(args[0])) {
				String[] runArgs = Arrays.copyOfRange(args, 1, args.length);
				NibbleResiduesParser.main(runArgs);
				System.exit(0);
			}
        }
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
	public void stop(BundleContext _bundleContext) throws Exception {}

	private void addService(final ServiceHandler serviceHandler) {
		// register service - an extension point
		try {
			ServiceReference[] serviceReferences = bundleContext.getAllServiceReferences(serviceHandler.getClassName(), null);
			if (serviceReferences != null) {
				for (ServiceReference serviceReference : serviceReferences) {
					serviceHandler.addService(bundleContext.getService(serviceReference));
				}
			}
			bundleContext.addServiceListener(
				new ServiceListener() {
					@Override
					public void serviceChanged(ServiceEvent event) {
						ServiceReference serviceReference = event.getServiceReference();
						if (event.getType() == ServiceEvent.UNREGISTERING || event.getType() == ServiceEvent.MODIFIED || event.getType() == ServiceEvent.MODIFIED_ENDMATCH) {
							serviceHandler.removeService(bundleContext.getService(serviceReference));
						}
						if (event.getType() == ServiceEvent.REGISTERED || event.getType() == ServiceEvent.MODIFIED) {
							serviceHandler.addService(bundleContext.getService(serviceReference));
						}
					}
				}
			, "(objectClass=" + serviceHandler.getClassName() + ")");
		}
		catch (InvalidSyntaxException x) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "error loading/unloading " + serviceHandler.getClassName(), x.getMessage());
		}
	}

	private abstract class ServiceHandler {
		private Class<?> clazz;
		private ServiceHandler(Class<?> clazz) {
			super();
			this.clazz = clazz;
		}
		public String getClassName() {
			return clazz.getName();
		}
		public abstract void addService(Object o);
		public abstract void removeService(Object o);
	}

	/**
	 * method to start IGB, called when the window service is available,
	 * creates and initializes IGB and registers the IGBService
	 * add any extension points handling here
	 * @param windowServiceReference - the OSGi ServiceReference for the window service
	 */
	private void run(ServiceReference windowServiceReference) {
        IWindowService windowService = (IWindowService) bundleContext.getService(windowServiceReference);
        final IGB igb = new IGB();
        igb.init(args);
        final IGBTabPanel[] tabs = igb.setWindowService(windowService);
        // set IGBService
		bundleContext.registerService(IGBService.class.getName(), IGBServiceImpl.getInstance(), new Properties());
		// register tabs created in IGB itself - IGBTabPanel is an extension point
		for (IGBTabPanel tab : tabs) {
			bundleContext.registerService(IGBTabPanel.class.getName(), tab, new Properties());
		}
		addService(
			new ServiceHandler(TierLabelManager.PopupListener.class) {
				@Override
				public void addService(Object o) {
					igb.getMapView().getTierManager().addPopupListener((TierLabelManager.PopupListener)o);
				}
				@Override
				public void removeService(Object o) {
					igb.getMapView().getTierManager().removePopupListener((TierLabelManager.PopupListener)o);
				}
			}
		);
		addService(
			new ServiceHandler(TierLabelManager.TrackClickListener.class) {
				@Override
				public void addService(Object o) {
					igb.getMapView().getTierManager().addTrackClickListener((TierLabelManager.TrackClickListener)o);
				}
				@Override
				public void removeService(Object o) {}
			}
		);
		addService(
			new ServiceHandler(GlyphProcessor.class) {
				@Override
				public void addService(Object o) {
					GlyphProcessorHolder.getInstance().addGlyphProcessor((GlyphProcessor)o);
				}
				@Override
				public void removeService(Object o) {
					GlyphProcessorHolder.getInstance().removeGlyphProcessor((GlyphProcessor)o);
				}
			}
		);
		addService(
			new ServiceHandler(TierMaintenanceListener.class) {
				@Override
				public void addService(Object o) {
					TierMaintenanceListenerHolder.getInstance().addTierMaintenanceListener((TierMaintenanceListener)o);
				}
				@Override
				public void removeService(Object o) {
					TierMaintenanceListenerHolder.getInstance().removeTierMaintenanceListener((TierMaintenanceListener)o);
				}
			}
		);
		addService(
			new ServiceHandler(AnnotationOperator.class) {
				@Override
				public void addService(Object o) {
					AnnotationOperatorHolder.getInstance().addAnnotationOperator((AnnotationOperator)o);
				}
				@Override
				public void removeService(Object o) {
					AnnotationOperatorHolder.getInstance().removeAnnotationOperator((AnnotationOperator)o);
				}
			}
		);
	}
}
