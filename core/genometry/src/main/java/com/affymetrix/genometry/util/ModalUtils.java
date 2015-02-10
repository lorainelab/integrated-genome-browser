package com.affymetrix.genometry.util;

import com.affymetrix.common.CommonUtils;
import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.awt.Component;
import java.util.prefs.Preferences;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;

/**
 *
 * @author dcnorris
 */
public class ModalUtils {

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

        //return JOptionPane.showConfirmDialog(comp, params, "Confirm", JOptionPane.YES_NO_OPTION);
    }

}
