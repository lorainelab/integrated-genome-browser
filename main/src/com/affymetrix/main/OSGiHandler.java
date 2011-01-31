package com.affymetrix.main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.StringMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class OSGiHandler {
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("main");
	private static final ResourceBundle CONFIG_BUNDLE = ResourceBundle.getBundle("config");
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
		for (String key : CONFIG_BUNDLE.keySet()) {
			configMap.put(key, CONFIG_BUNDLE.getString(key));
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

	public void startOSGi(String[] args) {
		setLaf();

		loadFelix(Arrays.toString(args));

        try
        {
            felix.start();
            BundleContext bundleContext = felix.getBundleContext();
            List<String> jars = new ArrayList<String>(Arrays.asList(BUNDLE.getString("pluginsList").split(",")));
            jars.addAll(addedRequiredPlugins);
           	// load uncached jars
    		for (String jarSpec : jars) {
    			String[] parts = jarSpec.split(";");
    			String jarName = parts[0];
    			boolean startBundle = true;
    			if (parts.length > 1 && parts[1].toLowerCase().startsWith("start=n")) {
    				startBundle = false;
    			}
    			URL locationURL = OSGiHandler.class.getResource(jarName);
    			if (locationURL != null){
					Logger.getLogger(getClass().getName()).log(Level.INFO, "loading {0}",new Object[]{jarName});
    				Bundle bundle = bundleContext.installBundle(locationURL.toString());
    				if (startBundle) {
    					bundle.start();
    				}
    			}else{
					Logger.getLogger(getClass().getName()).log(Level.WARNING, "Could not find {0}",new Object[]{jarName});
				}
    		}
          	Logger.getLogger(getClass().getName()).log(Level.INFO, "OSGi is started");
        }
        catch (Exception ex)
        {
        	ex.printStackTrace(System.err);
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Could not create framework, plugins disabled: {0}", ex.getMessage());
        }
    }
	
	private static void setLaf() {

		// Turn on anti-aliased fonts. (Ignored prior to JDK1.5)
		System.setProperty("swing.aatext", "true");

		// Letting the look-and-feel determine the window decorations would
		// allow exporting the whole frame, including decorations, to an eps file.
		// But it also may take away some things, like resizing buttons, that the
		// user is used to in their operating system, so leave as false.
		JFrame.setDefaultLookAndFeelDecorated(false);

		// if this is != null, then the user-requested l-and-f has already been applied
		if (System.getProperty("swing.defaultlaf") == null) {
			String os = System.getProperty("os.name");
			if (os != null && os.toLowerCase().contains("windows")) {
				try {
					// It this is Windows, then use the Windows look and feel.
					Class<?> cl = Class.forName("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
					LookAndFeel look_and_feel = (LookAndFeel) cl.newInstance();

					if (look_and_feel.isSupportedLookAndFeel()) {
						UIManager.setLookAndFeel(look_and_feel);
					}
				} catch (Exception ulfe) {
					// Windows look and feel is only supported on Windows, and only in
					// some version of the jre.  That is perfectly ok.
				}
			}
		}
	}
}