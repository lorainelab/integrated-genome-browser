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

	public ExportBookmarkAction() {
		super(BUNDLE.getString("exportBookmarks"), null, "toolbarButtonGraphics/general/Export16.gif", KeyEvent.VK_E, null, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
	    BookmarkManagerView.getSingleton().makeExportAction().actionPerformed(e);
	}
}
