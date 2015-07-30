package com.affymetrix.genometry.util;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.common.PreferenceUtils;
import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.awt.Component;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.util.prefs.Preferences;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.Timer;

/**
 *
 * @author dcnorris
 */
public class ModalUtils {

    /**
     * Shows a panel asking for the user to confirm something.
     *
     * @param comp
     * @param message the message String to display to the user
     * @return true if the user confirms, else false.
     */
    public static boolean confirmPanel(String message) {
        return ModalUtils.confirmPanel(getActiveWindow(), message, null, null, false);
    }

    public static boolean confirmPanel(final String message, final String check,
            final boolean def_val) {
        return ModalUtils.confirmPanel(getActiveWindow(), message, PreferenceUtils.getTopNode(), check, def_val);
    }

    public static boolean confirmPanel(final String message, final Preferences node,
            final String check, final boolean def_val) {
        return confirmPanel(getActiveWindow(), message, node, check, def_val, "Do not show this message again");
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
        HashFunction hf = Hashing.md5();
        HashCode hc = hf.newHasher().putString(check, Charsets.UTF_8).hash();
        final String md5_check = hc.toString();
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
        JOptionPane pane = new JOptionPane(params, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION, CommonUtils.getInstance().getApplicationSmallIcon());
        javax.swing.JDialog dialog = pane.createDialog(comp, "Confirm");
        dialog.setVisible(true);

        Object value = pane.getValue();
        if (value == null) {
            return JOptionPane.NO_OPTION;
        }
        return (Integer) value;
    }

    public static void infoPanel(final String message, final String check, final boolean def_val) {

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

    public static void infoPanel(JLabel label) {
        label.setFont(new Font("serif", Font.PLAIN, 14));
        JOptionPane.showMessageDialog(null, label);
    }

    public static void infoPanel(String message) {
        JOptionPane.showMessageDialog(getActiveWindow(), message, "IGB", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void errorPanel(String message) {
        final Component activeWindow = getActiveWindow();
        if (activeWindow != null && activeWindow.isVisible()) {
            JOptionPane.showMessageDialog(activeWindow, message, "IGB", JOptionPane.ERROR_MESSAGE);
        } else {
            //wait 3 seconds and display anyway -- better solution may be possible, but this solves immediate issue of pop-up appearing before the main window
            Timer timer = new Timer(3 * 1000, evt -> {
                JOptionPane.showMessageDialog(getActiveWindow(), message, "IGB", JOptionPane.ERROR_MESSAGE);
            });
            timer.setRepeats(false);
            timer.start();
        }
    }

    private static Component getActiveWindow() {
        Component comp = KeyboardFocusManager.getCurrentKeyboardFocusManager().
                getActiveWindow();
        if (comp == null) {
            comp = KeyboardFocusManager.getCurrentKeyboardFocusManager().getCurrentFocusCycleRoot();
        }
        return comp;
    }

}
