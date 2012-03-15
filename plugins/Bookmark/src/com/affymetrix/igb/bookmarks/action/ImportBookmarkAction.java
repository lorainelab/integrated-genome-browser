package com.affymetrix.igb.bookmarks.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.bookmarks.BookmarkManagerView;
import static com.affymetrix.igb.bookmarks.BookmarkManagerView.BUNDLE;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class ImportBookmarkAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	private static final ImportBookmarkAction ACTION = new ImportBookmarkAction();

	public static ImportBookmarkAction getAction() {
		return ACTION;
	}

	@Override
	public String getText() {
		return BUNDLE.getString("importBookmarks");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
	    BookmarkManagerView.getSingleton().makeImportAction().actionPerformed(e);
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_I;
	}

	@Override
	public boolean isPopup() {
		return true;
	}
}
