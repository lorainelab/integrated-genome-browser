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

	private ShowConsoleAction() {
		super(BUNDLE.getString("showConsole"), null, "toolbarButtonGraphics/general/History16.gif", KeyEvent.VK_C, null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		ConsoleView.showConsole(IGBConstants.APP_NAME);
	}

	@Override
	public boolean isPopup() {
		return true;
	}
}
