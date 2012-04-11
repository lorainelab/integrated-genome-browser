package com.affymetrix.igb.bookmarks.action;

import static com.affymetrix.igb.bookmarks.BookmarkManagerView.BUNDLE;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class AddPositionBookmarkAction extends AddBookmarkAction {
	private static final long serialVersionUID = 1L;
	private static final AddPositionBookmarkAction ACTION = new AddPositionBookmarkAction();

	public static AddPositionBookmarkAction getAction() {
		return ACTION;
	}

	@Override
	public String getText() {
		return BUNDLE.getString("addBookmark");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
	     bookmarkCurrentPosition();
	}

	@Override
	public String getIconPath() {
		return "toolbarButtonGraphics/general/Bookmarks16.gif";
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_P;
	}

	@Override
	public boolean isPopup() {
		return true;
	}
}
