package com.affymetrix.igb.action;

import com.affymetrix.igb.IGBConstants;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.ConsoleView;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class ShowConsoleAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final ShowConsoleAction ACTION = new ShowConsoleAction();

	public static ShowConsoleAction getAction() {
		return ACTION;
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		ConsoleView.showConsole(IGBConstants.APP_NAME);
	}

	@Override
	public String getText() {
		return BUNDLE.getString("showConsole");
	}

	@Override
	public String getIconPath() {
		return "toolbarButtonGraphics/general/History16.gif";
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_C;
	}

	@Override
	public boolean isPopup() {
		return true;
	}
}
