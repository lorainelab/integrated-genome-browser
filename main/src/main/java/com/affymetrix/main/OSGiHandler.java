package com.affymetrix.main;

import com.affymetrix.common.CommonUtils;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * all OSGi functionality is handled here. Singleton pattern.
 */
public class OSGiHandler {

    private static final ResourceBundle CONFIG_BUNDLE = ResourceBundle.getBundle("config");
    private static final Logger ourLogger = Logger.getLogger(OSGiHandler.class.getPackage().getName());
    private Framework framework;
    private String bundlePathToInstall;
    private String bundleSymbolicNameToUninstall;

    public static final boolean IS_WINDOWS
            = System.getProperty("os.name").toLowerCase().contains("windows");
    public static final boolean IS_MAC
            = System.getProperty("os.name").toLowerCase().contains("mac");
    public static final boolean IS_LINUX
            = System.getProperty("os.name").toLowerCase().contains("linux");

    private static final OSGiHandler instance = new OSGiHandler();

    public static OSGiHandler getInstance() {
        return instance;
    }

    private OSGiHandler() {
    }

    public static void main(final String[] args) {
        getInstance().startOSGi(args);
    }

    /**
     * start OSGi, load and start the OSGi implementation load the embedded
     * bundles, if not cached, and start all bundles
     *
     * @param args the command line arguments
     */
    public void startOSGi(String[] args) {
        if (isDevelopmentMode()) {
            deleteDirectory(new File(getCacheFolder()));
        }
        if (CommonUtils.getInstance().getArg("-cbc", args) != null) { // just clear bundle cache and return
            clearCache();
            return;
        }
        ourLogger.log(Level.INFO, "Starting OSGi");
        setLaf();

        ourLogger.log(Level.INFO, "Loading OSGi framework");
        String argArray = Arrays.toString(args);
        loadFramework(argArray.substring(1, argArray.length() - 1));

        try {
            BundleContext bundleContext = framework.getBundleContext();
            if (bundleContext.getBundles().length <= 1) {
                ourLogger.log(Level.INFO, "Loading embedded OSGi bundles");
                loadEmbeddedBundles(bundleContext);
            }
            uninstallBundles(bundleContext, CommonUtils.getInstance().getArg("-uninstall_bundle", args));
            installBundles(bundleContext, CommonUtils.getInstance().getArg("-install_bundle", args));
            for (Bundle bundle : bundleContext.getBundles()) {
                if (isDevelopmentMode()) {
                    System.out.println("Starting Bundle: " + bundle.getSymbolicName());
                }
                bundle.start();
            }
            ourLogger.log(Level.INFO, "OSGi is started with {0} version {1}",
                    new Object[]{framework.getSymbolicName(), framework.getVersion()});
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            ourLogger.log(Level.WARNING,
                    "Could not create framework, plugins disabled: {0}", ex.getMessage());
        }
    }

    private boolean isDevelopmentMode() {
        String developmentMode = System.getProperty("developmentMode");
        if (developmentMode != null && !developmentMode.isEmpty()) {
            return System.getProperty("developmentMode").equals("true");
        }
        return false;
    }

    private String getCacheFolder() {
        return CommonUtils.getInstance().getAppDataDirectory() + "bundles/";
    }

    /**
     * get the OSGi cache directory
     *
     * @return the OSGi cache directory
     */
    private String getCacheDir() {
        return getCacheFolder() + "v" + CommonUtils.getInstance().getAppVersion() + "-bundle-cache";
    }

    /**
     * clear the OSGi cache
     */
    public void clearCache() {
        deleteDirectory(new File(getCacheFolder()));
    }

    /**
     * delete the specified directory, and all its contents
     *
     * @param path the path of the directory to delete
     * @return true if and only if the file or directory is successfully
     * deleted; false otherwise
     */
    private boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return (path.delete());
    }

    private static FrameworkFactory getFrameworkFactory() throws Exception {
        ServiceLoader<FrameworkFactory> factoryLoader
                = ServiceLoader.load(FrameworkFactory.class);
        Iterator<FrameworkFactory> it = factoryLoader.iterator();
        if (!it.hasNext()) {
            System.err.println("Could not create framework, no OSGi implementation found");
            System.exit(0);
        }
        return it.next();
    }

    private void loadFramework(String argArray) {
        try {
            Map<String, String> configProps = new HashMap<String, String>();
            configProps.put(FRAMEWORK_STORAGE, getCacheDir());
            for (String key : CONFIG_BUNDLE.keySet()) {
                configProps.put(key, CONFIG_BUNDLE.getString(key));
            }
            configProps.put("args", argArray);
            FrameworkFactory factory = getFrameworkFactory();
            framework = factory.newFramework(configProps);
            framework.init();
            if (CommonUtils.getInstance().isHelp(framework.getBundleContext())) {
                System.out.println(CommonUtils.getInstance().getAppName() + " " + CommonUtils.getInstance().getAppVersion());
                System.out.println("Options:");
                System.out.println("-install_bundle - install an OSGi bundle (plugin) in the specified .jar file");
                System.out.println("-uninstall_bundle - uninstall an installed OSGi bundle (plugin)");
                System.out.println("-cbc - clear bundle cache and exit - this will ignore all other options");
            }
            framework.start();
        } catch (Exception ex) {
            System.err.println("Could not create framework: " + ex);
            ex.printStackTrace(System.err);
            System.exit(0);
        }
    }

    private void uninstallBundles(BundleContext bundleContext, String uninstall_bundle) throws BundleException {
        if (uninstall_bundle != null) {
            for (Bundle bundle : bundleContext.getBundles()) {
                if (uninstall_bundle.equals(bundle.getSymbolicName())) {
                    bundle.uninstall();
                    ourLogger.log(Level.INFO, "uninstalled bundle: {0}", uninstall_bundle);
                }
            }
        }
        if (bundleSymbolicNameToUninstall != null) {
            for (Bundle bundle : bundleContext.getBundles()) {
                if (bundleSymbolicNameToUninstall.equals(bundle.getSymbolicName())) {
                    bundle.uninstall();
                    ourLogger.log(Level.INFO, "uninstalled bundle: {0}", bundleSymbolicNameToUninstall);
                }
            }
        }
    }

    private void installBundles(BundleContext bundleContext, String install_bundle) throws BundleException {
        if (install_bundle != null) {
            Bundle bundle = bundleContext.installBundle(install_bundle);
            if (bundle != null) {
                ourLogger.log(Level.INFO, "installed bundle: {0}", install_bundle);
            }
        }
        if (bundlePathToInstall != null) {
            Bundle bundle = bundleContext.installBundle(bundlePathToInstall);
            if (bundle != null) {
                ourLogger.log(Level.INFO, "installed bundle: {0}", bundlePathToInstall);
            }
        }
    }

    private void loadEmbeddedBundles(BundleContext bundleContext) throws IOException {
        for (String fileName : getJarFileNames()) {
            URL locationURL = OSGiHandler.class.getClassLoader().getResource(fileName);
            if (locationURL == null) {
                String filePrefix = "bundles/";
                if (!new File(filePrefix).exists()) {
                    filePrefix = "../bundles/";
                }
                locationURL = new File(filePrefix + fileName).toURI().toURL();
            }
            if (locationURL != null) {
                try {
                    bundleContext.installBundle(locationURL.toString());
                    ourLogger.log(Level.INFO, "loading {0}", new Object[]{fileName});
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                    ourLogger.log(Level.WARNING, "Could not install {0}", new Object[]{fileName});
                }
            } else {
                ourLogger.log(Level.WARNING, "Could not find {0}", new Object[]{fileName});
            }
        }
    }

    private List<String> getJarFileNames() throws IOException {
        String OSGiImplFile = ResourceBundle.getBundle("main").getString("OSGiImplFile");
        List<String> entries = new ArrayList<String>();
        URL codesource = this.getClass().getProtectionDomain().getCodeSource().getLocation();
        if (codesource.toString().endsWith(".jar")) { // ant exe or webstart

            ZipInputStream zipinputstream
                    = new ZipInputStream(codesource.openStream());
            ZipEntry zipentry = zipinputstream.getNextEntry();

            while (zipentry != null) {
                //for each entry to be extracted

                String entryName = zipentry.getName();
                if (zipentry.isDirectory()) {
                    File file = new File("lib");
                    //        System.out.println("DEBUG: exist:" + file.exists());
                }
                if (entryName.endsWith(".jar")) {
                    entries.add(entryName);
                }

                zipinputstream.closeEntry();
                zipentry = zipinputstream.getNextEntry();
            }//while
            zipinputstream.close();
        } else { // ant maven gradle run
            entries = getDevelopmentModeJarFileNames();
        }
        entries.remove(OSGiImplFile); // don't install OSGiImpl as a bundle
        return entries;
    }

    private List<String> getDevelopmentModeJarFileNames() {
        List<String> entries = new ArrayList<String>();
        File dir = new File("bundles");
        if (!dir.exists()) {
            dir = new File("../bundles");
        }
        FilenameFilter ff = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        };
        entries.addAll(Arrays.asList(dir.list(ff)));
        if (entries.isEmpty()) {
            String messageWrapper = "------------------------------------------------------------------------";
            System.out.println(messageWrapper);
            System.out.println("ERROR: Bundles directory is empty, you must build the project and run again. (i.e. run mvn install from the root (i.e. parent project) of the repo)");
            System.out.println(messageWrapper);
            System.exit(0);
        }
        return entries;
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
            try {
                if (IS_WINDOWS) {
                    // If this is Windows and Nimbus is not installed, then use the Windows look and feel.
                    Class<?> cl = Class.forName("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                    LookAndFeel look_and_feel = (LookAndFeel) cl.newInstance();
                    if (look_and_feel.isSupportedLookAndFeel()) {
                        UIManager.setLookAndFeel(look_and_feel);
                    }
                }
            } catch (Exception ulfe) {
                // Windows look and feel is only supported on Windows, and only in
                // some version of the jre.  That is perfectly ok.
            }
        }

    }

    public BundleContext getBundleContext() {
        if (framework == null) {
            return null;
        }
        return framework.getBundleContext();
    }

    public boolean installBundle(String filePath) {
        if (framework == null) {
            bundlePathToInstall = filePath;
            return true;
        }
        Bundle bundle = null;
        if (filePath != null) {
            try {
                BundleContext bundleContext = framework.getBundleContext();
                bundle = bundleContext.installBundle(filePath);
                bundle.start();
            } catch (Exception x) {
                ourLogger.log(Level.SEVERE, "error installing bundle", x);
                bundle = null;
            }
            if (bundle != null) {
                ourLogger.log(Level.INFO, "installed bundle: {0}", filePath);
            }
        }
        return bundle != null;
    }

    public boolean uninstallBundle(String symbolicName) {
        if (framework == null) {
            bundleSymbolicNameToUninstall = symbolicName;
            return true;
        }
        boolean found = false;
        if (symbolicName != null) {
            BundleContext bundleContext = framework.getBundleContext();
            for (Bundle bundle : bundleContext.getBundles()) {
                if (symbolicName.equals(bundle.getSymbolicName())) {
                    try {
                        bundle.uninstall();
                        ourLogger.log(Level.INFO, "uninstalled bundle: {0}", symbolicName);
                        found = true;
                    } catch (Exception x) {
                        ourLogger.log(Level.SEVERE, "error uninstalling bundle", x);
                        found = false;
                    }
                }
            }
        }
        return found;
    }
}
