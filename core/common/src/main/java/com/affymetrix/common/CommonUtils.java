package com.affymetrix.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * utilities used by both the main, starting class, and the bundles. Singleton pattern.
 *
 */
public class CommonUtils {

    private static final Logger logger = LoggerFactory.getLogger(CommonUtils.class);
    private static final CommonUtils instance = new CommonUtils();
    private String igbDataHome;
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("common");
    public static final String APP_NAME = BUNDLE.getString("appName");
    public static final String APP_NAME_SHORT = BUNDLE.getString("appNameShort");
    public static final String APP_VERSION = BUNDLE.getString("appVersion");
    private static final String GOOGLE_ANALYTICS_ID = BUNDLE.getString("googleAnalyticsId");
    final public static boolean IS_WINDOWS
            = System.getProperty("os.name").toLowerCase().contains("windows");
    final public static boolean IS_MAC
            = System.getProperty("os.name").toLowerCase().contains("mac");
    final public static boolean IS_LINUX
            = System.getProperty("os.name").toLowerCase().contains("linux");

    final public static boolean IS_UBUNTU = IS_LINUX && isDistro("ubuntu");

    private CommonUtils() {
    }

    public final static CommonUtils getInstance() {
        return instance;
    }

    public String getUserAgent() {
        return MessageFormat.format(
                "{0}/{1}; {2}/{3} ({4}); {5};",
                APP_NAME_SHORT,
                APP_VERSION,
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"),
                Locale.getDefault().toString());
    }

    public static boolean isDistro(String name) {
        try {
            Process process = Runtime.getRuntime().exec("cat /etc/issue");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.toLowerCase();
                if (line.contains(name)) {
                    return true;
                }
            }
        } catch (IOException ex) {
            return false;
        }
        return false;
    }

    /**
     * get the current version of IGB
     *
     * @return the IGB version
     */
    public String getAppVersion() {
        return APP_VERSION;
    }

    public String getGoogleAnalyticsId() {
        return GOOGLE_ANALYTICS_ID;
    }

    /**
     * Get a large(133x133) application icon
     *
     * @return
     */
    public Optional<ImageIcon> getApplicationIcon() {
        return Optional.ofNullable(getIcon("images/igb.png"));
    }

    /**
     * Get a small(64x64) application icon
     *
     * @return
     */
    public ImageIcon getApplicationSmallIcon() {
        return CommonUtils.getInstance().getIcon("images/igb_small.png");
    }

    /**
     * Returns the value of the argument indicated by label. If arguments are "-flag_2 -foo bar", then get_arg("foo",
     * args) returns "bar", get_arg("flag_2") returns a non-null string, and get_arg("flag_5") returns null.
     */
    public String getArg(String label, String[] args) {
        String arg = null;
        boolean argExist = false;
        if (label != null && args != null) {
            for (String item : args) {
                if (argExist) {
                    arg = item;
                    break;
                }
                if (item.equals(label)) {
                    argExist = true;
                }
            }
        }
        if (argExist && arg == null) {
            arg = "true";
        }
        return arg;
    }

    public String[] getArgs(BundleContext bundleContext) {
        if (bundleContext.getProperty("args") == null) {
            return null;
        }
        return bundleContext.getProperty("args").split(", ");
    }

    /**
     * Returns the location of the application data directory. The String will always end with "/".
     *
     * @return the application directory
     */
    public String getAppDataDirectory() {
        if (igbDataHome == null) {
            boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
            String igbHomeDirName = ".igb";
            if (IS_WINDOWS) {
                igbDataHome = System.getenv("AppData") + File.separator + "IGB";
            } else {
                igbDataHome = System.getProperty("user.home") + File.separator + igbHomeDirName;
            }
            File igbDataHomeFile = new File(igbDataHome);
            igbDataHomeFile.mkdir();
        }
        return igbDataHome + File.separator;
    }

    /**
     * Loads an ImageIcon from the specified system resource. The system resource should be in the classpath, for
     * example, it could be in the jlfgr-1_0.jar file. If the resource is absent or can't be found, this routine will
     * not throw an exception, but will return null. For example: "toolbarButtonGraphics/general/About16.gif".
     *
     * @return An ImageIcon or null if the one specified could not be found.
     */
    public ImageIcon getIcon(String resource_name) {
        ImageIcon icon = getIcon(CommonUtils.class, resource_name);
        if (icon == null) {
            return getIcon(CommonUtils.class, "org/freedesktop/tango/" + resource_name);
        }
        return icon;
    }

    public ImageIcon getIcon(Class<?> clazz, String resource_name) {
        ImageIcon icon = null;
        try {
            // Note: MenuUtil.class.getResource(resource_name) does not work;
            // ClassLoader.getSystemResource(resource_name) works locally, but not with WebStart;
            //
            // Both of these work locally and with WebStart:
            //  MenuUtil.class.getClassLoader().getResource(resource_name)
            //  Thread.currentThread().getContextClassLoader().getResource(resource_name)
            java.net.URL url = clazz.getClassLoader().getResource(resource_name);
            if (url != null) {
                icon = new ImageIcon(url);
            }
        } catch (Exception ex) {
            logger.error("Could not retrieve icon", ex);
        }
        if (icon == null || icon.getImageLoadStatus() == MediaTracker.ABORTED
                || icon.getIconHeight() <= 0 || icon.getIconWidth() <= 0) {
            icon = null;
        }

        return icon;
    }

    public ImageIcon getAlternateIcon(String resource_name) {
        try {
            java.net.URL resource_url = CommonUtils.class.getClassLoader().getResource(resource_name);
            if (resource_url != null) {
                BufferedImage resource_img = ImageIO.read(resource_url);
                Graphics2D resource_graphics = resource_img.createGraphics();

                int width = resource_img.getWidth();
                int height = resource_img.getHeight();
                int half_width = width / 2;
                int half_height = height / 2;

                resource_graphics.setColor(Color.BLACK);
                resource_graphics.setStroke(new BasicStroke(1.5f));
                resource_graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                resource_graphics.drawOval(half_width - 2, half_height - 2, half_width, half_height);
                resource_graphics.drawLine(half_width, half_height, 2 * half_width - 4, 2 * half_height - 4);
//				resource_graphics.drawLine(0, resource_img.getHeight(), resource_img.getWidth(), 0);
                return new ImageIcon(resource_img);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            // It isn't a big deal if we can't find the icon, just return null
        }
        return null;
    }

    public static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    public static boolean equals(String str1, String str2) {
        return str1 == null ? str2 == null : str1.equals(str2);
    }

    public static boolean isDevelopmentMode() {
        String developmentMode = System.getProperty("developmentMode");
        if (isNotBlank(developmentMode)) {
            return System.getProperty("developmentMode").equals("true");
        }
        return false;
    }
}
