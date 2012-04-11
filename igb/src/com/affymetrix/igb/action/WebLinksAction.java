package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;

import com.affymetrix.igb.view.WebLinksViewGUI;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

import java.awt.event.*;
import javax.swing.*;

/**
 *  A panel for viewing and editing weblinks.
 */
public final class WebLinksAction extends GenericAction {
  private static final long serialVersionUID = 1L;
	private static final WebLinksAction ACTION = new WebLinksAction();

	public static WebLinksAction getAction() {
		return ACTION;
	}

	private WebLinksAction() {
		super();
	    putValue(Action.SHORT_DESCRIPTION, "Manage Web Links");
	}

	@Override
    public void actionPerformed(ActionEvent evt) {
    	super.actionPerformed(evt);
		WebLinksViewGUI.getSingleton().displayPanel();
    }

	@Override
	public String getText() {
		return BUNDLE.getString("configureWebLinks");
	}

	@Override
	public String getIconPath() {
		return "toolbarButtonGraphics/general/Search16.gif";
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_W;
	}

	@Override
	public boolean isPopup() {
		return true;
	}
}
