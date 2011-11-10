package com.affymetrix.igb.bookmarks.action;

import static com.affymetrix.igb.bookmarks.BookmarkManagerView.BUNDLE;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class AddPositionAndDataBookmarkAction extends AddBookmarkAction {
	private static final long serialVersionUID = 1L;
	private static final AddPositionAndDataBookmarkAction ACTION = new AddPositionAndDataBookmarkAction();

	public static AddPositionAndDataBookmarkAction getAction() {
		return ACTION;
	}

	@Override
	public String getText() {
		return BUNDLE.getString("addPosition&DataBookmark");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	     bookmarkCurrentPosition(true);
	}

	@Override
	public String getIconPath() {
		//return "images/addPositionDataBookmark16.png";
		return null;
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_G;
	}
}
