package com.affymetrix.igb.update;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.StatusAlert;
import java.text.MessageFormat;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import static com.affymetrix.igb.update.ShowUpdate.*;

/**
 *
 * @author hiralv
 */
public class UpdateStatusAlert implements StatusAlert {
	private static final String UPDATEAVAILABLE = "Update avilable";
	private static final String ICONPATH = "16x16/actions/warning.png";
	
	private final Update update;
	
	public UpdateStatusAlert(Update update) {
		this.update = update;
	}
	
	public Icon getIcon() {
		return null;
	}

	public String getDisplayMessage() {
		return UPDATEAVAILABLE;
	}

	public String getToolTip() {
		return MessageFormat.format("A new version {0} of IGB is available", update.getVersion().toString());
	}

	public int actionPerformed() {
		return showUpdate(update) ? StatusAlert.HIDE_ALERT : StatusAlert.KEEP_ALERT;
	}	
}
