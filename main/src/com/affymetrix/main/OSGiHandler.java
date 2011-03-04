package com.affymetrix.main;

import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;

import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.StringMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.affymetrix.common.CommonUtils;

/**
 * all OSGi functionality is handled here. Singleton pattern.
 */
public class OSGiHandler {
	private static final ResourceBundle CONFIG_BUNDLE = ResourceBundle.getBundle("config");
	private static final String FORWARD_SLASH = "/";
	private static Felix felix;

	private static OSGiHandler instance = new OSGiHandler();
	public static OSGiHandler getInstance() {
		return instance;
	}

	/**
	 * get the OSGi cache directory
	 * @return the OSGi cache directory
	 */
	private String getCacheDir() {
		return CommonUtils.getInstance().getAppDataDirectory() + "cache/v" + CommonUtils.getInstance().getAppVersionFull() + "-bundle-cache";
	}

	/**
	 * clear the OSGi cache
	 */
	public void clearCache() {
		deleteDirectory(new File(getCacheDir()));
	}

	/**
	 * delete the specified directory, and all its contents
	 * @param path the path of the directory to delete
	 * @return true if and only if the file or directory is successfully deleted; false otherwise
	 */
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

	/**
	 * load the OSGi implementation - Apache felix.
	 * @param argArray the command line arguments joined into a String
	 */
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

	/**
	 * start OSGi, load and start the OSGi implementation
	 * load the embedded bundles, if not cached, and start all bundles
	 * @param args the command line arguments
	 */
	public void startOSGi(String[] args) {
		setLaf();

		loadFelix(Arrays.toString(args));

        try
        {
            felix.start();
            BundleContext bundleContext = felix.getBundleContext();
            if (bundleContext.getBundles().length <= 1) {
	           	// load embedded bundles
	            String[] jarNames = getResourceListing(OSGiHandler.class, "");
	    		for (String fileName : jarNames) {
	    			if (fileName.endsWith(".jar")) {
		     			URL locationURL = OSGiHandler.class.getResource(FORWARD_SLASH + fileName);
		    			if (locationURL != null){
							Logger.getLogger(getClass().getName()).log(Level.INFO, "loading {0}",new Object[]{fileName});
							try {
								bundleContext.installBundle(locationURL.toString());
							}
			    	        catch (Exception ex)
			    	        {
			    	        	ex.printStackTrace(System.err);
								Logger.getLogger(getClass().getName()).log(Level.WARNING, "Could not install {0}",new Object[]{fileName});
			    	        }
		    			}
		    			else{
							Logger.getLogger(getClass().getName()).log(Level.WARNING, "Could not find {0}",new Object[]{fileName});
						}
	    			}
	    		}
            }
    		for (Bundle bundle : bundleContext.getBundles()) {
    			bundle.start();
    		}
          	Logger.getLogger(getClass().getName()).log(Level.INFO, "OSGi is started");
        }
        catch (Exception ex)
        {
        	ex.printStackTrace(System.err);
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Could not create framework, plugins disabled: {0}", ex.getMessage());
        }
    }

	private static final String FILE_PROTOCOL = "file";
	private static final String WEB_PROTOCOL = "http";
	private static final String SECURE_WEB_PROTOCOL = "https";

	/**
	 * List directory contents for a resource folder. Not recursive. This is
	 * basically a brute-force implementation. Works for regular files and also
	 * JARs.
	 *
	 * @author Greg Briggs
	 *    modified LF 02/02/2011 to support java web start
	 * @param clazz
	 *            Any java class that lives in the same place as the resources
	 *            you want.
	 * @param path
	 *            Should end with "/", but not start with one.
	 * @return Just the name of each member item, not the full paths.
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	private String[] getResourceListing(Class<?> clazz, String path)
			throws URISyntaxException, IOException {
		URL dirURL = clazz.getClassLoader().getResource(path);
		if (dirURL != null && dirURL.getProtocol().equals("file")) {
			/* A file path: easy enough */
			return new File(dirURL.toURI()).list();
		}

		if (dirURL == null) {
			/*
			 * In case of a jar file, we can't actually find a directory. Have
			 * to assume the same jar as clazz.
			 */
			String me = clazz.getName().replace(".", "/") + ".class";
			dirURL = clazz.getClassLoader().getResource(me);
		}

		if (dirURL.getProtocol().equals("jar")) {
			/* A JAR path */
			String protocol = dirURL.getPath().substring(0,
					dirURL.getPath().indexOf(":"));
			Set<String> result = new HashSet<String>(); // avoid duplicates in
														// case it is a
														// subdirectory
			if (FILE_PROTOCOL.equals(protocol)) {
				String jarPath = dirURL.getPath().substring(5,
						dirURL.getPath().indexOf("!")); // strip out only the
														// JAR
														// file
				JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
				Enumeration<JarEntry> entries = jar.entries(); // gives ALL
																// entries
																// in jar
				while (entries.hasMoreElements()) {
					String name = entries.nextElement().getName();
					if (name.startsWith(path)) { // filter according to the path
						String entry = name.substring(path.length());
						int checkSubdir = entry.indexOf("/");
						if (checkSubdir >= 0) {
							// if it is a subdirectory, we just return the
							// directory
							// name
							entry = entry.substring(0, checkSubdir);
						}
						result.add(entry);
					}
				}
			}
			if (WEB_PROTOCOL.equals(protocol) || SECURE_WEB_PROTOCOL.equals(protocol)) {
				final ProtectionDomain domain = OSGiHandler.class.getProtectionDomain();
				final CodeSource source = domain.getCodeSource();
				URL url = source.getLocation();
				if (url.toExternalForm().endsWith(".jar")) {
					try {
						JarInputStream jarStream = new JarInputStream(url.openStream(), false);
						for (String entry : jarStream.getManifest().getEntries().keySet()) {
							result.add(entry);
						}
					}
					catch (IOException e) {
						Logger.getLogger(getClass().getName()).log(Level.WARNING, "error reading manifest", e.getMessage());
					}
				}
			}
			if (result.size() > 0) {
				return result.toArray(new String[result.size()]);
			}
		}

		throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
	}

	/**
	 * set the Swing look and feel
	 */
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
