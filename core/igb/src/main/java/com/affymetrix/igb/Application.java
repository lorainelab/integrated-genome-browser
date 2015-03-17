package com.affymetrix.igb;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.StatusAlert;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.StatusBar;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    private static final int delay = 2; //delay in seconds
    static Application singleton = null;

    public static Application getSingleton() {
        return singleton;
    }

    private final LinkedList<StatusAlert> statusAlertList = new LinkedList<>(); // list of status alert messages.
    private final ActionListener status_alert_listener = e -> {
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

    protected final StatusBar statusBar;

    public Application() {
        singleton = this;
        statusBar = new StatusBar(status_alert_listener);
    }

    public StatusBar getStatusBar() {
        return statusBar;
    }

    abstract public javax.swing.JFrame getFrame();

    abstract public SeqMapView getMapView();

    public void addNotLockedUpMsg(final String s) {
        synchronized (progressStringList) {
            progressStringList.addFirst(s);
        }
        update_status_bar.actionPerformed(null);

        if (!timer.isRunning()) {
            timer.start();
        }
    }

    public void removeNotLockedUpMsg(final String s) {
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
    public void setStatus(String s) {
        setStatus(s, true);
    }

    /**
     * Sets the text in the status bar. Will optionally echo a copy of the
     * string to System.out. It is safe to call this method even if the status
     * bar is not being displayed.
     *
     * @param echo Whether to echo a copy to System.out.
     */
    public void setStatus(final String s, final boolean echo) {
        statusBar.setStatus(s);
        if (echo && s != null && !s.isEmpty()) {
            logger.info(s);
        }
    }

    public void addStatusAlert(final StatusAlert s) {
        synchronized (statusAlertList) {
            statusAlertList.addFirst(s);
        }
        setStatusAlert(s);
    }

    public void removeStatusAlert(final StatusAlert s) {
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
