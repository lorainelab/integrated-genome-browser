package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.GeneralUtils;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

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

	private DocumentationAction() {
		super(BUNDLE.getString("documentation"), null, "16x16/status/dialog-information.png", "22x22/status/dialog-information.png", KeyEvent.VK_D, null, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		GeneralUtils.browse("http://wiki.transvar.org/confluence/display/igbman");
	}
}
