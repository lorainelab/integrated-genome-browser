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
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.StringMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.view.BundleRepositoryPrefsView;

public class OSGiHandler {
	public static int TAB_PLUGIN_PREFS = -1;
	private static final String LOCAL_PLUGINS_URL = "classpath:";

	private static Felix felix;
	private static IGBService service;

	private static OSGiHandler instance = new OSGiHandler();
	public static OSGiHandler getInstance() {
		return instance;
	}

	private OSGiHandler() {}

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

	public void startOSGi() {
        HashSet<String> tier1Bundles = new HashSet<String>(); // bundle symbolic names
        HashSet<String> tier2Bundles = new HashSet<String>(); // bundle symbolic names
		loadFelix();
		String[] pluginUrls;
		try {
			pluginUrls = BUNDLE.getString("pluginUrls").split(",");
		}
		catch (MissingResourceException x) {
			pluginUrls = new String[]{LOCAL_PLUGINS_URL};
		}

        try
        {
            felix.start();
            tier1Bundles.add(felix.getSymbolicName());
            BundleContext bundleContext = felix.getBundleContext();
            List<String> requiredJars = new ArrayList<String>(Arrays.asList(BUNDLE.getString("pluginsRequiredList").split(",")));
            List<String> optionalJars = new ArrayList<String>(Arrays.asList(BUNDLE.getString("pluginsOptionalList").split(",")));
            // update cached local jars
           	for (Bundle bundle : bundleContext.getBundles()) {
           		String jarName = getLocalJarName(pluginUrls, bundle.getLocation());
           		if (jarName != null) {
           			if (requiredJars.contains(jarName)) {
           				bundle.update();
                    	tier1Bundles.add(bundle.getSymbolicName());
           				requiredJars.remove(jarName);
           			}
           			if (optionalJars.contains(jarName)) {
           				bundle.update();
                    	tier2Bundles.add(bundle.getSymbolicName());
           				optionalJars.remove(jarName);
          			}
           		}
           	}
           	// load uncached required jars
			for (String jarName : requiredJars) {
				String bundleName = loadBundleRetry(bundleContext, pluginUrls, jarName);
            	tier1Bundles.add(bundleName);
			}
			((IGBServiceImpl)service).setTier1Bundles(tier1Bundles);
           	// load uncached optional jars
			for (String jarName : optionalJars) {
				String bundleName = loadBundleRetry(bundleContext, pluginUrls, jarName);
            	tier2Bundles.add(bundleName);
			}
			((IGBServiceImpl)service).setTier2Bundles(tier2Bundles);
			// register IGB service
			bundleContext.registerService(IGBService.class.getName(), service, new Properties());
        }
        catch (Exception ex)
        {
			Logger.getLogger(service.getClass().getName()).log(
					Level.SEVERE, "Could not create framework, plugins disabled: {0}", ex.getMessage());
        }

		final PreferencesPanel pp = PreferencesPanel.getSingleton();
		TAB_PLUGIN_PREFS = pp.addPrefEditorComponent(new BundleRepositoryPrefsView());
    }

	private String loadBundleRetry(BundleContext bundleContext, String[] pluginUrls, String jarName) {
    	Bundle bundle = null;
    	for (int i = 0; i < pluginUrls.length && bundle == null; i++) {
    		try {
    			bundle = bundleContext.installBundle(pluginUrls[i] + jarName);
    		}
    		catch (Exception x) {
    			bundle = null;
    		}
    	}
    	if (bundle == null) {
			Logger.getLogger(service.getClass().getName()).log(Level.SEVERE, "Could not load plugin " + jarName);
			return null;
    	}
   		try {
			bundle.start();
   		}
   		catch (BundleException x) {
   			Logger.getLogger(service.getClass().getName()).log(Level.SEVERE, "Could not start plugin " + jarName, x);
   		}
		return bundle.getSymbolicName();
	}

	private String getLocalJarName(String[] pluginUrls, String location) {
		String localJarName = null;
    	for (int i = 0; i < pluginUrls.length && localJarName == null; i++) {
       		if (location.startsWith(pluginUrls[i])) {
       			localJarName = location.substring(pluginUrls[i].length());
    		}
    	}
		return localJarName;
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
