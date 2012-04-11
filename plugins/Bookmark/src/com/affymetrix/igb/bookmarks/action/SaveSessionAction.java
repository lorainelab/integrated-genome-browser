package com.affymetrix.igb.bookmarks.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.bookmarks.Bookmark;
import com.affymetrix.igb.bookmarks.BookmarkController;
import com.affymetrix.igb.bookmarks.BookmarkManagerView;
import com.affymetrix.igb.osgi.service.IGBService;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URLEncoder;

import javax.swing.JFileChooser;

public class SaveSessionAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private IGBService igbService;

	public SaveSessionAction(IGBService igbService) {
		super();
		this.igbService = igbService;
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		JFileChooser chooser = PreferenceUtils.getJFileChooser();
		int option = chooser.showSaveDialog(igbService.getFrame().getContentPane());
		if (option == JFileChooser.APPROVE_OPTION) {
			try {
				File f = chooser.getSelectedFile();
				igbService.saveState();
				Bookmark bookmark = BookmarkController.getCurrentBookmark(true, igbService.getSeqMapView().getVisibleSpan());
				if (bookmark != null) {
					PreferenceUtils.getSessionPrefsNode().put("bookmark", URLEncoder.encode(bookmark.getURL().toString(), Bookmark.ENC));
				}
				PreferenceUtils.exportPreferences(PreferenceUtils.getTopNode(), f);
				PreferenceUtils.getSessionPrefsNode().removeNode();
			}
			catch (Exception x) {
				ErrorHandler.errorPanel("ERROR", "Error saving session to file", x);
			}
		}
	}

	@Override
	public String getText() {
		return BookmarkManagerView.BUNDLE.getString("saveSession");
	}

	@Override
	public String getIconPath() {
		return "toolbarButtonGraphics/general/Save16.gif";
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_S;
	}

	@Override
	public boolean isPopup() {
		return true;
	}
}
