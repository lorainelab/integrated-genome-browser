package com.affymetrix.igb.update;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import java.text.MessageFormat;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 *
 * @author hiralv
 */
public class ShowUpdate {

    private static final String UPDATEMESSAGE = "Good news! IGB {0} is available. Click here to get the new version.";
    private final static String[] options = new String[]{"Go To Download Page", "Maybe later"};

    public static boolean showUpdate(final Update update) {
        JOptionPane op = new JOptionPane(MessageFormat.format(UPDATEMESSAGE, update.getVersion().toString()),
                JOptionPane.PLAIN_MESSAGE, JOptionPane.YES_NO_OPTION,
                CommonUtils.getInstance().getApplicationSmallIcon(),
                options);

        op.setInitialValue(options[0]);
        JDialog dialog = op.createDialog(UpdateStatusAlert.UPDATE_AVAILABLE);
        dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        dialog.setResizable(true);
        dialog.setAlwaysOnTop(true);
        dialog.pack();
        dialog.setVisible(true);

        boolean ret = false;
        if (op.getValue() != null) {
            //Mark survey as taken 
//            if (op.getValue() != options[2]) {
//                PreferenceUtils.getTopNode().putBoolean(Update.UPDATE_PREFIX + update.getVersion().toString(), true);
//                ret = true;
//            }

            if (op.getValue() == options[0]) {
                GeneralUtils.browse(update.getLink());
            }
        }
        return ret;
    }
}
