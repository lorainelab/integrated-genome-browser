/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.bookmarks.action;

import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import static com.affymetrix.igb.bookmarks.BookmarkManagerView.BUNDLE;

/**
 *
 * @author lorainelab
 */
public class AddFolderAction extends AddBookmarkAction {

	private static final long serialVersionUID = 1L;
	private static final AddFolderAction ACTION = new AddFolderAction();

	public static AddFolderAction getAction() {
		return ACTION;
	}

	private AddFolderAction() {
		super(BUNDLE.getString("addBookmarkFolder"), null, null, KeyEvent.VK_G, null, false);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		addBookmarkFolder();
	}
}
