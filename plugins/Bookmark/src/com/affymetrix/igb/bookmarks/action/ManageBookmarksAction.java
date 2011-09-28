package com.affymetrix.igb.bookmarks.action;

import static com.affymetrix.igb.bookmarks.BookmarkManagerView.BUNDLE;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.osgi.service.IGBTabPanel.TabState;

public class ManageBookmarksAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	private static final ManageBookmarksAction ACTION = new ManageBookmarksAction();

	public static ManageBookmarksAction getAction() {
		return ACTION;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		BookmarkActionManager.getInstance().setTabState(TabState.COMPONENT_STATE_WINDOW);
	}

	@Override
	public String getText() {
		return BUNDLE.getString("manageBookmarks");
	}

	@Override
	public int getShortcut() {
		return KeyEvent.VK_M;
	}
}
