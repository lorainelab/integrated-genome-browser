package com.affymetrix.genometryImpl.event;

import com.affymetrix.genometryImpl.util.GeneralUtils;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 *
 * @author sgblanch
 * @version $Id: ReportBugAction.java 9589 2011-12-20 15:54:10Z lfrohman $
 */
public class ReportBugAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final ReportBugAction ACTION = new ReportBugAction();

	public static ReportBugAction getAction() {
		return ACTION;
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		GeneralUtils.browse("http://sourceforge.net/tracker/?group_id=129420&atid=714744");
	}

	@Override
	public String getText() {
		return "Report a bug";
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_R;
	}

	@Override
	public boolean isPopup() {
		return true;
	}

	@Override
	public String getIconPath() {
		return "toolbarButtonGraphics/general/Edit16.gif";
	}
}
