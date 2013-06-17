package com.affymetrix.igb.bookmarks.action;

import static com.affymetrix.igb.bookmarks.BookmarkManagerView.BUNDLE;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.bookmarks.Bookmark;

public class CopyBookmarkToClipboardAction extends BookmarkAction {

	private static final long serialVersionUID = 1L;
	private static final CopyBookmarkToClipboardAction ACTION = new CopyBookmarkToClipboardAction();

	
	public static CopyBookmarkToClipboardAction getAction() {
		return ACTION;
	}

	private CopyBookmarkToClipboardAction() {
		super(BUNDLE.getString("copyBookmarkToClipboard"), null, "16x16/actions/edit-paste.png",
				"22x22/actions/edit-paste.png", KeyEvent.VK_C, null, false);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		Bookmark bookmark = getCurrentPosition(true);
		if (bookmark != null) {
			StringSelection data = new StringSelection(bookmark.getURL().toString());
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(data, data);
		}
	}
}
