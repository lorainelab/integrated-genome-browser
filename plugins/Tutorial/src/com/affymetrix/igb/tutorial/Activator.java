package com.affymetrix.igb.tutorial;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenu;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.window.service.IWindowService;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.prefs.Preferences;
import javax.swing.JMenuItem;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

	private BundleContext bundleContext;
	private static final String DEFAULT_PREFS_TUTORIAL_RESOURCE = "/tutorial_default_prefs.xml";

	private void handleWindowService(JRPMenu help_menu, ServiceReference<IWindowService> windowServiceReference) {
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
				help_menu.add(tutorialMenu);
			} catch (FileNotFoundException fnfe) {
				System.out.println("Activator.handleWindowService: " + fnfe);
				System.out.println("          continuing...");
			} catch (java.net.ConnectException ce) {
				System.out.println("Activator.handleWindowService: " + ce);
				System.out.println("          continuing...");
			}
		} catch (Exception ex) {
			System.out.println(this.getClass().getName() + " - Exception in handleWindowService() -> " + ex.getMessage());
			ex.printStackTrace(System.out);
			System.out.println("          continuing...");
		}
	}

	private void handleIGBService(ServiceReference<IGBService> igbServiceReference) {
		try {
			IGBService igbService = bundleContext.getService(igbServiceReference);
			final JRPMenu help_menu = igbService.getMenu("help");

			ServiceReference<IWindowService> windowServiceReference = bundleContext.getServiceReference(IWindowService.class);
			if (windowServiceReference != null) {
				handleWindowService(help_menu, windowServiceReference);
			} else {
				ServiceTracker<IWindowService, Object> serviceTracker = new ServiceTracker<IWindowService, Object>(bundleContext, IWindowService.class.getName(), null) {

					@Override
					public Object addingService(ServiceReference<IWindowService> windowServiceReference) {
						handleWindowService(help_menu, windowServiceReference);
						return super.addingService(windowServiceReference);
					}
				};
				serviceTracker.open();
			}
		} catch (Exception ex) {
			System.out.println(this.getClass().getName() + " - Exception in handleIGBService() -> " + ex.getMessage());
			ex.printStackTrace(System.out);
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
	}

	private void initActions(){
		TweeningZoomAction.getAction();
		VerticalStretchZoomAction.getAction();
	}
	
	private void loadDefaultTutorialPrefs() {
//		// Return if there are already Preferences defined.
//		// Since we define keystroke shortcuts, this is a reasonable test.
//		try {
//			if ((getTopNode()).nodeExists("tutorials")) {
//				return;
//			}
//		} catch (BackingStoreException ex) {
//		}

		InputStream default_prefs_stream = null;
		/**
		 * load default prefs from jar (with Preferences API). This will be the
		 * standard method soon.
		 */
		try {
			default_prefs_stream = Activator.class.getResourceAsStream(DEFAULT_PREFS_TUTORIAL_RESOURCE);
			System.out.println("loading default tutorial preferences from: "
					+ DEFAULT_PREFS_TUTORIAL_RESOURCE);
			Preferences.importPreferences(default_prefs_stream);
			//prefs_parser.parse(default_prefs_stream, "", prefs_hash);
		} catch (Exception ex) {
			System.out.println("Problem parsing prefs from: "
					+ DEFAULT_PREFS_TUTORIAL_RESOURCE
					+ ": " + ex);
			ex.printStackTrace();
			System.out.println("          continuing...");
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
