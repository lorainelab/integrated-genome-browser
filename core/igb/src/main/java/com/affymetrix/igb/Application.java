package com.affymetrix.igb;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.genometry.util.StatusAlert;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.StatusBar;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import javax.swing.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    private static final int delay = 2; //delay in seconds
    private static final String COMMAND_KEY = "meta";
    private static final String CONTROL_KEY = "ctrl";

    private final LinkedList<StatusAlert> statusAlertList = new LinkedList<>(); // list of status alert messages.
    private ActionListener status_alert_listener = e -> {
        if (e.getActionCommand().equals(String.valueOf(StatusAlert.HIDE_ALERT))) {
            removeStatusAlert((StatusAlert) e.getSource());
        }
    };

    private final LinkedList<String> progressStringList = new LinkedList<>(); // list of progress bar messages.
    ActionListener update_status_bar = ae -> {
        synchronized (progressStringList) {
            String s = progressStringList.pop();
            progressStringList.addLast(s);
            setNotLockedUpStatus(s);
        }
    };
    Timer timer = new Timer(delay * 1000, update_status_bar);

    static Application singleton = null;
    protected final StatusBar statusBar;

    public Application() {
        singleton = this;
        statusBar = new StatusBar(status_alert_listener);
        loadDefaultToolbarActionsAndKeystrokeBindings();
    }

    private void loadDefaultToolbarActionsAndKeystrokeBindings() {
        String fileName = IGBConstants.DEFAULT_PREFS_API_RESOURCE;
        /**
         * load default prefs from jar (with Preferences API). This will be the
         * standard method soon.
         */
        try (InputStream default_prefs_stream = IGB.class.getResourceAsStream(fileName);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();) {
            //Save current preferences
            PreferenceUtils.getTopNode().exportSubtree(outputStream);
            if (default_prefs_stream != null) {
                logger.debug("loading default User preferences from: " + fileName);
                Preferences.importPreferences(default_prefs_stream);

                /**
                 * Use 'command' instead of 'control' in keystrokes for Mac OS.
                 */
                if (IGB.IS_MAC) {
                    String[] keys = PreferenceUtils.getKeystrokesNode().keys();
                    for (int i = 0; i < keys.length; i++) {
                        String action = PreferenceUtils.getKeystrokesNode().keys()[i];
                        String keyStroke = PreferenceUtils.getKeystrokesNode().get(action, "");
                        if (keyStroke.contains(CONTROL_KEY)) {
                            keyStroke = keyStroke.replace(CONTROL_KEY, COMMAND_KEY);
                            PreferenceUtils.getKeystrokesNode().put(action, keyStroke);
                        }
                    }
                }
                //Load back saved preferences
                try (ByteArrayInputStream outputInputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
                    Preferences.importPreferences(outputInputStream);
                }
            }
        } catch (Exception ex) {
            logger.debug("Problem parsing prefs from: {}", fileName, ex);
        }
    }

    public static Application getSingleton() {
        return singleton;
    }

    public StatusBar getStatusBar() {
        return statusBar;
    }

    abstract public java.awt.Image getIcon();

    abstract public javax.swing.ImageIcon getSmallIcon();

    abstract public javax.swing.JFrame getFrame();

    abstract public SeqMapView getMapView();

    public final void addNotLockedUpMsg(final String s) {
        synchronized (progressStringList) {
            progressStringList.addFirst(s);
        }
        update_status_bar.actionPerformed(null);

        if (!timer.isRunning()) {
            timer.start();
        }
    }

    public final void removeNotLockedUpMsg(final String s) {
        synchronized (progressStringList) {
            progressStringList.remove(s);

            if (progressStringList.isEmpty()) {
                setNotLockedUpStatus(null);
                timer.stop();
            }
        }
    }

    /**
     * Set the status text, and show a little progress bar so that the
     * application doesn't look locked up.
     *
     * @param s text of the message
     */
    private synchronized void setNotLockedUpStatus(String s) {
        statusBar.setStatus(s);
    }

    /**
     * Sets the text in the status bar. Will also echo a copy of the string to
     * System.out. It is safe to call this method even if the status bar is not
     * being displayed.
     */
    public final void setStatus(String s) {
        setStatus(s, true);
    }

    /**
     * Sets the text in the status bar. Will optionally echo a copy of the
     * string to System.out. It is safe to call this method even if the status
     * bar is not being displayed.
     *
     * @param echo Whether to echo a copy to System.out.
     */
    public final void setStatus(final String s, final boolean echo) {
        statusBar.setStatus(s);
        if (echo && s != null && !s.isEmpty()) {
            logger.info(s);
        }
    }

    public final void addStatusAlert(final StatusAlert s) {
        synchronized (statusAlertList) {
            statusAlertList.addFirst(s);
        }
        setStatusAlert(s);
    }

    public final void removeStatusAlert(final StatusAlert s) {
        synchronized (statusAlertList) {
            statusAlertList.remove(s);
        }

        if (statusAlertList.isEmpty()) {
            setStatusAlert(null);
        } else {
            setStatusAlert(statusAlertList.pop());
        }
    }

    private synchronized void setStatusAlert(StatusAlert s) {
        statusBar.setStatusAlert(s);
    }

    public abstract void setSelField(Map<String, Object> properties, String s, SeqSymmetry sym);

    public void showError(String title, String message, List<GenericAction> actions, Level level) {
        statusBar.showError(title, message, actions, level);
    }

}
