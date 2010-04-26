package com.affymetrix.igb.action;

import com.affymetrix.igb.menuitem.MenuUtil;
import com.affymetrix.igb.prefs.PreferencesPanel;
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
public class PreferencesAction extends AbstractAction {
	private static final long serialVersionUID = 1l;

	public PreferencesAction() {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("preferences")),
				MenuUtil.getIcon("toolbarButtonGraphics/general/Preferences16.gif"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_E);
	}

	public void actionPerformed(ActionEvent e) {
		PreferencesPanel.getSingleton().getFrame().setVisible(true);
	}

}
