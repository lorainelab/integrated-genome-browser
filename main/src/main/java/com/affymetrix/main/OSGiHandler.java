package com.affymetrix.main;

import com.affymetrix.common.CommonUtils;
import static com.affymetrix.common.CommonUtils.isDevelopmentMode;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
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
    private static final Logger logger = LoggerFactory.getLogger(OSGiHandler.class);
    private Framework framework;
    private final String[] args;

    public OSGiHandler(String[] args) {
        this.args = args;
        if (isClearBundleCacheRun()) {
            clearCache();
            System.exit(1);
        }
        if (isDevelopmentMode()) {
            clearCache();
        }
    }

    public void startOSGi() {
        logger.info("Loading OSGi framework");
        setUserAgent();
        disableSSLCertValidation();
        String commandLineArguments = Arrays.toString(args);
        commandLineArguments = commandLineArguments.substring(1, commandLineArguments.length() - 1); // remove brackets
        loadFramework(commandLineArguments);
        loadBundles();
    }

    //This is important for metric gathering, do not modify without thinking about the implications
    private void setUserAgent() {
        System.setProperty("http.agent", CommonUtils.getInstance().getUserAgent());
    }

    private void loadBundles() {
        try {
            BundleContext bundleContext = framework.getBundleContext();
            if (bundleContext.getBundles().length <= 1) {
                logger.info("Loading embedded OSGi bundles");
                loadEmbeddedBundles(bundleContext);
            }
            for (Bundle bundle : bundleContext.getBundles()) {
                logger.info("Starting Bundle: " + bundle.getSymbolicName());
                //fyi bundle fragments cannot be started
                if (!bundleIsFragment(bundle)) {
                    try {
                        bundle.start();
                    } catch (Throwable t) {
                        logger.error(t.getMessage(), t);
                        logger.error("Error starting bundle {}:{}", bundle.getSymbolicName(), bundle.getVersion().toString());
                    }
                }
            }
            logger.info("OSGi is started with {} version {}",
                    new Object[]{framework.getSymbolicName(), framework.getVersion()});
        } catch (IOException ex) {
            logger.warn("Could not create framework, plugins disabled: {}", ex);
        }
    }

    private boolean isClearBundleCacheRun() {
        return CommonUtils.getInstance().getArg("-cbc", args) != null;
    }

    private boolean bundleIsFragment(Bundle bundle) {
        return (bundle.adapt(BundleRevision.class).getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
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
    private void clearCache() {
        deleteDirectory(new File(getCacheFolder()));
    }

    /**
     * delete the specified directory, and all its contents
     *
     * @param path the path of the directory to delete
     * @return true if and only if the file or directory is successfully deleted; false otherwise
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
            System.setProperty("jsse.enableSNIExtension", "false");
            Map<String, String> configProps = new HashMap<>();
            configProps.put(FRAMEWORK_STORAGE, getCacheDir());
            CONFIG_BUNDLE.keySet().stream().forEach((key) -> configProps.put(key, CONFIG_BUNDLE.getString(key)));
            if (isDevelopmentMode()) {
                configProps.put("org.osgi.service.http.port", "8888");
                //TODO configProps.put("obr.repository.url", "localurl");
                configProps.put("felix.webconsole.manager.root", "/system/console");
                configProps.put("org.apache.felix.http.jettyEnabled", "true");
                configProps.put("felix.webconsole.username", "igbdev");
                configProps.put("felix.webconsole.password", "igbdev");
                //File Install Properties
                configProps.put("felix.fileinstall.dir", "../bundles/dynamic");
            }
            configProps.put("args", argArray);
            FrameworkFactory factory = getFrameworkFactory();
            framework = factory.newFramework(configProps);
            framework.init();
            framework.start();
            addShutdownHook();
        } catch (Exception ex) {
            System.err.println("Could not create framework: " + ex);
            ex.printStackTrace(System.err);
            System.exit(0);
        }
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread("Felix Shutdown Hook") {
            @Override
            public void run() {
                try {
                    if (framework != null) {
                        framework.stop();
                        framework.waitForStop(2);
                    }
                } catch (Exception ex) {
                    logger.error("Error stopping framework: ", ex);
                }
            }
        });
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
                    logger.info("loading {}", new Object[]{fileName});
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                    logger.warn("Could not install {}", new Object[]{fileName});
                }
            } else {
                logger.warn("Could not find {}", new Object[]{fileName});
            }
        }
    }

    private List<String> getJarFileNames() throws IOException {
        String OSGiImplFile = ResourceBundle.getBundle("main").getString("OSGiImplFile");
        List<String> entries = new ArrayList<>();
        URL codesource = this.getClass().getProtectionDomain().getCodeSource().getLocation();
        if (codesource.toString().endsWith(".jar")) {
            try ( // ant exe or webstart
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

    public static boolean isNullOrEmpty(String string) {
        return string == null || string.length() == 0;
    }

    private void disableSSLCertValidation() {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };

// Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (GeneralSecurityException e) {
        }
    }
}
