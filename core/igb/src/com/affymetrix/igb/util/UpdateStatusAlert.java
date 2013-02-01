package com.affymetrix.igb.util;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.util.StatusAlert;
import javax.swing.Icon;
import javax.swing.JOptionPane;

/**
 *
 * @author hiralv
 */
public class UpdateStatusAlert implements StatusAlert{
	private static final String TOOLTIP = "You might not be on latest revision.";
	private static final String DISPLAYMESSAGE = "Update avilable";
	private static final String ICONPATH = "16x16/actions/warning.png";
	
	public Icon getIcon() {
		return CommonUtils.getInstance().getIcon(ICONPATH);
	}

	public String getDisplayMessage() {
		return DISPLAYMESSAGE;
	}

	public String getToolTip() {
		return TOOLTIP;
	}

	public int actionPerformed() {
		JOptionPane.showMessageDialog(null, UPDATEMESSAGE, UPDATEAVAILABLE, JOptionPane.INFORMATION_MESSAGE);
		return StatusAlert.HIDE_ALERT;
	}
	
}
