package com.affymetrix.igb;

import com.affymetrix.igb.prefs.PreferencesPanel;
import java.awt.Image;
import java.awt.desktop.PreferencesEvent;
import java.awt.desktop.PreferencesHandler;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;

/**
 * Class to handle Integration of IGB so it will behave more mac-like on OS X.
 * This is achieved using reflection so that the apple-specific classes will not
 * interfere with IGB on other platforms.
 *
 * @author sgblanch
 * @version $Id: MacIntegration.java 11085 2012-04-13 16:09:40Z lfrohman $
 */
public class MacIntegration implements PreferencesHandler {

    /**
     * private instance of MacIntegration for singleton pattern
     */
    private static MacIntegration instance = null;
    private static final Logger ourLogger = Logger.getLogger(MacIntegration.class.getPackage().getName());

    /**
     * Initialize the singleton copy of MacIntegration. This should only be
     * called once by the application, but it protects itself against multiple
     * invocations. Do not call this function on anything platform other than
     * Macintosh: Undefined things will happen.
     *
     * @return a singleton instance of MacIntegration
     */
    public static synchronized MacIntegration getInstance() {
        if (instance == null) {
            instance = new MacIntegration();
        }
        return instance;
    }
    private Class<?> applicationClass;
    private Object application;

    /**
     * Private constructor to enforce singleton pattern
     */
    private MacIntegration() {
        applicationClass = null;
        application = null;

        try {
            applicationClass = Class.forName("com.apple.eawt.Application");
            Method getApplication = applicationClass.getDeclaredMethod("getApplication");
            application = getApplication.invoke(null);

            Method setEnabledPreferencesMenu = applicationClass.getDeclaredMethod("setPreferencesHandler", PreferencesHandler.class);
            setEnabledPreferencesMenu.invoke(application, this);

        } catch (Exception ex) {
        }
    }

    /**
     * Wrapper around Apple's com.apple.eawt.setDockIconImage.
     *
     * @param image the Image to use as the Dock icon.
     */
    public void setDockIconImage(Image image) {
        try {
            Method setDockIconImage = applicationClass.getDeclaredMethod("setDockIconImage", Image.class);
            setDockIconImage.invoke(application, image);
        } catch (Exception ex) {
            ourLogger.log(Level.SEVERE, "?", ex);
        }
    }

    @Override
    public void handlePreferences(PreferencesEvent e) {
        PreferencesPanel pv = PreferencesPanel.getSingleton();
        JFrame f = pv.getFrame();
        f.setVisible(true);
    }

}
