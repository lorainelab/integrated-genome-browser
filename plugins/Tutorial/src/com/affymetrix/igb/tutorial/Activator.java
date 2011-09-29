package com.affymetrix.igb.tutorial;

import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import javax.swing.JMenuItem;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenu;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.window.service.IWindowService;

public class Activator implements BundleActivator {
	private BundleContext bundleContext;
	private static final String tutorialUri = "http://research-pub.gene.com/igb_plugins/tutorials/";

	private void handleWindowService(JRPMenu help_menu, ServiceReference windowServiceReference) {
        try {
	        IWindowService windowService = (IWindowService) bundleContext.getService(windowServiceReference);
	    	ServiceReference igbServiceReference = bundleContext.getServiceReference(IGBService.class.getName());
        	IGBService igbService = (IGBService) bundleContext.getService(igbServiceReference);
	    	final TutorialManager tutorialManager = new TutorialManager(igbService, windowService);
	    	GenericActionHolder.getInstance().addGenericActionListener(tutorialManager);
			JRPMenu tutorialMenu = new JRPMenu("Tutorial_tutorialMenu", "Tutorials");
			Properties tutorials = new Properties();
			tutorials.load(new URL(tutorialUri + "tutorials.properties").openStream());
			Enumeration<?> tutorialNames = tutorials.propertyNames();
			while (tutorialNames.hasMoreElements()) {
				String name = (String)tutorialNames.nextElement();
				RunTutorialAction rta = new RunTutorialAction(tutorialManager, name, tutorialUri + "/" + name + ".txt");
				JMenuItem item = new JMenuItem(rta);
				tutorialMenu.add(item);
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
}
