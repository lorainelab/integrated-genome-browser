/**
 *   Copyright (c) 2010 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.InputStream;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.StringMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.view.BundleRepositoryPrefsView;
import com.affymetrix.igb.window.service.IGetDefaultWindowService;
import com.affymetrix.igb.window.service.IWindowService;
import com.affymetrix.igb.window.service.WindowServiceListener;

public class OSGiHandler {
	public static int TAB_PLUGIN_PREFS = -1;
	private static final String FORWARD_SLASH = "/";

	private static Felix felix;
	private static IGBService service;
	private final List<WindowServiceListener> windowServiceListeners;

	private static OSGiHandler instance = new OSGiHandler();
	public static OSGiHandler getInstance() {
		return instance;
	}

	private OSGiHandler() {
		windowServiceListeners = new ArrayList<WindowServiceListener>();
	}
		 
	public void addWindowListener(WindowServiceListener windowServiceListener) {
		windowServiceListeners.add(windowServiceListener);
	}

	public void removeWindowListener(WindowServiceListener windowServiceListener) {
		windowServiceListeners.remove(windowServiceListener);
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	private final void loadFelix() {
		Map configMap = new StringMap(false);
		configMap.put("org.osgi.framework.storage", PreferenceUtils.getAppDataDirectory() + "cache/felix-cache");
		for (String key : BUNDLE.getString("pluginsConfigList").split(",")) {
			configMap.put(key, BUNDLE.getString(key));
		}
/*
		ResourceBundle manifest = ResourceBundle.getBundle("manifest");
		for (String key : manifest.keySet()) {
			configMap.put(key, manifest.getString(key));
		}
*/
		List list = new ArrayList();
        service = IGBServiceImpl.getInstance();
        list.add(service);
        configMap.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, list);
        Runtime.getRuntime().addShutdownHook(new Thread("Felix Shutdown Hook") {
        	public void run() {
        		stopOSGi();
        	}
        });
        felix = new Felix(configMap);
	}

	private void notifyWindowService(IWindowService windowService) {
        try {
            for (WindowServiceListener windowServiceListener : windowServiceListeners) {
            	windowServiceListener.addWindowService(windowService);
            }
        }
        catch (Exception ex) {
            System.out.println(this.getClass().getName() + " - Exception in Activator.start() -> " + ex.getMessage());
        }
	}

	public void startOSGi() {
        HashSet<String> tier1Bundles = new HashSet<String>(); // bundle symbolic names
        HashSet<String> tier2Bundles = new HashSet<String>(); // bundle symbolic names
		loadFelix();

        try
        {
            felix.start();
            tier1Bundles.add(felix.getSymbolicName());
            BundleContext bundleContext = felix.getBundleContext();
            List<String> requiredJars = new ArrayList<String>(Arrays.asList(BUNDLE.getString("pluginsRequiredList").split(",")));
            List<String> optionalJars = new ArrayList<String>(Arrays.asList(BUNDLE.getString("pluginsOptionalList").split(",")));
            // update cached local jars
           	for (Bundle bundle : bundleContext.getBundles()) {
				String location = bundle.getLocation();
				if(!location.contains(FORWARD_SLASH))
					continue;
				
				String jarName = location.substring(location.lastIndexOf(FORWARD_SLASH));

				//Check if it is local plugin then update it with local copy of plugin.
				InputStream istr = OSGiHandler.class.getResourceAsStream(jarName);
				if (istr != null) {
					bundle.update(istr);
					if (requiredJars.contains(jarName)) {
						tier1Bundles.add(bundle.getSymbolicName());
						requiredJars.remove(jarName);
					}
					if (optionalJars.contains(jarName)) {
						tier2Bundles.add(bundle.getSymbolicName());
						optionalJars.remove(jarName);
					}
					GeneralUtils.safeClose(istr);
				}
			}
           	// load uncached required jars
			for (String jarName : requiredJars) {
				String location = OSGiHandler.class.getResource(jarName).toString();
				if (location != null){
					Bundle bundle = bundleContext.installBundle(location);
					bundle.start();
					tier1Bundles.add(bundle.getSymbolicName());
				}
			}
			((IGBServiceImpl)service).setTier1Bundles(tier1Bundles);
           	// load uncached optional jars
			for (String jarName : optionalJars) {
				String location = OSGiHandler.class.getResource(jarName).toString();
				if(location != null){
					Bundle bundle = bundleContext.installBundle(location);
					bundle.start();
					tier2Bundles.add(bundle.getSymbolicName());
				}
			}
			((IGBServiceImpl)service).setTier2Bundles(tier2Bundles);
			// register IGB service
			bundleContext.registerService(IGBService.class.getName(), service, new Properties());

			IWindowService windowService;
			ServiceReference serviceReference = bundleContext.getServiceReference(IWindowService.class.getName());
			if (serviceReference == null) { // no window service registered, use default
				ServiceReference defaultServiceReference = bundleContext.getServiceReference(IGetDefaultWindowService.class.getName());
				if (defaultServiceReference == null) {
					Logger.getLogger(service.getClass().getName()).log(Level.SEVERE, "Could not find window service");
					System.exit(100);
				}
				IGetDefaultWindowService getDefaultWindowService = (IGetDefaultWindowService) bundleContext.getService(defaultServiceReference);
				windowService = getDefaultWindowService.getWindowService();
				bundleContext.registerService(IWindowService.class.getName(), windowService, new Properties());
			}
			else {
				windowService = (IWindowService) bundleContext.getService(serviceReference);
			}
           	notifyWindowService(windowService);
        }
        catch (Exception ex)
        {
        	ex.printStackTrace(System.err);
			Logger.getLogger(service.getClass().getName()).log(
					Level.WARNING, "Could not create framework, plugins disabled: {0}", ex.getMessage());
        }

		final PreferencesPanel pp = PreferencesPanel.getSingleton();
		TAB_PLUGIN_PREFS = pp.addPrefEditorComponent(new BundleRepositoryPrefsView());
    }

	private void stopOSGi() {
        try
        {
            if (felix != null)
            {
            	felix.stop();
//            	felix.waitForStop(0);
            	felix = null;
            }
        }
        catch (Exception ex)
        {
			Logger.getLogger(service.getClass().getName()).log(
					Level.WARNING, "Could not stop framework, plugins disabled: {0}", ex.getMessage());
        }
	}
}