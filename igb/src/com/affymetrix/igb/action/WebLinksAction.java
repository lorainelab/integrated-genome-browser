package com.affymetrix.igb.action;

import com.affymetrix.igb.prefs.WebLink;
import com.affymetrix.igb.shared.FileTracker;
import com.affymetrix.igb.view.WebLinkEditorPanel;
import com.affymetrix.igb.view.WebLinksView;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.UniFileChooser;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.DisplayUtils;

import static com.affymetrix.igb.IGBConstants.APP_NAME;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.MessageFormat;
import javax.swing.*;
import javax.swing.event.*;

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

    public void actionPerformed(ActionEvent evt) {
    	super.actionPerformed(evt);
    	WebLinksView.getInstance().showManager();
    }

	@Override
	public String getText() {
		return MessageFormat.format(
				BUNDLE.getString("menuItemHasDialog"),
				BUNDLE.getString("configureWebLinks"));
	}

	@Override
	public String getIconPath() {
		return "toolbarButtonGraphics/general/Search16.gif";
	}

	@Override
	public int getShortcut() {
		return KeyEvent.VK_W;
	}
}
