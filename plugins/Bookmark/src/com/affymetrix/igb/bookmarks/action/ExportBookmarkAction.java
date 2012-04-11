package com.affymetrix.igb.bookmarks.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.bookmarks.BookmarkManagerView;
import static com.affymetrix.igb.bookmarks.BookmarkManagerView.BUNDLE;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class ExportBookmarkAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	private static final ExportBookmarkAction ACTION = new ExportBookmarkAction();

	public static ExportBookmarkAction getAction() {
		return ACTION;
	}

	@Override
	public String getText() {
		return BUNDLE.getString("exportBookmarks");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
	    BookmarkManagerView.getSingleton().makeExportAction().actionPerformed(e);
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_E;
	}

	@Override
	public boolean isPopup() {
		return true;
	}

	@Override
	public String getIconPath() {
		return "toolbarButtonGraphics/general/Export16.gif";
	}
}
