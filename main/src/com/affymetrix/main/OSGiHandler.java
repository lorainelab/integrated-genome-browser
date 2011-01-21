package com.affymetrix.main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.StringMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.affymetrix.igb.osgi.service.IStopRoutine;

public class OSGiHandler implements IStopRoutine {
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("main");
	private static final String IGB_TIER_HEADER = "IGB-Tier";
	private static final String FORWARD_SLASH = "/";
	private List<String> addedRequiredPlugins;
	private static Felix felix;

	private static OSGiHandler instance = new OSGiHandler();
	public static OSGiHandler getInstance() {
		return instance;
	}

	private OSGiHandler() {
		addedRequiredPlugins = new ArrayList<String>();
	}
		 
	private String getAppDir() {
		// return PreferenceUtils.getAppDataDirectory();
		return "";
	}

	private String getCacheDir() {
		return getAppDir() + "cache/felix-cache";
	}

	public void clearCache() {
		deleteDirectory(new File(getCacheDir()));
	}

	private boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private final void loadFelix(String argArray) {
		Map configMap = new StringMap(false);
		configMap.put("org.osgi.framework.storage", getCacheDir());
		for (String key : BUNDLE.getString("pluginsConfigList").split(",")) {
			configMap.put(key, BUNDLE.getString(key));
		}
		configMap.put("args", argArray);
//        Runtime.getRuntime().addShutdownHook(new Thread("Felix Shutdown Hook") {
//        	public void run() {
//        		stop();
//        	}
//        });
        felix = new Felix(configMap);
	}

	public void addRequiredPlugin(String plugin) {
		addedRequiredPlugins.add(FORWARD_SLASH + plugin + ".jar");
	}

	private void loadUncachedJars(BundleContext bundleContext, HashSet<String> tierBundles, List<String> jars)
		throws Exception {
		for (String jarSpec : jars) {
			String[] parts = jarSpec.split(";");
			String jarName = parts[0];
			boolean startBundle = true;
			if (parts.length > 1 && parts[1].toLowerCase().startsWith("start=n")) {
				startBundle = false;
			}
			System.out.println("loading " + jarName);
			String location = OSGiHandler.class.getResource(jarName).toString();
			if (location != null){
				Bundle bundle = bundleContext.installBundle(location);
				if (startBundle) {
					bundle.start();
				}
				tierBundles.add(bundle.getSymbolicName());
			}
		}
	}

	public void startOSGi(String[] args) {
		loadFelix(Arrays.toString(args));

        try
        {
            felix.start();
         	HashSet<String> tier1Bundles = new HashSet<String>();
        	HashSet<String> tier2Bundles = new HashSet<String>();
            tier1Bundles.add(felix.getSymbolicName());
            BundleContext bundleContext = felix.getBundleContext();
            List<String> requiredJars = new ArrayList<String>(Arrays.asList(BUNDLE.getString("pluginsRequiredList").split(",")));
            requiredJars.addAll(addedRequiredPlugins);
            List<String> optionalJars = new ArrayList<String>(Arrays.asList(BUNDLE.getString("pluginsOptionalList").split(",")));
           	// load uncached required jars
           	loadUncachedJars(bundleContext, tier1Bundles, requiredJars);
           	// load uncached optional jars
          	loadUncachedJars(bundleContext, tier2Bundles, optionalJars);
			Logger.getLogger(getClass().getName()).log(Level.INFO, "OSGi is started");
        }
        catch (Exception ex)
        {
        	ex.printStackTrace(System.err);
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Could not create framework, plugins disabled: {0}", ex.getMessage());
        }
    }

	public int getTier(Bundle bundle) {
		if (bundle.getBundleId() == 0) { // system bundle
			return 0;
		}
		int tier = 3;
		String tierString = ((String)bundle.getHeaders().get(IGB_TIER_HEADER));
		if (tierString != null) {
			try {
				tier = Integer.parseInt(tierString.trim());
			}
			catch (Exception x) {}
		}
		return tier;
	}

	public void stop() {
        try
        {
            if (felix != null)
            {
            	// do not cache included (tier 1 and tier 2) bundles, so that they are reloaded with any updates
                BundleContext bundleContext = felix.getBundleContext();
               	for (Bundle bundle : bundleContext.getBundles()) {
               		int tier = getTier(bundle);
               		if (tier > 0 && tier < 3) {
               			bundle.uninstall();
               		}
               	}
            	felix.stop();
//            	felix.waitForStop(0);
            	felix = null;
            }
			Logger.getLogger(getClass().getName()).log(Level.INFO, "OSGi is stopped");
        }
        catch (Exception ex)
        {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Could not stop framework", ex.getMessage());
        }
	}
}