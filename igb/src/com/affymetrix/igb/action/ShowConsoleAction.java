package com.affymetrix.igb.action;

import com.affymetrix.igb.IGBConstants;
import com.affymetrix.genometryImpl.util.ConsoleView;
import com.affymetrix.genoviz.swing.MenuUtil;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import javax.swing.AbstractAction;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class ShowConsoleAction extends AbstractAction {
	private static final long serialVersionUID = 1l;

	public ShowConsoleAction() {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("showConsole")),
				MenuUtil.getIcon("toolbarButtonGraphics/development/Host16.gif"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_C);
	}

	public void actionPerformed(ActionEvent e) {
		ConsoleView.showConsole(IGBConstants.APP_NAME);
	}
}
