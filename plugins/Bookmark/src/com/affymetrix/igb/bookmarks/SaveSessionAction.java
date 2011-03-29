package com.affymetrix.igb.bookmarks;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.util.ErrorHandler;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.MessageFormat;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

public class SaveSessionAction extends AbstractAction {
	private static final long serialVersionUID = 1l;
	private IGBService igbService;

	public SaveSessionAction(IGBService igbService) {
		super(MessageFormat.format(
					BookmarkManagerView.BUNDLE.getString("menuItemHasDialog"),
					BookmarkManagerView.BUNDLE.getString("saveSession")),
				MenuUtil.getIcon("toolbarButtonGraphics/general/Export16.gif"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_S);
		this.igbService = igbService;
	}

	public void actionPerformed(ActionEvent e) {
		JFileChooser chooser = PreferenceUtils.getJFileChooser();
		int option = chooser.showSaveDialog(igbService.getFrame().getContentPane());
		if (option == JFileChooser.APPROVE_OPTION) {
			try {
				File f = chooser.getSelectedFile();
				igbService.saveState();
				PreferenceUtils.exportPreferences(PreferenceUtils.getTopNode(), f);
				PreferenceUtils.getSessionPrefsNode().removeNode();
			}
			catch (Exception x) {
				ErrorHandler.errorPanel("ERROR", "Error saving session to file", x);
			}
		}
	}
}
