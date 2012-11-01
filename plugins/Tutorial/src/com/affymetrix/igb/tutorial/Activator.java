package com.affymetrix.igb.tutorial;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genoviz.swing.AMenuItem;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenu;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.window.service.IWindowService;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import javax.swing.JMenuItem;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
	
	private static final String DEFAULT_PREFS_TUTORIAL_RESOURCE = "/tutorial_default_prefs.xml";
	private static final Logger ourLogger =
			Logger.getLogger(Activator.class.getPackage().getName());

	private BundleContext bundleContext;
	private ServiceRegistration<AMenuItem> menuRegistration;
	
	private void handleWindowService(ServiceReference<IWindowService> windowServiceReference) {
		loadDefaultTutorialPrefs();
		try {
			IWindowService windowService = bundleContext.getService(windowServiceReference);
			ServiceReference<IGBService> igbServiceReference = bundleContext.getServiceReference(IGBService.class);
			IGBService igbService = bundleContext.getService(igbServiceReference);
			final TutorialManager tutorialManager = new TutorialManager(igbService, windowService);
			GenericActionHolder.getInstance().addGenericActionListener(tutorialManager);
			JRPMenu tutorialMenu = new JRPMenu("Tutorial_tutorialMenu", "Tutorials");
			Properties tutorials = new Properties();
			Preferences tutorialsNode = getTutorialsNode();
			try {
				for (String key : tutorialsNode.keys()) {
					String tutorialUri = tutorialsNode.get(key, null);
					tutorials.clear();
					tutorials.load(new URL(tutorialUri + "/tutorials.properties").openStream());
					Enumeration<?> tutorialNames = tutorials.propertyNames();
					while (tutorialNames.hasMoreElements()) {
						String name = (String) tutorialNames.nextElement();
						String description = (String) tutorials.get(name);
						RunTutorialAction rta = new RunTutorialAction(tutorialManager, name + " - " + description, tutorialUri + "/" + name);
						JMenuItem item = new JMenuItem(rta);
						tutorialMenu.add(item);
					}
				}
			menuRegistration = bundleContext.registerService(AMenuItem.class, new AMenuItem(tutorialMenu, "help"), null);
			} catch (FileNotFoundException fnfe) {
				ourLogger.log(Level.WARNING,
						"Could not find file {0}.\n          coninuing...",
						fnfe.getMessage());
			} catch (java.net.ConnectException ce) {
				ourLogger.log(Level.WARNING,
						"Could not connect: {0}.\n          coninuing...",
						ce.getMessage());
			}
		} catch (Exception ex) {
			ourLogger.logp(Level.SEVERE, this.getClass().getName(),
					"handleWindowService", "?", ex);
			ourLogger.severe("          continuing...");
		}
	}

	private void handleIGBService(ServiceReference<IGBService> igbServiceReference) {
		try {
			ServiceReference<IWindowService> windowServiceReference = bundleContext.getServiceReference(IWindowService.class);
			if (windowServiceReference != null) {
				handleWindowService(windowServiceReference);
			} else {
				ServiceTracker<IWindowService, Object> serviceTracker = new ServiceTracker<IWindowService, Object>(bundleContext, IWindowService.class.getName(), null) {
					@Override
					public Object addingService(ServiceReference<IWindowService> windowServiceReference) {
						handleWindowService(windowServiceReference);
						return super.addingService(windowServiceReference);
					}
				};
				serviceTracker.open();
			}
		} catch (Exception ex) {
			ourLogger.logp(Level.SEVERE, this.getClass().getName(),
					"handleIGBService", "?", ex);
			ourLogger.severe("          continuing...");
		}
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		this.bundleContext = bundleContext;
    	if (CommonUtils.getInstance().isExit(bundleContext)) {
    		return;
    	}
		ServiceReference<IGBService> igbServiceReference = bundleContext.getServiceReference(IGBService.class);

		if (igbServiceReference != null) {
			handleIGBService(igbServiceReference);
		} else {
			ServiceTracker<IGBService, Object> serviceTracker = new ServiceTracker<IGBService, Object>(bundleContext, IGBService.class.getName(), null) {

				@Override
				public Object addingService(ServiceReference<IGBService> igbServiceReference) {
						handleIGBService(igbServiceReference);
					return super.addingService(igbServiceReference);
				}
			};
			serviceTracker.open();
		}
		initActions();
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		if(menuRegistration != null){
			menuRegistration.unregister();
		}
	}

	private void initActions(){
		TweeningZoomAction.getAction();
		VerticalStretchZoomAction.getAction();
	}
	
	/**
	 * Load default prefs from jar (with Preferences API).
	 * This will be the standard method soon.
	 */
	private void loadDefaultTutorialPrefs() {

		InputStream default_prefs_stream = null;
		try {
			ourLogger.log(Level.INFO, "loading default tutorial preferences from: {0}",
					DEFAULT_PREFS_TUTORIAL_RESOURCE);
			default_prefs_stream = Activator.class.getResourceAsStream(DEFAULT_PREFS_TUTORIAL_RESOURCE);
			Preferences.importPreferences(default_prefs_stream);
			//prefs_parser.parse(default_prefs_stream, "", prefs_hash);
		} catch (InvalidPreferencesFormatException ex) {
			ourLogger.log(Level.SEVERE, DEFAULT_PREFS_TUTORIAL_RESOURCE, ex);
		} catch (IOException ex) {
			ourLogger.log(Level.SEVERE,	DEFAULT_PREFS_TUTORIAL_RESOURCE, ex);
			ourLogger.log(Level.INFO, "          continuing...");
		} finally {
			GeneralUtils.safeClose(default_prefs_stream);
		}
	}

	private Preferences getTopNode() {
		return Preferences.userRoot().node("/com/affymetrix/igb");
	}

	private Preferences getTutorialsNode() {
		return getTopNode().node("tutorials");
	}
}
