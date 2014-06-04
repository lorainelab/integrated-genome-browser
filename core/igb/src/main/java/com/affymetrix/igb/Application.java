package com.affymetrix.igb;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import javax.swing.FocusManager;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.Timer;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.StatusAlert;
import com.affymetrix.genometryImpl.util.UrlToFileName;
import com.affymetrix.igb.view.StatusBar;

public abstract class Application {

    public final static boolean DEBUG_EVENTS = false;
    private static final int delay = 2; //delay in seconds

    private final LinkedList<StatusAlert> statusAlertList = new LinkedList<StatusAlert>(); // list of status alert messages.
    private ActionListener status_alert_listener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals(String.valueOf(StatusAlert.HIDE_ALERT))) {
                removeStatusAlert((StatusAlert) e.getSource());
            }
        }
    };
    private final LinkedList<String> progressStringList = new LinkedList<String>(); // list of progress bar messages.
    ActionListener update_status_bar = new ActionListener() {

        public void actionPerformed(java.awt.event.ActionEvent ae) {
            synchronized (progressStringList) {
                String s = progressStringList.pop();
                progressStringList.addLast(s);
                setNotLockedUpStatus(s);
            }
        }
    };
    Timer timer = new Timer(delay * 1000, update_status_bar);

    static Application singleton = null;
    public final StatusBar status_bar;

    public Application() {
        singleton = this;
        status_bar = new StatusBar(status_alert_listener);
    }

    public static Application getSingleton() {
        return singleton;
    }

    abstract public java.awt.Image getIcon();

    abstract public javax.swing.ImageIcon getSmallIcon();

    abstract public javax.swing.JFrame getFrame();

    abstract public com.affymetrix.igb.view.SeqMapView getMapView();

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
        status_bar.setStatus(s);
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
        status_bar.setStatus(s);
        if (echo && s != null && !s.isEmpty()) {
            System.out.println(s);
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
        status_bar.setStatusAlert(s);
    }

    public abstract void setSelField(Map<String, Object> properties, String s);

    public void showError(String title, String message, List<GenericAction> actions, Level level) {
        status_bar.showError(title, message, actions, level);
    }

    /**
     * Shows a panel asking for the user to confirm something.
     *
     * @param message the message String to display to the user
     * @return true if the user confirms, else false.
     */
    public static boolean confirmPanel(String message) {
        return confirmPanel(getActiveWindow(), message, null, null, false);
    }

    public static boolean confirmPanel(final String message, final String check,
            final boolean def_val) {
        return confirmPanel(getActiveWindow(), message, PreferenceUtils.getTopNode(), check, def_val);
    }

    public static boolean confirmPanel(final Component comp, final String message, final Preferences node,
            final String check, final boolean def_val) {
        return confirmPanel(comp, message, node, check, def_val, "Do not show this message again");
    }

    public static boolean confirmPanel(final Component comp, final String message, final Preferences node,
            final String check, final boolean def_val, final String save_string) {
        Object[] params;

        //If no node is provided then show default message
        if (node == null) {
            params = new Object[]{message};
            return JOptionPane.YES_OPTION == showConfirmDialog(comp, params);
        }
        //Large key does not work in preferences. So convert key into md5 value.
        final String md5_check = UrlToFileName.toMd5(check);
        //If all parameters are provided then look up for boolean value from preference.
        final boolean b = node.getBoolean(md5_check, def_val);

        //If user has already set preference then return true.
        if (b != def_val) {
            return true;
        }

        //If preference is not set then show message with option to disable it.
        final JCheckBox checkbox = new JCheckBox(save_string);
        params = new Object[]{message, checkbox};

        int ret = showConfirmDialog(comp, params);

        if (JOptionPane.YES_OPTION == ret) {
            if (checkbox.isSelected()) {
                node.putBoolean(md5_check, checkbox.isSelected() != b);
            }
            return true;
        }

        return false;
    }

    private static int showConfirmDialog(final Component comp, Object[] params) {
        JOptionPane pane = new JOptionPane(params, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION, getSingleton().getSmallIcon());
        javax.swing.JDialog dialog = pane.createDialog(comp, "Confirm");
        dialog.setVisible(true);

        Object value = pane.getValue();
        if (value == null) {
            return JOptionPane.NO_OPTION;
        }
        return (Integer) value;

        //return JOptionPane.showConfirmDialog(comp, params, "Confirm", JOptionPane.YES_NO_OPTION);
    }

    public static void infoPanel(final String message, final String check, final boolean def_val) {
//		if(node == null){
//			JOptionPane.showMessageDialog(comp, message, "IGB", JOptionPane.INFORMATION_MESSAGE);
//			return;
//		}

        final JCheckBox checkbox = new JCheckBox("Do not show this message again.");
        final Object[] params = new Object[]{message, checkbox};
        final Preferences node = PreferenceUtils.getTopNode();

        //If all parameters are provided then look up for boolean value from preference.
        final boolean b = node.getBoolean(check, def_val);

        //If user has already set preference then return true.
        if (b != def_val) {
            return;
        }

        JOptionPane.showMessageDialog(getActiveWindow(), params, "IGB", JOptionPane.INFORMATION_MESSAGE);

        if (checkbox.isSelected()) {
            node.putBoolean(check, checkbox.isSelected() != b);
        }
    }

    private static Component getActiveWindow() {
        Component comp = FocusManager.getCurrentManager().getActiveWindow();
        if (comp == null) {
            Application app = getSingleton();
            comp = (app == null) ? null : app.getFrame().getRootPane();
        }
        return comp;
    }
}
