package com.affymetrix.igb.bookmarks.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import static com.affymetrix.igb.bookmarks.BookmarkManagerView.BUNDLE;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class AddPositionBookmarkAction extends AddBookmarkAction {

	private static final long serialVersionUID = 1L;
	private static final AddPositionBookmarkAction ACTION = new AddPositionBookmarkAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static AddPositionBookmarkAction getAction() {
		return ACTION;
	}

	private AddPositionBookmarkAction() {
		super(BUNDLE.getString("addBookmark"), null, "16x16/actions/bookmark-new.png",
				"22x22/actions/bookmark-new.png", KeyEvent.VK_P, null, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		bookmarkCurrentPosition();
	}
}
