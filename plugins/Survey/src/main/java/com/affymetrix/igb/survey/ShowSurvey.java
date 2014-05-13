package com.affymetrix.igb.survey;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 *
 * @author hiralv
 */
public class ShowSurvey {

    private final static String[] options = new String[]{"Go to page", "No thanks", "Maybe later"};

    public static boolean showSurvey(final Survey survey) {
        JOptionPane op = new JOptionPane(survey.getDescription(),
                JOptionPane.PLAIN_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION,
                CommonUtils.getInstance().getApplicationSmallIcon(),
                options);

        JDialog dialog = op.createDialog(survey.getName());
        dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        dialog.setResizable(true);
        dialog.setAlwaysOnTop(true);
        dialog.pack();
        dialog.setVisible(true);

        boolean ret = false;
        if (op.getValue() != null) {
            //Mark survey as taken
            if (op.getValue() != options[2]) {
                PreferenceUtils.getTopNode().putBoolean(survey.getId(), true);
                ret = true;
            }

            if (op.getValue() == options[0]) {
                GeneralUtils.browse(survey.getLink());
            }
        }
        return ret;
    }
}
