package com.affymetrix.igb.bookmarks.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.bookmarks.BookmarkManagerView;
import static com.affymetrix.igb.bookmarks.BookmarkManagerView.BUNDLE;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class ImportBookmarkAction extends GenericAction {

	private static final long serialVersionUID = 1L;
	private static final ImportBookmarkAction ACTION = new ImportBookmarkAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ImportBookmarkAction getAction() {
		return ACTION;
	}

	public ImportBookmarkAction() {
		super(BUNDLE.getString("importBookmarks"), null, "16x16/actions/go-bottom.png",
				"22x22/actions/go-bottom.png", KeyEvent.VK_I, null, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		BookmarkManagerView.getSingleton().importBookmarks();
	}
}
