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
	private static final String UPDATEMESSAGE = "You are running version {0} of IGB. The most up-to-date version of IGB is {1}. \nWould you like to update your IGB now?";
	private final static String[] options = new String[]{"Yes", "No thanks", "Maybe later"};
	
	public static boolean showUpdate(final Update update) {
		JOptionPane op = new JOptionPane(MessageFormat.format(UPDATEMESSAGE, 
				CommonUtils.getInstance().getAppVersion(), update.getVersion().toString()),
				JOptionPane.PLAIN_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION,
				CommonUtils.getInstance().getApplicationSmallIcon(),
				options);
		
		op.setInitialValue(options[0]);
		JDialog dialog = op.createDialog("Update available");
		dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		dialog.setResizable(true);
		dialog.setAlwaysOnTop(true);
		dialog.pack();
		dialog.setVisible(true);

		boolean ret = false;
		if (op.getValue() != null) {
			//Mark survey as taken
			if (op.getValue() != options[2]) {
				PreferenceUtils.getTopNode().putBoolean(Update.UPDATE_PREFIX+update.getVersion().toString(), true);
				ret = true;
			}

			if (op.getValue() == options[0]) {
				GeneralUtils.browse(update.getLink());
			}
		}
		return ret;
	}
}

