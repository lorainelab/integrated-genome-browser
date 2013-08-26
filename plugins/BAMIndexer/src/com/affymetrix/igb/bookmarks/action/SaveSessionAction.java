package com.affymetrix.igb.bookmarks.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.bookmarks.Bookmark;
import com.affymetrix.igb.bookmarks.BookmarkController;
import com.affymetrix.igb.bookmarks.BookmarkManagerView;
import com.affymetrix.igb.osgi.service.IGBService;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;

public class SaveSessionAction extends GenericAction {

	private static final long serialVersionUID = 1l;
	private IGBService igbService;
	
	private static SaveSessionAction ACTION;
	
	public static void createAction(IGBService igbService){
		ACTION = new SaveSessionAction(igbService);
	}
	
	public static SaveSessionAction getAction(){
		return ACTION;
	}
	
	private SaveSessionAction(IGBService igbService) {
		super(BookmarkManagerView.BUNDLE.getString("saveSession"), null,
				"16x16/actions/save_session.png", "22x22/actions/save_session.png",
				KeyEvent.VK_S, null, true);
		this.igbService = igbService;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		JFileChooser chooser = PreferenceUtils.getJFileChooser();
		int option = chooser.showSaveDialog(igbService.getFrame().getContentPane());
		if (option == JFileChooser.APPROVE_OPTION) {
			try {
				saveSession(chooser.getSelectedFile());
			} catch (Exception x) {
				ErrorHandler.errorPanel("ERROR", "Error saving session to file", x);
			}
		}
	}
	
	public void saveSession(File f) throws Exception {
		igbService.saveState();
		Bookmark bookmark = BookmarkController.getCurrentBookmark(true,
				igbService.getSeqMapView().getVisibleSpan());
		if (bookmark != null) {
			String bk = URLEncoder.encode(bookmark.getURL().toString(), Bookmark.ENC);
			if (bk.length() < Preferences.MAX_VALUE_LENGTH) {
				PreferenceUtils.getSessionPrefsNode().put("bookmark", bk);
			} else {
				int j = 0;
				for (int i = 0; i < bk.length(); i += Preferences.MAX_VALUE_LENGTH) {
					String sb_bk = bk.substring(i, Math.min(bk.length(), i + Preferences.MAX_VALUE_LENGTH));
					PreferenceUtils.getSessionPrefsNode().put("bookmark" + j++, sb_bk);
				}
			}
		}
		PreferenceUtils.exportPreferences(PreferenceUtils.getTopNode(), f);
		PreferenceUtils.getSessionPrefsNode().removeNode();
	}
}
