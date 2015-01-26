package com.affymetrix.main;

import com.affymetrix.common.CommonUtils;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.wiring.BundleRevision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * all OSGi functionality is handled here. Singleton pattern.
 */
public class OSGiHandler {

    private static final ResourceBundle CONFIG_BUNDLE = ResourceBundle.getBundle("config");
    private static final Logger log = LoggerFactory.getLogger(OSGiHandler.class);
    private Framework framework;
    private String bundlePathToInstall;
    private String bundleSymbolicNameToUninstall;

    public static final boolean IS_WINDOWS
            = System.getProperty("os.name").toLowerCase().contains("windows");
    public static final boolean IS_MAC
            = System.getProperty("os.name").toLowerCase().contains("mac");
    public static final boolean IS_LINUX
            = System.getProperty("os.name").toLowerCase().contains("linux");

    public static final String WINDOW_SERVICE_DEF_NAME = "windowsServiceDef";

    /**
     * start OSGi, load and start the OSGi implementation load the embedded
     * bundles, if not cached, and start all bundles
     *
     * @param args the command line arguments
     */
    public void startOSGi(String[] args) {
        if (isDevelopmentMode()) {
            clearCache();
        }

        if (CommonUtils.getInstance().getArg("-cbc", args) != null) { // just clear bundle cache and return
            clearCache();
            return;
        }

        log.info("Loading OSGi framework");
        String argArray = Arrays.toString(args);
        loadFramework(argArray.substring(1, argArray.length() - 1));

        try {
            BundleContext bundleContext = framework.getBundleContext();
            if (bundleContext.getBundles().length <= 1) {
                log.info("Loading embedded OSGi bundles");
                loadEmbeddedBundles(bundleContext);
            }
            Bundle windowServiceDefBundle = null;
            for (Bundle bundle : bundleContext.getBundles()) {
                log.info("Starting Bundle: " + bundle.getSymbolicName());
                //fyi bundle fragments cannot be started
                if (!bundleIsFragment(bundle)) {
                    //window service hack
                    if (!isNullOrEmpty(bundle.getSymbolicName()) && bundle.getSymbolicName().equals(WINDOW_SERVICE_DEF_NAME)) {
                        windowServiceDefBundle = bundle;
                    } else {
                        bundle.start();
                    }
                }
            }
            if (windowServiceDefBundle != null) {
                windowServiceDefBundle.start();
            }
            log.info("OSGi is started with {} version {}",
                    new Object[]{framework.getSymbolicName(), framework.getVersion()});
        } catch (Exception ex) {
            log.warn("Could not create framework, plugins disabled: {}", ex.getMessage());
        }
    }

    private boolean bundleIsFragment(Bundle bundle) {
        return (bundle.adapt(BundleRevision.class).getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
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
            Map<String, String> configProps = new HashMap<>();
            configProps.put(FRAMEWORK_STORAGE, getCacheDir());
            CONFIG_BUNDLE.keySet().stream().forEach((key) -> configProps.put(key, CONFIG_BUNDLE.getString(key)));
            configProps.put("args", argArray);
            FrameworkFactory factory = getFrameworkFactory();
            framework = factory.newFramework(configProps);
            framework.init();
            framework.start();
        } catch (Exception ex) {
            System.err.println("Could not create framework: " + ex);
            ex.printStackTrace(System.err);
            System.exit(0);
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
                    log.info("loading {}", new Object[]{fileName});
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                    log.warn("Could not install {}", new Object[]{fileName});
                }
            } else {
                log.warn("Could not find {}", new Object[]{fileName});
            }
        }
    }

    private List<String> getJarFileNames() throws IOException {
        String OSGiImplFile = ResourceBundle.getBundle("main").getString("OSGiImplFile");
        List<String> entries = new ArrayList<>();
        URL codesource = this.getClass().getProtectionDomain().getCodeSource().getLocation();
        if (codesource.toString().endsWith(".jar")) { try ( // ant exe or webstart
                ZipInputStream zipinputstream = new ZipInputStream(codesource.openStream())) {
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
            }
        } else { // ant maven gradle run
            entries = getDevelopmentModeJarFileNames();
        }
        entries.remove(OSGiImplFile); // don't install OSGiImpl as a bundle
        return entries;
    }

    private List<String> getDevelopmentModeJarFileNames() {
        List<String> entries = new ArrayList<>();
        File dir = new File("bundles");
        if (!dir.exists()) {
            dir = new File("../bundles");
        }
        FilenameFilter ff = (File dir1, String name) -> name.endsWith(".jar");
        entries.addAll(Arrays.asList(dir.list(ff)));
        if (entries.isEmpty()) {
            String messageWrapper = "------------------------------------------------------------------------";
            System.out.println(messageWrapper);
            System.out.println("ERROR: Bundles directory is empty, you must build the project and run again. (i.e. run mvn install from the root (i.e. parent project) of the repo)");
            System.out.println(messageWrapper);
            System.exit(0);
        }
        //this shouldn't be needed, but since windowservicedef needs all tabs to already registered it is... also this ordering is consistent with the jar ordering 
        Collections.sort(entries);
        return entries;
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
                log.error("error installing bundle", x);
                bundle = null;
            }
            if (bundle != null) {
                log.info("installed bundle: {}", filePath);
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
                        log.info("uninstalled bundle: {}", symbolicName);
                        found = true;
                    } catch (Exception x) {
                        log.error("error uninstalling bundle", x);
                        found = false;
                    }
                }
            }
        }
        return found;
    }

    public static boolean isNullOrEmpty(String string) {
        return string == null || string.length() == 0;
    }
}
