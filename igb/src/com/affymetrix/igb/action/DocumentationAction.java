package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.GeneralUtils;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class DocumentationAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final DocumentationAction ACTION = new DocumentationAction();

	public static DocumentationAction getAction() {
		return ACTION;
	}

	public void actionPerformed(ActionEvent e) {
		GeneralUtils.browse("http://wiki.transvar.org/confluence/display/igbman");
	}

	@Override
	public String getText() {
		return MessageFormat.format(
				BUNDLE.getString("menuItemHasDialog"),
				BUNDLE.getString("documentation"));
	}

	@Override
	public String getIconPath() {
		return "toolbarButtonGraphics/general/Help16.gif";
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_D;
	}
}
