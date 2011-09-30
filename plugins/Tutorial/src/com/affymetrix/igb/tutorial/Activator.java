package com.affymetrix.igb.tutorial;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JMenuItem;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenu;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.window.service.IWindowService;

public class Activator implements BundleActivator {
	private BundleContext bundleContext;
	private static final String DEFAULT_PREFS_TUTORIAL_RESOURCE = "/tutorial_default_prefs.xml";

	private void handleWindowService(JRPMenu help_menu, ServiceReference windowServiceReference) {
		loadDefaultTutorialPrefs();
		try {
	        IWindowService windowService = (IWindowService) bundleContext.getService(windowServiceReference);
	    	ServiceReference igbServiceReference = bundleContext.getServiceReference(IGBService.class.getName());
        	IGBService igbService = (IGBService) bundleContext.getService(igbServiceReference);
	    	final TutorialManager tutorialManager = new TutorialManager(igbService, windowService);
	    	GenericActionHolder.getInstance().addGenericActionListener(tutorialManager);
			JRPMenu tutorialMenu = new JRPMenu("Tutorial_tutorialMenu", "Tutorials");
			Properties tutorials = new Properties();
			Preferences tutorialsNode = getTutorialsNode();
			for (String key : tutorialsNode.keys()) {
				String tutorialUri = tutorialsNode.get(key, null);
				tutorials.load(new URL(tutorialUri + "/tutorials.properties").openStream());
				Enumeration<?> tutorialNames = tutorials.propertyNames();
				while (tutorialNames.hasMoreElements()) {
					String name = (String)tutorialNames.nextElement();
					RunTutorialAction rta = new RunTutorialAction(tutorialManager, name, tutorialUri + "/" + name + ".txt");
					JMenuItem item = new JMenuItem(rta);
					tutorialMenu.add(item);
				}
			}
			help_menu.add(tutorialMenu);
        }
        catch (Exception ex) {
            System.out.println(this.getClass().getName() + " - Exception in handleWindowService() -> " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
	}
	
	private void handleIGBService(ServiceReference igbServiceReference) {
        try {
        	IGBService igbService = (IGBService) bundleContext.getService(igbServiceReference);
    		final JRPMenu help_menu = igbService.getHelpMenu();

    		ServiceReference windowServiceReference = bundleContext.getServiceReference(IWindowService.class.getName());
            if (windowServiceReference != null)
            {
            	handleWindowService(help_menu, windowServiceReference);
            }
            else
            {
            	ServiceTracker serviceTracker = new ServiceTracker(bundleContext, IWindowService.class.getName(), null) {
            	    public Object addingService(ServiceReference windowServiceReference) {
            	    	handleWindowService(help_menu, windowServiceReference);
            	        return super.addingService(windowServiceReference);
            	    }
            	};
            	serviceTracker.open();
            }
        }
        catch (Exception ex) {
            System.out.println(this.getClass().getName() + " - Exception in handleIGBService() -> " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		this.bundleContext = bundleContext;
    	ServiceReference igbServiceReference = bundleContext.getServiceReference(IGBService.class.getName());

        if (igbServiceReference != null) {
        	handleIGBService(igbServiceReference);
        }
        else {
        	ServiceTracker serviceTracker = new ServiceTracker(bundleContext, IGBService.class.getName(), null) {
        	    public Object addingService(ServiceReference igbServiceReference) {
        	    	handleIGBService(igbServiceReference);
        	        return super.addingService(igbServiceReference);
        	    }
        	};
        	serviceTracker.open();
        }
    }

	@Override
	public void stop(BundleContext bundleContext) throws Exception {}

	private void loadDefaultTutorialPrefs() {
		// Return if there are already Preferences defined.  (Since we define keystroke shortcuts, this is a reasonable test.)
		try {
			if ((getTopNode()).nodeExists("tutorials")) {
				return;
			}
		} catch (BackingStoreException ex) {
		}

		InputStream default_prefs_stream = null;
		/**  load default prefs from jar (with Preferences API).  This will be the standard method soon.*/
		try {
			default_prefs_stream = Activator.class.getResourceAsStream(DEFAULT_PREFS_TUTORIAL_RESOURCE);
			System.out.println("loading default tutorial preferences from: " + DEFAULT_PREFS_TUTORIAL_RESOURCE);
			Preferences.importPreferences(default_prefs_stream);
			//prefs_parser.parse(default_prefs_stream, "", prefs_hash);
		} catch (Exception ex) {
			System.out.println("Problem parsing prefs from: " + DEFAULT_PREFS_TUTORIAL_RESOURCE);
			ex.printStackTrace();
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
